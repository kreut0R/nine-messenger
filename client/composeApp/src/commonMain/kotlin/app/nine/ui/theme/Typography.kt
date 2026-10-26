package app.nine.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nine.resources.Res
import app.nine.resources.inter_bold
import app.nine.resources.inter_medium
import app.nine.resources.inter_regular
import app.nine.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/** Все текстовые стили приложения. Иерархия: имя заметнее превью, время тише всех. */
@Immutable
class NineTypography(private val family: FontFamily) {
    private fun style(size: Float, weight: FontWeight, lineHeight: Float = size * 1.4f, spacing: Float = 0f) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = lineHeight.sp,
        letterSpacing = spacing.sp,
        color = NineColors.TextPrimary,
    )

    val screenTitle = style(32f, FontWeight.Bold, lineHeight = 38f, spacing = -0.8f)
    val profileName = style(20f, FontWeight.Bold)
    val chatName = style(16f, FontWeight.SemiBold)
    val chatPreview = style(14f, FontWeight.Normal).copy(color = NineColors.TextSecondary)
    val time = style(13f, FontWeight.Normal).copy(color = NineColors.TextTertiary)
    val messageText = style(15f, FontWeight.Normal, lineHeight = 22.5f)
    val stamp = style(11.5f, FontWeight.Normal).copy(color = NineColors.TextTertiary)
    val dayHeader = style(12f, FontWeight.Medium).copy(color = NineColors.TextSecondary)
    val section = style(13f, FontWeight.Medium)
    val tab = style(13f, FontWeight.Medium).copy(color = NineColors.TextSecondary)
    val tabSelected = style(13f, FontWeight.Bold)
    val field = style(14.5f, FontWeight.Normal)
    val hint = style(14.5f, FontWeight.Normal).copy(color = NineColors.TextTertiary)
    val button = style(14f, FontWeight.SemiBold)
    val badge = style(11.5f, FontWeight.Bold).copy(color = NineColors.OnBadge)
    val code = style(12f, FontWeight.Normal, spacing = 1.5f).copy(fontFamily = FontFamily.Monospace, color = NineColors.TextTertiary)
    val avatarLetter = style(16f, FontWeight.SemiBold).copy(color = androidx.compose.ui.graphics.Color.White)
}

private val LocalNineTypography = compositionLocalOf<NineTypography> { error("NineTheme is not provided") }

/** Доступ к стилям из любого места внутри [NineTheme]. */
object NineTheme {
    val typography: NineTypography
        @Composable @ReadOnlyComposable get() = LocalNineTypography.current
}

/** Шрифт Inter лежит в ресурсах приложения, чтобы вид не зависел от шрифтов, установленных в системе. */
@Composable
internal fun rememberTypography(): NineTypography {
    val regular = Font(Res.font.inter_regular, FontWeight.Normal)
    val medium = Font(Res.font.inter_medium, FontWeight.Medium)
    val semibold = Font(Res.font.inter_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.inter_bold, FontWeight.Bold)
    return remember(regular, medium, semibold, bold) {
        NineTypography(FontFamily(regular, medium, semibold, bold))
    }
}

internal fun provideTypography(typography: NineTypography) = LocalNineTypography provides typography
