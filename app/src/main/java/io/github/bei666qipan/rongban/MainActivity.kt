package io.github.bei666qipan.rongban

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.bei666qipan.rongban.cards.CardActivity
import io.github.bei666qipan.rongban.core.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint class MainActivity : ComponentActivity() {
 private val vm: AppViewModel by viewModels()
 private var voiceRequest by mutableIntStateOf(0)
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  if (intent.getBooleanExtra("voice", false)) voiceRequest++
  setContent { MainApp(vm, voiceRequest) }
 }
 override fun onNewIntent(intent: Intent) {
  super.onNewIntent(intent)
  if (intent.getBooleanExtra("voice", false)) voiceRequest++
 }
 override fun onStop() { vm.stopSpeech(); super.onStop() }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun MainApp(vm: AppViewModel, voiceRequest: Int) {
 val prefs by vm.preferences.collectAsStateWithLifecycle()
 val conversations by vm.conversations.collectAsStateWithLifecycle()
 val memories by vm.memories.collectAsStateWithLifecycle()
 val skills by vm.skills.collectAsStateWithLifecycle()
 val ready by vm.ready.collectAsStateWithLifecycle()
 val status by vm.status.collectAsStateWithLifecycle()
 val context = androidx.compose.ui.platform.LocalContext.current
 var route by rememberSaveable { mutableStateOf("home") }
 var editingMemory by remember { mutableStateOf<Memory?>(null) }
 var editingSkill by remember { mutableStateOf<Skill?>(null) }
 var deleteMemory by remember { mutableStateOf<Memory?>(null) }
 var deleteConversation by remember { mutableStateOf<Conversation?>(null) }
 var selectedMemories by rememberSaveable { mutableStateOf(setOf<String>()) }
 var csv by rememberSaveable { mutableStateOf("") }
 val snackbar = remember { SnackbarHostState() }
 val scope = rememberCoroutineScope()
 val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) vm.active.value?.let(vm::listen) else vm.notify("麦克风未开启，你仍可以输入文字")
 }
 fun listen(id: String) {
  vm.active.value = id
  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.listen(id)
  else mic.launch(Manifest.permission.RECORD_AUDIO)
 }
 val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
  vm.notify(if (it) "提醒已开启" else "提醒未开启，仍可在 App 内查看专注进度")
 }
 val skillImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) scope.launch {
   runCatching {
    context.contentResolver.openInputStream(uri)!!.bufferedReader().use { reader ->
     val chars = CharArray(65537); val n = reader.read(chars)
     require(n <= 65536 && reader.read() == -1) { "技能文件不能超过 64KB" }
     String(chars, 0, n.coerceAtLeast(0))
    }
   }.onSuccess(vm::importSkill).onFailure { vm.notify(it.message ?: "无法读取文件") }
  }
 }
 val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
  if (uri == null) vm.notify("已取消保存")
  else scope.launch {
   runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(csv.toByteArray()) } }
    .onSuccess { vm.notify("CSV 已保存") }
    .onFailure { vm.notify("保存失败，请重新选择位置") }
  }
 }
 LaunchedEffect(status) { status?.let { snackbar.showSnackbar(it); vm.status.value = null } }
 LaunchedEffect(voiceRequest) { if (voiceRequest > 0) vm.createConversation { id -> route = "chat:$id"; listen(id) } }
 fun cards() { context.startActivity(Intent(context, CardActivity::class.java)) }
 val destinations = listOf("home" to "陪伴", "chats" to "对话", "memories" to "记忆", "skills" to "技能", "settings" to "我的")
 RongbanTheme(prefs.theme) {
  Scaffold(
   containerColor = MaterialTheme.colorScheme.background,
   snackbarHost = { SnackbarHost(snackbar) },
   bottomBar = {
    if (!route.startsWith("chat:")) NavigationBar(
     containerColor = MaterialTheme.colorScheme.surface,
     tonalElevation = 0.dp
    ) {
     destinations.forEach { (key, label) ->
      NavigationBarItem(
       selected = route == key,
       onClick = { route = key },
       icon = { Text(when (key) { "home" -> "◉"; "chats" -> "◌"; "memories" -> "◇"; "skills" -> "✦"; else -> "☰" }) },
       label = { Text(label) },
       colors = NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
       )
      )
     }
    }
   }
  ) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).semantics { testTagsAsResourceId = true }) {
    WatermarkBar(prefs.studentName, prefs.studentNumber)
    if (!ready) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else when {
     route == "home" -> HomePage(
      prefs, memories.count { it.enabled && !it.pending }, vm,
      onChat = { vm.createConversation { route = "chat:$it" } },
      onVoice = { vm.createConversation { route = "chat:$it"; listen(it) } },
      onCards = { cards() },
      onMemory = { route = "memories" },
      onNotification = { if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
     )
     route == "chats" -> ListPage("我们的对话", "每次相遇，都留下一点温度。", "新的相遇", { vm.createConversation { route = "chat:$it" } }) {
      if (conversations.isEmpty()) item { CompanionEmpty("还没有对话", "从一句问候，或一个语音指令开始。") }
      items(conversations, key = { it.id }) { c ->
       CompanionNote(c.title, "打开这段相遇", onClick = { vm.active.value = c.id; route = "chat:${c.id}" }) {
        TextButton(onClick = { deleteConversation = c }) { Text("删除") }
       }
      }
     }
     route.startsWith("chat:") -> ChatPage(route.substringAfter(":"), vm, onBack = {
      vm.stopSpeech(); if (vm.busy.value) vm.cancel(); route = "chats"
     }, onVoice = { listen(route.substringAfter(":")) }, onCards = { cards() })
     route == "memories" -> {
      var query by rememberSaveable { mutableStateOf("") }
      ListPage("记忆小屋", "你决定什么值得被记住。", "添一条", { editingMemory = Memory(title = "", content = "") }) {
       item {
        OutlinedTextField(
         query, { query = it },
         placeholder = { Text("搜索记忆与关键词") },
         modifier = Modifier.fillMaxWidth(),
         singleLine = true,
         shape = RoundedCornerShape(18.dp)
        )
       }
       item {
        Row(verticalAlignment = Alignment.CenterVertically) {
         Text("已选 ${selectedMemories.size} 条", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
         TextButton(onClick = {
          if (selectedMemories.isEmpty()) vm.notify("先勾选需要导出的记忆")
          else { csv = MemoryContext.csv(memories.filter { it.id in selectedMemories }); csvSave.launch("绒伴记忆.csv") }
         }) { Text("导出 CSV") }
        }
       }
       val visible = memories.filter { query.isBlank() || (it.title + it.content + it.aliases).contains(query, true) }
       if (visible.isEmpty()) item { CompanionEmpty("这里还很安静", "告诉绒伴：记住，我喜欢清淡的食物。") }
       items(visible, key = { it.id }) { m ->
        CompanionPanel {
         Row(verticalAlignment = Alignment.Top) {
          Checkbox(checked = m.id in selectedMemories, onCheckedChange = { selectedMemories = if (it) selectedMemories + m.id else selectedMemories - m.id })
          Column(Modifier.weight(1f).padding(start = 4.dp, top = 10.dp).clickableRow { editingMemory = m }) {
           Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            StatusMark(if (m.pending) "待确认" else if (!m.enabled) "已停用" else if (m.kind == "summary") "近况" else "记得", quiet = !m.enabled || m.pending)
           }
           Text(m.content, Modifier.padding(top = 6.dp), maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
           Text(m.source, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
         }
         Row {
          if (m.pending) TextButton(onClick = { vm.saveMemory(m.copy(pending = false)) }) { Text("记住这件事") }
          TextButton(onClick = { vm.saveCard(m.title, m.content, "memory"); cards() }) { Text("做成卡片") }
          TextButton(onClick = { deleteMemory = m }) { Text(if (m.pending) "忽略" else "遗忘") }
         }
        }
       }
      }
     }
     route == "skills" -> ListPage("技能口袋", "在需要的时候，选择一种陪伴方式。", "导入技能", { skillImport.launch(arrayOf("text/*", "application/octet-stream")) }) {
      if (skills.isEmpty()) item { CompanionEmpty("还没有技能", "内置技能会在首次打开时准备好。") }
      items(skills, key = { it.id }) { s ->
       CompanionPanel {
        Column(Modifier.fillMaxWidth().clickableRow { editingSkill = s }) {
         Text(s.name, style = MaterialTheme.typography.titleMedium)
         Text(s.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
         StatusMark(if (s.executor in listOf("remember", "recall", "focus", "card")) "本地" else "AI")
         Spacer(Modifier.weight(1f))
         Switch(s.enabled, { vm.saveSkill(s.copy(enabled = it)) })
        }
       }
      }
     }
     route == "settings" -> SettingsPage(
      prefs, vm,
      onOverlay = {
       if (Settings.canDrawOverlays(context)) ContextCompat.startForegroundService(context, Intent(context, PetOverlayService::class.java))
       else {
        vm.notify("开启悬浮权限后，返回这里点击「开启桌宠」")
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
       }
      },
      onNotifications = { if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
     )
    }
   }
  }
  editingMemory?.let { m -> MemoryDialog(m, onDismiss = { editingMemory = null }, onSave = { vm.saveMemory(it); editingMemory = null }) }
  editingSkill?.let { s ->
   SkillDialog(s, onDismiss = { editingSkill = null }, onSave = { vm.saveSkill(it); editingSkill = null }, onDelete = { vm.deleteSkill(s); editingSkill = null }, onRun = { updated, input ->
    editingSkill = null
    vm.createConversation { id ->
     route = "chat:$id"
     val command = when (updated.executor) {
      "remember" -> "记住：$input"
      "recall" -> "回忆：$input"
      "focus" -> "开始专注${input.ifBlank { "25" }}分钟"
      "review" -> "今日回顾"
      "card" -> "生成卡片"
      "encourage" -> "鼓励我"
      else -> input
     }
     vm.send(id, command, updated)
    }
   })
  }
  deleteMemory?.let { m ->
   AlertDialog(onDismissRequest = { deleteMemory = null }, title = { Text("移除这条记忆？") }, text = { Text("后续相遇不再引用它。原始对话会保留，近期摘要也会清理。") }, confirmButton = { TextButton(onClick = { vm.forget(m); deleteMemory = null }) { Text("遗忘") } }, dismissButton = { TextButton(onClick = { deleteMemory = null }) { Text("保留") } })
  }
  deleteConversation?.let { c ->
   AlertDialog(onDismissRequest = { deleteConversation = null }, title = { Text("删除这段对话？") }, text = { Text("对话内容会删除，已确认的记忆单独保留。") }, confirmButton = { TextButton(onClick = { vm.removeConversation(c); deleteConversation = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleteConversation = null }) { Text("取消") } })
  }
 }
}

private fun Modifier.clickableRow(onClick: () -> Unit) = clickable(onClick = onClick)

@Composable private fun ListPage(title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
 LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  item { CompanionHeader(title, subtitle, action, onAction) }
  content()
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomePage(p: Preferences, memoryCount: Int, vm: AppViewModel, onChat: () -> Unit, onVoice: () -> Unit, onCards: () -> Unit, onMemory: () -> Unit, onNotification: () -> Unit) {
 val state by vm.speechState.collectAsStateWithLifecycle()
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(p.focusEnds) {
  now = System.currentTimeMillis()
  while (p.focusEnds > now) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() }
 }
 val focusing = p.focusEnds > now
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  item {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Text("绒伴  /  RONGBAN", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MM月dd日")), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item {
   Text("今天，也陪着你。", style = MaterialTheme.typography.headlineLarge)
   Text("${p.petName}记得你，也想听听你的今天。", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(40.dp, 40.dp, 24.dp, 24.dp)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp)) {
     Pet(Modifier.size(196.dp), if (focusing) "sleep" else state)
     Text(if (focusing) "安静陪你专注" else "轻轻说一句，我在听。", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
   }
  }
  item {
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Button(onClick = onVoice, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(26.dp)) { Text("◉  和我说话") }
    FilledTonalButton(onClick = onChat, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(26.dp)) { Text("写点什么") }
   }
  }
  item {
   Text("此刻的心情", style = MaterialTheme.typography.titleMedium)
   Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("平静", "开心", "有点累", "焦虑").forEach { m ->
     FilterChip(selected = p.mood == m, onClick = { vm.updatePreferences { it.copy(mood = m) } }, label = { Text(m) })
    }
   }
  }
  item {
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(onClick = onMemory, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
     Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("$memoryCount", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
      Text("珍藏的记忆", style = MaterialTheme.typography.bodyMedium)
     }
    }
    Card(onClick = onCards, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
     Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("◇", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
      Text("卡片工作室", style = MaterialTheme.typography.bodyMedium)
     }
    }
   }
  }
  item {
   CompanionPanel {
    Text(if (focusing) "${p.focusLabel} · ${(p.focusEnds - now + 59999) / 60000} 分钟" else "一起，专注一小会儿", style = MaterialTheme.typography.titleMedium)
    Text("把注意力交给眼前的一件事。后台提醒可能稍有延迟。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row {
     if (p.focusEnds > 0) TextButton(onClick = vm::stopFocus) { Text("结束专注") }
     else TextButton(onClick = { vm.startFocus(25, "陪你专注"); onNotification() }) { Text("开始 25 分钟") }
    }
   }
  }
 }
}

