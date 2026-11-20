package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Makes one web request and returns the reply's text. Swapped for a fake in tests. */
fun interface Http {
    fun request(method: String, url: String, headers: Map<String, String>, body: String?): String

    companion object {
        val DEFAULT = Http { method, url, headers, body ->
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }
                val code = conn.responseCode
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}

/** Commits per day from GitHub's contributions data, like the server's app/collectors/github.py. */
object GitHub {
    const val SOURCE = "github"

    // viewer is the token's own account, so only the token is needed.
    private val QUERY = """
        query(${'$'}from: DateTime!, ${'$'}to: DateTime!) {
          viewer {
            contributionsCollection(from: ${'$'}from, to: ${'$'}to) {
              commitContributionsByRepository(maxRepositories: 100) {
                contributions(first: 100) { nodes { occurredAt commitCount } }
              }
            }
          }
        }
    """.trimIndent()

    fun fetch(token: String, today: LocalDate, daysBack: Int = 3, http: Http = Http.DEFAULT): List<Observation> {
        val start = today.minusDays(daysBack - 1L)
        val body = JSONObject().put("query", QUERY)
            .put("variables", JSONObject().put("from", "${start}T00:00:00Z").put("to", "${today}T23:59:59Z"))
        val reply = JSONObject(http.request("POST", "https://api.github.com/graphql",
            mapOf("Authorization" to "Bearer $token"), body.toString()))
        reply.optJSONArray("errors")?.let { throw IllegalStateException(it.optJSONObject(0)?.optString("message") ?: "GitHub error") }
        return commits(reply, start, daysBack)
    }

    /** Every day in the window gets a row, so a day with no commits counts as 0 rather than missing. */
    fun commits(reply: JSONObject, start: LocalDate, daysBack: Int): List<Observation> {
        val counts = linkedMapOf<String, Double>()
        for (i in 0 until daysBack) counts[start.plusDays(i.toLong()).toString()] = 0.0
        val repos = reply.getJSONObject("data").getJSONObject("viewer").getJSONObject("contributionsCollection")
            .getJSONArray("commitContributionsByRepository")
        for (r in 0 until repos.length()) {
            val nodes = repos.getJSONObject(r).getJSONObject("contributions").getJSONArray("nodes")
            for (n in 0 until nodes.length()) {
                val node = nodes.getJSONObject(n)
                val day = node.getString("occurredAt").take(10)
                counts[day]?.let { counts[day] = it + node.getDouble("commitCount") }
            }
        }
        return counts.map { (d, n) -> Observation(LocalDate.parse(d), "github.commits", n, "count") }
    }
}

data class Fill(val id: String, val filledAt: String, val symbol: String, val side: String, val qty: Double, val price: Double) {
    fun toTrade() = Trade(symbol, side, qty, price, runCatching { Instant.parse(filledAt) }.getOrNull())
}

data class Position(val symbol: String, val qty: Double, val marketValue: Double, val unrealizedPl: Double)

/** What the dashboard's trading card shows, from the trades and positions saved on the phone. */
data class Trading(val recent: List<Fill>, val lastBuy: Fill?, val lastSell: Fill?, val top: List<Position>) {
    companion object {
        val EMPTY = Trading(emptyList(), null, null, emptyList())
    }
}

/**
 * Reads the Alpaca accounts the same way as the server's app/collectors/alpaca.py: daily equity and P/L, fills and
 * open positions. GET requests only, so the app can never place or change a trade.
 */
object Alpaca {
    const val PAPER_URL = "https://paper-api.alpaca.markets"
    const val LIVE_URL = "https://api.alpaca.markets"
    private val MARKET_TZ: ZoneId = ZoneId.of("America/New_York")

    data class Account(val keyId: String, val secret: String)

    /** Pairs of key id and secret, separated by spaces, commas, colons or new lines. */
    fun parseAccounts(text: String): List<Account> =
        text.split(Regex("[\\s,:;]+")).filter { it.isNotEmpty() }.chunked(2).filter { it.size == 2 }
            .map { Account(it[0], it[1]) }

    /** Each account's rows get their own source (alpaca1, alpaca2, ...), so the day's totals add the accounts up. */
    fun source(index: Int) = "alpaca${index + 1}"

    class Result(val rows: Map<String, List<Observation>>, val fills: List<Fill>, val positions: List<Position>)

    fun fetch(accounts: List<Account>, baseUrl: String, http: Http = Http.DEFAULT): Result {
        val rows = linkedMapOf<String, List<Observation>>()
        val fills = mutableListOf<Fill>()
        val positions = mutableListOf<Position>()
        accounts.forEachIndexed { i, a ->
            val headers = mapOf("APCA-API-KEY-ID" to a.keyId, "APCA-API-SECRET-KEY" to a.secret)
            fun get(path: String) = http.request("GET", baseUrl.trimEnd('/') + path, headers, null)
            rows[source(i)] = dailyPl(JSONObject(get("/v2/account/portfolio/history?period=1M&timeframe=1D")))
            // Fill activities rather than orders: most orders are cancelled or expired ones.
            fills += fills(JSONArray(get("/v2/account/activities/FILL?direction=desc&page_size=100")))
            positions += positions(JSONArray(get("/v2/positions")))
        }
        return Result(rows, fills, combine(positions))
    }

    /**
     * Daily equity and that day's P/L from a 1D portfolio history. Alpaca's profit_loss adds up from the start of the
     * window, so a day's P/L is the change from the day before; the first day only gets its equity.
     */
    fun dailyPl(history: JSONObject): List<Observation> {
        val ts = history.getJSONArray("timestamp")
        val equity = history.getJSONArray("equity")
        val pl = history.getJSONArray("profit_loss")
        val rows = mutableListOf<Observation>()
        var prev: Double? = null
        for (i in 0 until minOf(ts.length(), equity.length(), pl.length())) {
            val e = if (equity.isNull(i)) null else equity.getDouble(i)
            if (e == null || e == 0.0) {
                prev = null
                continue
            }
            val day = Instant.ofEpochSecond(ts.getLong(i)).atZone(MARKET_TZ).toLocalDate()
            rows += Observation(day, "trading.equity", e, "usd")
            val p = if (pl.isNull(i)) null else pl.getDouble(i)
            if (prev != null && p != null) rows += Observation(day, "trading.pl_day", p - prev, "usd")
            prev = p
        }
        return rows
    }

    fun fills(list: JSONArray): List<Fill> = (0 until list.length()).map { i ->
        val f = list.getJSONObject(i)
        Fill(f.getString("id"), f.getString("transaction_time"), f.getString("symbol"), f.getString("side"),
            f.getString("qty").toDouble(), f.getString("price").toDouble())
    }

    fun positions(list: JSONArray): List<Position> = (0 until list.length()).map { i ->
        val p = list.getJSONObject(i)
        Position(p.getString("symbol"), p.getString("qty").toDouble(), p.getString("market_value").toDouble(),
            p.getString("unrealized_pl").toDouble())
    }

    /** Two bots holding the same stock add up to one position. */
    fun combine(positions: List<Position>): List<Position> =
        positions.groupBy { it.symbol }.map { (s, ps) ->
            Position(s, ps.sumOf { it.qty }, ps.sumOf { it.marketValue }, ps.sumOf { it.unrealizedPl })
        }

    /** The dashboard's view of the saved trades: the 5 latest fills, last buy and sell, and the 5 biggest positions. */
    fun trading(fills: List<Fill>, positions: List<Position>): Trading {
        val byTime = fills.sortedByDescending { it.filledAt }
        return Trading(
            recent = byTime.take(5),
            lastBuy = byTime.firstOrNull { it.side.startsWith("buy") },
            // startsWith so short sales (sell_short) count as sells.
            lastSell = byTime.firstOrNull { it.side.startsWith("sell") },
            top = positions.sortedByDescending { it.marketValue }.take(5),
        )
    }
}
