package com.gtech.client

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of the KCache interface, providing a thread-safe, high-performance in-memory cache
 * with support for expiration, LRU eviction, and periodic cleanup. Designed for efficient caching
 * and non-blocking asynchronous operations.
 *
 * @param ttlMillis Time-to-live (TTL) for cache entries in milliseconds. Entries older than this value are considered expired.
 * @param maxSize Maximum number of items the cache can hold. Evicts the least recently used (LRU) item if the size is exceeded.
 * @param cleanupIntervalMillis Interval in milliseconds for periodic cleanup of expired cache items.
 * @param enableAutoCleanup Enables or disables automatic cleanup of expired items.
 * @param logger Optional callback for logging cache operations.
 * @param evictionCallback Optional callback invoked upon eviction of cache entries.
 */
class InMemoryCache<K, V>(
    private val ttlMillis: Long = 60000L,                   // Default TTL: 1 minute
    private val maxSize: Int = 100,                         // Default max size: 100 entries
    private val cleanupIntervalMillis: Long = 10000L,       // Default cleanup interval: 10 seconds
    private val enableAutoCleanup: Boolean = true,          // Enable auto cleanup by default
    private val logger: ((String) -> Unit)? = null,         // Optional logger callback
    private val evictionCallback: ((K, V) -> Unit)? = null  // Optional eviction callback
) : KCache<K, V> {

    private val cache = ConcurrentHashMap<K, CacheItem<V>>() // Internal storage for cached entries
    private val cleanupJob: Job? = if (enableAutoCleanup) {
        CoroutineScope(Dispatchers.Default).launch { periodicCleanup() }
    } else null

    /**
     * Represents a cache entry.
     * @property value The cached value.
     * @property timestamp The time the entry was added to the cache.
     * @property lastAccessed The time the entry was last accessed.
     */
    private data class CacheItem<V>(
        val value: V,               // The value stored in the cache
        val timestamp: Long,        // Timestamp when the item was added
        var lastAccessed: Long      // Timestamp of the last access to this item
    )

    init {
        logger?.invoke("KCache initialized with TTL: $ttlMillis ms, Max Size: $maxSize")
    }

    /**
     * Synchronously adds a key-value pair to the cache.
     * Evicts the least recently used (LRU) entry if the cache exceeds the maximum size.
     *
     * @param key The key to be added.
     * @param value The value associated with the key.
     */
    override fun put(key: K, value: V) {
        if (cache.size >= maxSize) evictOldest()
        cache[key] = CacheItem(value, System.currentTimeMillis(), System.currentTimeMillis())
        logger?.invoke("Item added to cache with key: $key")
    }

    /**
     * Asynchronously adds a key-value pair to the cache.
     * This is a non-blocking equivalent of the `put` method.
     *
     * @param key The key to be added.
     * @param value The value associated with the key.
     */
    override suspend fun putAsync(key: K, value: V) = withContext(Dispatchers.Default) { put(key, value) }

    /**
     * Synchronously retrieves the value associated with a key.
     * Removes the entry if it has expired.
     *
     * @param key The key to retrieve the value for.
     * @return The value if found and not expired, or `null` otherwise.
     */
    override fun get(key: K): V? {
        val item = cache[key]
        return if (item != null && !isExpired(item)) {
            item.lastAccessed = System.currentTimeMillis() // Update last accessed timestamp
            item.value
        } else {
            cache.remove(key) // Remove expired or missing items
            null
        }
    }

    /**
     * Asynchronously retrieves the value associated with a key.
     * This is a non-blocking equivalent of the `get` method.
     *
     * @param key The key to retrieve the value for.
     * @return The value if found and not expired, or `null` otherwise.
     */
    override suspend fun getAsync(key: K): V? = withContext(Dispatchers.Default) { get(key) }

    /**
     * Removes an entry from the cache.
     *
     * @param key The key to remove from the cache.
     */
    override fun remove(key: K) {
        cache.remove(key)
        logger?.invoke("Item removed from cache with key: $key")
    }

    /**
     * Clears all entries from the cache.
     */
    override fun clear() {
        cache.clear()
        logger?.invoke("Cache cleared.")
    }

    /**
     * Provides a mechanism for retrieving or generating a value for a key.
     * If the key exists in the cache, the value is returned. Otherwise, the callback is executed to generate the value,
     * which is then cached and returned.
     *
     * @param key The key to retrieve or generate a value for.
     * @param callback The callback to generate the value if the key is not found in the cache.
     * @return The cached or generated value.
     */
    override fun withCache(key: K, callback: () -> V?): V? {
        return get(key) ?: callback()?.also { put(key, it) }
    }

    /**
     * Asynchronous variant of the `withCache` function.
     * Uses a suspendable callback to generate the value if the key is not found.
     *
     * @param key The key to retrieve or generate a value for.
     * @param callback The suspendable callback to generate the value if the key is not found in the cache.
     * @return The cached or generated value.
     */
    override suspend fun withCacheAsync(key: K, callback: suspend () -> V?): V? {
        return getAsync(key) ?: callback()?.also { putAsync(key, it) }
    }

    /**
     * Stops the periodic cleanup task if enabled.
     */
    override fun stopCleanup() {
        cleanupJob?.cancel()
        logger?.invoke("Periodic cleanup stopped.")
    }

    /**
     * Logs the current state of the cache for debugging purposes.
     * Includes size and details of each entry.
     */
    override fun debugCacheUsage() {
        logger?.invoke("Cache Usage - Size: ${cache.size}, Max Size: $maxSize")
        cache.forEach { (key, value) ->
            logger?.invoke("Key: $key, Last Accessed: ${value.lastAccessed}")
        }
    }

    /**
     * Checks if a cache entry has expired based on its timestamp and the TTL.
     *
     * @param item The cache entry to check.
     * @return `true` if the entry has expired, otherwise `false`.
     */
    private fun isExpired(item: CacheItem<V>): Boolean {
        return System.currentTimeMillis() - item.timestamp > ttlMillis
    }

    /**
     * Evicts the least recently used (LRU) entry from the cache to make space for a new one.
     */
    private fun evictOldest() {
        val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
        if (oldestKey != null) {
            val evictedItem = cache.remove(oldestKey)
            evictedItem?.let {
                evictionCallback?.invoke(oldestKey, it.value)
                logger?.invoke("Evicted oldest item with key: $oldestKey")
            }
        }
    }

    /**
     * Periodically cleans up expired entries from the cache.
     * Runs on a coroutine with the specified cleanup interval.
     */
    private suspend fun periodicCleanup() {
        withContext(Dispatchers.Default) {
            while (isActive) {
                delay(cleanupIntervalMillis)
                cleanExpiredItems()
            }
        }
    }

    /**
     * Removes all expired items from the cache.
     * Invokes the eviction callback for each removed item.
     */
    private fun cleanExpiredItems() {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (isExpired(entry.value)) {
                iterator.remove()
                evictionCallback?.invoke(entry.key, entry.value.value)
                logger?.invoke("Expired item removed with key: ${entry.key}")
            }
        }
    }

}
