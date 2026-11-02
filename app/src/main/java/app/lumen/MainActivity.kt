package app.lumen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One quote at a time; tap for the next. The pick here is the one on the widget. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            var quote by remember { mutableStateOf(Lumen.current(ctx)) }
            var show by remember { mutableStateOf(Lumen.show(ctx)) }
            val dim = Color(0xFF8C8C8C)
            Box(
                Modifier.fillMaxSize().background(Color.Black)
                    .clickable(interactionSource = null, indication = null) { quote = Lumen.advance(ctx) }
                    .systemBarsPadding().padding(28.dp)
            ) {
                Crossfade(quote, Modifier.align(Alignment.CenterStart), label = "quote") { q ->
                    Column {
                        Text("“${q.line}”", color = Color.White, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium)
                        Text("${q.who} · ${q.film}".uppercase(), color = Color(if (q.villain) VILLAIN_TINT else HERO_TINT),
                            fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 20.dp))
                    }
                }
                Column(Modifier.align(Alignment.BottomStart)) {
                    Row {
                        listOf("all" to "All", "heroes" to "Heroes", "villains" to "Villains").forEach { (value, label) ->
                            Text(label, color = if (value == show) Color.White else dim, fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    show = value
                                    Lumen.setShow(ctx, value)
                                    quote = Lumen.advance(ctx)
                                }.padding(top = 12.dp, bottom = 12.dp, end = 24.dp))
                        }
                    }
                    Text("Tap for another. Widget: long-press your home screen, Add widget, Lumen.", color = dim, fontSize = 13.sp)
                }
            }
        }
    }
}
