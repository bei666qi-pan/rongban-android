package io.github.bei666qipan.rongban

import androidx.room.Room
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryAndSkillTest {
 @Test fun editedDisabledDeletedAndDuplicateCandidatesHavePredictableContext()=runBlocking {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(c,RongbanDatabase::class.java).build()
  val dao=db.dao();val vm=AppViewModel(dao,SettingsStore(c),CredentialStore(c),AiClient(),c)
  try {
   while(!vm.ready.value) delay(20)
   val original=Memory(title="饮茶",content="喜欢红茶")
   dao.put(original);dao.put(Memory(title="旧摘要",content="喜欢红茶",kind="summary"))
   vm.saveMemory(original.copy(content="喜欢绿茶"));withTimeout(5000) {while(dao.memoryList().any {it.kind=="summary"} || dao.memoryList().firstOrNull {it.id==original.id}?.content!="喜欢绿茶") delay(20)}
   assertFalse(MemoryContext.system(Preferences(),dao.memoryList(),"饮茶").contains("红茶"))
   vm.saveMemory(original.copy(content="喜欢绿茶",enabled=false));withTimeout(5000) {while(dao.memoryList().first {it.id==original.id}.enabled) delay(20)}
   assertTrue(MemoryContext.retrieve(dao.memoryList(),"饮茶").isEmpty())
   vm.saveMemory(Memory(title="重复候选",content="喜欢绿茶",pending=true));delay(100)
   assertEquals(1,dao.memoryList().size)
   vm.forget(original);withTimeout(5000) {while(dao.memoryList().isNotEmpty()) delay(20)}
   assertTrue(MemoryContext.retrieve(dao.memoryList(),"饮茶").isEmpty())
  } finally {vm.viewModelScope.cancel();db.close()}
 }
 @Test fun importedSkillAndDisabledExecutorAreEnforced()=runBlocking {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(c,RongbanDatabase::class.java).build()
  val dao=db.dao();val vm=AppViewModel(dao,SettingsStore(c),CredentialStore(c),AiClient(),c)
  try {
   while(!vm.ready.value) delay(20)
   vm.importSkill("---\nname: 复习教练\ndescription: 帮助整理\n---\n用三个问题帮助我复习。")
   withTimeout(5000) {while(dao.skillList().none {it.name=="复习教练"}) delay(20)}
   assertEquals("prompt",dao.skillList().first {it.name=="复习教练"}.executor)
   val count=dao.skillList().size;vm.importSkill("非法文件");delay(100);assertEquals(count,dao.skillList().size)
   dao.put(BuiltinSkills.all.first {it.id=="remember"}.copy(enabled=false))
   val conversation=Conversation();dao.put(conversation);vm.send(conversation.id,"记住，我喜欢咖啡")
   withTimeout(5000) {while(dao.messageList(conversation.id).none {it.role=="assistant"}) delay(20)}
   assertTrue(dao.memoryList().isEmpty());assertTrue(dao.messageList(conversation.id).last().content.contains("已关闭"))
  } finally {vm.viewModelScope.cancel();db.close()}
 }
}
