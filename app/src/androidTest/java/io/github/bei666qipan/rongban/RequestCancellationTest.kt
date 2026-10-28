package io.github.bei666qipan.rongban

import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RequestCancellationTest {
 @Test fun immediateRetryDoesNotLetCancelledRequestOverwriteNewReply()=runBlocking {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val settings=SettingsStore(c);val original=settings.flow.first();val credentials=CredentialStore(c)
  val server=MockWebServer();server.start()
  val config=ProviderConfig(id="cancellation-fixture",baseUrl=server.url("/v1").toString(),model="fixture")
  val db=Room.inMemoryDatabaseBuilder(c,RongbanDatabase::class.java).build()
  val vm=AppViewModel(db.dao(),settings,credentials,AiClient(),c)
  try {
   settings.update {it.copy(providers=listOf(config),activeProvider=config.id,memoryEnabled=false)}
   credentials.put(config.id,"non-secret-fixture")
   val first="data: {\"choices\":[{\"delta\":{\"content\":\"旧回复\"}}]}\n\n"
   server.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody(first.repeat(300)).throttleBody(first.toByteArray().size.toLong(),1,TimeUnit.SECONDS))
   server.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody("data: {\"choices\":[{\"delta\":{\"content\":\"新回复完整\"}}]}\n\ndata: [DONE]\n\n"))
   val conversation=Conversation();db.dao().put(conversation)
   withTimeout(5000) {while(!vm.ready.value) delay(20)}
   android.util.Log.i("RongbanRequestFixture","Loopback cleartext permitted: "+android.security.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("localhost"))
   withContext(Dispatchers.Main) {vm.send(conversation.id,"first request")}
   try {withTimeout(15000) {while(db.dao().messageList(conversation.id).none {it.content.contains("旧回复")}) delay(30)}} catch(e:TimeoutCancellationException) {android.util.Log.i("RongbanRequestFixture","First stage: "+db.dao().messageList(conversation.id).map {it.state+":"+it.content}.joinToString()+"; status="+vm.status.value);throw e}
   withContext(Dispatchers.Main) {vm.cancel();vm.send(conversation.id,"second request")}
   withTimeout(15000) {while(vm.busy.value || db.dao().messageList(conversation.id).none {it.content=="新回复完整" && it.state=="complete"} || db.dao().messageList(conversation.id).none {it.content.contains("旧回复") && it.state=="cancelled"}) delay(30)}
   val messages=db.dao().messageList(conversation.id)
   assertEquals("cancelled",messages.first {it.content.contains("旧回复")}.state)
   assertEquals("complete",messages.first {it.content=="新回复完整"}.state)
   assertFalse(vm.busy.value)
  } finally {vm.viewModelScope.cancel();settings.update {original};credentials.put(config.id,"");db.close();server.shutdown()}
 }
}
