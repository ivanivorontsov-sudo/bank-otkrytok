package org.ivanivorontsov.otkrytki

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

const val CARD_SYSTEM_TEXT = "Ты — автор поздравительных открыток. Каждый твой ответ — готовый текст для красивой открытки на русском языке: тёплый, грамотный, искренний, без канцелярита и штампов. " +
    "Отвечай только самим текстом открытки: без кавычек, без эмодзи, без хештегов, без пояснений и вариантов. Не больше 4 коротких строк, чтобы текст красиво лёг на картинку."

const val CARD_SYSTEM_IMAGE = "Это иллюстрация для поздравительной открытки. Требования: профессиональная, очень красивая и качественная работа, гармоничная композиция, мягкий выразительный свет, " +
    "приятная палитра, высокая детализация, без искажений и артефактов, без лишних пальцев и деформированных лиц. На изображении НЕ должно быть никакого текста, букв, цифр, подписей, водяных знаков и логотипов. " +
    "Оставь спокойную свободную область (небо, фон или однотонную часть) в нижней или верхней трети для надписи. Вертикальный формат."

object Ai {
    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 30000
        readTimeout = 180000
        setRequestProperty("User-Agent", "Otkrytki/1.0 Android")
    }

    private fun readBytes(c: HttpURLConnection): ByteArray {
        val code = c.responseCode
        if (code !in 200..299) {
            val err = c.errorStream?.bufferedReader()?.readText()?.take(300) ?: ""
            throw RuntimeException("HTTP $code $err")
        }
        return c.inputStream.use { it.readBytes() }
    }

    private fun postJson(url: String, body: JSONObject, key: String?): JSONObject {
        val c = open(url)
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        if (!key.isNullOrBlank()) c.setRequestProperty("Authorization", "Bearer $key")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        return JSONObject(String(readBytes(c)))
    }

    fun decode(bytes: ByteArray): Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw RuntimeException("Не удалось прочитать картинку")

    suspend fun download(url: String): Bitmap = withContext(Dispatchers.IO) {
        if (url.startsWith("data:")) decode(Base64.decode(url.substringAfter(","), Base64.DEFAULT))
        else decode(readBytes(open(url)))
    }

    suspend fun image(s: Settings, prompt: String): Bitmap = withContext(Dispatchers.IO) {
        if (s.provider == "api") {
            val body = JSONObject().put("model", s.imageModel).put("prompt", "$CARD_SYSTEM_IMAGE\n\n$prompt").put("n", 1).put("size", "1024x1024")
            val d = postJson("${s.apiBase}/images/generations", body, s.apiKey).getJSONArray("data").getJSONObject(0)
            val b64 = d.optString("b64_json")
            if (b64.isNotBlank()) decode(Base64.decode(b64, Base64.DEFAULT)) else decode(readBytes(open(d.getString("url"))))
        } else {
            val enc = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val seed = Random.nextInt(1, 999999)
            decode(readBytes(open("https://image.pollinations.ai/prompt/$enc?width=1024&height=1280&nologo=true&seed=$seed")))
        }
    }

    suspend fun caption(s: Settings, occasion: String, extra: String): String = withContext(Dispatchers.IO) {
        val ask = "Напиши короткое тёплое поздравление для открытки на русском языке. Повод: $occasion. ${extra.trim()} Не больше 4 коротких строк, без кавычек, без эмодзи, без пояснений."
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", CARD_SYSTEM_TEXT)).put(JSONObject().put("role", "user").put("content", ask))
        val json = if (s.provider == "api") {
            postJson("${s.apiBase}/chat/completions", JSONObject().put("model", s.textModel).put("messages", messages), s.apiKey)
        } else {
            postJson("https://text.pollinations.ai/openai", JSONObject().put("model", "openai").put("messages", messages), null)
        }
        json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim().trim('"', '«', '»')
    }

    private suspend fun chat(s: Settings, system: String, user: String): String = withContext(Dispatchers.IO) {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user))
        val json = if (s.provider == "api" && s.apiKey.isNotBlank()) postJson("${s.apiBase}/chat/completions", JSONObject().put("model", s.textModel).put("messages", messages), s.apiKey)
        else postJson("https://text.pollinations.ai/openai", JSONObject().put("model", "openai").put("messages", messages), null)
        json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }

    /** ИИ выбирает из стихов Ивана Воронцова самый подходящий к поводу. Возвращает индекс в списке. */
    suspend fun pickPoem(s: Settings, occasion: String, extra: String, poems: List<Quote>): Int {
        val list = poems.take(60).mapIndexed { i, q -> "$i. «${q.title}»: ${q.text.lines().take(4).joinToString(" / ")}" }.joinToString("\n")
        val ans = chat(s, "Ты помогаешь подобрать стихотворение автора Ивана Воронцова для поздравительной открытки. Отвечай только номером стихотворения, одним числом.",
            "Повод: $occasion. ${extra.trim()}\nВыбери самое подходящее и красивое для открытки стихотворение:\n$list")
        return Regex("\\d+").find(ans)?.value?.toIntOrNull()?.takeIf { it in poems.indices } ?: 0
    }
}
