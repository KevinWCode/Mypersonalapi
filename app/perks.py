"""Kevin's six stat groups, the titles and perk tokens each tier unlocks, and the classes they make.

A perk token is a one-time real-life reward. It's earned when the group's level reaches the tier and claimed by
logging perk.<group>.<tier>.claimed (the dashboard will get a button). A few tokens also change that day's scoring.
"""

# Group key -> (name, ability, stats in the group). A group's level is its highest stat level.
GROUPS = {
    "str": ("Strength & Athletics", "STR", ["kettlebell", "workout", "catch"]),
    "con": ("Vitality & Endurance", "CON", ["sleep", "heart", "steps"]),
    "int": ("Intelligence & Crafting", "INT", ["github", "email"]),
    "wis": ("Focus & Mental Discipline", "WIS", ["social", "youtube"]),
    "cha": ("Wealth & Economy", "CHA", ["stocks"]),
    "per": ("Mindfulness & Life Organization", "PER", ["journal", "calendar"]),
}

# Tier -> (rank, group level that unlocks it).
TIERS = {1: ("Novice", 10), 2: ("Adept", 28), 3: ("Expert", 56), 4: ("Master", 84)}

# (group, tier) -> (title, token name, what it is)
PERKS = {
    ("str", 1): ("Grip Master", "Post-Heavy Session Feast",
                 "One free-pass cheat meal (pizza, burger or takeaway) after a personal best volume or weight."),
    ("str", 2): ("Lord of the Kettlebells", "Athletic Equipment Upgrade",
                 "A gear upgrade: chalk, grip tape, wrist wraps or fresh athletic socks."),
    ("str", 3): ("Wind Walker of the Track", "The Heavy Metal Drop",
                 "A heavier kettlebell or new high-performance training shoes."),
    ("str", 4): ("Titan of the Iron", "Sports Massage & Recovery Session",
                 "A 60-minute deep tissue sports massage or a sauna visit."),
    ("con", 1): ("The Rested Initiate", "Snooze Button Pass",
                 "One guilt-free weekend lie-in. The day you claim it counts as Well-Rested."),
    ("con", 2): ("Unbroken Vigor", "Zero-Alarm Weekend Day",
                 "A full day with no alarms. The day you claim it counts as Well-Rested."),
    ("con", 3): ("Sovereign of Sleep", "Luxury Bedding Upgrade",
                 "A memory foam pillow, weighted blanket or blackout blinds."),
    ("con", 4): ("Immortal Vitality", "Full Spa & Sauna Day Pass",
                 "A thermal bath, cold plunge and relaxation day."),
    ("int", 1): ("Code Weaver", "Digital Tool Upgrade",
                 "A premium software utility, dev tool subscription or IDE plugin."),
    ("int", 2): ("Archon of Intellect", "Wishlist Book / Course Unlock",
                 "Any technical book, D&D sourcebook or online course on your list."),
    ("int", 3): ("Master of the Arcane Script", "Desk Setup Upgrade",
                 "A mechanical keyboard, precision mouse or desk mat."),
    ("int", 4): ("Grand Technomancer", "Ergonomics & Hardware Overhaul",
                 "A monitor arm, chair upgrade or second screen."),
    ("wis", 1): ("Keeper of the Mind", "Unrestricted Media Pass",
                 "3 hours of guilt-free media. The day you claim it, social media and YouTube can't cost XP."),
    ("wis", 2): ("High Archon of Focus", "Cinema & Night Out",
                 "A guilt-free evening at the movies or an event, phone stats unchecked."),
    ("wis", 3): ("Untouchable Mind", "Gaming Marathon Pass",
                 "A full weekend afternoon of guilt-free PC gaming."),
    ("wis", 4): ("Zenith of Discipline", "Digital Detox Retreat",
                 "A weekend unplugged. The day you claim it and the next score at least your 14-day average."),
    ("cha", 1): ("Asset Collector", "Specialty Roast & Brew Pass",
                 "A top-shelf bag of coffee beans or premium local craft drinks."),
    ("cha", 2): ("Baron of the Vault", "Fun Money Splurge",
                 "10% of the month's portfolio gains as spending money."),
    ("cha", 3): ("Master of Assets", "Fine Dining Voucher",
                 "A high-end restaurant meal to celebrate."),
    ("cha", 4): ("Economic Paragon", "Major Asset Dividend Claim",
                 "A long-term luxury item you've held off buying for 6+ months."),
    ("per", 1): ("Chronicler of Fate", "Stationery Upgrade",
                 "A premium hardcover journal or a high-quality pen."),
    ("per", 2): ("Master of the Daily Block", "Chef Exemption Pass",
                 "Skip a cooking block for a quality takeaway. The day you claim it counts one extra calendar block."),
    ("per", 3): ("Grand Strategist", "Wildcard Schedule Day",
                 "Clear a full day's calendar and do whatever comes up."),
    ("per", 4): ("Architect of Life", "Custom Leather Journal / Desk Artifact",
                 "A bespoke item, like an engraved notebook or desk plaque, celebrating mastery."),
}

# Classes from the two strongest groups, or a pure class when one group's level is double the next.
PURE_CLASSES = {"str": "Juggernaut", "con": "Warden", "int": "Artificer", "wis": "Sage", "cha": "Merchant",
                "per": "Strategist"}
DUAL_CLASSES = {frozenset(k.split("+")): v for k, v in {
    "str+con": "Ironclad", "str+int": "Forgeknight", "str+wis": "Battle Monk", "str+cha": "Mercenary",
    "str+per": "Ranger", "con+int": "Alchemist", "con+wis": "Druid", "con+cha": "Steward",
    "con+per": "Pathfinder", "int+wis": "Runesmith", "int+cha": "Guildmaster", "int+per": "Tactician",
    "wis+cha": "Oracle", "wis+per": "Seer", "cha+per": "Diplomat",
}.items()}


def tier_for(level: int) -> int:
    return max((t for t, (_, unlock) in TIERS.items() if level >= unlock), default=0)


def claim_metric(group: str, tier: int) -> str:
    return f"perk.{group}.{tier}.claimed"
