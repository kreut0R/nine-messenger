package app.nine.ui.lock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nine.ui.components.NineLogo
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

private val ArtShape = RoundedCornerShape(20.dp)
private val ArtBase = Color(0xFF050505)

/**
 * The left picture of the sign-in screen: a black field with blurred gray spots, the app mark on top and a caption at the bottom.
 * Only the spots are blurred, so the text stays sharp.
 */
@Composable
fun AuthArt(eyebrow: String, headline: String, modifier: Modifier = Modifier) {
    val type = NineTheme.typography
    Box(
        modifier
            .clip(ArtShape)
            .background(ArtBase)
            .border(1.dp, NineColors.Rim, ArtShape),
    ) {
        Canvas(Modifier.fillMaxSize().blur(64.dp)) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFF3A3A3A), radius = w * 0.42f, center = Offset(w * 0.86f, h * 0.12f))
            drawCircle(Color(0xFF0F0F0F), radius = w * 0.62f, center = Offset(w * 0.12f, h * 0.55f))
            drawCircle(Color(0xFF7A7A7A), radius = w * 0.18f, center = Offset(w * 0.88f, h * 0.30f))
            drawCircle(Color(0xFF383838), radius = w * 0.34f, center = Offset(w * 0.78f, h * 0.70f))
            drawCircle(Color.Black, radius = w * 0.26f, center = Offset(w * 0.20f, h * 0.45f))
        }
        NineLogo(Color.White, Modifier.padding(28.dp).align(Alignment.TopStart), size = 34.dp)
        Column(
            Modifier.align(Alignment.BottomStart).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(eyebrow, style = type.chatPreview.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp))
            BasicText(headline, style = type.profileName.copy(fontSize = 26.sp, lineHeight = 32.sp))
        }
    }
}
