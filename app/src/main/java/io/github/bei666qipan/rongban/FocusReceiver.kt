package io.github.bei666qipan.rongban
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.bei666qipan.rongban.core.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
@AndroidEntryPoint class FocusReceiver:BroadcastReceiver() {
 @Inject lateinit var settings:SettingsStore
 override fun onReceive(context:Context,intent:Intent) {
  val result=goAsync()
  CoroutineScope(Dispatchers.IO).launch {
   try {
    val p=settings.flow.first();if(p.focusEnds==0L || p.focusEnds>System.currentTimeMillis()) return@launch
    settings.update { it.copy(focusEnds=0) }
    if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return@launch
    val manager=context.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel("focus","专注提醒",NotificationManager.IMPORTANCE_DEFAULT))
    manager.notify(41,NotificationCompat.Builder(context,"focus").setSmallIcon(R.drawable.ic_pet).setContentTitle("这一段专注完成了").setContentText("${p.focusLabel}。喝口水，伸个懒腰吧。").setContentIntent(PendingIntent.getActivity(context,41,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build())
   } finally { result.finish() }
  }
 }
}
