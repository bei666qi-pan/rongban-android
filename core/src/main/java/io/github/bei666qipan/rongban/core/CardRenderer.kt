package io.github.bei666qipan.rongban.core

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate

object CardRenderer {
 private const val WIDTH=1080
 private const val HEIGHT=1440
 private const val BODY_HEIGHT=910
 private fun textPaint(size:Float,color:Int)=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {textSize=size;this.color=color;typeface=Typeface.create("sans-serif",Typeface.NORMAL)}
 private fun layout(text:String,paint:TextPaint)=StaticLayout.Builder.obtain(text,0,text.length,paint,900).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(12f,1f).setIncludePad(false).build()
 fun pageRanges(body:String):List<IntRange> {
  val l=layout(body,textPaint(38f,0xFF344038.toInt()))
  val ranges=mutableListOf<IntRange>();var start=0
  while(start<l.lineCount) {
   var end=start
   while(end+1<l.lineCount && l.getLineBottom(end+1)-l.getLineTop(start)<=BODY_HEIGHT) end++
   ranges+=(l.getLineStart(start) until l.getLineEnd(end));start=end+1
  }
  return ranges.ifEmpty {listOf(0 until 0)}
 }
 fun render(card:CardDraft,p:Preferences,page:Int=0):Bitmap {
  require(card.body.length<=20000) {"卡片内容过长，请先精简到 20000 字以内"}
  val ranges=pageRanges(card.body);require(page in ranges.indices)
  val bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap)
  c.drawColor(0xFFFAF7EF.toInt())
  val accent=when(card.template) {"memory"->0xFFE4AB84.toInt();"review"->0xFF91ADB1.toInt();else->0xFF718875.toInt()}
  val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=accent}
  c.drawRoundRect(72f,64f,1008f,1360f,44f,44f,Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.WHITE})
  c.drawCircle(940f,142f,34f,paint)
  c.drawText("绒伴  /  RONGBAN",100f,145f,textPaint(30f,accent))
  val title=layout(card.title.take(60),textPaint(54f,0xFF344038.toInt()))
  c.save();c.translate(100f,205f);c.clipRect(0,0,880,140);title.draw(c);c.restore()
  val range=ranges[page];val body=if(range.isEmpty()) "" else card.body.substring(range.first,range.last+1)
  val content=layout(body,textPaint(38f,0xFF344038.toInt()))
  c.save();c.translate(90f,370f);content.draw(c);c.restore()
  c.drawLine(100f,1300f,980f,1300f,paint.apply {strokeWidth=2f})
  c.drawText(LocalDate.now().toString()+"  ·  "+when(card.template) {"memory"->"记忆便签";"review"->"今日回顾";else->"对话摘录"},100f,1340f,textPaint(24f,0xFF718875.toInt()))
  if(ranges.size>1) c.drawText("${page+1} / ${ranges.size}",880f,1340f,textPaint(24f,accent))
  if(p.studentName.isNotBlank() && p.studentNumber.isNotBlank()) c.drawText("${p.studentName}  ${p.studentNumber}",100f,1400f,textPaint(22f,0xFF718875.toInt()))
  return bitmap
 }
 fun export(context:Context,card:CardDraft,p:Preferences):List<File> {
  val directory=File(context.cacheDir,"cards").apply {mkdirs()}
  directory.listFiles()?.filter {System.currentTimeMillis()-it.lastModified()>86400000}?.forEach {it.delete()}
  val result=mutableListOf<File>()
  try {for(page in pageRanges(card.body).indices) {
   val file=File(directory,"rongban-${card.id}-${page+1}.png");val bitmap=render(card,p,page)
   try {file.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) {"图片写入失败"}}} finally {bitmap.recycle()}
   result+=file
  };return result} catch(e:Exception) {result.forEach {it.delete()};throw e}
 }
 fun uri(context:Context,file:File)=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
}