@Composable private fun ChatPage(id: String, vm: AppViewModel, onBack: () -> Unit, onVoice: () -> Unit, onCards: () -> Unit) {
 val messages by remember(id) { vm.dao.messages(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
 val busy by vm.busy.collectAsStateWithLifecycle()
 val speech by vm.speechState.collectAsStateWithLifecycle()
 val partial by vm.voiceText.collectAsStateWithLifecycle()
 val recognized by vm.recognizedText.collectAsStateWithLifecycle()
 var text by rememberSaveable(id) { mutableStateOf("") }
 LaunchedEffect(recognized) { if (recognized.isNotBlank()) { text = recognized; vm.recognizedText.value = "" } }
 val scroll = rememberLazyListState()
 LaunchedEffect(messages.size, messages.lastOrNull()?.content) { if (messages.isNotEmpty()) scroll.animateScrollToItem(messages.lastIndex) }
 Column(Modifier.fillMaxSize().imePadding()) {
  Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
   Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    TextButton(onClick = onBack) { Text("‹ 返回") }
    Text("这一段相遇", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
    TextButton(onClick = onCards) { Text("卡片") }
   }
  }
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   if (messages.isEmpty()) item { CompanionEmpty("我在这里", "可以聊天，也可以说：记住，我喜欢……") }
   items(messages, key = { it.id }) { m ->
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.role == "user") Alignment.End else Alignment.Start) {
     ChatBubble(m.role, m.state, m.content)
     if (m.state in listOf("error", "cancelled")) {
      val previousUser = messages.takeWhile { it.id != m.id }.lastOrNull { it.role == "user" }
      TextButton(onClick = { previousUser?.let { vm.send(id, it.content) } }) { Text("重试") }
     }
     if (m.role == "assistant" && m.content.isNotBlank()) Row {
      TextButton(enabled = m.state != "streaming", onClick = { vm.speak(m.content) }) { Text("朗读") }
      TextButton(enabled = m.state != "streaming", onClick = { vm.saveCard("这一段相遇", m.content); onCards() }) { Text("做成卡片") }
     }
    }
   }
  }
  AnimatedVisibility(speech == "preparing") { Text("正在准备本地语音，出现「正在听」后再说话。", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
  AnimatedVisibility(speech == "listening") { Text(partial.ifBlank { "正在听，停顿后自动完成，最多 30 秒。" }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
  if (busy) Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
   CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
   Text(" 绒伴正在回应", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
   TextButton(onClick = vm::cancel) { Text("停止") }
  }
  Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
   Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(text, { text = it }, placeholder = { Text("告诉绒伴……") }, modifier = Modifier.weight(1f).testTag("chat-input"), maxLines = 4, shape = RoundedCornerShape(22.dp))
    FilledTonalIconButton(onClick = { if (speech == "speaking") vm.stopSpeech() else onVoice() }, enabled = !busy && speech != "preparing") { Text(if (speech == "listening" || speech == "speaking") "■" else "◉") }
    Button(onClick = { val value = text; text = ""; vm.send(id, value) }, enabled = text.isNotBlank() && !busy && speech != "listening" && speech != "preparing", contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) { Text("发送") }
   }
  }
 }
}

