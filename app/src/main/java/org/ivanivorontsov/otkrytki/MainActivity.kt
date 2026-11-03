package org.ivanivorontsov.otkrytki

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFFF6A4D), secondary = Color(0xFF7E57C2), background = Color(0xFFFDF6F0), surface = Color(0xFFFFFFFF))) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App() }
            }
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data class Editor(val occasion: Occasion) : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data class Viewer(val file: File) : Screen()
}

data class StockImage(val file: String, val category: String, val title: String, val license: String, val source: String, val author: String)

fun stockList(ctx: Context): List<StockImage> {
    val a = JSONArray(ctx.assets.open("stock/licenses.json").bufferedReader().readText())
    return (0 until a.length()).map { val o = a.getJSONObject(it); StockImage(o.getString("file"), o.getString("category"), o.getString("title"), o.getString("license"), o.getString("source"), o.optString("author")) }
}

fun stockCats(o: Occasion): List<String> = when (o.id) {
    "birthday" -> listOf("birthday", "flowers", "animals")
    "newyear" -> listOf("newyear")
    "march8" -> listOf("march8", "flowers")
    "feb23" -> listOf("nature", "newyear")
    "victory" -> listOf("victory")
    "love" -> listOf("love", "flowers")
    "friends" -> listOf("animals", "flowers", "nature")
    "thanks" -> listOf("flowers", "march8")
    "support", "nature" -> listOf("nature")
    "poems" -> listOf("nature", "flowers")
    else -> emptyList()
}

fun loadAsset(ctx: Context, path: String): Bitmap = ctx.assets.open(path).use { BitmapFactory.decodeStream(it) }

fun shareFile(ctx: Context, f: File) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, "Отправить открытку"))
}

fun saveToPhone(ctx: Context, b: Bitmap): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val v = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "otkrytka_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Открытки")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return true
}

