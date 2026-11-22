package com.kevinwcode.personalapi

/**
 * Kevin's six stat groups, the titles and perk tokens each tier unlocks, and the classes they make.
 * Generated from the server's app/perks.py, so both use the same text.
 */
object Perks {
    data class Group(val name: String, val ability: String, val stats: List<String>)
    data class Perk(val title: String, val token: String, val detail: String)

    // A group's level is its highest stat level. Workouts count toward both STR and CON.
    val GROUPS: Map<String, Group> = linkedMapOf(
        "str" to Group("Strength & Athletics", "STR", listOf("kettlebell", "workout", "catch")),
        "con" to Group("Vitality & Endurance", "CON", listOf("sleep", "heart", "steps", "workout")),
        "int" to Group("Intelligence & Learning", "INT", listOf("study", "github")),
        "wis" to Group("Wisdom & Reflection", "WIS", listOf("reading", "journal")),
        "cha" to Group("Charisma & Connection", "CHA", listOf("connect")),
        "per" to Group("Perception & Focus", "PER", listOf("social", "youtube")),
    )

    /** Tier to (rank, group level that unlocks it). */
    val TIERS: Map<Int, Pair<String, Int>> = linkedMapOf(
        1 to ("Novice" to 10),
        2 to ("Adept" to 28),
        3 to ("Expert" to 56),
        4 to ("Master" to 84),
    )

    val PERKS: Map<Pair<String, Int>, Perk> = linkedMapOf(
        ("str" to 1) to Perk("Grip Master", "Post-Heavy Session Feast", "One free-pass cheat meal (pizza, burger or takeaway) after a personal best volume or weight."),
        ("str" to 2) to Perk("Lord of the Kettlebells", "Athletic Equipment Upgrade", "A gear upgrade: chalk, grip tape, wrist wraps or fresh athletic socks."),
        ("str" to 3) to Perk("Wind Walker of the Track", "The Heavy Metal Drop", "A heavier kettlebell or new high-performance training shoes."),
        ("str" to 4) to Perk("Titan of the Iron", "Sports Massage & Recovery Session", "A 60-minute deep tissue sports massage or a sauna visit."),
        ("con" to 1) to Perk("The Rested Initiate", "Snooze Button Pass", "One guilt-free weekend lie-in. The day you claim it counts as Well-Rested."),
        ("con" to 2) to Perk("Unbroken Vigor", "Zero-Alarm Weekend Day", "A full day with no alarms. The day you claim it counts as Well-Rested."),
        ("con" to 3) to Perk("Sovereign of Sleep", "Luxury Bedding Upgrade", "A memory foam pillow, weighted blanket or blackout blinds."),
        ("con" to 4) to Perk("Immortal Vitality", "Full Spa & Sauna Day Pass", "A thermal bath, cold plunge and relaxation day."),
        ("int" to 1) to Perk("Code Weaver", "Digital Tool Upgrade", "A premium software utility, dev tool subscription or IDE plugin."),
        ("int" to 2) to Perk("Archon of Intellect", "Wishlist Book / Course Unlock", "Any technical book, D&D sourcebook or online course on your list."),
        ("int" to 3) to Perk("Master of the Arcane Script", "Desk Setup Upgrade", "A mechanical keyboard, precision mouse or desk mat."),
        ("int" to 4) to Perk("Grand Technomancer", "Ergonomics & Hardware Overhaul", "A monitor arm, chair upgrade or second screen."),
        ("per" to 1) to Perk("Keeper of the Mind", "Unrestricted Media Pass", "3 hours of guilt-free media. The day you claim it, social media and YouTube can't cost XP."),
        ("per" to 2) to Perk("High Archon of Focus", "Cinema & Night Out", "A guilt-free evening at the movies or an event, phone stats unchecked."),
        ("per" to 3) to Perk("Untouchable Mind", "Gaming Marathon Pass", "A full weekend afternoon of guilt-free PC gaming."),
        ("per" to 4) to Perk("Zenith of Discipline", "Digital Detox Retreat", "A weekend unplugged. The day you claim it and the next score at least your 14-day average."),
        ("cha" to 1) to Perk("Asset Collector", "Specialty Roast & Brew Pass", "A top-shelf bag of coffee beans or premium local craft drinks."),
        ("cha" to 2) to Perk("Baron of the Vault", "Fun Money Splurge", "10% of the month's portfolio gains as spending money."),
        ("cha" to 3) to Perk("Master of Assets", "Fine Dining Voucher", "A high-end restaurant meal to celebrate."),
        ("cha" to 4) to Perk("Economic Paragon", "Major Asset Dividend Claim", "A long-term luxury item you've held off buying for 6+ months."),
        ("wis" to 1) to Perk("Chronicler of Fate", "Stationery Upgrade", "A premium hardcover journal or a high-quality pen."),
        ("wis" to 2) to Perk("Master of the Daily Block", "Chef Exemption Pass", "Skip cooking one night for a quality takeaway."),
        ("wis" to 3) to Perk("Grand Strategist", "Wildcard Schedule Day", "Clear a full day's calendar and do whatever comes up."),
        ("wis" to 4) to Perk("Architect of Life", "Custom Leather Journal / Desk Artifact", "A bespoke item, like an engraved notebook or desk plaque, celebrating mastery."),
    )

    val PURE_CLASSES: Map<String, String> = mapOf(
        "str" to "Juggernaut",
        "con" to "Warden",
        "int" to "Savant",
        "wis" to "Mystic",
        "cha" to "Envoy",
        "per" to "Watcher",
    )

    val DUAL_CLASSES: Map<Set<String>, String> = mapOf(
        setOf("con", "str") to "Warrior",
        setOf("int", "wis") to "Sage",
        setOf("con", "wis") to "Druid",
        setOf("int", "per") to "Scholar",
        setOf("cha", "wis") to "Bard",
        setOf("per", "str") to "Ranger",
        setOf("con", "per") to "Sentinel",
        setOf("cha", "int") to "Artificer",
        setOf("int", "str") to "Forgeknight",
        setOf("str", "wis") to "Battle Monk",
        setOf("cha", "str") to "Champion",
        setOf("con", "int") to "Alchemist",
        setOf("cha", "con") to "Steward",
        setOf("per", "wis") to "Seer",
        setOf("cha", "per") to "Diplomat",
    )

    fun tierFor(level: Int): Int = TIERS.filter { (_, v) -> level >= v.second }.keys.maxOrNull() ?: 0

    fun claimMetric(group: String, tier: Int) = "perk.$group.$tier.claimed"
}
