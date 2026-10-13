package io.github.bei666qipan.rongban
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bei666qipan.rongban.core.*
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);setContent { RongbanTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(28.dp)) { Text("绒伴",style=MaterialTheme.typography.headlineLarge);Text("每一次相遇，都记得你。",style=MaterialTheme.typography.titleMedium);Pet(Modifier.fillMaxWidth().height(320.dp));Text("陪伴 · 对话 · 记忆 · 技能 · 卡片",style=MaterialTheme.typography.bodyLarge) } } } } }
}
