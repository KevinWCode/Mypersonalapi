package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject

/**
 * The game as JSON, in the same shape as the server's GET /game, so the dashboard page runs unchanged in the app.
 * [DashboardParityTest] checks it against the server's own output.
 */
object GameJson {
    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().apply {
        for ((k, v) in pairs) put(k, v ?: JSONObject.NULL)
    }

    private fun arr(items: List<Any?>) = JSONArray().apply { items.forEach { put(it ?: JSONObject.NULL) } }

    private fun level(l: Game.LevelInfo) = listOf(
        "level" to l.level, "tier" to l.tier, "level_xp" to l.levelXp, "level_cost" to l.levelCost, "xp_to_next" to l.xpToNext,
    )

    fun line(l: Game.Line) = obj("key" to l.key, "label" to l.label, "stat" to l.stat, "xp" to l.xp, "max" to l.max, "detail" to l.detail)

    fun toJson(g: Game.State, bars: List<Vitals.Bar> = Vitals.BARS): JSONObject = obj(
        "date" to g.date.toString(),
        *level(g.level).toTypedArray(),
        "max_level" to Game.MAX_LEVEL,
        "total_xp" to g.totalXp,
        "xp_to_max" to Game.XP_TO_MAX,
        "day_number" to g.dayNumber,
        "class" to obj("name" to g.characterClass.name, "groups" to arr(g.characterClass.groups), "detail" to g.characterClass.detail),
        "title" to g.title,
        "today" to obj(
            "xp" to g.todayXp, "max" to g.dailyMax,
            "buffs" to arr(g.buffs.map { obj("name" to it.name, "detail" to it.detail, "xp" to it.xp) }),
            "metrics" to arr(g.lines.map(::line)),
        ),
        "avg_daily_xp_14d" to g.avgDailyXp14d,
        "days_to_max_level" to g.daysToMaxLevel,
        "max_level_date" to g.daysToMaxLevel?.let { g.date.plusDays(it.toLong()).toString() },
        "streaks" to arr(g.streaks.map { obj("key" to it.key, "label" to it.label, "days" to it.days, "unit" to it.unit) }),
        "vitals" to arr(g.vitals.map { vital(it, bars) }),
        "stats" to arr(g.stats.map {
            obj("key" to it.key, "label" to it.label, "stat" to it.stat, "xp" to it.xp, "level" to it.level, "rank" to it.rank,
                "score" to it.score, "modifier" to it.modifier, "level_xp" to it.levelXp, "level_cost" to it.levelCost,
                "xp_to_next" to it.xpToNext)
        }),
        "groups" to arr(g.groups.map { group(it, g.profile) }),
        "history" to arr(g.history.map {
            obj("date" to it.date.toString(), "xp" to it.xp, "well_rested" to it.wellRested, "social_ok" to it.socialOk,
                "committed" to it.committed)
        }),
    )

    private fun group(s: Game.GroupSheet, profile: Profile): JSONObject {
        val g = Perks.GROUPS.getValue(s.key)
        return obj(
            // Only the stats this person tracks show under the group.
            "key" to s.key, "name" to s.name, "ability" to s.ability, "stats" to arr(g.stats.filter(profile::lineOn)), "level" to s.level,
            "tier" to s.tier, "score" to s.score, "modifier" to s.modifier, "title" to s.title, "next_tier_level" to s.nextTierLevel,
            "perks" to arr(Perks.TIERS.map { (t, rankUnlock) ->
                val (rank, unlock) = rankUnlock
                val p = Perks.PERKS.getValue(s.key to t)
                obj("tier" to t, "rank" to rank, "unlock_level" to unlock, "title" to p.title, "token" to p.token,
                    "detail" to p.detail, "earned" to (s.level >= unlock), "claimed_on" to s.claimed[t],
                    "claim_metric" to Perks.claimMetric(s.key, t))
            }),
        )
    }

    private fun vital(v: Vitals.BarState, bars: List<Vitals.Bar>): JSONObject {
        val tick = v.tickPerDay ?: bars.first { it.key == v.key }.tickPerDay
        return obj(
            "key" to v.key, "name" to v.name, "about" to v.about, "value" to v.value, "max" to Vitals.TOP, "low" to v.low,
            "tick_per_day" to tick,
            "tick_from_hour" to if (tick < 0) Vitals.AWAKE_FROM_HOUR else 0,
            "tick_hours" to if (tick < 0) Vitals.AWAKE_HOURS else 24,
            "top" to v.top, "start_of_day" to v.startOfDay, "gained_today" to v.gainedToday, "end_of_day" to v.endOfDay,
            "today" to arr(v.today.map { e ->
                obj("label" to e.label, "points" to e.points).apply { if (e.temporary) put("temporary", true) }
            }),
        )
    }
}
