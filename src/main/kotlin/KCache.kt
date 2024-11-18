package com.gtech.client

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe, high-performance in-memory cache that supports expiration, eviction, and asynchronous access.
 * Implements the Least Recently Used (LRU) eviction policy and periodic cleanup for expired cache items.
 *
 * @param ttlMillis Time-to-live (TTL) for cache entries. Entries older than this value will be considered expired.
 * @param maxSize Maximum number of items the cache can hold before evicting the least recently used (LRU) item.
 * @param cleanupIntervalMillis Interval for periodic cleanup of expired items (in milliseconds).
 * @param enableAutoCleanup Flag to enable or disable the automatic periodic cleanup.
 * @param logger Optional callback for debugging cache status.
 * @param evictionCallback Optional callback for eviction notifications.
 */
class KCache<K, V>(
    private val ttlMillis: Long = 60000L,                   // Default TTL: 60 seconds
    private val maxSize: Int = 100,                         // Default max size of the cache
    private val cleanupIntervalMillis: Long = 10000L,       // Default cleanup interval: 10 seconds
    private val enableAutoCleanup: Boolean = true,          // Flag to enable/disable auto cleanup
    private val logger: ((String) -> Unit)? = null,         // Optional debug callback
    private val evictionCallback: ((K, V) -> Unit)? = null  // Optional eviction callback
) {

    private val cache = ConcurrentHashMap<K, CacheItem<V>>()
    private val cleanupJob: Job? = if (enableAutoCleanup) {
        CoroutineScope(Dispatchers.Default).launch {
            periodicCleanup()
        }
    } else null

    // Data class to store the cache item with value, timestamp, and last accessed time
    private data class CacheItem<V>(
        val value: V,               // Value stored in the cache
        val timestamp: Long,        // Time when the item was added to the cache
        var lastAccessed: Long      // Time of the last access to the cache item
    )

    init {
        logger?.invoke("Cache initialized with TTL: $ttlMillis, Max Size: $maxSize")
    }

    /**
     * Puts an item into the cache asynchronously. This operation does not block the caller.
     *
     * @param key The key to associate with the cache item.
     * @param value The value to be stored in the cache.
     */
    suspend fun putAsync(key: K, value: V) {
        withContext(Dispatchers.Default) {
            put(key, value)
        }
    }

    /**
     * Puts an item into the cache synchronously. This operation may block the caller if necessary.
     *
     * @param key The key to associate with the cache item.
     * @param value The value to be stored in the cache.
     */
    fun put(key: K, value: V) {
        // If the cache size exceeds maxSize, evict the oldest item
        if (cache.size >= maxSize) {
            evictOldest()
        }
        // Add the item to the cache with a timestamp and the current last accessed time
        cache[key] = CacheItem(value, System.currentTimeMillis(), System.currentTimeMillis())
        logger?.invoke("Item added to cache: $key")
    }

    /**
     * Gets an item from the cache asynchronously. If the item is expired or doesn't exist, returns null.
     *
     * @param key The key of the item to retrieve.
     * @return The cached value or null if not found or expired.
     */
    suspend fun getAsync(key: K): V? {
        return withContext(Dispatchers.Default) {
            get(key)
        }
    }

    /**
     * Gets an item from the cache. If the item is expired or doesn't exist, removes it and returns null.
     *
     * @param key The key of the item to retrieve.
     * @return The cached value or null if not found or expired.
     */
    fun get(key: K): V? {
        val cacheItem = cache[key]
        return if (cacheItem != null && !isExpired(cacheItem)) {
            // Update the last accessed time whenever the item is accessed
            cacheItem.lastAccessed = System.currentTimeMillis()
            cacheItem.value
        } else {
            // Remove expired or nonexistent item
            cache.remove(key)
            null
        }
    }

    /**
     * Removes an item from the cache.
     *
     * @param key The key of the item to remove.
     */
    fun remove(key: K) {
        cache.remove(key)
        logger?.invoke("Item removed from cache: $key")
    }

    /**
     * Clears all items from the cache.
     */
    fun clear() {
        cache.clear()
        logger?.invoke("Cache cleared.")
    }

    /**
     * Checks if a cache item is expired based on its TTL.
     *
     * @param item The cache item to check for expiration.
     * @return True if the item is expired, false otherwise.
     */
    private fun isExpired(item: CacheItem<V>): Boolean {
        return System.currentTimeMillis() - item.timestamp > ttlMillis
    }

    /**
     * Evicts the least recently used (LRU) item from the cache if the cache size exceeds the maximum size.
     * Triggers the eviction callback after an item is removed.
     */
    private fun evictOldest() {
        val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
        if (oldestKey != null) {
            val evictedItem = cache.remove(oldestKey)
            evictedItem?.let {
                evictionCallback?.invoke(oldestKey, it.value)
                logger?.invoke("Evicted item: $oldestKey")
            }
        }
    }

    /**
     * Periodically cleans up expired cache items in the background.
     * This runs in a coroutine and checks for expired items at the specified interval.
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
     * Removes expired cache items.
     */
    private fun cleanExpiredItems() {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (isExpired(entry.value)) {
                iterator.remove()
                evictionCallback?.invoke(entry.key, entry.value.value)
                logger?.invoke("Expired item removed: ${entry.key}")
            }
        }
    }

    /**
     * Stops the periodic cleanup job.
     */
    fun stopCleanup() {
        cleanupJob?.cancel()
    }

    /**
     * Debug method to analyze memory usage and find elements in the cache.
     */
    fun debugCacheUsage() {
        logger?.invoke("Cache Size: ${cache.size}, Max Size: $maxSize")
        cache.forEach { (key, value) ->
            logger?.invoke("Key: $key, Last Accessed: ${value.lastAccessed}")
        }
    }

    /**
     * Builder for KCache to enable flexible configuration.
     */
    class CacheBuilder<K, V> {

        private var ttlMillis: Long = 60000L
        private var maxSize: Int = 100
        private var cleanupIntervalMillis: Long = 10000L
        private var enableAutoCleanup: Boolean = true
        private var debugCallback: ((String) -> Unit)? = null
        private var evictionCallback: ((K, V) -> Unit)? = null

        fun ttlMillis(ttlMillis: Long) = apply { this.ttlMillis = ttlMillis }
        fun maxSize(maxSize: Int) = apply { this.maxSize = maxSize }
        fun cleanupIntervalMillis(cleanupIntervalMillis: Long) = apply { this.cleanupIntervalMillis = cleanupIntervalMillis }
        fun enableAutoCleanup(enableAutoCleanup: Boolean) = apply { this.enableAutoCleanup = enableAutoCleanup }
        fun debugCallback(debugCallback: (String) -> Unit) = apply { this.debugCallback = debugCallback }
        fun evictionCallback(evictionCallback: (K, V) -> Unit) = apply { this.evictionCallback = evictionCallback }

        fun build(): KCache<K, V> {
            return KCache(
                ttlMillis,
                maxSize,
                cleanupIntervalMillis,
                enableAutoCleanup,
                debugCallback,
                evictionCallback
            )
        }
    }

}
