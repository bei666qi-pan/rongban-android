package io.github.bei666qipan.rongban.cards
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint class CardActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { Text("绒伴卡片") } } }
}
