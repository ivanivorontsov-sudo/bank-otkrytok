package org.ivanivorontsov.otkrytki

import android.content.Context
import org.json.JSONArray
import java.io.File

data class Occasion(val id: String, val title: String, val emoji: String, val imageHint: String, val poemCategories: List<String>, val wishes: List<String>)

data class Quote(val category: String, val title: String, val text: String, val categories: List<String> = listOf(category))

val occasions = listOf(
    Occasion("birthday", "День рождения", "🎂", "festive birthday card, cake with candles, balloons, confetti, warm colors", listOf("Радость и отдых", "Поддержка", "Восхищение и любовь"), listOf("С днём рождения!\nПусть каждый день будет светлым!", "Счастья, здоровья\nи исполнения желаний!", "С праздником!\nПусть сбудется всё задуманное!")),
    Occasion("newyear", "Новый год", "🎄", "cozy new year card, decorated fir tree, snow, garlands, night sky with fireworks", listOf("Новый год", "Природа", "Поддержка"), listOf("С Новым годом!\nПусть он будет добрым и щедрым!", "Пусть Новый год принесёт\nрадость, тепло и удачу!")),
    Occasion("march8", "8 Марта", "🌷", "spring card for women's day, tulips and mimosa, soft pastel colors", listOf("Восхищение и любовь", "Природа"), listOf("С 8 Марта!\nВесеннего настроения и улыбок!", "Пусть весна дарит\nсчастье и вдохновение!")),
    Occasion("feb23", "23 Февраля", "🎖️", "card for defender of the fatherland day, red star, ribbon, winter sky, elegant", listOf("Поддержка"), listOf("С 23 Февраля!\nСилы, мужества и мира!", "С Днём защитника Отечества!")),
    Occasion("victory", "День Победы", "🎗️", "victory day card, red carnations, st george ribbon, may 9, solemn", listOf("День Победы"), listOf("С Днём Победы!\nВечная память героям!")),
    Occasion("wedding", "Свадьба", "💍", "elegant wedding card, white roses, rings, soft golden light, delicate lace", listOf("Восхищение и любовь"), listOf("С днём свадьбы!\nЛюбви, согласия и счастья!", "Пусть ваш союз\nбудет крепким и светлым!")),
    Occasion("condolence", "Соболезнование", "🕯️", "calm respectful condolence card, white lilies, soft candle light, muted colors, peaceful", listOf("Мудрость", "Поддержка"), listOf("Скорбим вместе с вами.\nСветлая память.", "Примите искренние соболезнования.")),
    Occasion("love", "Любовь", "❤️", "romantic card, hearts, roses, soft light, tender colors", listOf("Восхищение и любовь"), listOf("Ты — моё вдохновение!", "Люблю тебя!\nСпасибо, что ты есть!")),
    Occasion("friends", "Дружба", "🤝", "friendship card, two cups of tea, sunny park, cheerful colors", listOf("Радость и отдых", "Поддержка"), listOf("Спасибо, что ты рядом!", "Настоящий друг — это счастье!")),
    Occasion("thanks", "Спасибо", "🙏", "thank you card, bouquet of flowers, warm golden light", listOf("Восхищение и любовь"), listOf("Спасибо от всего сердца!", "Благодарю за доброту и помощь!")),
    Occasion("support", "Поддержка", "🌤️", "hopeful card, sun breaking through clouds over a field, calm", listOf("Поддержка", "Мудрость"), listOf("Всё получится!\nЯ в тебя верю!", "Держись!\nЗа тучами всегда солнце.")),
    Occasion("nature", "Природа и время года", "🍂", "beautiful seasonal landscape card, forest, river, golden autumn", listOf("Природа"), listOf("Хорошего дня\nи доброго настроения!")),
    Occasion("poems", "Мои стихи", "✒️", "poetic artistic card, watercolor landscape, soft light, space for text", listOf("Поддержка", "Природа", "Восхищение и любовь", "Мудрость", "Радость и отдых", "День Победы", "Новый год"), emptyList()),
    Occasion("any", "Просто так", "✨", "cheerful greeting card, flowers and soft bokeh", listOf("Радость и отдых", "Мудрость"), listOf("Просто хочу пожелать\nхорошего дня!", "Улыбнись — ты чудесный человек!"))
)

object Quotes {
    private var cache: List<Quote>? = null
    fun all(ctx: Context): List<Quote> {
        cache?.let { return it }
        val list = listOf("quotes.json", "poems.json").flatMap { f ->
            val arr = JSONArray(ctx.assets.open(f).bufferedReader().readText())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                val cs = o.optJSONArray("categories")?.let { a -> (0 until a.length()).map { i -> a.getString(i) } } ?: listOf(o.getString("category"))
                Quote(o.getString("category"), o.getString("title"), o.getString("text"), cs)
            }
        }.distinctBy { it.text }
        cache = list
        return list
    }
}

fun poemsFor(all: List<Quote>, o: Occasion): List<Quote> =
    all.filter { q -> q.categories.any { it in o.poemCategories } }.sortedBy { q -> q.categories.indexOfFirst { it in o.poemCategories } * 10 + (q.text.length / 400) }.ifEmpty { all }

class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var apiBase: String
        get() = p.getString("apiBase", "https://api.openai.com/v1") ?: ""
        set(v) = p.edit().putString("apiBase", v.trim().trimEnd('/')).apply()
    var apiKey: String
        get() = p.getString("apiKey", "") ?: ""
        set(v) = p.edit().putString("apiKey", v.trim()).apply()
    var imageModel: String
        get() = p.getString("imageModel", "dall-e-3") ?: ""
        set(v) = p.edit().putString("imageModel", v.trim()).apply()
    var textModel: String
        get() = p.getString("textModel", "gpt-4o-mini") ?: ""
        set(v) = p.edit().putString("textModel", v.trim()).apply()
    var provider: String
        get() = p.getString("provider", "chat") ?: "chat"
        set(v) = p.edit().putString("provider", v).apply()
    var chatSite: String
        get() = p.getString("chatSite", "alice") ?: "alice"
        set(v) = p.edit().putString("chatSite", v).apply()
    var aiLabel: Boolean
        get() = p.getBoolean("aiLabel", true)
        set(v) = p.edit().putBoolean("aiLabel", v).apply()
}

object Gallery {
    fun dir(ctx: Context): File = File(ctx.filesDir, "cards").apply { mkdirs() }
    fun list(ctx: Context): List<File> = dir(ctx).listFiles { f -> f.extension == "png" || f.extension == "jpg" }?.sortedByDescending { it.lastModified() } ?: emptyList()
}
