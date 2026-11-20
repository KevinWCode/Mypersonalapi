package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.fail

/** Compares two JSON values and names the first place they differ. Numbers match by value (1 and 1.0 are the same). */
object JsonMatch {
    fun assertSame(expected: Any?, actual: Any?) {
        diff("$", expected, actual)?.let { fail(it) }
    }

    private fun isNull(v: Any?) = v == null || v == JSONObject.NULL

    private fun diff(path: String, e: Any?, a: Any?): String? = when {
        isNull(e) || isNull(a) -> if (isNull(e) && isNull(a)) null else "$path: expected $e but was $a"
        e is Number && a is Number ->
            if (Math.abs(e.toDouble() - a.toDouble()) < 1e-9) null else "$path: expected $e but was $a"
        e is JSONObject && a is JSONObject -> {
            val keys = e.keySet() + a.keySet()
            keys.sorted().firstNotNullOfOrNull { k ->
                when {
                    !e.has(k) -> "$path.$k: not expected, was ${a.opt(k)}"
                    !a.has(k) -> "$path.$k: missing, expected ${e.opt(k)}"
                    else -> diff("$path.$k", e.opt(k), a.opt(k))
                }
            }
        }
        e is JSONArray && a is JSONArray ->
            if (e.length() != a.length()) "$path: expected ${e.length()} items but was ${a.length()}: $a"
            else (0 until e.length()).firstNotNullOfOrNull { diff("$path[$it]", e.opt(it), a.opt(it)) }
        else -> if (e == a) null else "$path: expected $e but was $a"
    }
}
