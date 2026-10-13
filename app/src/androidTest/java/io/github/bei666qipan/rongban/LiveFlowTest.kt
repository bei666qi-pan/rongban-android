package io.github.bei666qipan.rongban

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bei666qipan.rongban.core.CredentialStore
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Optional real-provider UI test. CI skips it without a locally provisioned key. */
@RunWith(AndroidJUnit4::class) class LiveFlowTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun screenshot(name:String) {
  val i=InstrumentationRegistry.getInstrumentation();val dir=File(i.targetContext.getExternalFilesDir(null),"evidence").apply {mkdirs()}
  i.uiAutomation.takeScreenshot()?.let {bitmap->File(dir,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 }
 @Test fun contextualConversationAndIndependentCardShare() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  assumeTrue(CredentialStore(context).get("mimo").isNotBlank())
  ui.waitUntil(30000) {ui.onAllNodesWithText("今天，也陪着你。").fetchSemanticsNodes().isNotEmpty()}
  screenshot("home")
  ui.onNodeWithText("写点什么").performClick()
  ui.onNodeWithTag("chat-input").performTextInput("记住，我喜欢喝茶，也喜欢猫咪。")
  ui.onNodeWithText("发送").performClick()
  ui.waitUntil(15000) {ui.onAllNodesWithText("记住了：",substring=true).fetchSemanticsNodes().isNotEmpty() || ui.onAllNodesWithText("已经记得",substring=true).fetchSemanticsNodes().isNotEmpty()}
  screenshot("local-command")
  ui.onNodeWithText("‹ 返回").performClick()
  ui.onAllNodesWithText("新的相遇").onFirst().performClick()
  ui.onNodeWithTag("chat-input").performTextInput("根据你记得的我的偏好，给我一个下午休息的建议。请用一句话回答。")
  ui.onNodeWithText("发送").performClick()
  ui.waitUntil(90000) {ui.onAllNodesWithText("朗读").fetchSemanticsNodes().isNotEmpty() && ui.onAllNodesWithText("绒伴正在回应",substring=true).fetchSemanticsNodes().isEmpty()}
  ui.onAllNodesWithText("绒伴 · 请求未完成").assertCountEquals(0)
  screenshot("context-chat")
  ui.onAllNodesWithText("做成卡片").onLast().performClick()
  ui.waitUntil(15000) {ui.onAllNodesWithText("卡片工作室").fetchSemanticsNodes().isNotEmpty()}
  ui.onAllNodesWithText("这一段相遇").onFirst().performClick()
  ui.waitUntil(15000) {ui.onAllNodesWithText("卡片预览").fetchSemanticsNodes().isNotEmpty()}
  ui.waitUntil(15000) {ui.onAllNodesWithContentDescription("导出图片预览").fetchSemanticsNodes().isNotEmpty()}
  screenshot("card-preview")
  ui.onNodeWithText("分享到其他 App").performScrollTo().performClick()
  Thread.sleep(2000)
  screenshot("system-share")
 }
}
