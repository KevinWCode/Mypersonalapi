package com.kevinwcode.personalapi

/**
 * The home screen widgets' colours for each of the app's eight themes, so a widget looks like the app it opens.
 * The page tells the app which theme is chosen ([DashboardActivity.App.setWidgetTheme]); the ids are the page's.
 * Backgrounds and tiles are drawables (res/drawable/widget_bg_<id>.xml, widget_tile_<id>.xml), because a widget
 * can't draw gradients or borders from code; everything else is a colour from here. Colours are ARGB.
 * Kept apart from Android so it can be unit tested.
 */
data class WidgetTheme(
    val id: String,
    val name: String,
    /** Main text. */
    val ink: Int,
    /** Labels and small lines. */
    val sub: Int,
    val accent: Int,
    /** The empty part of a bar. */
    val track: Int,
    val good: Int,
    val warn: Int,
    val bad: Int,
    /** A day with no data on the Today widget's chart. */
    val empty: Int,
    /** Corner radius of bars, in px at the bitmap's scale (the theme's own shape: sharp for Neon Grid, round for Glass). */
    val barRadius: Float,
) {
    companion object {
        private fun c(hex: String): Int {
            val h = hex.removePrefix("#")
            val argb = if (h.length == 6) "FF$h" else h
            return argb.toLong(16).toInt()
        }

        val VITAL_COLORS = mapOf("hp" to c("#E5484D"), "stamina" to c("#F2A93B"), "mana" to c("#4C8DFF"), "focus" to c("#A77BF3"))
        /** The two-tap meal qualities, the same colours as the page's meal tiles. */
        val QUALITY_COLORS = mapOf("green" to c("#1F9D55"), "amber" to c("#E0A100"), "red" to c("#E5484D"), "protein-rich" to c("#4C8DFF"))

        val ALL = listOf(
            WidgetTheme("light", "Daylight", c("#16191D"), c("#5B6168"), c("#2457D6"), c("#E3E6E0"),
                c("#1F6B3A"), c("#B4501A"), c("#C4302B"), c("#D5D8D2"), 10f),
            WidgetTheme("dark", "Dark", c("#ECEEF1"), c("#A1A8B1"), c("#7FA2FF"), c("#2A2F36"),
                c("#5FC98A"), c("#F0965A"), c("#F07178"), c("#3A4048"), 10f),
            WidgetTheme("glass", "Glass", c("#111318"), c("#3F4654"), c("#0A64D8"), c("#33111318"),
                c("#1F7A45"), c("#B4501A"), c("#C4302B"), c("#26111318"), 12f),
            WidgetTheme("fantasy", "Ember Keep", c("#F3E6C8"), c("#CDB78E"), c("#E8B84F"), c("#3E2C18"),
                c("#8FD07A"), c("#F08C4E"), c("#F07164"), c("#5C4526"), 5f),
            WidgetTheme("cyber", "Neon Grid", c("#EAF6FF"), c("#9BA6C9"), c("#00E5FF"), c("#2B1A4D"),
                c("#3DFF9A"), c("#FF4FD8"), c("#FF4F6D"), c("#2B2350"), 1f),
            WidgetTheme("system", "Awakened System", c("#DDEBFF"), c("#93AED6"), c("#4FA8FF"), c("#12305F"),
                c("#5FE3B5"), c("#FFB35C"), c("#FF6B7A"), c("#1C3566"), 2f),
            WidgetTheme("gothic", "Gothic", c("#EADFD2"), c("#B5A59A"), c("#D44A5E"), c("#4A1A28"),
                c("#8CC49A"), c("#E3A35A"), c("#F08494"), c("#3A2530"), 3f),
            WidgetTheme("board", "Quest Board", c("#2E2012"), c("#5E4A33"), c("#8A3F12"), c("#D2B983"),
                c("#2F5E1E"), c("#9A3412"), c("#A3241C"), c("#CDB68A"), 2f),
        )

        /** The theme with this id, or Daylight (the page's default) for anything else. */
        fun of(id: String?): WidgetTheme = ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}
