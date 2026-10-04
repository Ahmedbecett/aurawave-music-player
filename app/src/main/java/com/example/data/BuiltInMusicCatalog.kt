package com.example.data

import com.example.R
import com.example.model.LyricLine
import com.example.model.Song

object BuiltInMusicCatalog {

    fun parseLyrics(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val regex = Regex("\\[(\\d{2}):(\\d{2}(?:\\.\\d{1,2})?)\\](.*)")
        val lines = mutableListOf<LyricLine>()
        raw.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val secStr = match.groupValues[2]
                val sec = (secStr.toDoubleOrNull() ?: 0.0) * 1000.0
                val totalMs = (min * 60 * 1000) + sec.toLong()
                val text = match.groupValues[3].trim()
                lines.add(LyricLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    val defaultSongs: List<Song> = listOf(
        Song(
            id = "song_1",
            title = "أندلسيات العود",
            artist = "فرقة سحر الشرق",
            album = "ليالي الأندلس",
            durationMs = 215000,
            dataUri = "builtin://song_1",
            albumArtRes = R.drawable.album_oriental_1791115616894,
            genre = "موسيقى شرقية",
            year = 2026,
            isBundled = true,
            synthPatternId = 1,
            lyrics = """
                [00:04.00] همس الأوتار في سكون الليل الجميل
                [00:15.00] تعزف ألحاناً من زمن الأندلس الأصيل
                [00:28.00] وترٌ يشدو بنغم الشوق والحنين
                [00:42.00] وإيقاعٌ ينساب كخرير ماء العيون
                [00:58.00] بين القصور العتيقة ورائحة الياسمين
                [01:14.00] يروي العود حكاية نغمٍ لا يموت
                [01:32.00] سحر المقامات يعلو بنقاء في السماء
                [01:50.00] مقام البيات يلامس أوتار القلوب
                [02:10.00] تتراقص الأنغام كنجوم الليل البهي
                [02:30.00] حتى ينبلج فجر الصباح الهادئ المعطر
                [02:50.00] ويبقى اللحن خالداً في ذاكرة الزمان
            """.trimIndent()
        ),
        Song(
            id = "song_2",
            title = "أفق النيون والسرعة",
            artist = "أحمد بن ستي (Ahmed Becetti)",
            album = "نبضات المستقبل",
            durationMs = 198000,
            dataUri = "builtin://song_2",
            albumArtRes = R.drawable.album_synthwave_1791115629921,
            genre = "سينث ويف",
            year = 2026,
            isBundled = true,
            synthPatternId = 2,
            lyrics = """
                [00:05.00] أنوار النيون تتوهج في شوارع المدينة
                [00:18.00] نبضات السينث ترسم خطاً نحو الأفق البعيد
                [00:32.00] إيقاع إلكتروني سريع يملأ الروح بالحياة
                [00:48.00] سرعة وحرية تحت سماء أرجوانية متألقة
                [01:05.00] نغمات المستقبل تنطلق بقوة وجرأة
                [01:25.00] ترددات البيس تهز المكان بحماس
                [01:45.00] في رحلة رقمية لا تنتهي بين النجوم
                [02:08.00] نغمات متسارعة تحاكي سرعة العصر
                [02:30.00] ونبقى نحلم على صدى الموسيقى النقية
            """.trimIndent()
        ),
        Song(
            id = "song_3",
            title = "سكينة البيانو الهادئ",
            artist = "سارة المنصور",
            album = "همسات الفجر",
            durationMs = 184000,
            dataUri = "builtin://song_3",
            albumArtRes = R.drawable.album_piano_1791115642905,
            genre = "كلاسيك حديث",
            year = 2026,
            isBundled = true,
            synthPatternId = 3,
            lyrics = """
                [00:06.00] لمسات هادئة على مفاتيح البيانو الرقيقة
                [00:20.00] تنشر السلام والسكينة في أرجاء المكان
                [00:36.00] نغمٌ عذب يداعب المشاعر والوجدان
                [00:54.00] قطرات المطر اللطيفة تعزف مع اللحن
                [01:12.00] لحظات من الصفاء والنقاء الروحي العميق
                [01:30.00] سكونٌ تام يعيد ترتيب الأفكار بهدوء
                [01:52.00] نغمات تعلو برفق وتلامس السحاب
                [02:15.00] ثم تهدأ بسلام كنسيم المساء الرائع
            """.trimIndent()
        ),
        Song(
            id = "song_4",
            title = "جاز مقهى المطر",
            artist = "رباعي الجاز الأزرق",
            album = "رذاذ منتصف الليل",
            durationMs = 220000,
            dataUri = "builtin://song_4",
            albumArtRes = R.drawable.album_synthwave_1791115629921,
            genre = "جاز",
            year = 2025,
            isBundled = true,
            synthPatternId = 4,
            lyrics = """
                [00:08.00] صوت الساكسفون ينساب دافئاً ومريحاً
                [00:22.00] كوب قهوة ساخن على طاولة خشبية عتيقة
                [00:38.00] قطرات المطر تنقر زجاج النافذة برقة
                [00:56.00] وتريات الباص تدندن بانتظام ساحر وممتع
                [01:16.00] ارتجال موسيقي دافئ يخفف برد المساء
                [01:40.00] رقصة هادئة تجمع بين الإيقاع والميلودي
                [02:05.00] ذكريات جميلة تستيقظ مع كل نغمة جاز
                [02:35.00] سهرة ممتعة تدوم حتى آخر الليل
            """.trimIndent()
        ),
        Song(
            id = "song_5",
            title = "نسائم الصحراء الذهبية",
            artist = "أحمد بن ستي (Ahmed Becetti)",
            album = "رحلة الرمال",
            durationMs = 240000,
            dataUri = "builtin://song_5",
            albumArtRes = R.drawable.album_oriental_1791115616894,
            genre = "موسيقى عالمية",
            year = 2026,
            isBundled = true,
            synthPatternId = 5,
            lyrics = """
                [00:06.00] هبوب نسيم الفجر فوق الرمال الذهبية اللامعة
                [00:24.00] قافلة تعبر الصحراء تحت بريق النجوم اللامعة
                [00:44.00] أصداء الناي الرقيق تتردد في المدى الواسع
                [01:08.00] دفء الضيافة والنار المتقدة تحت خيمة المساء
                [01:35.00] حكايا الأجداد وتراث الأصالة ترويها الأوتار
                [02:05.00] إيقاع الطبول الأصيلة يعانق أفق الكثبان
                [02:40.00] نغمٌ يختزل عظمة الصحراء وسحر الشرق
            """.trimIndent()
        ),
        Song(
            id = "song_6",
            title = "نبض الطاقة والإيقاع",
            artist = "دي جي كوانتوم",
            album = "طاقة إلكترونية",
            durationMs = 205000,
            dataUri = "builtin://song_6",
            albumArtRes = R.drawable.nagham_app_icon_1791115603017,
            genre = "إلكتروني",
            year = 2026,
            isBundled = true,
            synthPatternId = 6,
            lyrics = """
                [00:05.00] رفع مستوى الصوت والبيس لأعلى مستوى
                [00:18.00] ترددات صوتية نقية تشعل الحماس
                [00:35.00] موجات الإيقاع المتتالية تملأ الأجواء حيوية
                [00:55.00] تحرك مع كل نغمة وانسَ كل القيود
                [01:20.00] طاقة إيجابية متدفقة تلهم الروح
                [01:45.00] هارموني مذهل بين التقنية والإحساس الفني
                [02:10.00] تجربة استماع غامرة تأخذك لأبعاد جديدة
            """.trimIndent()
        )
    )
}
