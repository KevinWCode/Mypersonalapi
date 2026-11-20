package com.kevinwcode.personalapi

import java.util.Locale

/** The app's Character card as plain lines of text, from the game worked out on the phone. */
object CharacterText {
    fun lines(g: Game.State): List<String> {
        val out = mutableListOf<String>()
        val lv = g.level
        out += "Level ${lv.level} ${lv.tier} · ${g.characterClass.name}" + (g.title?.let { " · $it" } ?: "")
        out += if (lv.levelCost != null) "${fmt(lv.levelXp)} / ${fmt(lv.levelCost)} XP to level ${lv.level + 1}"
        else "Max level"
        out += "Today: ${fmt(g.todayXp)} / ${fmt(g.dailyMax)} XP" +
            g.buffs.joinToString("") { " · ${it.name}" + if (it.xp != 0) " +${it.xp}" else "" }
        out += g.vitals.joinToString("   ") { "${it.name} ${it.value}" + if (it.low) " (low)" else "" }
        val streaks = g.streaks.filter { it.days > 0 }
        if (streaks.isNotEmpty()) {
            out += "Streaks: " + streaks.joinToString(", ") { "${it.label} ${it.days} ${it.unit}${if (it.days == 1) "" else "s"}" }
        }
        out += "Day ${g.dayNumber} · total ${fmt(g.totalXp)} XP"
        return out
    }

    /** Today's score per stat, best first, leaving out stats with no XP either way. */
    fun todayLines(g: Game.State): List<String> =
        g.lines.filter { it.xp != 0 }.sortedByDescending { it.xp }.map { "${it.label}: ${if (it.xp > 0) "+" else ""}${it.xp} (${it.detail})" }

    private fun fmt(n: Int) = String.format(Locale.UK, "%,d", n)
}
