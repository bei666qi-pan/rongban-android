package io.github.bei666qipan.rongban.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

class SpeechEngine(private val context:Context) {
 private val stopped=AtomicBoolean(false)
 private val mutex=Mutex()
 @Volatile private var mic:AudioRecord?=null
 @Volatile private var track:AudioTrack?=null
 fun stop() { stopped.set(true);runCatching { mic?.stop() };runCatching { track?.pause();track?.flush() } }
 @SuppressLint("MissingPermission")
 suspend fun listen(onPartial:(String)->Unit):String=mutex.withLock { withContext(Dispatchers.Default) {
  check(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) { "请允许麦克风，或使用文字输入" }
  stopped.set(false)
  val model="models/asr"
  val recognizer=OnlineRecognizer(context.assets,OnlineRecognizerConfig(modelConfig=OnlineModelConfig(transducer=OnlineTransducerModelConfig("$model/encoder.int8.onnx","$model/decoder.onnx","$model/joiner.int8.onnx"),tokens="$model/tokens.txt",numThreads=2)))
  val stream=recognizer.createStream();var text=""
  try {
   val size=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(3200)
   val recorder=AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size*2)
   mic=recorder;check(recorder.state==AudioRecord.STATE_INITIALIZED) { "麦克风暂时不可用，请改用文字输入" }
   recorder.startRecording();val buffer=ShortArray(1600);val start=System.currentTimeMillis()
   while(isActive && !stopped.get() && System.currentTimeMillis()-start<30000) {
    val count=recorder.read(buffer,0,buffer.size)
    if(count<=0) break
    stream.acceptWaveform(FloatArray(count) { buffer[it]/32768f },16000)
    while(recognizer.isReady(stream)) recognizer.decode(stream)
    text=recognizer.getResult(stream).text;withContext(Dispatchers.Main) { onPartial(text) }
    if(text.isNotBlank() && recognizer.isEndpoint(stream)) break
   }
   stream.acceptWaveform(FloatArray(8000),16000);stream.inputFinished()
   while(recognizer.isReady(stream)) recognizer.decode(stream)
   text=recognizer.getResult(stream).text
   text
  } finally { runCatching { mic?.stop() };mic?.release();mic=null;stream.release();recognizer.release() }
 } }
 suspend fun speak(text:String)=mutex.withLock { withContext(Dispatchers.Default) {
  stopped.set(false);val base="models/tts"
  val tts=OfflineTts(context.assets,OfflineTtsConfig(model=OfflineTtsModelConfig(vits=OfflineTtsVitsModelConfig(model="$base/model.onnx",lexicon="$base/lexicon.txt",tokens="$base/tokens.txt"),numThreads=2),ruleFsts="$base/phone.fst,$base/date.fst,$base/number.fst"))
  try {
   val audio=tts.generate(text.replace(Regex("[*#`]"),"").take(400),sid=21,speed=1f)
   ensureActive();if(stopped.get()) return@withContext
   val format=AudioFormat.Builder().setSampleRate(audio.sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build()
   val player=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(format).setBufferSizeInBytes((audio.samples.size*4).coerceAtLeast(4096)).setTransferMode(AudioTrack.MODE_STATIC).build()
   track=player;player.write(audio.samples,0,audio.samples.size,AudioTrack.WRITE_BLOCKING);player.play()
   var played=0
   while(isActive && !stopped.get() && played<audio.samples.size) { delay(60);played=player.playbackHeadPosition }
  } finally { runCatching { track?.stop() };track?.release();track=null;tts.release() }
 } }
 // Same native recognizer used by microphone capture, exposed for reproducible PCM tests.
 suspend fun recognizePcm(samples:FloatArray):String=withContext(Dispatchers.Default) {
  val b="models/asr";val r=OnlineRecognizer(context.assets,OnlineRecognizerConfig(modelConfig=OnlineModelConfig(transducer=OnlineTransducerModelConfig("$b/encoder.int8.onnx","$b/decoder.onnx","$b/joiner.int8.onnx"),tokens="$b/tokens.txt",numThreads=2)))
  val s=r.createStream()
  try { s.acceptWaveform(samples+FloatArray(8000),16000);s.inputFinished();while(r.isReady(s)) r.decode(s);r.getResult(s).text } finally { s.release();r.release() }
 }
}
