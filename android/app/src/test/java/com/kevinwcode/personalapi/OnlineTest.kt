package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The same cases as the server's tests/test_github.py and tests/test_alpaca.py. */
class OnlineTest {
    private fun ts(day: String) = LocalDateTime.parse("${day}T00:00:00").atZone(ZoneId.of("America/New_York")).toEpochSecond()

    private val history = JSONObject()
        .put("timestamp", JSONArray(listOf(ts("2026-10-20"), ts("2026-10-21"), ts("2026-10-22"), ts("2026-10-23"))))
        .put("equity", JSONArray(listOf(1000.0, 1010.0, 1005.0, 1030.0)))
        .put("profit_loss", JSONArray(listOf(0.0, 10.0, 5.0, 30.0)))

    private val fills = JSONArray("""[
        {"id": "f4", "symbol": "NVDA", "side": "sell_short", "qty": "1", "price": "141", "transaction_time": "2026-10-23T16:00:00Z"},
        {"id": "f3", "symbol": "GMEX", "side": "sell", "qty": "2", "price": "2.84", "transaction_time": "2026-10-23T15:00:00Z"},
        {"id": "f2", "symbol": "AAPL", "side": "buy", "qty": "1", "price": "230", "transaction_time": "2026-10-22T15:00:00Z"},
        {"id": "f1", "symbol": "MSFT", "side": "buy", "qty": "1", "price": "410", "transaction_time": "2026-10-21T15:00:00Z"}
    ]""")

    private val positions = JSONArray("""[
        {"symbol": "AAPL", "qty": "1", "market_value": "232", "unrealized_pl": "2"},
        {"symbol": "MSFT", "qty": "1", "market_value": "415", "unrealized_pl": "5"}
    ]""")

    @Test
    fun githubCommitsAreSummedAcrossReposWithZeroDays() {
        val reply = JSONObject("""{"data": {"viewer": {"contributionsCollection": {"commitContributionsByRepository": [
            {"contributions": {"nodes": [{"occurredAt": "2026-10-22T07:00:00Z", "commitCount": 3}]}},
            {"contributions": {"nodes": [{"occurredAt": "2026-10-22T07:00:00Z", "commitCount": 2},
                                         {"occurredAt": "2026-10-23T07:00:00Z", "commitCount": 1}]}}
        ]}}}}""")
        var auth: String? = null
        val rows = GitHub.fetch("tok", LocalDate.of(2026, 10, 23), daysBack = 3, http = { method, url, headers, _ ->
            assertEquals("POST", method)
            assertEquals("https://api.github.com/graphql", url)
            auth = headers["Authorization"]
            reply.toString()
        })
        assertEquals("Bearer tok", auth)
        assertEquals(mapOf("2026-10-21" to 0.0, "2026-10-22" to 5.0, "2026-10-23" to 1.0), rows.associate { it.date.toString() to it.value })
        assertTrue(rows.all { it.metric == "github.commits" })
    }

    @Test
    fun dailyPlIsTheChangeFromThePreviousDay() {
        val rows = Alpaca.dailyPl(history)
        val pl = rows.filter { it.metric == "trading.pl_day" }.associate { it.date.toString() to it.value }
        assertEquals(mapOf("2026-10-21" to 10.0, "2026-10-22" to -5.0, "2026-10-23" to 25.0), pl)
        assertEquals(4, rows.count { it.metric == "trading.equity" })
    }

    @Test
    fun emptyDaysAreSkipped() {
        val h = JSONObject().put("timestamp", JSONArray(listOf(ts("2026-10-20"), ts("2026-10-21"), ts("2026-10-22"))))
            .put("equity", JSONArray().put(JSONObject.NULL).put(0.0).put(1000.0))
            .put("profit_loss", JSONArray().put(JSONObject.NULL).put(0.0).put(4.0))
        val rows = Alpaca.dailyPl(h)
        assertEquals(listOf("trading.equity"), rows.map { it.metric })
    }

    @Test
    fun accountsAreReadOnlyAndAddUp() {
        val seen = mutableListOf<String>()
        val http = Http { method, url, headers, _ ->
            assertEquals("GET", method)
            seen += "${headers["APCA-API-KEY-ID"]} ${url.substringBefore("?")}"
            when (url.removePrefix(Alpaca.PAPER_URL).substringBefore("?")) {
                "/v2/account/portfolio/history" -> history.toString()
                "/v2/account/activities/FILL" -> fills.toString()
                "/v2/positions" -> positions.toString()
                else -> error(url)
            }
        }
        val accounts = Alpaca.parseAccounts("k1 s1\nk2:s2")
        assertEquals(listOf(Alpaca.Account("k1", "s1"), Alpaca.Account("k2", "s2")), accounts)
        val r = Alpaca.fetch(accounts, Alpaca.PAPER_URL, http)
        assertEquals(listOf("alpaca1", "alpaca2"), r.rows.keys.toList())
        assertEquals(6, seen.size)
        assertTrue(seen.first().startsWith("k1 "))
        // Two bots holding the same stock add up to one position.
        assertEquals(Position("MSFT", 2.0, 830.0, 10.0), r.positions.single { it.symbol == "MSFT" })

        val t = Alpaca.trading(r.fills, r.positions)
        assertEquals("NVDA", t.recent.first().symbol)
        assertEquals("AAPL", t.lastBuy!!.symbol)
        assertEquals("NVDA", t.lastSell!!.symbol) // short sales count as sells
        assertEquals("MSFT", t.top.first().symbol)
    }

    @Test
    fun widgetAndDashboardShowTheSavedTrades() {
        val f = Alpaca.fills(fills)
        val days = mapOf(LocalDate.of(2026, 10, 23) to mapOf("trading.pl_day" to 25.0, "trading.equity" to 1030.0))
        val summary = LocalSummary.toJson(LocalSummary.build(days, LocalDate.of(2026, 10, 23), lastTrade = f.first()))
        val last = summary.getJSONObject("last_trade")
        assertEquals("NVDA", last.getString("symbol"))
        assertEquals("2026-10-23T16:00:00Z", last.getString("filled_at"))
        assertEquals(25.0, summary.getDouble("pl_7d"), 0.0)

        val trading = PhoneApi.tradingJson(days, LocalDate.of(2026, 10, 23), Alpaca.trading(f, Alpaca.positions(positions)))
        assertEquals(4, trading.getJSONArray("recent_trades").length().coerceAtMost(5))
        assertEquals("AAPL", trading.getJSONObject("last_buy").getString("symbol"))
        assertEquals("MSFT", trading.getJSONArray("top_positions").getJSONObject(0).getString("symbol"))
        assertNull(LocalSummary.build(days, LocalDate.of(2026, 10, 23)).lastTrade)
    }
}
