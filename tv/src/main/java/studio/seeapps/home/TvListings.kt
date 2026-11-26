package studio.seeapps.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.media.tv.TvContract.Channels
import android.media.tv.TvContract.PreviewPrograms
import android.media.tv.TvContract.WatchNextPrograms
import android.net.Uri
import android.os.Build

internal data class TvListing(
    val title: String?,
    val type: Int,
    val link: String?,
    val art: String?,
    val duration: Long,
    val position: Long,
    val aspectCode: Int? = null,
) {
    fun toProgram(pkg: String = SeeClap.PKG, section: String? = null): Program? {
        val seeClap = SeeClap.isSeeClap(pkg)
        val live = type == PreviewPrograms.TYPE_CHANNEL

        if (seeClap && !live && type != PreviewPrograms.TYPE_MOVIE &&
            type != PreviewPrograms.TYPE_TV_SERIES && type != PreviewPrograms.TYPE_TV_EPISODE) return null
        val name = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (link.isNullOrBlank() || (seeClap && !link.startsWith("seeclap://open#"))) return null
        val timed = !live && duration in 1..Int.MAX_VALUE.toLong() && position in 1 until duration

        val minutes = if (timed && !(seeClap && duration == 3_600_000L)) ((duration - position + 59_999) / 60_000).toInt() else 0
        return Program(
            live = live, title = name.take(200), meta = "",
            image = art?.takeIf { !seeClap || it.startsWith("https://image.tmdb.org/t/p/") },
            progress = if (timed) position.toFloat() / duration else 0f, remainingMin = minutes,
            link = link, restartLink = null, opensDetails = true, section = section,
            artAspect = cardAspect(pkg, type, aspectCode),
        )
    }
}

private const val YOUTUBE_TV = "com.google.android.youtube.tv"
private const val MOVIE_POSTER = 5

private fun isVod(type: Int) = type == PreviewPrograms.TYPE_MOVIE ||
    type == PreviewPrograms.TYPE_TV_SERIES || type == PreviewPrograms.TYPE_TV_EPISODE

private fun cardAspect(pkg: String, type: Int, code: Int?): Float {
    if (SeeClap.isSeeClap(pkg) && isVod(type)) return 2f / 3
    if (pkg == YOUTUBE_TV) return 16f / 9
    return when (code) {
        PreviewPrograms.ASPECT_RATIO_16_9 -> 16f / 9
        PreviewPrograms.ASPECT_RATIO_3_2 -> 3f / 2
        PreviewPrograms.ASPECT_RATIO_4_3 -> 4f / 3
        PreviewPrograms.ASPECT_RATIO_1_1 -> 1f
        PreviewPrograms.ASPECT_RATIO_2_3, MOVIE_POSTER -> 2f / 3
        else -> if (type == PreviewPrograms.TYPE_MOVIE || type == PreviewPrograms.TYPE_TV_SERIES) 2f / 3 else 16f / 9
    }
}

internal data class TvChannel(val id: Long, val name: String, val onSystemHome: Boolean)

internal object TvListings {
    const val PERMISSION = "android.permission.READ_TV_LISTINGS"
    private const val LIMIT = 8
    private data class Card(val program: Program, val preferredArt: Boolean)

    fun allowed(context: Context) = context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun observedUris(): List<Uri> = if (Build.VERSION.SDK_INT >= 26) {
        listOf(WatchNextPrograms.CONTENT_URI, PreviewPrograms.CONTENT_URI, Channels.CONTENT_URI)
    } else emptyList()

    fun channelsForFavorite(channels: List<TvChannel>): List<TvChannel> =
        channels.filter { it.onSystemHome }.ifEmpty { channels }

    fun read(context: Context, pkg: String): List<Program> {
        if (Build.VERSION.SDK_INT < 26 || !allowed(context)) return emptyList()
        val seeClap = SeeClap.isSeeClap(pkg)
        val cards = LinkedHashMap<String, Card>()
        if (!seeClap) {
            readCards(context, pkg, WatchNextPrograms.CONTENT_URI, null, cards)
            if (cards.size >= LIMIT) return cards.values.map { it.program }
        }
        val uri = forPackage(Channels.CONTENT_URI, pkg)
        val columns = arrayOf("package_name", "_id", "type", "browsable", "display_name", "searchable")
        val channels = ArrayList<TvChannel>()
        runCatching {
            context.contentResolver.query(uri, columns, null, null, "_id ASC")?.use { c ->
                while (c.moveToNext()) {
                    if (c.getString(0) == pkg && c.getString(2) == Channels.TYPE_PREVIEW && c.getInt(5) == 1) {
                        channels += TvChannel(c.getLong(1), c.getString(4).orEmpty(), c.getInt(3) == 1)
                    }
                }
            }
        }
        for (channel in channelsForFavorite(channels)) {
            if (!seeClap && cards.size >= LIMIT) break
            readCards(context, pkg, TvContract.buildPreviewProgramsUriForChannel(channel.id), channel.name, cards)
        }
        if (seeClap) {
            readCards(context, pkg, WatchNextPrograms.CONTENT_URI, null, cards)
            return shelfSections(cards.values.map { it.program })
        }
        return cards.values.map { it.program }
    }

