
# KCache: Lightweight In-Memory Cache with TTL, Eviction, and Async Access

`KCache` is a lightweight, high-performance, thread-safe, in-memory caching solution tailored for small-scale caching needs. It is designed for applications requiring low-latency access to frequently used data, with built-in **time-to-live (TTL)**, **least recently used (LRU)** eviction, and **asynchronous operations** using Kotlin coroutines.

Ideal for use cases such as caching configurations, API responses, or temporary session data, `KCache` emphasizes simplicity, efficiency, and developer-friendliness while keeping memory usage manageable for small caches.

---

## **Why KCache?**

### **Limitations of Other Caching Libraries**

While popular caching libraries like **Caffeine**, **Guava**, and **Ehcache** are robust, they often come with significant memory overhead and are optimized for large-scale applications. These libraries:
- Prioritize scalability for thousands or millions of keys.
- Often require complex configurations.
- May introduce dependencies unsuitable for lightweight projects.

### **Motivation to Build KCache**

The goal of `KCache` was to create a **focused solution** for smaller applications or components needing:
1. A lightweight, **low-overhead** caching mechanism.
2. **TTL-based expiration** for time-sensitive data.
3. Basic **LRU eviction** for memory control.
4. Asynchronous operations with minimal blocking.
5. Simplicity and ease of integration.

`KCache` delivers these features in a minimalistic package, designed to **"do one thing and do it well."**

---

## **Installation**

### **Using JitPack**

`KCache` is hosted on [JitPack](https://jitpack.io). Add the JitPack repository to your project.

#### **For Gradle (Groovy)**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.username:KCache:1.0.0'
}
```

#### **For Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.username:KCache:1.0.0")
}
```

---

## **Core Features**

### **Lightweight Design**
- Small memory footprint, ideal for applications requiring a compact cache.
- Supports caches up to a few thousand keys efficiently.

### **TTL and Expiration**
- Automatically expires entries after the specified `ttlMillis`.

### **LRU Eviction**
- Evicts the least recently used entries when the cache exceeds its maximum size.

### **Asynchronous Support**
- Provides `suspend` functions for non-blocking cache interactions.

### **Periodic Cleanup**
- Removes expired entries periodically to optimize memory usage.

### **Debug and Eviction Callbacks**
- Hooks to monitor cache operations and handle evictions.

---

## **API Overview**

### **Cache Operations**

| Function                     | Description                                                |
|------------------------------|------------------------------------------------------------|
| `put(key, value)`            | Adds or updates an entry synchronously.                    |
| `putAsync(key, value)`       | Adds or updates an entry asynchronously.                   |
| `get(key)`                   | Retrieves an entry synchronously.                          |
| `getAsync(key)`              | Retrieves an entry asynchronously.                         |
| `remove(key)`                | Removes a specific entry.                                  |
| `clear()`                    | Clears all entries in the cache.                           |
| `withCache(key, block)`      | Retrieves or computes and caches the value synchronously.  |
| `withCacheAsync(key, block)` | Retrieves or computes and caches the value asynchronously. |
| `stopCleanup()`              | Stops automatic cleanup of expired entries.                |

---

## **Detailed API Usage**

### **Initialization**

Create a cache instance using the `CacheBuilder`:

```kotlin
val cache = KCache.CacheBuilder<String, String>()
    .ttlMillis(60000L)                 // 1-minute TTL
    .maxSize(100)                      // Max size of 100 entries
    .cleanupIntervalMillis(10000L)     // Cleanup expired items every 10 seconds
    .enableAutoCleanup(true)           // Enable automatic cleanup
    .debugCallback { println(it) }     // Debugging logs
    .evictionCallback { key, value -> println("Evicted: $key -> $value") }
    .build()
```

### **Adding and Retrieving Items**

#### Synchronous Operations
```kotlin
cache.put("key1", "value1")
val value = cache.get("key1")
```

#### Asynchronous Operations
```kotlin
cache.putAsync("key2", "value2")
val valueAsync = cache.getAsync("key2")
```

### **WithCache and WithCacheAsync**

#### **`withCache`**
Ensures the value is cached or computes it on demand:
```kotlin
val value = cache.withCache("key1") {
    // Compute value if not in cache
    "computedValue"
}
```

#### **`withCacheAsync`**
Async variant of `withCache`:
```kotlin
val valueAsync = cache.withCacheAsync("key2") {
    // Compute value asynchronously if not in cache
    delay(100) // Simulate computation
    "asyncComputedValue"
}
```

---

## **Advanced Configuration**

### **Eviction Callback**
```kotlin
cache.evictionCallback { key, value ->
    println("Evicted: $key -> $value")
}
```

### **Debugging**
```kotlin
cache.debugCallback { message ->
    println("Cache Event: $message")
}
```

### **Automatic Cleanup**
Enable or disable automatic cleanup of expired items:
```kotlin
cache.stopCleanup() // Disable cleanup
```

---

## **Periodic Cleanup**

`KCache` runs a background job (if enabled) to clean expired entries periodically. This helps reduce memory usage without needing manual intervention.

```kotlin
val cache = KCache.CacheBuilder<String, String>()
    .enableAutoCleanup(true)
    .cleanupIntervalMillis(5000L) // Cleanup every 5 seconds
    .build()
```

Stop cleanup:
```kotlin
cache.stopCleanup()
```

---

## **Why Use KCache?**

1. **Simplicity:** Minimal learning curve with easy-to-understand APIs.
2. **Small Memory Footprint:** Perfect for applications requiring small caches.
3. **Customizable:** Configure TTL, eviction policies, and hooks.
4. **Lightweight:** Ideal for microservices or mobile applications where resources are limited.
5. **Thread-Safe:** Built with thread safety for concurrent environments.

---

## **Conclusion**

`KCache` is a powerful yet lightweight caching library for small-scale use cases. With its TTL-based expiration, LRU eviction, and support for asynchronous operations, it offers developers a focused and efficient caching tool. Whether you're building a microservice, caching configuration settings, or optimizing local data access, `KCache` is a great fit for your needs.
