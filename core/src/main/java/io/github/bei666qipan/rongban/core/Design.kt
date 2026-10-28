package io.github.bei666qipan.rongban.core

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

object CompanionState { val emotion = kotlinx.coroutines.flow.MutableStateFlow("idle") }

val Sage = Color(0xFF718875)
val Cream = Color(0xFFFAF7EF)
val Apricot = Color(0xFFE4AB84)
private val Ink = Color(0xFF2C382F)
private val Mist = Color(0xFF5C6B5E)
private val Paper = Color(0xFFFFFCF6)
private val SageDeep = Color(0xFF4E6353)
private val SageSoft = Color(0xFFDDE8DC)
private val Peach = Color(0xFFF3DFCE)
private val Clay = Color(0xFFAF714D)

private val LightScheme: ColorScheme = lightColorScheme(
 primary = SageDeep,
 onPrimary = Color(0xFFF7FBF6),
 primaryContainer = SageSoft,
 onPrimaryContainer = Color(0xFF243428),
 secondary = Clay,
 onSecondary = Color(0xFFFFF8F1),
 secondaryContainer = Peach,
 onSecondaryContainer = Color(0xFF4A3428),
 tertiary = Color(0xFF6E8B8E),
 onTertiary = Color(0xFFF4FBFC),
 tertiaryContainer = Color(0xFFD7E6E7),
 onTertiaryContainer = Color(0xFF243638),
 background = Cream,
 onBackground = Ink,
 surface = Paper,
 onSurface = Ink,
 surfaceVariant = Color(0xFFE6ECE2),
 onSurfaceVariant = Mist,
 surfaceContainerLowest = Color(0xFFFFFDF9),
 surfaceContainerLow = Color(0xFFF6F3EA),
 surfaceContainer = Color(0xFFEEEDE4),
 surfaceContainerHigh = Color(0xFFE7EDDF),
 surfaceContainerHighest = Color(0xFFDEE6D8),
 outline = Color(0xFFB7C2B5),
 outlineVariant = Color(0xFFD4DDD0),
 error = Color(0xFFB23B32),
 onError = Color(0xFFFFF7F6),
 errorContainer = Color(0xFFF8D6D2),
 onErrorContainer = Color(0xFF5C1814),
 inverseSurface = Color(0xFF2A332C),
 inverseOnSurface = Color(0xFFEEF3EA),
 inversePrimary = Color(0xFFB5CBB8),
 scrim = Color(0xFF1A211C)
)

private val DarkScheme: ColorScheme = darkColorScheme(
 primary = Color(0xFFB5CBB8),
 onPrimary = Color(0xFF223328),
 primaryContainer = Color(0xFF394B3E),
 onPrimaryContainer = Color(0xFFDCE8DC),
 secondary = Apricot,
 onSecondary = Color(0xFF3A2A20),
 secondaryContainer = Color(0xFF554236),
 onSecondaryContainer = Color(0xFFF3DFCE),
 tertiary = Color(0xFFAEC8CA),
 onTertiary = Color(0xFF1C2E30),
 tertiaryContainer = Color(0xFF3A4F51),
 onTertiaryContainer = Color(0xFFD7E6E7),
 background = Color(0xFF1C231E),
 onBackground = Color(0xFFE6EEE4),
 surface = Color(0xFF242C26),
 onSurface = Color(0xFFE6EEE4),
 surfaceVariant = Color(0xFF354035),
 onSurfaceVariant = Color(0xFFC2CDC3),
 surfaceContainerLowest = Color(0xFF171E19),
 surfaceContainerLow = Color(0xFF222A24),
 surfaceContainer = Color(0xFF283028),
 surfaceContainerHigh = Color(0xFF303A31),
 surfaceContainerHighest = Color(0xFF354035),
 outline = Color(0xFF8A968B),
 outlineVariant = Color(0xFF3E4A40),
 error = Color(0xFFE6A39C),
 onError = Color(0xFF4A1613),
 errorContainer = Color(0xFF7A2C27),
 onErrorContainer = Color(0xFFF8D6D2),
 inverseSurface = Color(0xFFE6EEE4),
 inverseOnSurface = Color(0xFF2A332C),
 inversePrimary = SageDeep,
 scrim = Color(0xFF000000)
)

private val CompanionTypography = Typography(
 headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
 headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
 headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
 titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
 titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
 titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
 bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
 bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
 bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
 labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
 labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
 labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
)

@Composable fun RongbanTheme(theme: String = "system", content: @Composable () -> Unit) {
 val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
 MaterialTheme(
  colorScheme = if (dark) DarkScheme else LightScheme,
  typography = CompanionTypography,
  shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
  content = content
 )
}

