package app.nine.ui.theme

import androidx.compose.ui.graphics.Color
import app.nine.domain.BubbleColor

/**
 * The app colors. There is one theme, dark: a black background, neutral panels and green only where
 * it means something: the selected chat, own messages, ticks and accents. Colored tiles exist only on settings rows.
 * There are no gradients in the design, all colors are flat.
 */
object NineColors {
    val Background = Color(0xFF000000)
    val Panel = Color(0xFF0D0D0D)

    // Thin light lines: the panel outline, the highlight on its top edge and the list dividers.
    val Rim = Color.White.copy(alpha = 0.12f)
    val RimHighlight = Color.White.copy(alpha = 0.20f)
    val Hairline = Color.White.copy(alpha = 0.07f)

    val TextPrimary = Color(0xFFF3F6F4)
    val TextSecondary = Color(0xFFA0A5A2)
    val TextTertiary = Color(0xFF838885)

    val Accent = Color(0xFF4CC47C)
    /** Errors and irreversible actions. */
    val Danger = Color(0xFFE5736A)
    val AccentSoft = Color(0xFF8FCAA0)

    val SelectedRow = Color(0xFF0F4D2F).copy(alpha = 0.50f)
    val HoverRow = Color(0xFF0F4D2F).copy(alpha = 0.28f)

    // Backings of input fields, tabs and buttons.
    val Control = Color.White.copy(alpha = 0.07f)
    /** The border of an input field on the sign-in screens. */
    val FieldBorder = Color.White.copy(alpha = 0.22f)
    val ControlHover = Color.White.copy(alpha = 0.11f)
    val TabThumb = Color(0xFF2D2D2D)

    val Badge = Color(0xFFE8EEEB)
    val OnBadge = Color(0xFF0B100E)

    val IncomingBubble = Color.White.copy(alpha = 0.075f)
    val IncomingBubbleBorder = Color.White.copy(alpha = 0.07f)
    val OwnBubbleBorder = Color(0xFF96EBAF).copy(alpha = 0.14f)
    val OnOwnBubble = Color(0xFFF2FBF4)

    /** The settings sidebar is slightly darker than the page next to it. */
    val Sidebar = Color(0xFF080808)

    /** A popup menu is slightly lighter than the panels it lies on. */
    val Menu = Color(0xFF171717)

    /** The header and the input field lie over the conversation: the background is almost opaque, but messages show through it faintly. */
    val FloatingBar = Color(0xE6121212)
    val Scrim = Color(0xB3000000)

    // Tiles with icons in settings: a solid color and a white glyph. A gray tile only for settings that do not exist yet.
    val TileGreen = Color(0xFF2E9D68)
    val TileBlue = Color(0xFF3B7FD4)
    val TileAmber = Color(0xFFC7781F)
    val TileRed = Color(0xFFD0524B)
    val OnTile = Color.White

    val MaskFill = Color(0xFF1D6A40)
    val MaskStroke = Color(0xFF7FDCA3)
}

/** The color of own messages for the variant chosen in settings. */
val BubbleColor.color: Color
    get() = when (this) {
        BubbleColor.Forest -> Color(0xFF0F4D2F)
        BubbleColor.Emerald -> Color(0xFF1C6B3E)
        BubbleColor.Olive -> Color(0xFF3D4F1F)
        BubbleColor.Moss -> Color(0xFF35483A)
        BubbleColor.Graphite -> Color(0xFF2B2D2C)
    }

/** The color name for screen readers and tooltips. */
val BubbleColor.label: String
    get() = when (this) {
        BubbleColor.Forest -> "Forest"
        BubbleColor.Emerald -> "Emerald"
        BubbleColor.Olive -> "Olive"
        BubbleColor.Moss -> "Moss"
        BubbleColor.Graphite -> "Graphite"
    }