    fun shelfSections(items: Collection<Program>): List<Program> {
        val groups = items.groupBy { it.section }.values
        val chosen = groups.flatMap { it.withIndex() }.sortedBy { it.index }.take(LIMIT).mapTo(HashSet()) { it.value.link }
        return groups.flatten().filter { it.link in chosen }
    }

    private fun forPackage(uri: Uri, pkg: String) = uri.buildUpon().appendQueryParameter("package", pkg).build()

    private fun readCards(context: Context, pkg: String, uri: Uri, channel: String?, cards: MutableMap<String, Card>) {
        val watchNext = channel == null
        val seeClap = SeeClap.isSeeClap(pkg)
        val columns = arrayOf(
            "package_name", "browsable", "title", "type", "intent_uri", "poster_art_uri",
            "duration_millis", "last_playback_position_millis", "thumbnail_uri", "short_description",
        ) + if (watchNext) arrayOf("watch_next_type") else emptyArray()
        val order = if (watchNext) "last_engagement_time_utc_millis DESC" else "weight DESC"
        runCatching {
            val target = forPackage(uri, pkg)
            val aspects = arrayOf(PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO)
            fun query(projection: Array<String>) = context.contentResolver.query(target, projection, null, null, order)

            val cursor = try { query(columns + aspects) } catch (_: IllegalArgumentException) { query(columns) }
                catch (_: android.database.sqlite.SQLiteException) { query(columns) }
            cursor?.use { c ->
                val posterRatio = c.getColumnIndex(aspects[0])
                val thumbRatio = c.getColumnIndex(aspects[1])
                while ((seeClap || cards.size < LIMIT) && c.moveToNext()) {
                    if (c.getString(0) != pkg || c.getInt(1) != 1) continue
                    val resume = watchNext && c.getInt(10) == WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE
                    if (seeClap && watchNext && !resume) continue
                    val type = c.getInt(3)
                    val section = if (seeClap) context.getString(when (type) {
                        PreviewPrograms.TYPE_MOVIE -> R.string.shelf_movies
                        PreviewPrograms.TYPE_TV_SERIES, PreviewPrograms.TYPE_TV_EPISODE -> R.string.shelf_series
                        PreviewPrograms.TYPE_CHANNEL -> R.string.live_now
                        else -> continue
                    }) else if (resume) null else channel.orEmpty().take(100)
                    val raw = c.getString(4) ?: continue
                    val intent = cardIntent(pkg, raw) ?: continue
                    val link = if (seeClap) intent.dataString ?: continue else intent.toUri(Intent.URI_INTENT_SCHEME)
                    val previous = cards[link]
                    if (previous?.preferredArt == true) continue
                    if (seeClap && previous == null && cards.values.count { it.program.section == section } >= LIMIT) continue
                    val thumb = artwork(context, pkg, c.getString(8))
                    val poster = artwork(context, pkg, c.getString(5))
                    fun aspectAt(index: Int) = if (index >= 0 && !c.isNull(index)) c.getInt(index) else null
                    val thumbAspect = aspectAt(thumbRatio)
                    val posterAspect = aspectAt(posterRatio)
                    fun matchesFrame(aspect: Int?) = when {
                        seeClap && isVod(type) -> aspect == PreviewPrograms.ASPECT_RATIO_2_3 || aspect == MOVIE_POSTER
                        pkg == YOUTUBE_TV -> aspect == PreviewPrograms.ASPECT_RATIO_16_9
                        else -> true
                    }
                    val usePoster = poster != null && (thumb == null || (matchesFrame(posterAspect) && !matchesFrame(thumbAspect)))
                    val image = if (usePoster) poster else thumb
                    val aspect = if (usePoster) posterAspect else thumbAspect
                    val preferred = image != null && matchesFrame(aspect)
                    if (previous != null) {
                        if (image != null && (preferred || previous.program.image == null)) {
                            cards[link] = Card(previous.program.copy(image = image), preferred)
                        }
                        continue
                    }
                    val row = TvListing(c.getString(2), type, link, image, c.getLong(6), c.getLong(7), aspect)
                    val program = row.toProgram(pkg, section) ?: continue
                    cards[link] = Card(program.copy(overview = c.getString(9).orEmpty().take(600)), preferred)
                }
            }
        }
    }

    private fun artwork(context: Context, pkg: String, value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (SeeClap.isSeeClap(pkg)) return value.takeIf { it.startsWith("https://image.tmdb.org/t/p/") }
        val uri = Uri.parse(value)
        val authority = uri.authority ?: return null
        return when (uri.scheme) {
            "https" -> value
            "android.resource" -> value.takeIf { authority == pkg }
            "content" -> value.takeIf { context.packageManager.resolveContentProvider(authority, 0)?.packageName == pkg }
            else -> null
        }
    }

    private fun cardIntent(pkg: String, raw: String): Intent? = runCatching {
        val intent = Intent.parseUri(raw, 0)
        if (intent.selector != null || intent.component?.packageName?.let { it != pkg } == true ||
            intent.`package`?.let { it != pkg } == true || intent.data?.scheme == "file") return null
        intent.setPackage(pkg).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.getOrNull()

    fun open(context: Context, pkg: String, link: String?): Boolean {
        val intent = link?.let { cardIntent(pkg, it) } ?: return false
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
