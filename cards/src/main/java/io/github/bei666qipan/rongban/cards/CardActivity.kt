package io.github.bei666qipan.rongban.cards
import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import dagger.hilt.android.AndroidEntryPoint
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@AndroidEntryPoint class CardActivity:ComponentActivity() {
 private val vm:CardViewModel by viewModels()
 override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);enableEdgeToEdge();setContent {CardsApp(vm)}}
}
@Composable private fun CardsApp(vm:CardViewModel) {
 val p by vm.preferences.collectAsStateWithLifecycle();val cards by vm.cards.collectAsStateWithLifecycle();val memories by vm.memories.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val status by vm.status.collectAsStateWithLifecycle()
 val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope();val snackbar=remember {SnackbarHostState()}
 var editing by rememberSaveable {mutableStateOf<String?>(null)};var chooseSource by remember {mutableStateOf(false)};var recent by remember {mutableStateOf<List<Message>>(emptyList())};var pendingFiles by remember {mutableStateOf<List<File>>(emptyList())};var exporting by remember {mutableStateOf(false)}
 val savePng=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {uri->if(uri==null) vm.status.value="已取消保存" else scope.launch {runCatching {withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)!!.use {output->pendingFiles.first().inputStream().use {it.copyTo(output)}}}}.onSuccess {vm.status.value="图片已保存"}.onFailure {vm.status.value="保存失败，请重新选择位置"}}}
 val saveZip=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {uri->if(uri==null) vm.status.value="已取消保存" else scope.launch {runCatching {withContext(Dispatchers.IO) {ZipOutputStream(context.contentResolver.openOutputStream(uri)!!).use {zip->pendingFiles.forEach {file->zip.putNextEntry(ZipEntry(file.name));file.inputStream().use {it.copyTo(zip)};zip.closeEntry()}}}}.onSuccess {vm.status.value="分页图片已保存"}.onFailure {vm.status.value="保存失败，请重新选择位置"}}}
 LaunchedEffect(status) {status?.let {snackbar.showSnackbar(it);vm.status.value=null}}
 fun export(card:CardDraft,share:Boolean) {scope.launch {exporting=true;try {pendingFiles=withContext(Dispatchers.IO) {CardRenderer.export(context,card,p)};if(share) {
  val uris=pendingFiles.map {CardRenderer.uri(context,it)}
  val intent=Intent(if(uris.size==1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).setType("image/png").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  if(uris.size==1) intent.putExtra(Intent.EXTRA_STREAM,uris.first()) else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,ArrayList(uris))
  intent.clipData=ClipData.newUri(context.contentResolver,"绒伴卡片",uris.first()).apply {uris.drop(1).forEach {addItem(ClipData.Item(it))}}
  context.startActivity(Intent.createChooser(intent,"分享这张绒伴卡片"))
 } else if(pendingFiles.size==1) savePng.launch("绒伴卡片.png") else saveZip.launch("绒伴分页卡片.zip")} catch(e:Exception) {vm.status.value="导出失败：${e.message ?: "请稍后重试"}"} finally {exporting=false}}}
 RongbanTheme(p.theme) {Scaffold(snackbarHost={SnackbarHost(snackbar)}) {padding->
  Column(Modifier.fillMaxSize().padding(padding)) {
   if(p.studentName.isNotBlank() && p.studentNumber.isNotBlank()) Text("${p.studentName}  ${p.studentNumber}",Modifier.padding(horizontal=20.dp),style=MaterialTheme.typography.labelSmall)
   val card=cards.firstOrNull {it.id==editing}
   if(editing!=null && card!=null) {
    var title by rememberSaveable(card.id) {mutableStateOf(card.title)};var body by rememberSaveable(card.id) {mutableStateOf(card.body)};var template by rememberSaveable(card.id) {mutableStateOf(card.template)}
    val draft=card.copy(title=title,body=body,template=template)
    var preview by remember {mutableStateOf<android.graphics.Bitmap?>(null)}
    var previewPage by rememberSaveable(card.id) {mutableIntStateOf(0)}
    var previewError by remember {mutableStateOf<String?>(null)}
    val pageCount=remember(body) {CardRenderer.pageRanges(body).size}
    LaunchedEffect(title,body,template,previewPage,p.studentName,p.studentNumber) {
     delay(250)
     previewPage=previewPage.coerceIn(0,pageCount-1)
     try {preview=withContext(Dispatchers.Default) {CardRenderer.render(draft,p,previewPage)};previewError=null}
     catch(e:CancellationException) {throw e}
     catch(e:Exception) {preview=null;previewError=e.message ?: "预览失败，请精简内容后重试"}
    }
    
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
     item {Row(verticalAlignment=Alignment.CenterVertically) {TextButton(onClick={vm.save(draft);editing=null}) {Text("‹ 工作室")};Text("卡片预览",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)}}
     item {preview?.let {Image(it.asImageBitmap(),"导出图片预览",Modifier.fillMaxWidth().height(320.dp))} ?: Text(previewError ?: "正在生成预览……",color=MaterialTheme.colorScheme.primary)}
     item {Row(verticalAlignment=Alignment.CenterVertically) {Text("${previewPage+1} / $pageCount · 1080 × 1440",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);if(pageCount>1) {TextButton(enabled=previewPage>0,onClick={previewPage--}) {Text("上一页")};TextButton(enabled=previewPage<pageCount-1,onClick={previewPage++}) {Text("下一页")}}}}
     item {Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("conversation" to "对话摘录","memory" to "记忆便签","review" to "今日回顾").forEach {(key,label)->FilterChip(template==key,{template=key},label={Text(label)})}}}
     item {OutlinedTextField(title,{title=it},label={Text("标题")},modifier=Modifier.fillMaxWidth().testTag("card-title"))}
     item {OutlinedTextField(body,{body=it},label={Text("卡片内容")},modifier=Modifier.fillMaxWidth().testTag("card-body"),minLines=4,maxLines=10)}
     item {Row {TextButton(enabled=!busy && body.isNotBlank(),onClick={vm.summarize(draft) {body=it;vm.save(draft.copy(body=it))}}) {Text(if(busy) "正在整理……" else "AI 精简")};TextButton(onClick={vm.save(draft);vm.status.value="草稿已保存"}) {Text("保存草稿")}}}
     item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {Button(enabled=!exporting && body.isNotBlank() && body.length<=20000,onClick={vm.save(draft);export(draft,true)},modifier=Modifier.weight(1f)) {Text("分享到其他 App")};FilledTonalButton(enabled=!exporting && body.isNotBlank() && body.length<=20000,onClick={vm.save(draft);export(draft,false)},modifier=Modifier.weight(1f)) {Text("保存图片")}}}
     item {Text("长文自动分页，保存为包含 PNG 的 ZIP。分享前请检查内容。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
   } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    item {Text("卡片工作室",style=MaterialTheme.typography.headlineLarge);Text("把值得留住的瞬间，送给自己或别人。",Modifier.padding(top=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
    item {Button(onClick={chooseSource=true;scope.launch {recent=vm.dao.recentMessages().filter {it.content.isNotBlank()}}},modifier=Modifier.fillMaxWidth().height(54.dp)) {Text("＋ 从记忆或对话创建")}}
    item {OutlinedButton(onClick={vm.create("值得留住的一刻","","conversation") {editing=it}},modifier=Modifier.fillMaxWidth()) {Text("写一张新的卡片")}}
    if(cards.isEmpty()) item {Pet(Modifier.fillMaxWidth().height(240.dp),"happy");Text("还没有卡片，从一条记忆开始吧。",Modifier.alignCenter())}
    items(cards,key={it.id}) {d->Card(Modifier.fillMaxWidth().clickable {editing=d.id}) {Column(Modifier.padding(20.dp)) {Text(d.title,style=MaterialTheme.typography.titleLarge);Text(d.body,Modifier.padding(top=8.dp),maxLines=3);TextButton(onClick={vm.delete(d.id)}) {Text("删除草稿")}}}}
   }
  }
 }
 if(chooseSource) AlertDialog(onDismissRequest={chooseSource=false},title={Text("选择要分享的内容")},text={LazyColumn(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  if(memories.none {!it.pending} && recent.isEmpty()) item {Text("主 App 还没有数据。可以先添加记忆，或者写一张新卡片。")}
  items(memories.filter {!it.pending}) {m->TextButton(onClick={vm.create(m.title,m.content,"memory") {editing=it};chooseSource=false}) {Text("记忆 · ${m.title}")}}
  items(recent.take(30)) {m->TextButton(onClick={vm.create("这一段相遇",m.content,"conversation") {editing=it};chooseSource=false}) {Text("${if(m.role=="user") "你" else "绒伴"} · ${m.content.take(40)}")}}
 }},confirmButton={TextButton(onClick={chooseSource=false}) {Text("关闭")}})
 }
}
private fun Modifier.alignCenter()=this.fillMaxWidth().padding(12.dp)
