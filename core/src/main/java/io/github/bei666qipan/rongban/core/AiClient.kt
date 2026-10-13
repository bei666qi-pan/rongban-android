// Text-only protocol adapters adapted from RikkaHub 0966502 (AGPL-3.0).
// The upstream stream decoders are reduced to text events because Rongban has no agent/tool loop.
package io.github.bei666qipan.rongban.core

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class AiClient @Inject constructor() {
 private val client=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).callTimeout(120,TimeUnit.SECONDS).build()
 fun stream(config:ProviderConfig,key:String,messages:List<Pair<String,String>>):Flow<String> = callbackFlow {
  require(key.isNotBlank()) {"请先在「我的」页面填写 API Key"}
  val request=ProtocolAdapter.request(config,key,messages)
  val source=EventSources.createFactory(client).newEventSource(request,object:EventSourceListener() {
   override fun onEvent(eventSource:EventSource,id:String?,type:String?,data:String) {
    if(data=="[DONE]") {close();return}
    try {val text=ProtocolAdapter.decode(config.protocol,type,data);if(text.isNotEmpty()) trySend(text)} catch(e:Exception) {close(IllegalStateException("模型响应格式无法识别，请检查协议配置"))}
   }
   override fun onClosed(eventSource:EventSource) {close()}
   override fun onFailure(eventSource:EventSource,t:Throwable?,response:Response?) {
    val code=response?.code
    close(IllegalStateException(when(code) {401,403->"连接未授权（HTTP $code），请检查 API Key 或服务权限";429->"请求过于频繁或额度不足，请稍后重试";null->"连接超时或网络不可用，本地技能仍可使用";else->"模型服务暂时不可用（HTTP $code）"}))
   }
  });awaitClose {source.cancel()}
 }
 suspend fun complete(config:ProviderConfig,key:String,messages:List<Pair<String,String>>):String=stream(config,key,messages).toList().joinToString("")
}
object ProtocolAdapter {
 private val json=Json {ignoreUnknownKeys=true}
 fun request(c:ProviderConfig,key:String,messages:List<Pair<String,String>>):Request {
  require(c.baseUrl.startsWith("https://") || c.baseUrl.startsWith("http://127.0.0.1") || c.baseUrl.startsWith("http://localhost")) {"请填写完整的 HTTPS Base URL"}
  require(c.model.isNotBlank()) {"请填写模型名称"}
  val base=c.baseUrl.trimEnd('/')
  val system=messages.filter {it.first=="system"}.joinToString("\n") {it.second}
  val history=messages.filter {it.first!="system"}
  val builder=Request.Builder().header("Content-Type","application/json")
  val body:JsonObject
  when(c.protocol) {
   "anthropic"->{builder.url("$base/messages").header("x-api-key",key).header("anthropic-version","2023-06-01");body=buildJsonObject {put("model",c.model);put("stream",true);put("max_tokens",1024);put("system",system);put("messages",JsonArray(history.map {(role,text)->buildJsonObject {put("role",role);put("content",text)}}))}}
   "gemini"->{builder.url("$base/models/${c.model}:streamGenerateContent?alt=sse").header("x-goog-api-key",key);body=buildJsonObject {put("systemInstruction",buildJsonObject {put("parts",buildJsonArray {add(buildJsonObject {put("text",system)})})});put("contents",JsonArray(history.map {(role,text)->buildJsonObject {put("role",if(role=="assistant") "model" else "user");put("parts",buildJsonArray {add(buildJsonObject {put("text",text)})})}}));put("generationConfig",buildJsonObject {put("maxOutputTokens",1024)})}}
   else->{builder.url("$base/chat/completions").header("Authorization","Bearer $key");body=buildJsonObject {put("model",c.model);put("stream",true);if(c.model.startsWith("mimo-")) put("max_completion_tokens",1024) else put("max_tokens",1024);put("messages",JsonArray(messages.map {(role,text)->buildJsonObject {put("role",role);put("content",text)}}));if(c.model.startsWith("mimo-")) put("thinking",buildJsonObject {put("type","disabled")})}}
  }
  return builder.post(body.toString().toRequestBody("application/json".toMediaType())).build()
 }
 fun decode(protocol:String,type:String?,data:String):String {
  if(data.isBlank() || data=="[DONE]") return ""
  val obj=json.parseToJsonElement(data).jsonObject
  if(obj["error"]!=null) error("Provider error")
  return when(protocol) {
   "anthropic"->if(type=="content_block_delta" || obj["type"]?.jsonPrimitive?.content=="content_block_delta") obj["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty() else ""
   "gemini"->obj["candidates"]?.jsonArray?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.joinToString("") {part->if(part.jsonObject["thought"]?.jsonPrimitive?.booleanOrNull==true) "" else part.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty()}.orEmpty()
   else->obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty()
  }
 }
}
