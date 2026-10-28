package io.github.bei666qipan.rongban.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class ProviderConfig(val id:String="mimo",val name:String="MiMo Token Plan",val protocol:String="openai",val baseUrl:String="https://token-plan-cn.xiaomimimo.com/v1",val model:String="mimo-v2.6-pro")
@Serializable data class Preferences(val petName:String="绒绒",val mood:String="平静",val theme:String="system",val studentName:String="",val studentNumber:String="",val focusEnds:Long=0,val focusLabel:String="",val memoryEnabled:Boolean=true,val providers:List<ProviderConfig> = listOf(ProviderConfig()),val activeProvider:String="mimo") {
 fun provider()=providers.firstOrNull { it.id==activeProvider } ?: providers.first()
}
private val Context.rongbanSettings by preferencesDataStore("rongban_settings")
@Singleton class SettingsStore @Inject constructor(@ApplicationContext private val context:Context) {
 private val key=stringPreferencesKey("preferences")
 private val json=Json { ignoreUnknownKeys=true }
 val flow:Flow<Preferences> = context.rongbanSettings.data.map { runCatching { json.decodeFromString<Preferences>(it[key] ?: "{}") }.getOrDefault(Preferences()) }
 suspend fun update(block:(Preferences)->Preferences) { context.rongbanSettings.edit { p -> val old=runCatching { json.decodeFromString<Preferences>(p[key] ?: "{}") }.getOrDefault(Preferences());p[key]=json.encodeToString(block(old)) } }
}
@Singleton class CredentialStore @Inject constructor(@ApplicationContext context:Context) {
 private val prefs=context.getSharedPreferences("encrypted_credentials",Context.MODE_PRIVATE)
 private fun key():SecretKey {
  val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (ks.getKey("rongban-api",null) as? SecretKey)?.let { return it }
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply { init(KeyGenParameterSpec.Builder("rongban-api",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()) }.generateKey()
 }
 @Synchronized fun put(id:String,value:String) {
  if(value.isBlank()) { prefs.edit().remove(id).apply();return }
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
  prefs.edit().putString(id,Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(value.toByteArray()),Base64.NO_WRAP)).apply()
 }
 @Synchronized fun get(id:String):String = runCatching {
  val parts=prefs.getString(id,null)?.split(":") ?: return ""
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
  String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)))
 }.getOrDefault("")
}
