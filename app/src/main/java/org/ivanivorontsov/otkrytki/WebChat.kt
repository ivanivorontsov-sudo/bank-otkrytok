package org.ivanivorontsov.otkrytki

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatSite(val id: String, val title: String, val url: String)

val chatSites = listOf(
    ChatSite("alice", "Алиса", "https://alice.yandex.ru/"),
    ChatSite("chatgpt", "ChatGPT", "https://chatgpt.com/"),
    ChatSite("gemini", "Gemini", "https://gemini.google.com/app"),
    ChatSite("grok", "Grok", "https://grok.com/"),
    ChatSite("qwen", "Qwen", "https://chat.qwen.ai/"),
    ChatSite("copilot", "Copilot", "https://copilot.microsoft.com/"),
    ChatSite("glm", "GLM", "https://chat.z.ai/")
)

fun cardPrompt(occasion: Occasion, details: String, style: String): String =
    "Сгенерируй изображение (нарисуй картинку) для поздравительной открытки. Ты — профессиональный художник-иллюстратор открыток. " +
        "Сделай очень красивую, качественную, детальную и гармоничную иллюстрацию с профессиональным светом и приятными цветами, вертикальный формат 4:5. " +
        "ВАЖНО: на изображении не должно быть никакого текста, букв, цифр и надписей — подпись добавят отдельно; оставь немного спокойного свободного места для подписи. " +
        "Сразу создай изображение, ничего не уточняй и не пиши пояснений. " +
        "Повод: ${occasion.title}. Идея: ${occasion.imageHint}." +
        (if (details.isNotBlank()) " Пожелания автора: ${details.trim()}." else "") +
        (if (style.isNotBlank()) " Стиль: $style." else "")

private fun js(s: String) = JSONObject.quote(s)

private const val SEND_JS = """(function(P){var vis=function(e){return e&&e.getClientRects().length};
var eds=[].slice.call(document.querySelectorAll('textarea,[contenteditable=true],div[role=textbox]')).filter(vis);var ed=eds[eds.length-1];
if(!ed)return 'noinput';window.__okOld={};[].slice.call(document.images).forEach(function(i){window.__okOld[i.currentSrc||i.src]=1});window.__okT=Date.now();
ed.focus();if(ed.tagName==='TEXTAREA'||ed.tagName==='INPUT'){var s=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(ed),'value').set;s.call(ed,P);ed.dispatchEvent(new Event('input',{bubbles:true}));}
else{document.execCommand('selectAll',false);document.execCommand('insertText',false,P);}
setTimeout(function(){var b=[].slice.call(document.querySelectorAll('button')).filter(function(b){return vis(b)&&!b.disabled&&/send|отправ|submit/i.test((b.getAttribute('aria-label')||'')+' '+(b.getAttribute('data-testid')||'')+' '+(b.title||''))}).pop();
if(b)b.click();else ed.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true}));},700);return 'sent';})"""

private const val POLL_JS = """(function(){var old=window.__okOld||{};var vis=function(e){return e&&e.getClientRects().length};
var busy=[].slice.call(document.querySelectorAll('button,[role=button]')).some(function(b){return vis(b)&&/(^|\W)(stop|остановить|прервать|стоп)/i.test((b.getAttribute('aria-label')||'')+' '+(b.title||'')+' '+(b.getAttribute('data-testid')||''))});
var best=null,ba=0;[].slice.call(document.images).forEach(function(i){var s=i.currentSrc||i.src;if(!s||old[s]||!i.complete)return;var w=i.naturalWidth,h=i.naturalHeight;if(w<380||h<380)return;var r=i.getBoundingClientRect();if(r.width<120)return;if(w*h>ba){ba=w*h;best=s}});
return JSON.stringify({src:best,busy:busy});})()"""

private const val GRAB_JS = """(function(S){fetch(S,{credentials:'include'}).then(function(r){return r.blob()}).then(function(b){var f=new FileReader();f.onload=function(){OkBridge.onData(f.result)};f.readAsDataURL(b)}).catch(function(e){OkBridge.onUrl(S)});})"""

class Bridge(val onData: (String) -> Unit, val onUrl: (String) -> Unit) {
    @JavascriptInterface fun onData(d: String) = onData.invoke(d)
    @JavascriptInterface fun onUrl(u: String) = onUrl.invoke(u)
}