@Composable fun CompanionHeader(title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}) {
 Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
  Row(verticalAlignment = Alignment.CenterVertically) {
   Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
   action?.let { androidx.compose.material3.TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(it) } }
  }
  Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable fun CompanionEmpty(title: String, body: String, showPet: Boolean = true) {
 Column(Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  if (showPet) Pet(Modifier.size(120.dp), "happy") else Text("✦", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
  Spacer(Modifier.height(12.dp))
  Text(title, style = MaterialTheme.typography.titleMedium)
  Text(body, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable fun CompanionPanel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
 val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
 val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
 Card(
  modifier = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick) else modifier.fillMaxWidth(),
  colors = colors,
  elevation = elevation,
  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
  content = { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
 )
}

@Composable fun CompanionNote(title: String, body: String, onClick: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
 CompanionPanel(onClick = onClick) {
  Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
  Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = actions)
 }
}

@Composable fun StatusMark(text: String, quiet: Boolean = false) {
 val scheme = MaterialTheme.colorScheme
 val bg = if (quiet) scheme.surfaceContainerHighest else scheme.primaryContainer
 val fg = if (quiet) scheme.onSurfaceVariant else scheme.onPrimaryContainer
 Surface(color = bg, contentColor = fg, shape = CircleShape) {
  Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
 }
}

@Composable fun ChatBubble(role: String, state: String, content: String) {
 val user = role == "user"
 val scheme = MaterialTheme.colorScheme
 val color = when {
  user -> scheme.primaryContainer
  state == "error" -> scheme.errorContainer
  else -> scheme.surfaceContainerLow
 }
 val label = when {
  user -> "你"
  state == "cancelled" -> "绒伴 · 已停止"
  state == "error" -> "绒伴 · 请求未完成"
  else -> "绒伴"
 }
 Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
  Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
  Surface(
   shape = RoundedCornerShape(if (user) 22.dp else 20.dp, 22.dp, if (user) 8.dp else 22.dp, if (user) 22.dp else 8.dp),
   color = color,
   tonalElevation = if (user) 0.dp else 1.dp,
   modifier = Modifier.padding(top = 5.dp).widthIn(max = 320.dp)
  ) { Text(content.ifBlank { "正在想一想……" }, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.bodyLarge) }
 }
}

@Composable fun CardPreviewFrame(bitmap: android.graphics.Bitmap?, placeholder: String) {
 Surface(
  modifier = Modifier.fillMaxWidth(),
  shape = RoundedCornerShape(24.dp),
  color = MaterialTheme.colorScheme.surfaceContainer,
  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
 ) {
  if (bitmap != null) {
   Image(
    bitmap.asImageBitmap(),
    "导出图片预览",
    Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(24.dp)),
    contentScale = ContentScale.Fit
   )
  } else {
   Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
    Text(placeholder, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
   }
  }
 }
}

@Composable fun WatermarkBar(name: String, number: String) {
 if (name.isBlank() || number.isBlank()) return
 Text(
  "$name  $number",
  Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
  style = MaterialTheme.typography.labelSmall,
  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
 )
}

@Composable fun Pet(modifier: Modifier = Modifier, state: String = "idle") {
 val transition = rememberInfiniteTransition(label = "breathing")
 val breath by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if (state == "thinking") 700 else 2400), RepeatMode.Reverse), label = "breath")
 Box(modifier) {
  Canvas(Modifier.fillMaxSize()) {
   val w = size.width; val h = size.height; val b = breath * h * .015f
   drawOval(Color(0xFFD8DED1).copy(alpha = .45f), Offset(w * .18f, h * .82f), Size(w * .64f, h * .10f))
   val fur = Color(0xFFFFF8E9)
   val body = Path().apply {
    moveTo(w * .23f, h * .42f + b)
    cubicTo(w * .12f, h * .01f, w * .34f, h * .08f, w * .39f, h * .28f)
    cubicTo(w * .47f, h * .24f, w * .56f, h * .24f, w * .62f, h * .28f)
    cubicTo(w * .71f, h * .06f, w * .89f, h * .02f, w * .79f, h * .44f + b)
    cubicTo(w * .94f, h * .80f, w * .70f, h * .87f, w * .50f, h * .86f)
    cubicTo(w * .25f, h * .87f, w * .07f, h * .77f, w * .23f, h * .42f + b)
    close()
   }
   drawPath(body, fur); drawPath(body, Color(0xFFE0D4BF), style = Stroke(w * .006f))
   drawOval(Apricot.copy(alpha = .65f), Offset(w * .28f, h * .55f + b), Size(w * .1f, h * .05f))
   drawOval(Apricot.copy(alpha = .65f), Offset(w * .62f, h * .55f + b), Size(w * .1f, h * .05f))
   val ink = Color(0xFF495444)
   if (state == "sleep" || state == "happy") {
    drawLine(ink, Offset(w * .34f, h * .48f + b), Offset(w * .41f, h * .48f + b), w * .015f)
    drawLine(ink, Offset(w * .60f, h * .48f + b), Offset(w * .67f, h * .48f + b), w * .015f)
   } else {
    drawOval(ink, Offset(w * .36f, h * .45f + b), Size(w * .04f, h * .075f))
    drawOval(ink, Offset(w * .60f, h * .45f + b), Size(w * .04f, h * .075f))
   }
   drawOval(Apricot, Offset(w * .48f, h * .55f + b), Size(w * .04f, h * .025f))
   if (state == "speaking" || state == "listening") drawOval(ink, Offset(w * .475f, h * .59f), Size(w * .05f, h * (.025f + .025f * breath)))
   else drawArc(ink, 0f, 180f, false, Offset(w * .46f, h * .565f + b), Size(w * .08f, h * .065f), style = Stroke(w * .008f))
   drawOval(fur, Offset(w * .26f, h * .77f), Size(w * .19f, h * .09f))
   drawOval(fur, Offset(w * .55f, h * .77f), Size(w * .19f, h * .09f))
   if (state == "thinking") drawCircle(Sage.copy(alpha = .3f + breath * .4f), w * .025f, Offset(w * .83f, h * .22f))
  }
  if (state == "thinking") {
   val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.thinking))
   LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.align(Alignment.TopEnd).size(56.dp))
  }
 }
}