@Composable private fun MemoryDialog(m: Memory, onDismiss: () -> Unit, onSave: (Memory) -> Unit) {
 var title by remember { mutableStateOf(m.title) }
 var body by remember { mutableStateOf(m.content) }
 var aliases by remember { mutableStateOf(m.aliases) }
 var enabled by remember { mutableStateOf(m.enabled) }
 AlertDialog(
  onDismissRequest = onDismiss,
  title = { Text(if (m.title.isBlank()) "添一条记忆" else "记忆详情") },
  text = {
   Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(body, { body = it }, label = { Text("值得记住的内容") }, modifier = Modifier.fillMaxWidth().testTag("memory-body"), minLines = 3)
    OutlinedTextField(aliases, { aliases = it }, label = { Text("关键词，以逗号分隔") }, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) { Text("参与之后的对话", Modifier.weight(1f)); Switch(enabled, { enabled = it }) }
    Text("来源：${m.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   }
  },
  confirmButton = { TextButton(enabled = body.isNotBlank(), onClick = { onSave(m.copy(title = title.ifBlank { body.take(24) }, content = body, aliases = aliases, enabled = enabled, pending = false)) }) { Text("保存") } },
  dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
 )
}

@Composable private fun SkillDialog(s: Skill, onDismiss: () -> Unit, onSave: (Skill) -> Unit, onDelete: () -> Unit, onRun: (Skill, String) -> Unit) {
 var body by remember { mutableStateOf(s.body) }
 var input by remember { mutableStateOf("") }
 var enabled by remember { mutableStateOf(s.enabled) }
 AlertDialog(
  onDismissRequest = onDismiss,
  title = { Text(s.name) },
  text = {
   Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(s.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.CenterVertically) { Text("启用技能", Modifier.weight(1f)); Switch(enabled, { enabled = it }) }
    OutlinedTextField(body, { body = it }, label = { Text("技能说明") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    OutlinedTextField(input, { input = it }, label = { Text(if (s.executor == "focus") "时长，默认 25 分钟" else "这次想做什么") }, modifier = Modifier.fillMaxWidth())
    if (!s.builtin) TextButton(onClick = onDelete) { Text("删除技能") }
   }
  },
  confirmButton = { TextButton(enabled = enabled && (s.executor !in listOf("prompt", "remember") || input.isNotBlank()), onClick = { val updated = s.copy(body = body, enabled = enabled); onSave(updated); onRun(updated, input) }) { Text("使用一次") } },
  dismissButton = { TextButton(onClick = { onSave(s.copy(body = body, enabled = enabled)) }) { Text("保存") } }
 )
}

@Composable private fun SettingsPage(p: Preferences, vm: AppViewModel, onOverlay: () -> Unit, onNotifications: () -> Unit) {
 val settingsContext = androidx.compose.ui.platform.LocalContext.current
 var petName by remember(p.petName) { mutableStateOf(p.petName) }
 var studentName by remember(p.studentName) { mutableStateOf(p.studentName) }
 var studentNo by remember(p.studentNumber) { mutableStateOf(p.studentNumber) }
 var config by remember(p.activeProvider, p.providers) { mutableStateOf(p.provider()) }
 var apiKey by remember(config.id) { mutableStateOf("") }
 var drafting by remember { mutableStateOf(false) }
 ListPage("我的绒伴", "把陪伴调成舒服的样子。") {
  item {
   CompanionPanel {
    Text("桌宠与提醒", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(petName, { petName = it }, label = { Text("桌宠名字") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Button(onClick = { vm.updatePreferences { it.copy(petName = petName.ifBlank { "绒绒" }) } }) { Text("保存名字") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, label) ->
      FilterChip(p.theme == key, { vm.updatePreferences { it.copy(theme = key) } }, label = { Text(label) })
     }
    }
    Row {
     TextButton(onClick = onOverlay) { Text("开启桌宠") }
     TextButton(onClick = { settingsContext.stopService(Intent(settingsContext, PetOverlayService::class.java)) }) { Text("关闭桌宠") }
     TextButton(onClick = onNotifications) { Text("通知提醒") }
    }
   }
  }
  item {
   CompanionPanel {
    Text("情境记忆", style = MaterialTheme.typography.titleLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
     Text("使用记忆与近期摘要", Modifier.weight(1f))
     Switch(p.memoryEnabled, { value -> vm.updatePreferences { it.copy(memoryEnabled = value) } })
    }
    Text("自动提取的内容需要你确认。关闭后，记忆仍留在本地。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item {
   Text("模型连接", style = MaterialTheme.typography.titleLarge)
   Text("凭证加密保存在当前设备，不随卡片或安装包导出。", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
  item {
   Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    p.providers.forEach { provider ->
     FilterChip(p.activeProvider == provider.id && !drafting, onClick = { drafting = false; vm.updatePreferences { it.copy(activeProvider = provider.id) } }, label = { Text(provider.name) })
    }
    FilterChip(drafting, onClick = { drafting = true; config = ProviderConfig(id = newId(), name = "自定义模型", baseUrl = "", model = ""); apiKey = "" }, label = { Text("＋ 添加") })
   }
  }
  item {
   CompanionPanel {
    OutlinedTextField(config.name, { config = config.copy(name = it) }, label = { Text("配置名称") }, modifier = Modifier.fillMaxWidth())
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     listOf("openai" to "兼容接口", "anthropic" to "Anthropic", "gemini" to "Gemini").forEach { (type, label) ->
      FilterChip(config.protocol == type, { config = config.copy(protocol = type) }, label = { Text(label) })
     }
    }
    OutlinedTextField(config.baseUrl, { config = config.copy(baseUrl = it) }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(config.model, { config = config.copy(model = it) }, label = { Text("模型名称") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(apiKey, { apiKey = it }, label = { Text(if (vm.credentials.get(config.id).isBlank()) "API Key" else "API Key 已保存，留空保留") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Button(enabled = config.baseUrl.isNotBlank() && config.model.isNotBlank(), onClick = { vm.saveProvider(config, apiKey.takeIf { it.isNotBlank() }); apiKey = ""; drafting = false }) { Text("保存配置") }
     TextButton(onClick = { vm.testProvider(config, apiKey.takeIf { it.isNotBlank() }) }) { Text("测试连接") }
    }
   }
  }
  item {
   CompanionPanel {
    Text("课程演示水印", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(studentName, { studentName = it }, label = { Text("本人姓名") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(studentNo, { studentNo = it }, label = { Text("本人学号") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { vm.updatePreferences { it.copy(studentName = studentName, studentNumber = studentNo) } }) { Text("保存水印") }
    Text("两项填写完整后，界面显示水印。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item { Text("绒伴 0.1.0  ·  C3 + P4\n原生 Android / 本地数据 / 开源组件复用", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
 }
}