private fun downloadWithCookies(url: String, pageUrl: String): Bitmap {
    val c = URL(url).openConnection() as HttpURLConnection
    CookieManager.getInstance().getCookie(url)?.let { c.setRequestProperty("Cookie", it) }
    c.setRequestProperty("Referer", pageUrl)
    c.connectTimeout = 30000
    c.readTimeout = 60000
    if (c.responseCode !in 200..299) throw RuntimeException("HTTP ${c.responseCode}")
    return Ai.decode(c.inputStream.use { it.readBytes() })
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebChatScreen(settings: Settings, prompt: String, onImage: (Bitmap) -> Unit, onClose: () -> Unit) {
    var site by remember { mutableStateOf(chatSites.firstOrNull { it.id == settings.chatSite } ?: chatSites[0]) }
    var status by remember { mutableStateOf("Откройте чат. Если сайт просит войти — войдите один раз, вход запомнится.") }
    var working by remember { mutableStateOf(false) }
    var web by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    var finished by remember { mutableStateOf(false) }

    fun deliver(b: Bitmap) {
        if (finished) return
        finished = true
        working = false
        onImage(b)
    }

    fun grab(src: String) {
        status = "Забираю картинку…"
        web?.evaluateJavascript("$GRAB_JS(${js(src)})", null)
    }

    val bridge = remember {
        Bridge(
            onData = { d -> scope.launch { try { deliver(Ai.download(d)) } catch (e: Exception) { status = "Не удалось прочитать картинку: ${e.message}" } } },
            onUrl = { u -> scope.launch { try { val page = web?.url ?: ""; deliver(withContext(Dispatchers.IO) { downloadWithCookies(u, page) }) } catch (e: Exception) { status = "Не удалось скачать картинку: ${e.message}. Долгий тап по картинке — попробовать ещё раз." } } }
        )
    }

    fun poll(started: Long, last: String?, stable: Int) {
        val w = web ?: return
        if (finished || !working) return
        if (System.currentTimeMillis() - started > 300000) { working = false; status = "Чат долго не отвечает. Нажмите «Забрать картинку» или долгий тап по картинке."; return }
        w.evaluateJavascript(POLL_JS) { raw ->
            val o = try { JSONObject(JSONObject("{\"v\":$raw}").getString("v")) } catch (e: Exception) { null }
            val src = o?.optString("src")?.takeIf { it.isNotBlank() && it != "null" }
            val busy = o?.optBoolean("busy") ?: true
            val st = if (src != null && src == last && !busy) stable + 1 else 0
            if (src != null && st >= 2) grab(src) else main.postDelayed({ poll(started, src, st) }, 2500)
        }
    }

    fun send() {
        val w = web ?: return
        finished = false
        w.evaluateJavascript("$SEND_JS(${js(prompt)})") { r ->
            if (r?.contains("noinput") == true) { status = "Не нашёл поле ввода. Войдите в чат или дождитесь загрузки и нажмите ещё раз."; return@evaluateJavascript }
            working = true
            status = "Запрос отправлен. Жду картинку от ${site.title}…"
            main.postDelayed({ poll(System.currentTimeMillis(), null, 0) }, 6000)
        }
    }

    DisposableEffect(Unit) { onDispose { working = false; web?.destroy() } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chatSites.forEach { s ->
                FilterChip(selected = s == site, onClick = { site = s; settings.chatSite = s.id; working = false; web?.loadUrl(s.url) }, label = { Text(s.title) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { send() }, enabled = !working) { Text("Создать открытку") }
            OutlinedButton(onClick = {
                web?.evaluateJavascript(POLL_JS) { raw ->
                    val src = try { JSONObject(JSONObject("{\"v\":$raw}").getString("v")).optString("src") } catch (e: Exception) { "" }
                    if (src.isNotBlank() && src != "null") grab(src) else status = "Новой картинки пока нет. Можно долгим тапом выбрать любую картинку в чате."
                }
            }) { Text("Забрать картинку") }
            OutlinedButton(onClick = onClose) { Text("Назад") }
        }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(8.dp))
        Text(status, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            WebView(ctx).apply {
                this.settings.javaScriptEnabled = true
                this.settings.domStorageEnabled = true
                this.settings.databaseEnabled = true
                this.settings.loadWithOverviewMode = true
                this.settings.useWideViewPort = true
                this.settings.javaScriptCanOpenWindowsAutomatically = true
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                addJavascriptInterface(bridge, "OkBridge")
                setOnLongClickListener {
                    val h = hitTestResult
                    val u = h.extra
                    if ((h.type == WebView.HitTestResult.IMAGE_TYPE || h.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && u != null) { finished = false; grab(u); true } else false
                }
                loadUrl(site.url)
                web = this
            }
        })
    }
}