@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
    when (val s = screen) {
        Screen.Home -> HomeScreen(onOpen = { screen = it })
        is Screen.Editor -> EditorScreen(s.occasion, onBack = { screen = Screen.Home }, onSaved = { screen = Screen.Viewer(it) })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
        Screen.About -> AboutScreen(onBack = { screen = Screen.Home })
        is Screen.Viewer -> ViewerScreen(s.file, onBack = { screen = Screen.Home })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val cards = remember { Gallery.list(ctx) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Открытки", fontWeight = FontWeight.Bold) }, actions = {
            IconButton(onClick = { onOpen(Screen.About) }) { Icon(Icons.Default.Info, "О приложении") }
            IconButton(onClick = { onOpen(Screen.Settings) }) { Icon(Icons.Default.Settings, "Настройки") }
        })
    }) { pad ->
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(span = { GridItemSpan(3) }) { Text("Какой повод?", style = MaterialTheme.typography.titleLarge) }
            items(occasions) { o ->
                Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { onOpen(Screen.Editor(o)) }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(o.emoji, fontSize = 32.sp)
                        Text(o.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    }
                }
            }
            item(span = { GridItemSpan(3) }) { Text(if (cards.isEmpty()) "Здесь появятся ваши открытки" else "Мои открытки", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp)) }
            items(cards) { f ->
                val bmp = remember(f) { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 }) }
                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)).clickable { onOpen(Screen.Viewer(f)) }, contentScale = ContentScale.Crop)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(occasion: Occasion, onBack: () -> Unit, onSaved: (File) -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    val stock = remember { stockList(ctx) }
    val cats = stockCats(occasion)
    val myStock = remember { stock.filter { cats.isEmpty() || it.category in cats }.ifEmpty { stock } }
    var base by remember { mutableStateOf<Bitmap?>(null) }
    var style by remember { mutableStateOf(CardStyle(text = occasion.wishes.firstOrNull() ?: "", aiLabel = settings.aiLabel)) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var showGen by remember { mutableStateOf(false) }
    var showStock by remember { mutableStateOf(false) }
    var chatPrompt by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(if (occasion.id == "poems") 0 else 1) }
    var boxSize by remember { mutableStateOf(IntSize(1, 1)) }
    val styleNow by rememberUpdatedState(style)

    LaunchedEffect(Unit) { if (base == null) base = myStock.firstOrNull()?.let { loadAsset(ctx, "stock/" + it.file) } ?: gradientBitmap(backgrounds[0]) }
    LaunchedEffect(base, style) { val b = base ?: return@LaunchedEffect; preview = withContext(Dispatchers.Default) { renderCard(b, style, 900) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val b = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (b != null) { base = b; style = style.copy(aiImage = false) }
        }
    }

    chatPrompt?.let { p ->
        WebChatScreen(settings, p, onImage = { b -> base = b; style = style.copy(aiImage = true); chatPrompt = null }, onClose = { chatPrompt = null })
        BackHandler { chatPrompt = null }
        return
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("${occasion.emoji} ${occasion.title}") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } })
    }, bottomBar = {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                val b = base ?: return@OutlinedButton
                scope.launch {
                    val f = withContext(Dispatchers.IO) { val out = renderCard(b, style); File(ctx.cacheDir, "share.png").also { f -> f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
                    shareFile(ctx, f)
                }
            }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Отправить") }
            Button(onClick = {
                val b = base ?: return@Button
                scope.launch {
                    val f = withContext(Dispatchers.IO) { val out = renderCard(b, style); File(Gallery.dir(ctx), "card_${System.currentTimeMillis()}.png").also { f -> f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
                    Toast.makeText(ctx, "Сохранено в «Мои открытки»", Toast.LENGTH_SHORT).show()
                    onSaved(f)
                }
            }, modifier = Modifier.weight(1f)) { Text("Сохранить") }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
                preview?.let { p ->
                    Image(p.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(p.width.toFloat() / p.height).clip(RoundedCornerShape(16.dp)).onSizeChanged { boxSize = it }.pointerInput(Unit) {
                        detectDragGestures { change, d ->
                            change.consume()
                            val s = styleNow
                            style = s.copy(x = (s.x + d.x / boxSize.width).coerceIn(0f, 1f), y = (s.y + d.y / boxSize.height).coerceIn(0f, 1f))
                        }
                    }, contentScale = ContentScale.FillWidth)
                }
                busy?.let {
                    Column(Modifier.background(Color(0xAA000000), RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White); Text(it, color = Color.White)
                    }
                }
            }
            Text("Двигайте текст пальцем прямо по открытке", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(4.dp))

            Section("1. Картинка")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showGen = true }) { Text("✨ Создать ИИ") }
                OutlinedButton(onClick = { showStock = true }) { Text("Готовые") }
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Из галереи") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                backgrounds.forEach { g ->
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(g.map { Color(it) })).clickable { base = gradientBitmap(g); style = style.copy(aiImage = false) })
                }
            }

            Section("2. Текст")
            TabRow(selectedTabIndex = tab) {
                listOf("Мои стихи", "Пожелания", "ИИ").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            when (tab) {
                0 -> {
                    val qs = Quotes.all(ctx).filter { it.category in occasion.poemCategories }.ifEmpty { Quotes.all(ctx) }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        qs.forEach { q -> Suggestion(q.text, "«${q.title}»") { style = style.copy(text = q.text, aiText = false) } }
                    }
                }
                1 -> Column(Modifier.padding(vertical = 6.dp)) {
                    (occasion.wishes.ifEmpty { occasions.flatMap { it.wishes }.take(6) }).forEach { w -> Suggestion(w, null) { style = style.copy(text = w, aiText = false) } }
                }
                else -> {
                    var hint by remember { mutableStateOf("") }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        OutlinedTextField(hint, { hint = it }, label = { Text("Кому и о чём (необязательно)") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            busy = "Пишу поздравление…"
                            scope.launch {
                                try { val t = Ai.caption(settings, occasion.title, hint); style = style.copy(text = t, aiText = true) }
                                catch (e: Exception) { Toast.makeText(ctx, "Ошибка ИИ: ${e.message}", Toast.LENGTH_LONG).show() }
                                busy = null
                            }
                        }, modifier = Modifier.padding(top = 6.dp)) { Text("Придумать текст") }
                    }
                }
            }
            OutlinedTextField(style.text, { style = style.copy(text = it, aiText = false) }, label = { Text("Текст на открытке — можно писать свой") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Section("3. Оформление")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fonts.forEachIndexed { i, f -> FilterChip(selected = style.font == i, onClick = { style = style.copy(font = i) }, label = { Text(f.title) }) }
            }
            Text("Размер")
            Slider(style.size, { style = style.copy(size = it) }, valueRange = 0.03f..0.14f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                textColors.forEach { c ->
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c)).border(if (style.color == c) 3.dp else 1.dp, if (style.color == c) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape).clickable { style = style.copy(color = c) })
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Слева", "По центру", "Справа").forEachIndexed { i, t -> FilterChip(selected = style.align == i, onClick = { style = style.copy(align = i) }, label = { Text(t) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Вверху" to 0.15f, "В центре" to 0.5f, "Внизу" to 0.82f).forEach { (t, y) -> FilterChip(selected = false, onClick = { style = style.copy(y = y, x = 0.5f) }, label = { Text(t) }) }
            }
            SwitchRow("Подложка под текст", style.plate) { style = style.copy(plate = it) }
            SwitchRow("Метка «Создано ИИ»", style.aiLabel) { style = style.copy(aiLabel = it); settings.aiLabel = it }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showStock) {
        AlertDialog(onDismissRequest = { showStock = false }, confirmButton = { TextButton(onClick = { showStock = false }) { Text("Закрыть") } }, title = { Text("Готовые картинки") }, text = {
            var all by remember { mutableStateOf(false) }
            Column {
                Row { FilterChip(selected = !all, onClick = { all = false }, label = { Text("К поводу") }); Spacer(Modifier.width(6.dp)); FilterChip(selected = all, onClick = { all = true }, label = { Text("Все") }) }
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.height(420.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(if (all) stock else myStock) { s ->
                        val th = remember(s.file) { ctx.assets.open("stock/" + s.file).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 }) } }
                        if (th != null) Image(th.asImageBitmap(), s.title, Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { base = loadAsset(ctx, "stock/" + s.file); style = style.copy(aiImage = false); showStock = false }, contentScale = ContentScale.Crop)
                    }
                }
            }
        })
    }

    if (showGen) {
        var details by remember { mutableStateOf("") }
        var st by remember { mutableStateOf("акварель") }
        var provider by remember { mutableStateOf(settings.provider) }
        AlertDialog(onDismissRequest = { showGen = false }, title = { Text("Создать картинку ИИ") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(details, { details = it }, label = { Text("Что нарисовать (необязательно)") }, modifier = Modifier.fillMaxWidth())
                Text("Стиль", Modifier.padding(top = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("акварель", "реализм, фото", "масло", "мультяшный", "винтажная открытка", "3D").forEach { v -> FilterChip(selected = st == v, onClick = { st = v }, label = { Text(v) }) }
                }
                Text("Где создать", Modifier.padding(top = 8.dp))
                listOf("chat" to "Через веб-чат ИИ (как в Иван Оператор)", "free" to "Бесплатно без входа (Pollinations)", "api" to "Свой API-ключ (настройки)").forEach { (id, t) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { provider = id }) { RadioButton(provider == id, { provider = id }); Text(t) }
                }
            }
        }, confirmButton = {
            Button(onClick = {
                showGen = false
                settings.provider = provider
                val prompt = cardPrompt(occasion, details, st)
                if (provider == "chat") chatPrompt = prompt
                else {
                    if (provider == "api" && settings.apiKey.isBlank()) { Toast.makeText(ctx, "Укажите ключ в настройках", Toast.LENGTH_LONG).show(); return@Button }
                    busy = "Рисую открытку…"
                    scope.launch {
                        try { base = Ai.image(settings, prompt); style = style.copy(aiImage = true) }
                        catch (e: Exception) { Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show() }
                        busy = null
                    }
                }
            }) { Text("Создать") }
        }, dismissButton = { TextButton(onClick = { showGen = false }) { Text("Отмена") } })
    }
}

