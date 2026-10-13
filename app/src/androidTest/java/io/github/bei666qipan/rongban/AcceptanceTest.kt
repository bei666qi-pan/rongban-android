package io.github.bei666qipan.rongban

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.k2fsa.sherpa.onnx.*
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class AcceptanceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun nativeChineseSpeechRoundtripAndCardPagination()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val b="models/tts"
  val tts=OfflineTts(context.assets,OfflineTtsConfig(model=OfflineTtsModelConfig(vits=OfflineTtsVitsModelConfig(model="$b/model.onnx",lexicon="$b/lexicon.txt",tokens="$b/tokens.txt"),numThreads=2),ruleFsts="$b/phone.fst,$b/date.fst,$b/number.fst"))
  val audio=try {tts.generate("记住我喜欢喝茶。",sid=21,speed=1f)} finally {tts.release()}
  assertTrue(audio.samples.isNotEmpty())
  val count=(audio.samples.size.toLong()*16000/audio.sampleRate).toInt()
  val pcm=FloatArray(count) {i->val pos=i*audio.sampleRate/16000.0;val a=pos.toInt().coerceIn(0,audio.samples.lastIndex);val next=(a+1).coerceAtMost(audio.samples.lastIndex);(audio.samples[a]+(audio.samples[next]-audio.samples[a])*(pos-a)).toFloat()}
  val result=SpeechEngine(context).recognizePcm(pcm)
  android.util.Log.i("RongbanAcceptance","Offline transcript: $result")
  assertTrue(result,result.contains("喜欢"));assertTrue(result,CommandParser.parse(result) is Command.Remember)
  val card=CardDraft(title="本地分页测试",body="这是一条保留中文与长文的真实测试内容。\n".repeat(100))
  val pages=CardRenderer.pageRanges(card.body);assertTrue(pages.size>1)
  assertEquals(card.body,pages.joinToString("") {r->card.body.substring(r.first,r.last+1)})
  val files=CardRenderer.export(context,card,Preferences())
  assertEquals(pages.size,files.size);assertTrue(files.all {it.length()>10000})
  files.forEach {it.delete()}
 }
 @Test fun recognizerInitializationDoesNotExitProcess()=runBlocking {val c=InstrumentationRegistry.getInstrumentation().targetContext;assertEquals("",SpeechEngine(c).recognizePcm(FloatArray(16000)))}
 @Test fun memoryPersistsAcrossDatabaseReopen()=runBlocking {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val name="acceptance-${newId()}.db"
  val m=Memory(title="持久化验证",content="我喜欢抹茶")
  val first=Room.databaseBuilder(c,RongbanDatabase::class.java,name).build();first.dao().put(m);first.close()
  val second=Room.databaseBuilder(c,RongbanDatabase::class.java,name).build()
  try {assertEquals(m.content,second.dao().memoryList().single().content)} finally {second.close();c.deleteDatabase(name)}
 }
 @Test fun homeNavigationAndManualMemoryFlow() {
  ui.waitUntil(30000) {ui.onAllNodesWithText("今天，也陪着你。").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("记忆",useUnmergedTree=true).performClick()
  ui.onNodeWithText("添一条").performClick()
  val token="验收记忆-"+System.currentTimeMillis()
  ui.onNodeWithTag("memory-body").performTextInput("主流程 $token")
  ui.onAllNodesWithText("保存").onLast().performClick()
  ui.waitUntil(10000) {ui.onAllNodesWithText("主流程 $token").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("主流程 $token").assertExists()
 }
}
