package io.github.bei666qipan.rongban

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.github.bei666qipan.rongban.cards.CardActivity
import io.github.bei666qipan.rongban.core.RongbanDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/** Native accessibility operations preserve normal app clocks and animations in recordings. */
@RunWith(AndroidJUnit4::class)
class DemonstrationTest {
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private val device get()=UiDevice.getInstance(instrumentation)
 private var scenario:ActivityScenario<out ComponentActivity>?=null
 private val fast get()=InstrumentationRegistry.getArguments().getString("fast")=="true"
 @After fun close() {scenario?.close()}
 private fun pause(seconds:Long=5) {Thread.sleep(if(fast) 500 else seconds*1000)}
 private fun scroll(up:Boolean) {val w=device.displayWidth;val h=device.displayHeight;device.swipe(w-12,if(up) h*8/10 else h*3/10,w-12,if(up) h*3/10 else h*8/10,20);Thread.sleep(300)}
 private fun find(selector:BySelector):UiObject2 {
  device.findObject(selector)?.takeIf {it.visibleBounds.height()>0}?.let {return it}
  for(direction in listOf(true,false)) repeat(if(direction) 7 else 14) {scroll(direction);device.findObject(selector)?.takeIf {it.visibleBounds.height()>0}?.let {return it}}
  return device.wait(Until.findObject(selector),10000) ?: error("Visible UI control unavailable: $selector")
 }
 private fun click(text:String) {find(By.text(text)).click();Thread.sleep(300)}
 private fun field(id:String,value:String) {find(By.res(id)).text=value;Thread.sleep(300)}
 private fun hideKeyboard() {if(device.hasObject(By.pkg("com.google.android.inputmethod.latin"))) device.pressBack()}
 private fun shot(name:String) {val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"evidence").apply {mkdirs()};instrumentation.uiAutomation.takeScreenshot()?.let {b->File(dir,"$name.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}}
 private fun send(text:String) {field("chat-input",text);click("发送");hideKeyboard();pause(3)}
 private fun finish(start:Long) {if(!fast) {val remaining=165000-(System.currentTimeMillis()-start);if(remaining>0) Thread.sleep(remaining)}}
 @Test fun mainAppWalkthrough() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("demo")=="true")
  val start=System.currentTimeMillis();val c=instrumentation.targetContext
  runBlocking {val db=Room.databaseBuilder(c,RongbanDatabase::class.java,"rongban.db").build();try {db.dao().memoryList().filter {it.content.startsWith("主流程 验收记忆-")}.forEach {db.dao().deleteMemory(it.id)}} finally {db.close()}}
  scenario=ActivityScenario.launch(MainActivity::class.java)
  check(device.wait(Until.hasObject(By.text("今天，也陪着你。")),30000))
  shot("home");pause(6);click("有点累");pause(4);click("写点什么")
  send("记住");pause(4);send("我喜欢喝茶，也喜欢猫咪。");pause(5)
  send("回忆喝茶");pause(5);shot("offline-recall");click("朗读");pause(10)
  send("开始专注一分钟");pause(4);click("‹ 返回");click("记忆");pause(5);shot("memories")
  click("我喜欢喝茶，也喜欢猫咪");pause(5);click("取消")
  click("技能");pause(5);shot("skills");click("回忆近况");pause(5);click("使用一次");pause(5);click("‹ 返回")
  click("新的相遇");send("根据你记得的我的偏好，给我一个下午休息的建议。请用一句话回答。")
  check(device.wait(Until.hasObject(By.text("朗读")),90000))
  pause(5);shot("context-chat");click("‹ 返回");click("我的");pause(4);shot("settings")
  click("深色");pause(5);shot("dark-theme");click("浅色");pause(3);click("陪伴");pause(5);finish(start)
 }
 @Test fun independentCardsWalkthrough() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("demo")=="true")
  val start=System.currentTimeMillis();val c=instrumentation.targetContext
  scenario=ActivityScenario.launch(CardActivity::class.java)
  check(device.wait(Until.hasObject(By.text("卡片工作室")),30000));shot("card-studio");pause(7)
  click("＋ 从记忆或对话创建");pause(5);click("记忆 · 我喜欢喝茶，也喜欢猫咪")
  check(device.wait(Until.hasObject(By.desc("导出图片预览")),20000));pause(7);shot("card-preview")
  click("今日回顾");pause(5);click("记忆便签");pause(5)
  field("card-title","给未来的自己");hideKeyboard();pause(4)
  field("card-body","忙碌的时候，也记得给自己一杯茶的时间。\n今天的每一点努力，都值得留下。");hideKeyboard();pause(5)
  click("保存草稿");Thread.sleep(4500);click("保存图片")
  val save=device.wait(Until.findObject(By.text(Pattern.compile("(?i)save|保存")).enabled(true)),20000) ?: error("Document save action unavailable")
  shot("save-dialog");pause(5);save.click()
  check(device.wait(Until.hasObject(By.pkg(c.packageName)),20000));pause(6);shot("saved-card")
  click("分享到其他 App");check(device.wait(Until.hasObject(By.pkg(Pattern.compile("android|com.android.intentresolver"))),20000));pause(7);shot("system-share");device.pressBack()
  check(device.wait(Until.hasObject(By.pkg(c.packageName)),15000));pause(3)
  field("card-body",(1..65).joinToString("\n") {"第 $it 个小瞬间：停下来，认真看看身边的生活。"});hideKeyboard();pause(5)
  click("下一页");pause(6);shot("card-pagination");click("保存图片")
  check(device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")),20000));pause(5);device.pressBack();pause(5)
  click("‹ 工作室");pause(7);finish(start)
 }
}
