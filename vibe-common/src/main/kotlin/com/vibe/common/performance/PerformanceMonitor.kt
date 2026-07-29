package com.vibe.common.performance

import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility for tracking app performance metrics.
 * Tracks method execution times, memory usage, and custom metrics.
 */
object PerformanceMonitor {

    const val TAG = "PerformanceMonitor"
    
    // Metric storage
    private val executionTimes = ConcurrentHashMap<String, AtomicLong>()
    private val executionCounts = ConcurrentHashMap<String, AtomicLong>()
    private val memoryUsage = ConcurrentHashMap<String, AtomicLong>()
    
    // Performance thresholds (in milliseconds)
    const val WARNING_THRESHOLD_MS = 1000L
    const val CRITICAL_THRESHOLD_MS = 3000L

    /**
     * Track execution time of a code block.
     * @param metricName The name of the metric
     * @param block The code block to measure
     * @return The result of the code block
     */
    inline fun <T> trackExecution(metricName: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        recordExecutionTime(metricName, duration)
        
        if (duration > CRITICAL_THRESHOLD_MS) {
            VibeLogger.e(TAG, "CRITICAL PERFORMANCE: $metricName took ${duration}ms")
        } else if (duration > WARNING_THRESHOLD_MS) {
            VibeLogger.w(TAG, "WARNING PERFORMANCE: $metricName took ${duration}ms")
        } else {
            VibeLogger.d(TAG, "$metricName took ${duration}ms")
        }
        
        return result
    }

    /**
     * Record execution time for a metric.
     * @param metricName The name of the metric
     * @param durationMs The duration in milliseconds
     */
    fun recordExecutionTime(metricName: String, durationMs: Long) {
        executionTimes.getOrPut(metricName) { AtomicLong(0) }.addAndGet(durationMs)
        executionCounts.getOrPut(metricName) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * Record memory usage for a specific metric.
     * @param metricName The name of the metric
     * @param bytes The memory usage in bytes
     */
    fun recordMemoryUsage(metricName: String, bytes: Long) {
        memoryUsage.getOrPut(metricName) { AtomicLong(0) }.set(bytes)
    }

    /**
     * Get average execution time for a metric.
     * @param metricName The name of the metric
     * @return Average execution time in milliseconds, or 0 if metric not found
     */
    fun getAverageExecutionTime(metricName: String): Long {
        val totalTime = executionTimes[metricName]?.get() ?: 0L
        val count = executionCounts[metricName]?.get() ?: 0L
        return if (count > 0) totalTime / count else 0L
    }

    /**
     * Get total execution time for a metric.
     * @param metricName The name of the metric
     * @return Total execution time in milliseconds, or 0 if metric not found
     */
    fun getTotalExecutionTime(metricName: String): Long {
        return executionTimes[metricName]?.get() ?: 0L
    }

    /**
     * Get execution count for a metric.
     * @param metricName The name of the metric
     * @return Execution count, or 0 if metric not found
     */
    fun getExecutionCount(metricName: String): Long {
        return executionCounts[metricName]?.get() ?: 0L
    }

    /**
     * Get current memory usage for a metric.
     * @param metricName The name of the metric
     * @return Memory usage in bytes, or 0 if metric not found
     */
    fun getMemoryUsage(metricName: String): Long {
        return memoryUsage[metricName]?.get() ?: 0L
    }

    /**
     * Get current app memory usage.
     * @return Memory usage in bytes
     */
    fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /**
     * Get max available memory.
     * @return Max memory in bytes
     */
    fun getMaxMemory(): Long {
        return Runtime.getRuntime().maxMemory()
    }

    /**
     * Get memory usage percentage.
     * @return Memory usage as percentage (0-100)
     */
    fun getMemoryUsagePercentage(): Double {
        val current = getCurrentMemoryUsage()
        val max = getMaxMemory()
        return if (max > 0) (current.toDouble() / max.toDouble()) * 100.0 else 0.0
    }

    /**
     * Check if memory usage is critical.
     * @return true if memory usage is above 85%
     */
    fun isMemoryCritical(): Boolean {
        return getMemoryUsagePercentage() > 85.0
    }

    /**
     * Reset all metrics.
     */
    fun resetMetrics() {
        executionTimes.clear()
        executionCounts.clear()
        memoryUsage.clear()
        VibeLogger.d(TAG, "Performance metrics reset")
    }

    /**
     * Log performance summary.
     */
    fun logPerformanceSummary() {
        VibeLogger.i(TAG, "=== Performance Summary ===")
        VibeLogger.i(TAG, "Memory Usage: ${getMemoryUsagePercentage()}% (${getCurrentMemoryUsage()}/${getMaxMemory()} bytes)")
        
        executionTimes.keys.forEach { metricName ->
            val avgTime = getAverageExecutionTime(metricName)
            val count = getExecutionCount(metricName)
            val totalTime = getTotalExecutionTime(metricName)
            
            VibeLogger.i(TAG, "$metricName: avg=${avgTime}ms, count=$count, total=${totalTime}ms")
        }
    }

    /**
     * Performance tracker for async operations.
     */
    class AsyncPerformanceTracker(private val metricName: String) {
        private var startTime: Long = 0L
        
        fun start() {
            startTime = System.currentTimeMillis()
        }
        
        fun end() {
            val duration = System.currentTimeMillis() - startTime
            recordExecutionTime(metricName, duration)
            
            if (duration > CRITICAL_THRESHOLD_MS) {
                VibeLogger.e(TAG, "CRITICAL PERFORMANCE (Async): $metricName took ${duration}ms")
            } else if (duration > WARNING_THRESHOLD_MS) {
                VibeLogger.w(TAG, "WARNING PERFORMANCE (Async): $metricName took ${duration}ms")
            }
        }
    }

    /**
     * Memory pressure listener for monitoring memory usage.
     */
    interface MemoryPressureListener {
        fun onMemoryPressureCritical()
        fun onMemoryPressureWarning()
        fun onMemoryPressureNormal()
    }

    private val memoryPressureListeners = mutableListOf<MemoryPressureListener>()

    /**
     * Add memory pressure listener.
     * @param listener The listener to add
     */
    fun addMemoryPressureListener(listener: MemoryPressureListener) {
        memoryPressureListeners.add(listener)
    }

    /**
     * Remove memory pressure listener.
     * @param listener The listener to remove
     */
    fun removeMemoryPressureListener(listener: MemoryPressureListener) {
        memoryPressureListeners.remove(listener)
    }

    /**
     * Check memory pressure and notify listeners.
     */
    fun checkMemoryPressure() {
        val usagePercentage = getMemoryUsagePercentage()
        
        when {
            usagePercentage > 85.0 -> {
                memoryPressureListeners.forEach { it.onMemoryPressureCritical() }
                VibeLogger.w(TAG, "Memory pressure critical: ${usagePercentage}%")
            }
            usagePercentage > 70.0 -> {
                memoryPressureListeners.forEach { it.onMemoryPressureWarning() }
                VibeLogger.d(TAG, "Memory pressure warning: ${usagePercentage}%")
            }
            else -> {
                memoryPressureListeners.forEach { it.onMemoryPressureNormal() }
            }
        }
    }
}
