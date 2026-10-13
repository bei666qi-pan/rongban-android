package io.github.bei666qipan.rongban.core

import android.content.Context
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton
import java.util.UUID

fun newId() = UUID.randomUUID().toString()
@Entity data class Conversation(@PrimaryKey val id:String=newId(),val title:String="新的相遇",val created:Long=System.currentTimeMillis())
@Entity(indices=[Index("conversationId")]) data class Message(@PrimaryKey val id:String=newId(),val conversationId:String,val role:String,val content:String,val state:String="complete",val created:Long=System.currentTimeMillis())
// RikkaHub memory_file storage adapted to discrete, user-reviewable memories.
@Entity data class Memory(@PrimaryKey val id:String=newId(),val title:String,val content:String,val aliases:String="",val kind:String="preference",val source:String="手动记录",val sourceConversationId:String="",val enabled:Boolean=true,val pending:Boolean=false,val updated:Long=System.currentTimeMillis())
@Entity data class Skill(@PrimaryKey val id:String,val name:String,val description:String,val body:String,val executor:String="prompt",val enabled:Boolean=true,val builtin:Boolean=false)
@Entity data class CardDraft(@PrimaryKey val id:String=newId(),val title:String,val body:String,val template:String="conversation",val source:String="",val created:Long=System.currentTimeMillis())
@Dao interface RongbanDao {
 @Query("SELECT * FROM Conversation ORDER BY created DESC") fun conversations():Flow<List<Conversation>>
 @Query("SELECT * FROM Message WHERE conversationId=:id ORDER BY created") fun messages(id:String):Flow<List<Message>>
 @Query("SELECT * FROM Message WHERE conversationId=:id ORDER BY created") suspend fun messageList(id:String):List<Message>
 @Query("SELECT * FROM Message ORDER BY created DESC LIMIT 100") suspend fun recentMessages():List<Message>
 @Query("SELECT * FROM Memory ORDER BY updated DESC") fun memories():Flow<List<Memory>>
 @Query("SELECT * FROM Memory ORDER BY updated DESC") suspend fun memoryList():List<Memory>
 @Query("SELECT * FROM Skill ORDER BY builtin DESC,name") fun skills():Flow<List<Skill>>
 @Query("SELECT * FROM Skill") suspend fun skillList():List<Skill>
 @Query("SELECT * FROM CardDraft ORDER BY created DESC") fun cards():Flow<List<CardDraft>>
 @Upsert suspend fun put(c:Conversation)
 @Upsert suspend fun put(m:Message)
 @Upsert suspend fun put(m:Memory)
 @Upsert suspend fun put(s:Skill)
 @Upsert suspend fun put(c:CardDraft)
 @Query("DELETE FROM Memory WHERE id=:id") suspend fun deleteMemory(id:String)
 @Query("DELETE FROM Skill WHERE id=:id AND builtin=0") suspend fun deleteSkill(id:String)
 @Query("DELETE FROM CardDraft WHERE id=:id") suspend fun deleteCard(id:String)
 @Query("DELETE FROM Message WHERE conversationId=:id") suspend fun deleteMessages(id:String)
 @Query("DELETE FROM Conversation WHERE id=:id") suspend fun deleteConversationRow(id:String)
 @Transaction suspend fun deleteConversation(id:String) { deleteMessages(id);deleteConversationRow(id) }
}
@Database(entities=[Conversation::class,Message::class,Memory::class,Skill::class,CardDraft::class],version=1,exportSchema=true)
abstract class RongbanDatabase:RoomDatabase() { abstract fun dao():RongbanDao }
@Module @InstallIn(SingletonComponent::class) object StorageModule {
 @Provides @Singleton fun database(@ApplicationContext context:Context)=Room.databaseBuilder(context,RongbanDatabase::class.java,"rongban.db").build()
 @Provides fun dao(db:RongbanDatabase)=db.dao()
}
