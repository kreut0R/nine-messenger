package app.nine.ui.theme

import androidx.compose.ui.graphics.Color
import app.nine.domain.BubbleColor

/**
 * Цвета приложения. Тема одна, тёмная: чёрный фон, нейтральные панели и зелёный только там,
 * где он что-то значит: выбранный чат, свои сообщения, галочки и акценты. Цветные плитки есть только у строк настроек.
 * Градиентов в оформлении нет, все цвета плоские.
 */
object NineColors {
    val Background = Color(0xFF000000)
    val Panel = Color(0xFF0D0D0D)

    // Тонкие светлые линии: контур панели, блик на её верхней кромке и разделители списка.
    val Rim = Color.White.copy(alpha = 0.12f)
    val RimHighlight = Color.White.copy(alpha = 0.20f)
    val Hairline = Color.White.copy(alpha = 0.07f)

    val TextPrimary = Color(0xFFF3F6F4)
    val TextSecondary = Color(0xFFA0A5A2)
    val TextTertiary = Color(0xFF838885)

    val Accent = Color(0xFF4CC47C)
    /** Ошибки и необратимые действия. */
    val Danger = Color(0xFFE5736A)
    val AccentSoft = Color(0xFF8FCAA0)

    val SelectedRow = Color(0xFF0F4D2F).copy(alpha = 0.50f)
    val HoverRow = Color(0xFF0F4D2F).copy(alpha = 0.28f)

    // Подложки полей ввода, вкладок и кнопок.
    val Control = Color.White.copy(alpha = 0.07f)
    /** Рамка поля ввода на экранах входа. */
    val FieldBorder = Color.White.copy(alpha = 0.22f)
    val ControlHover = Color.White.copy(alpha = 0.11f)
    val TabThumb = Color(0xFF2D2D2D)

    val Badge = Color(0xFFE8EEEB)
    val OnBadge = Color(0xFF0B100E)

    val IncomingBubble = Color.White.copy(alpha = 0.075f)
    val IncomingBubbleBorder = Color.White.copy(alpha = 0.07f)
    val OwnBubbleBorder = Color(0xFF96EBAF).copy(alpha = 0.14f)
    val OnOwnBubble = Color(0xFFF2FBF4)

    /** Боковое меню настроек чуть темнее страницы рядом с ним. */
    val Sidebar = Color(0xFF080808)

    /** Всплывающее меню чуть светлее панелей, на которых оно лежит. */
    val Menu = Color(0xFF171717)

    /** Шапка и поле ввода лежат поверх переписки: фон почти непрозрачный, но сообщения под ним слегка просвечивают. */
    val FloatingBar = Color(0xE6121212)
    val Scrim = Color(0xB3000000)

    // Плитки с иконками в настройках: плотный цвет и белый значок. Серая плитка только у настроек, которых ещё нет.
    val TileGreen = Color(0xFF2E9D68)
    val TileBlue = Color(0xFF3B7FD4)
    val TileAmber = Color(0xFFC7781F)
    val TileRed = Color(0xFFD0524B)
    val OnTile = Color.White

    val MaskFill = Color(0xFF1D6A40)
    val MaskStroke = Color(0xFF7FDCA3)
}

/** Цвет собственных сообщений для выбранного в настройках варианта. */
val BubbleColor.color: Color
    get() = when (this) {
        BubbleColor.Forest -> Color(0xFF0F4D2F)
        BubbleColor.Emerald -> Color(0xFF1C6B3E)
        BubbleColor.Olive -> Color(0xFF3D4F1F)
        BubbleColor.Moss -> Color(0xFF35483A)
        BubbleColor.Graphite -> Color(0xFF2B2D2C)
    }

/** Название цвета для озвучивания экранным диктором и подсказок. */
val BubbleColor.label: String
    get() = when (this) {
        BubbleColor.Forest -> "Лесной"
        BubbleColor.Emerald -> "Изумрудный"
        BubbleColor.Olive -> "Оливковый"
        BubbleColor.Moss -> "Мшистый"
        BubbleColor.Graphite -> "Графитовый"
    }
