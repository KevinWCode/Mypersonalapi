package com.kevinwcode.personalapi

import java.time.Duration
import java.time.Instant

object HeartMath {
    /**
     * Lowest average heart rate over any [window] of samples, e.g. the calmest 5 minutes of a night's sleep.
     * Samsung Health doesn't share a resting heart rate with Health Connect, so this stands in for it.
     */
    fun lowestRollingAverage(samples: List<Pair<Instant, Long>>, window: Duration = Duration.ofMinutes(5)): Double? {
        val sorted = samples.sortedBy { it.first }
        var best: Double? = null
        var j = 0
        var sum = 0L
        for (i in sorted.indices) {
            // Grow the window from sample i to every sample within `window` of it.
            if (j < i) { j = i; sum = 0 }
            while (j < sorted.size && Duration.between(sorted[i].first, sorted[j].first) <= window) {
                sum += sorted[j].second
                j++
            }
            val avg = sum.toDouble() / (j - i)
            if (best == null || avg < best) best = avg
            sum -= sorted[i].second
        }
        return best
    }
}
