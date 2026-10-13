package io.github.bei666qipan.rongban

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import io.github.bei666qipan.rongban.core.RongbanDatabase
import io.github.bei666qipan.rongban.cards.CardActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Explicitly invoked, paced walkthroughs for genuine device screen recordings. */
@RunWith(AndroidJUnit4::class)
class DemonstrationTest {
 @get:Rule val ui=createEmptyComposeRule()
 private var scenario:ActivityScenario<out ComponentActivity>?=null
 @After fun closeActivity() {scenario?.close()}
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private val device get()=UiDevice.getInstance(instrumentation)
 private val fast get()=InstrumentationRegistry.getArguments().getString("fast")=="true"
 private fun pause(seconds:Long=5) {Thread.sleep(if(fast) 500 else seconds*1000)}
 private fun click(text:String) {
  ui.waitForIdle()
  if(text in listOf("陪伴","对话","记忆","技能","我的")) ui.onNodeWithText(text,useUnmergedTree=true).performClick()
  else {val node=ui.onAllNodesWithText(text).onFirst();runCatching {node.performScrollTo()};node.assertIsDisplayed().performClick()}
  ui.waitForIdle()
 }
 private fun shot(name:String) {val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"evidence").apply {mkdirs()};instrumentation.uiAutomation.takeScreenshot()?.let {b->File(dir,"$name.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}}
 private fun hideKeyboard() {if(device.hasObject(By.pkg("com.google.android.inputmethod.latin"))) device.pressBack()}
 private fun send(text:String) {ui.onNodeWithTag("chat-input").performTextReplacement(text);click("发送");hideKeyboard();pause(3)}
 private fun finishAt(start:Long) {if(fast) return;val remaining=168000-(System.currentTimeMillis()-start);if(remaining>0) Thread.sleep(remaining)}
 @Test fun mainAppWalkthrough() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("demo")=="true")
  scenario=ActivityScenario.launch(MainActivity::class.java)
  val start=System.currentTimeMillis()
  runBlocking {
   val c=instrumentation.targetContext;val db=Room.databaseBuilder(c,RongbanDatabase::class.java,"rongban.db").build()
   try {db.dao().memoryList().filter {it.content.startsWith("主流程 验收记忆-")}.forEach {db.dao().deleteMemory(it.id)}} finally {db.close()}
  }
  ui.waitUntil(30000) {ui.onAllNodesWithText("今天，也陪着你。").fetchSemanticsNodes().isNotEmpty()}
  shot("home");pause(7);click("有点累");pause();click("写点什么")
  send("记住");ui.onNodeWithText("你想让我记住什么？").assertExists();pause()
  send("我喜欢喝茶，也喜欢猫咪。");pause(7)
  send("回忆喝茶");pause(7);shot("offline-recall")
  click("朗读");pause(12)
  send("开始专注一分钟");pause();click("‹ 返回");pause(1);click("记忆");ui.waitUntil(10000) {ui.onAllNodesWithText("记忆小屋").fetchSemanticsNodes().isNotEmpty()};pause(7);shot("memories")
  ui.onAllNodesWithText("我喜欢喝茶，也喜欢猫咪").onFirst().performClick();pause(7);click("取消")
  click("技能");pause(7);shot("skills");click("回忆近况");pause(7);click("使用一次");pause(7);click("‹ 返回")
  click("新的相遇");send("根据你记得的我的偏好，给我一个下午休息的建议。请用一句话回答。")
  ui.waitUntil(90000) {ui.onAllNodesWithText("朗读").fetchSemanticsNodes().isNotEmpty() && ui.onAllNodesWithText("绒伴正在回应",substring=true).fetchSemanticsNodes().isEmpty()}
  pause(7);shot("context-chat");click("‹ 返回");click("我的");pause();shot("settings")
  click("深色");pause(7);shot("dark-theme");click("浅色");pause(4);click("陪伴");pause();finishAt(start)
 }
 @Test fun independentCardsWalkthrough() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("demo")=="true")
  val start=System.currentTimeMillis()
  val c=instrumentation.targetContext
  scenario=ActivityScenario.launch(CardActivity::class.java)
  ui.waitUntil(30000) {ui.onAllNodesWithText("卡片工作室").fetchSemanticsNodes().isNotEmpty()}
  shot("card-studio");pause(8);click("＋ 从记忆或对话创建");pause(7)
  ui.onAllNodesWithText("记忆 · 我喜欢喝茶，也喜欢猫咪").onFirst().performClick()
  ui.waitUntil(15000) {ui.onAllNodesWithContentDescription("导出图片预览").fetchSemanticsNodes().isNotEmpty()}
  pause(8);shot("card-preview");click("今日回顾");pause(7);click("记忆便签");pause(7)
  ui.onNodeWithTag("card-title").performScrollTo().performTextReplacement("给未来的自己")
  hideKeyboard();pause(5)
  ui.onNodeWithTag("card-body").performScrollTo().performTextReplacement("忙碌的时候，也记得给自己一杯茶的时间。\n今天的每一点努力，都值得留下。")
  hideKeyboard();pause(6);click("保存草稿");pause(5);click("保存图片");pause(5)
  val save=device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)save|保存"))),20000)
  shot("save-dialog")
  if(save!=null) save.click() else error("System document save action unavailable")
  pause(7);shot("saved-card");click("分享到其他 App");pause(8);shot("system-share");device.pressBack();pause(3)
  ui.onNodeWithTag("card-body").performScrollTo().performTextReplacement((1..65).joinToString("\n") {"第 $it 个小瞬间：停下来，认真看看身边的生活。"})
  hideKeyboard();pause(5)
  ui.onNodeWithText("下一页").performScrollTo().performClick();pause(8);shot("card-pagination")
  click("保存图片");pause(5);device.pressBack();pause(5)
  click("‹ 工作室");pause(8);finishAt(start)
 }
}
