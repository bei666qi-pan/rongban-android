package io.github.bei666qipan.rongban

import android.app.*
import android.content.*
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.File
import javax.inject.Inject

@HiltViewModel class AppViewModel @Inject constructor(val dao:RongbanDao,val settings:SettingsStore,val credentials:CredentialStore,private val ai:AiClient,@ApplicationContext private val context:Context):ViewModel() {
 val preferences=settings.flow.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Preferences())
 val conversations=dao.conversations().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val memories=dao.memories().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val skills=dao.skills().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val active=MutableStateFlow<String?>(null)
 val busy=MutableStateFlow(false)
 val speechState=MutableStateFlow("idle")
 val voiceText=MutableStateFlow("")
 val status=MutableStateFlow<String?>(null)
 val ready=MutableStateFlow(false)
 private val speech=SpeechEngine(context)
 private var request:Job?=null
 private var voice:Job?=null
 init {
  viewModelScope.launch {
   for(skill in BuiltinSkills.all) if(dao.skillList().none { it.id==skill.id }) dao.put(skill)
   val file=File(context.filesDir,"credential-once.json")
   if(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0 && file.exists()) {
    try { val obj=Json.parseToJsonElement(file.readText()).jsonObject;credentials.put(obj["id"]!!.jsonPrimitive.content,obj["key"]!!.jsonPrimitive.content) } finally { file.delete() }
   };ready.value=true
  }
 }
 fun notify(text:String) {status.value=text}
 fun updatePreferences(block:(Preferences)->Preferences) {viewModelScope.launch {settings.update(block)}}
 fun createConversation(after:(String)->Unit) {viewModelScope.launch {val c=Conversation();dao.put(c);active.value=c.id;after(c.id)}}
 fun removeConversation(c:Conversation) {viewModelScope.launch {dao.deleteConversation(c.id);if(active.value==c.id) active.value=null}}
 fun saveMemory(m:Memory) {viewModelScope.launch {val same=dao.memoryList().firstOrNull {it.id!=m.id && it.content.trim()==m.content.trim()};if(same!=null) notify("这件事已经在记忆里了") else {dao.put(m.copy(updated=System.currentTimeMillis()));notify("记忆已保存")}}}
 fun forget(m:Memory) {viewModelScope.launch {dao.deleteMemory(m.id);dao.memoryList().filter {it.kind=="summary"}.forEach {dao.deleteMemory(it.id)};notify("这条记忆已移除，后续相遇不再引用")}}
 fun saveSkill(s:Skill) {viewModelScope.launch {dao.put(s);notify("技能已保存")}}
 fun importSkill(content:String) {viewModelScope.launch {runCatching {BuiltinSkills.parse(content)}.onSuccess {dao.put(it);notify("技能已导入")}.onFailure {notify(it.message ?: "技能格式无法识别")}}}
 fun deleteSkill(s:Skill) {viewModelScope.launch {dao.deleteSkill(s.id)}}
 fun saveProvider(config:ProviderConfig,key:String?) {viewModelScope.launch {if(key!=null) credentials.put(config.id,key.trim());settings.update {p->p.copy(providers=p.providers.filterNot {it.id==config.id}+config,activeProvider=config.id)};notify("模型配置已保存")}}
 fun testProvider(config:ProviderConfig,key:String?) {viewModelScope.launch {notify("正在测试连接……");runCatching {ai.complete(config,key ?: credentials.get(config.id),listOf("user" to "请只回复：连接成功。"))}.onSuccess {notify("连接成功：${config.model}")}.onFailure {notify(it.message ?: "连接失败")}}}
 fun cancel() {request?.cancel();busy.value=false;speechState.value="idle";notify("已停止生成")}
 fun stopSpeech() {speech.stop();voice?.cancel();speechState.value="idle"}
 fun listen(id:String) {
  if(speechState.value=="listening") {speech.stop();return}
  stopSpeech();voiceText.value=""
  voice=viewModelScope.launch {
   speechState.value="listening"
   try {val result=speech.listen {voiceText.value=it};if(result.isBlank()) notify("没有听清，可以再试一次或输入文字") else send(id,result)} catch(e:CancellationException) {throw e} catch(e:Exception) {notify(e.message ?: "语音不可用，请用文字输入")} finally {speechState.value="idle"}
  }
 }
 fun speak(text:String) {stopSpeech();voice=viewModelScope.launch {speechState.value="speaking";try {speech.speak(text)} catch(e:CancellationException) {throw e} catch(e:Exception) {notify("朗读暂时不可用，请阅读文字")} finally {speechState.value="idle"}}}
 fun startFocus(minutes:Int,label:String) {
  val ends=System.currentTimeMillis()+minutes*60000L
  updatePreferences {it.copy(focusEnds=ends,focusLabel=label)}
  val pending=PendingIntent.getBroadcast(context,41,Intent(context,FocusReceiver::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,ends,pending)
 }
 fun stopFocus() {updatePreferences {it.copy(focusEnds=0,focusLabel="")};context.getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(context,41,Intent(context,FocusReceiver::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))}
 fun saveCard(title:String,body:String,template:String="conversation") {viewModelScope.launch {dao.put(CardDraft(title=title,body=body,template=template));notify("已存为卡片草稿")}}
 fun send(id:String,text:String,chosenSkill:Skill?=null) {
  if(text.isBlank() || busy.value) return
  request=viewModelScope.launch {
   busy.value=true;speechState.value="thinking"
   try {
    dao.put(Message(conversationId=id,role="user",content=text.trim()))
    val list=dao.messageList(id).filter {it.state=="complete"}
    if(list.count {it.role=="user"}==1) dao.put(Conversation(id=id,title=text.take(22)))
    val command=if(chosenSkill?.executor=="prompt") Command.Chat else CommandParser.parse(text)
    val required=when(command) {is Command.Remember->"remember";is Command.Recall->"recall";is Command.Focus->"focus";Command.Review->"review";Command.Card->"card";Command.Encourage->"encourage";else->null}
    val disabled=required!=null && dao.skillList().none {it.id==required && it.enabled}
    val response=if(disabled) "这个技能已关闭，你可以在技能页重新开启。" else when(command) {
     is Command.Remember->{val same=dao.memoryList().any {it.content.trim()==command.content.trim()};if(!same) dao.put(Memory(title=command.content.take(24),content=command.content,source="明确语音 / 文字指令",sourceConversationId=id));if(same) "这件事我已经记得了。" else "记住了：${command.content}。你随时可以在记忆页修改或移除。"}
     is Command.Recall->{val found=MemoryContext.retrieve(dao.memoryList(),command.query);if(found.isEmpty()) "我还没有可以回忆的内容。试着告诉我：记住，我喜欢……" else "我记得这些：\n"+found.joinToString("\n") {"· ${it.content}"}}
     is Command.Focus->{startFocus(command.minutes,command.label);"好，陪你专注 ${command.minutes} 分钟。开始之前，把手边最重要的一件事放在眼前。"}
     is Command.Missing->command.hint
     Command.Card->{val body=list.filter {it.role=="assistant"}.lastOrNull()?.content ?: "与你相遇的这一刻。";dao.put(CardDraft(title="我们的相遇",body=body,source=id));"卡片草稿已经准备好了。从卡片工作室打开，就能预览和分享。"}
     else -> {
      val pref=settings.flow.first();val config=pref.provider()
      val memoryList=dao.memoryList()
      val instruction=when(command) {Command.Review->"根据用户今天的对话做简短回顾。今天的对话："+dao.recentMessages().filter {it.created>=java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()}.reversed().joinToString("\n") {it.content.take(300)};Command.Encourage->"结合用户心情给出温柔具体的鼓励。";else->chosenSkill?.body.orEmpty()}
      val system=MemoryContext.system(pref,memoryList,text,instruction)
      val responseId=newId();var assembled=""
      dao.put(Message(id=responseId,conversationId=id,role="assistant",content="",state="streaming"))
      ai.stream(config,credentials.get(config.id),listOf("system" to system)+list.takeLast(16).map {it.role to it.content.take(2000)}).collect {delta->assembled+=delta;dao.put(Message(id=responseId,conversationId=id,role="assistant",content=assembled,state="streaming"))}
      check(assembled.isNotBlank()) {"模型没有返回文字，请重试"};dao.put(Message(id=responseId,conversationId=id,role="assistant",content=assembled))
      if(pref.memoryEnabled && command is Command.Chat && chosenSkill==null) extractMemory(id,text,assembled)
      assembled
     }
    }
    if(dao.messageList(id).lastOrNull()?.role!="assistant") dao.put(Message(conversationId=id,role="assistant",content=response))
   } catch(e:CancellationException) {withContext(NonCancellable) {dao.messageList(id).lastOrNull()?.takeIf {it.role=="assistant" && it.state=="streaming"}?.let {dao.put(it.copy(content=it.content.ifBlank {"已停止生成。"},state="cancelled"))}};throw e} catch(e:Exception) {val message=e.message ?: "没有完成，请重试";dao.messageList(id).lastOrNull()?.takeIf {it.role=="assistant" && it.state=="streaming"}?.let {dao.put(it.copy(content=it.content.ifBlank {message},state="error"))};notify(message)} finally {busy.value=false;speechState.value="idle"}
  }
 }
 private fun extractMemory(id:String,user:String,answer:String) {
  viewModelScope.launch {
   val pref=settings.flow.first();if(!pref.memoryEnabled) return@launch
   val p=pref.provider()
   runCatching {
    val result=ai.complete(p,credentials.get(p.id),listOf("system" to "从用户直接陈述中提取一个稳定偏好或重要事实。不要从助手回答中推测。只输出 JSON：{\"title\":\"短标题\",\"content\":\"用户事实\",\"aliases\":\"关键词\"}。没有合适事实则输出 {}。", "user" to user.take(2000)))
    val clean=result.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    val obj=Json.parseToJsonElement(clean).jsonObject
    val content=obj["content"]?.jsonPrimitive?.contentOrNull.orEmpty().take(1000)
    if(content.isNotBlank() && dao.memoryList().none {it.content.trim()==content.trim()}) dao.put(Memory(title=obj["title"]?.jsonPrimitive?.contentOrNull ?: content.take(24),content=content,aliases=obj["aliases"]?.jsonPrimitive?.contentOrNull.orEmpty(),source="AI 提取，等待你的确认",sourceConversationId=id,pending=true))
    val history=dao.messageList(id)
    if(history.count {it.role=="user"}%6==0) {
     val summary=ai.complete(p,credentials.get(p.id),listOf("system" to "把这段对话概括为 150 字以内的情境摘要。只依据真实内容，避免推测。", "user" to history.takeLast(12).joinToString("\n") {it.content.take(600)}))
     dao.put(Memory(title="最近的一段相遇",content=summary.take(1200),kind="summary",source="对话情境摘要",sourceConversationId=id))
    }
   }
  }
 }
 override fun onCleared() {speech.stop();super.onCleared()}
}
