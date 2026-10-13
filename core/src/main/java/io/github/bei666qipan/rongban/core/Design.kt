package io.github.bei666qipan.rongban.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.airbnb.lottie.compose.*

val Sage = Color(0xFF718875)
val Cream = Color(0xFFFAF7EF)
val Apricot = Color(0xFFE4AB84)
@Composable fun RongbanTheme(theme: String = "system", content: @Composable () -> Unit) {
 val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
 val scheme = if(dark) darkColorScheme(primary=Color(0xFFB5CBB8), secondary=Apricot, background=Color(0xFF202720), surface=Color(0xFF283028), surfaceVariant=Color(0xFF354035)) else lightColorScheme(primary=Sage, secondary=Color(0xFFAF714D), background=Cream, surface=Color(0xFFFFFCF6), surfaceVariant=Color(0xFFE6ECE2), onSurface=Color(0xFF343F35))
 MaterialTheme(colorScheme=scheme, shapes=Shapes(small=RoundedCornerShape(14.dp),medium=RoundedCornerShape(24.dp),large=RoundedCornerShape(32.dp)), content=content)
}
@Composable fun Pet(modifier: Modifier = Modifier, state: String = "idle") {
 val transition=rememberInfiniteTransition(label="breathing")
 val breath by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if(state=="thinking") 700 else 2400),RepeatMode.Reverse),label="breath")
 Box(modifier) {
 Canvas(Modifier.fillMaxSize()) {
  val w=size.width;val h=size.height; val b=breath*h*.015f
  drawOval(Color(0xFFD8DED1).copy(alpha=.45f),Offset(w*.18f,h*.82f),Size(w*.64f,h*.10f))
  val fur=Color(0xFFFFF8E9)
  val body=Path().apply { moveTo(w*.23f,h*.42f+b); cubicTo(w*.12f,h*.01f,w*.34f,h*.08f,w*.39f,h*.28f); cubicTo(w*.47f,h*.24f,w*.56f,h*.24f,w*.62f,h*.28f); cubicTo(w*.71f,h*.06f,w*.89f,h*.02f,w*.79f,h*.44f+b); cubicTo(w*.94f,h*.80f,w*.70f,h*.87f,w*.50f,h*.86f); cubicTo(w*.25f,h*.87f,w*.07f,h*.77f,w*.23f,h*.42f+b);close() }
  drawPath(body,fur);drawPath(body,Color(0xFFE0D4BF),style=Stroke(w*.006f))
  drawOval(Apricot.copy(alpha=.65f),Offset(w*.28f,h*.55f+b),Size(w*.1f,h*.05f));drawOval(Apricot.copy(alpha=.65f),Offset(w*.62f,h*.55f+b),Size(w*.1f,h*.05f))
  val ink=Color(0xFF495444)
  if(state=="sleep" || state=="happy") {
   drawLine(ink,Offset(w*.34f,h*.48f+b),Offset(w*.41f,h*.48f+b),w*.015f);drawLine(ink,Offset(w*.60f,h*.48f+b),Offset(w*.67f,h*.48f+b),w*.015f)
  } else {
   drawOval(ink,Offset(w*.36f,h*.45f+b),Size(w*.04f,h*.075f));drawOval(ink,Offset(w*.60f,h*.45f+b),Size(w*.04f,h*.075f))
  }
  drawOval(Apricot,Offset(w*.48f,h*.55f+b),Size(w*.04f,h*.025f))
  if(state=="speaking" || state=="listening") drawOval(ink,Offset(w*.475f,h*.59f),Size(w*.05f,h*(.025f+.025f*breath)))
  else drawArc(ink,0f,180f,false,Offset(w*.46f,h*.565f+b),Size(w*.08f,h*.065f),style=Stroke(w*.008f))
  drawOval(fur,Offset(w*.26f,h*.77f),Size(w*.19f,h*.09f));drawOval(fur,Offset(w*.55f,h*.77f),Size(w*.19f,h*.09f))
  if(state=="thinking") drawCircle(Sage.copy(alpha=.3f+breath*.4f),w*.025f,Offset(w*.83f,h*.22f))
 }
 if(state=="thinking") { val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.thinking));LottieAnimation(composition,iterations=LottieConstants.IterateForever,modifier=Modifier.align(Alignment.TopEnd).size(56.dp)) }
 }
}
