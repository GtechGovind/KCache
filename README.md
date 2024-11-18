Here’s a `README.md` file that explains every aspect of the `KCache` class, its usage, configuration options, and functionality:

---

# KCache: In-Memory Cache with Expiration, Eviction, and Asynchronous Access

`KCache` is a thread-safe, high-performance in-memory cache that supports expiration, eviction, and asynchronous access. It uses a Least Recently Used (LRU) eviction policy and can periodically clean up expired items to keep the cache optimized. The cache supports both synchronous and asynchronous operations, making it suitable for real-time applications where high performance and memory management are critical.

## Features

- **TTL (Time-To-Live):** Items in the cache can expire based on a configurable TTL.
- **Eviction Policy:** Uses LRU (Least Recently Used) to evict items when the cache exceeds the maximum size.
- **Automatic Cleanup:** Supports periodic cleanup of expired items, configurable via the builder.
- **Debugging:** Optional debug logging to track cache status, memory usage, and item access.
- **Asynchronous Access:** Cache operations can be performed asynchronously using Kotlin coroutines for non-blocking calls.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Cache Operations](#cache-operations)
- [Debugging](#debugging)
- [Custom Callbacks](#custom-callbacks)
- [Cleanup and Eviction](#cleanup-and-eviction)
- [CacheBuilder](#cachebuilder)

## Installation

Add the cache library to your project by including the following dependency in your `build.gradle` (if using Gradle):

```gradle
implementation "com.gtech.client:KCache:1.0"
```

## Usage

Here’s how to initialize and use the `KCache` class:

### Initialize the Cache

You can configure the cache using the builder pattern.

```kotlin
val cache = KCache.CacheBuilder<String, String>()
    .ttlMillis(60000L)                 // Time-to-live: 60 seconds
    .maxSize(100)                      // Max size: 100 items
    .cleanupIntervalMillis(10000L)     // Cleanup interval: 10 seconds
    .enableAutoCleanup(true)           // Enable auto cleanup
    .debugCallback { println(it) }     // Enable debug logging
    .evictionCallback { key, value ->  // Callback when item is evicted
        println("Evicted: $key -> $value")
    }
    .build()
```

### Cache Operations

Once the cache is built, you can perform various operations like adding, retrieving, and removing items.

#### Putting Items into the Cache

You can store items in the cache using the `put()` or `putAsync()` methods:

```kotlin
cache.put("key1", "value1")
cache.putAsync("key2", "value2")
```

#### Getting Items from the Cache

To retrieve items from the cache, use the `get()` or `getAsync()` methods:

```kotlin
val value = cache.get("key1")
val asyncValue = cache.getAsync("key2")
```

#### Removing Items from the Cache

To remove an item from the cache, use the `remove()` method:

```kotlin
cache.remove("key1")
```

#### Clearing the Cache

To clear all items from the cache:

```kotlin
cache.clear()
```

## Configuration

You can configure various aspects of the cache during initialization via the `CacheBuilder`:

- **ttlMillis (Long):** The time-to-live for cache entries in milliseconds. Items older than this will be considered expired. Default is `60000L` (60 seconds).
- **maxSize (Int):** The maximum number of items the cache can hold. When this limit is exceeded, the least recently used (LRU) item will be evicted. Default is `100`.
- **cleanupIntervalMillis (Long):** The interval in milliseconds between cleanup operations to remove expired items. Default is `10000L` (10 seconds).
- **enableAutoCleanup (Boolean):** Flag to enable or disable automatic periodic cleanup of expired items. Default is `true`.
- **logger (Optional Callback):** A debug callback function that will be called to log cache status, like when items are added, removed, or evicted. It accepts a `String` parameter.
- **evictionCallback (Optional Callback):** A callback function that will be triggered when an item is evicted due to size limits. It accepts the key and value of the evicted item.

## Cache Operations

### Put

You can add items to the cache either synchronously (`put()`) or asynchronously (`putAsync()`):

```kotlin
cache.put("key1", "value1")
cache.putAsync("key2", "value2")
```

### Get

You can retrieve cached items either synchronously (`get()`) or asynchronously (`getAsync()`):

```kotlin
val value = cache.get("key1")
val asyncValue = cache.getAsync("key2")
```

### Remove

To remove an item from the cache:

```kotlin
cache.remove("key1")
```

### Clear

To clear all items from the cache:

```kotlin
cache.clear()
```

## Debugging

To track cache usage, you can enable the `debugCallback` to log cache-related events:

```kotlin
cache.debugCacheUsage()
```

This will output information about the cache size, the last accessed time of each item, and other internal states.

## Custom Callbacks

### Eviction Callback

You can configure an `evictionCallback` to handle when an item is evicted due to reaching the maximum cache size:

```kotlin
cache.evictionCallback { key, value ->
    println("Evicted item: $key -> $value")
}
```

### Debug Callback

Use the `debugCallback` to log cache activities like adding/removing items and eviction events:

```kotlin
cache.debugCallback { message ->
    println(message)
}
```

## Cleanup and Eviction

### Automatic Cleanup

You can enable periodic cleanup of expired cache items by setting the `enableAutoCleanup` flag to `true`. This will run a background coroutine that cleans up expired items at the specified `cleanupIntervalMillis`.

To stop the automatic cleanup:

```kotlin
cache.stopCleanup()
```

### LRU Eviction

When the cache exceeds its maximum size (`maxSize`), it will automatically evict the least recently used item to make room for new items. The eviction callback will be triggered in this case.

## CacheBuilder

The `CacheBuilder` class provides a fluent API for configuring the cache:

```kotlin
val cache = KCache.CacheBuilder<String, String>()
    .ttlMillis(60000L)
    .maxSize(100)
    .cleanupIntervalMillis(10000L)
    .enableAutoCleanup(true)
    .debugCallback { println(it) }
    .evictionCallback { key, value -> println("Evicted: $key -> $value") }
    .build()
```

The `CacheBuilder` allows you to customize all aspects of the cache configuration. Use it to create a cache instance with specific settings.

---

## Conclusion

`KCache` is a flexible, high-performance caching solution designed for concurrent environments. It is ideal for use cases requiring fast, memory-efficient, and time-sensitive caching operations. By using the builder pattern, you can easily customize the cache behavior to suit your needs, whether you're handling large volumes of data, implementing a time-sensitive cache, or managing resource-constrained environments.