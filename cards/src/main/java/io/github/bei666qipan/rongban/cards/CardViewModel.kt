package io.github.bei666qipan.rongban.cards
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
@HiltViewModel class CardViewModel @Inject constructor(val dao:RongbanDao,val settings:SettingsStore,private val credentials:CredentialStore,private val ai:AiClient):ViewModel() {
 val cards=dao.cards().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val memories=dao.memories().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val preferences=settings.flow.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Preferences())
 val busy=MutableStateFlow(false)
 val status=MutableStateFlow<String?>(null)
 fun save(d:CardDraft) {viewModelScope.launch {dao.put(d)}}
 fun create(title:String,body:String,template:String,onCreated:(String)->Unit) {viewModelScope.launch {val d=CardDraft(title=title,body=body,template=template);dao.put(d);onCreated(d.id)}}
 fun delete(id:String) {viewModelScope.launch {dao.deleteCard(id)}}
 fun summarize(d:CardDraft,onResult:(String)->Unit) {viewModelScope.launch {busy.value=true;try {val p=settings.flow.first();val c=p.provider();val body=ai.complete(c,credentials.get(c.id),listOf("system" to "将用户选择的内容整理成 200 字以内可分享的卡片文字，保持事实，不增加经历。只输出正文。","user" to d.body.take(6000)));onResult(body)} catch(e:CancellationException) {throw e} catch(e:Exception) {status.value=e.message ?: "总结失败，仍可使用原文"} finally {busy.value=false}}}
}
