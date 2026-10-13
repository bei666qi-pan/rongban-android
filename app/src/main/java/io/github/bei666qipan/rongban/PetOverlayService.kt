package io.github.bei666qipan.rongban

import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import io.github.bei666qipan.rongban.core.CompanionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class PetOverlayService:Service() {
 private lateinit var windows:WindowManager
 private var pet:ImageView?=null
 private var paused=false
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private var breathing:ObjectAnimator?=null
 private fun animatePet(state:String) {val image=pet ?: return;breathing?.cancel();image.contentDescription="绒伴桌宠：$state";if(paused || state=="sleep") {image.alpha=.6f;return};image.alpha=1f;breathing=ObjectAnimator.ofFloat(image,"scaleY",1f,1.045f,1f).apply {duration=if(state in listOf("listening","speaking")) 800 else if(state in listOf("thinking","preparing")) 1200 else 2800;repeatCount=ValueAnimator.INFINITE;start()}}
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  when(intent?.action) { "stop"->{stopSelf();return START_NOT_STICKY};"pause"->{paused=!paused;animatePet(CompanionState.emotion.value);startForeground(31,notification());return START_NOT_STICKY} }
  if(!Settings.canDrawOverlays(this)) { stopSelf();return START_NOT_STICKY }
  val manager=getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("companion","桌宠陪伴",NotificationManager.IMPORTANCE_LOW))
  startForeground(31,notification())
  if(pet==null) showPet()
  return START_NOT_STICKY
 }
 private fun notification():Notification {
  fun action(name:String)=PendingIntent.getService(this,name.hashCode(),Intent(this,PetOverlayService::class.java).setAction(name),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val open=PendingIntent.getActivity(this,30,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,"companion").setSmallIcon(R.drawable.ic_pet).setContentTitle(if(paused) "绒伴正在休息" else "绒伴在这里陪你").setContentText("点击桌宠开始互动，拖动调整位置").setContentIntent(open).setOngoing(true).addAction(0,if(paused) "继续陪伴" else "休息一下",action("pause")).addAction(0,"关闭桌宠",action("stop")).build()
 }
 private fun showPet() {
  windows=getSystemService(WindowManager::class.java)
  val dp=resources.displayMetrics.density
  val prefs=getSharedPreferences("pet_position",MODE_PRIVATE)
  val params=WindowManager.LayoutParams((88*dp).toInt(),(88*dp).toInt(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START;x=prefs.getInt("x",0);y=prefs.getInt("y",400) }
  val image=ImageView(this).apply { setImageResource(R.drawable.ic_pet);contentDescription="绒伴悬浮桌宠，点击互动";background=GradientDrawable().apply {setColor(0xFFDDE8DC.toInt());cornerRadius=48*dp};clipToOutline=true }
  var startX=0;var startY=0;var downX=0f;var downY=0f;var moving=false
  image.setOnTouchListener { _,event ->
   when(event.action) {
    MotionEvent.ACTION_DOWN->{startX=params.x;startY=params.y;downX=event.rawX;downY=event.rawY;moving=false}
    MotionEvent.ACTION_MOVE->{val dx=event.rawX-downX;val dy=event.rawY-downY;moving=moving || abs(dx)+abs(dy)>10*dp;params.x=(startX+dx).toInt().coerceIn(0,(resources.displayMetrics.widthPixels-params.width).coerceAtLeast(0));params.y=(startY+dy).toInt().coerceIn(0,(resources.displayMetrics.heightPixels-params.height).coerceAtLeast(0));runCatching { windows.updateViewLayout(image,params) }}
    MotionEvent.ACTION_UP->{
     if(!moving && !paused) startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("voice",true))
     else if(moving) { val edge=if(params.x+params.width/2<resources.displayMetrics.widthPixels/2) 0 else resources.displayMetrics.widthPixels-params.width;ValueAnimator.ofInt(params.x,edge).apply { duration=220;addUpdateListener {params.x=it.animatedValue as Int;runCatching { windows.updateViewLayout(image,params) }};start()};prefs.edit().putInt("x",edge).putInt("y",params.y).apply() }
    }
   };true
  }
  try { windows.addView(image,params);pet=image;scope.launch {CompanionState.emotion.collectLatest {animatePet(it)}} } catch(e:WindowManager.BadTokenException) { stopSelf() } catch(e:SecurityException) { stopSelf() }
 }
 override fun onDestroy() { breathing?.cancel();scope.cancel();pet?.let { runCatching { windows.removeView(it) } };pet=null;super.onDestroy() }
}