@Composable
fun Section(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

@Composable
fun Suggestion(text: String, sub: String?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(10.dp)) {
            Text(text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SwitchRow(t: String, v: Boolean, on: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, Modifier.weight(1f)); Switch(v, on)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(file: File, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val bmp = remember(file) { BitmapFactory.decodeFile(file.path) }
    var ask by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Открытка") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }, actions = {
            IconButton(onClick = { ask = true }) { Icon(Icons.Default.Delete, "Удалить") }
        })
    }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.FillWidth)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { shareFile(ctx, file) }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Отправить") }
                OutlinedButton(onClick = {
                    val ok = bmp != null && saveToPhone(ctx, bmp)
                    Toast.makeText(ctx, if (ok) "Сохранено в Картинки/Открытки" else "Не получилось (нужен Android 10+)", Toast.LENGTH_SHORT).show()
                }) { Text("В галерею телефона") }
            }
        }
    }
    if (ask) AlertDialog(onDismissRequest = { ask = false }, title = { Text("Удалить открытку?") }, confirmButton = { TextButton(onClick = { file.delete(); ask = false; onBack() }) { Text("Удалить") } }, dismissButton = { TextButton(onClick = { ask = false }) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s = remember { Settings(ctx) }
    var base by remember { mutableStateOf(s.apiBase) }
    var key by remember { mutableStateOf(s.apiKey) }
    var im by remember { mutableStateOf(s.imageModel) }
    var tm by remember { mutableStateOf(s.textModel) }
    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }, navigationIcon = { IconButton(onClick = { s.apiBase = base; s.apiKey = key; s.imageModel = im; s.textModel = tm; onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Главный способ — веб-чат ИИ: ключ не нужен, вход в чат запоминается в приложении. Ниже — необязательный свой API (совместимый с OpenAI).")
            OutlinedTextField(base, { base = it }, label = { Text("Адрес API") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(key, { key = it }, label = { Text("Ключ API") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            OutlinedTextField(im, { im = it }, label = { Text("Модель картинок") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(tm, { tm = it }, label = { Text("Модель текста") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { s.apiBase = base; s.apiKey = key; s.imageModel = im; s.textModel = tm; Toast.makeText(ctx, "Сохранено", Toast.LENGTH_SHORT).show() }) { Text("Сохранить") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val stock = remember { stockList(ctx) }
    Scaffold(topBar = { TopAppBar(title = { Text("О приложении") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Открытки 1.0.0", style = MaterialTheme.typography.titleLarge)
            Text("Стихи — Иван Воронцов. Картинки, созданные ИИ, по умолчанию помечаются «Создано ИИ».", Modifier.padding(vertical = 8.dp))
            Text("Готовые картинки (общественное достояние / CC0, Wikimedia Commons):", fontWeight = FontWeight.Bold)
            stock.forEach { Text("• ${it.title.removePrefix("File:")} — ${it.license}${if (it.author.isNotBlank()) ", " + it.author else ""}\n  ${it.source}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}
