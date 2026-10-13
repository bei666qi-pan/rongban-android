package io.github.bei666qipan.rongban.core
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.SocketPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.toList
class ProtocolTest {
 @Test fun cancellationClosesCollectorAndTimeoutRemainsActionable()=runBlocking {
  val server=MockWebServer();server.start()
  try {
   val config=ProviderConfig(baseUrl=server.url("/v1").toString())
   server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
   val job=launch {AiClient().complete(config,"test",listOf("user" to "hello"))}
   withContext(Dispatchers.IO) {check(server.takeRequest(3,TimeUnit.SECONDS)!=null)}
   withTimeout(2000) {job.cancelAndJoin()};assertTrue(job.isCancelled)
   server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
   val client=OkHttpClient.Builder().readTimeout(150,TimeUnit.MILLISECONDS).callTimeout(300,TimeUnit.MILLISECONDS).build()
   val error=runCatching {AiClient(client).complete(config,"test",listOf("user" to "hello"))}.exceptionOrNull()
   assertTrue(error?.message.orEmpty().contains("本地技能仍可使用"))
  } finally {server.shutdown()}
 }
 @Test fun allProtocolsExtractOnlyVisibleText() {
  assertEquals("你好",ProtocolAdapter.decode("openai",null,"""{"choices":[{"delta":{"content":"你好","reasoning_content":"private"}}]}"""))
  assertEquals("欢迎",ProtocolAdapter.decode("anthropic","content_block_delta","""{"delta":{"type":"text_delta","text":"欢迎"}}"""))
  assertEquals("你好",ProtocolAdapter.decode("gemini",null,"""{"candidates":[{"content":{"parts":[{"text":"secret","thought":true},{"text":"你好"}]}}]}"""))
 }
 @Test fun providerSpecificHeadersAndBodies() {
  val messages=listOf("system" to "绒伴","user" to "你好")
  val a=ProtocolAdapter.request(ProviderConfig(protocol="anthropic",baseUrl="https://example.test/v1"),"test",messages)
  assertEquals("test",a.header("x-api-key"));assertTrue(a.url.toString().endsWith("/v1/messages"))
  val g=ProtocolAdapter.request(ProviderConfig(protocol="gemini",baseUrl="https://example.test/v1beta"),"test",messages)
  assertEquals("test",g.header("x-goog-api-key"));assertTrue(g.url.toString().contains("streamGenerateContent"))
 }
 @Test fun streamingAndAuthenticationFailureAreObservable()=runBlocking {
  val server=MockWebServer();server.start()
  try {
   val c=ProviderConfig(baseUrl=server.url("/v1").toString())
   server.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\ndata: [DONE]\n\n"))
   assertEquals("你好",AiClient().stream(c,"test",listOf("user" to "hello")).toList().joinToString(""))
   server.enqueue(MockResponse().setResponseCode(401))
   val e=runCatching {AiClient().complete(c,"test",listOf("user" to "hello"))}.exceptionOrNull()
   assertTrue(e?.message.orEmpty().contains("未授权"))
  } finally {server.shutdown()}
 }
}
