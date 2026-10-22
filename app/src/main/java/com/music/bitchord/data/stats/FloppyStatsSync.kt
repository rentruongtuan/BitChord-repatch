package com.music.bitchord.data.stats

import com.music.bitchord.data.Http
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.scrobbling.ListenBrainzManager
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Request
import java.time.LocalDate

/**
 * Đồng bộ số liệu Replay / Thống kê nghe nhạc từ máy chủ cá nhân Floppy.
 * Đảm bảo số phút nghe và top bài hát/nghệ sĩ trên Mobile và PC đồng nhất 100%.
 */
object FloppyStatsSync {
    private const val DEFAULT_TOKEN = "mcfP9mwncsQaWghEmcvgPtGjdShcra0N"
    private const val CACHE_MS = 60_000L

    private var cachedJson: JsonObject? = null
    private var lastFetchTime = 0L
    private val lock = Any()

    private fun getApiEndpoint(): String {
        val endpoint = ListenBrainzManager.apiUrl.trim()
        val base = if (endpoint.contains("/apis/")) {
            endpoint.substringBefore("/apis/")
        } else if (endpoint.contains("/api/")) {
            endpoint.substringBefore("/api/")
        } else {
            "https://floppy.chutun.xyz"
        }
        return "${base.trimEnd('/')}/api/v1/statistics/overview/"
    }

    private fun getToken(): String {
        val token = AppSettings.listenBrainzToken.value.trim()
        return token.ifBlank { ListenBrainzManager.token.ifBlank { DEFAULT_TOKEN } }
    }

    suspend fun fetchOverview(forceRefresh: Boolean = false): JsonObject? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (!forceRefresh && cachedJson != null && (now - lastFetchTime) < CACHE_MS) {
                return@withContext cachedJson
            }
        }

        try {
            val request = Request.Builder()
                .url(getApiEndpoint())
                .header("Authorization", "Token ${getToken()}")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 BitChord/1.8")
                .header("Accept", "application/json")
                .build()

            val response = Http.client.newCall(request).execute()
            if (response.isSuccessful) {
                val text = response.body?.string().orEmpty()
                if (text.isNotBlank()) {
                    val parsed = Json.parseToJsonElement(text).jsonObject
                    synchronized(lock) {
                        cachedJson = parsed
                        lastFetchTime = now
                    }
                    return@withContext parsed
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun summary(period: ReplayPeriod = ReplayPeriod.THIS_YEAR): ReplaySummary? {
        val root = fetchOverview() ?: return null
        val stats = root["statistics"]?.jsonObject ?: return null
        val mc = stats["music_consumption"]?.jsonObject ?: return null

        val minObj = mc["minutes"]?.jsonObject
        val playsObj = mc["plays"]?.jsonObject

        val (mins, playsCount) = when (period) {
            ReplayPeriod.THIS_WEEK -> {
                val m = minObj?.get("this_week")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("this_week")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
            ReplayPeriod.THIS_MONTH -> {
                val m = minObj?.get("this_month")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("this_month")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
            ReplayPeriod.THIS_YEAR -> {
                val m = minObj?.get("this_year")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("this_year")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
            ReplayPeriod.ALL_TIME -> {
                val m = minObj?.get("total")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("total")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
        }

        val totalMs = (mins * 60_000.0).toLong()

        val songs = mc["top_tracks"]?.jsonArray?.mapNotNull { el ->
            val obj = el.jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val artist = obj["artist"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val album = obj["album"]?.jsonPrimitive?.contentOrNull
            val art = obj["album_image"]?.jsonPrimitive?.contentOrNull
                ?: obj["cover_url"]?.jsonPrimitive?.contentOrNull
            val m = obj["minutes"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val p = obj["plays"]?.jsonPrimitive?.intOrNull ?: 0
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "$artist-$title"

            RankedSong(
                song = Song(
                    videoId = id,
                    title = title,
                    artist = artist,
                    thumbnailUrl = art,
                    albumName = album
                ),
                ms = (m * 60_000.0).toLong(),
                plays = p
            )
        } ?: emptyList()

        val artists = mc["top_artists"]?.jsonArray?.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val art = obj["image"]?.jsonPrimitive?.contentOrNull
                ?: obj["picture_url"]?.jsonPrimitive?.contentOrNull
            val m = obj["minutes"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val p = obj["plays"]?.jsonPrimitive?.intOrNull ?: 0

            RankedEntry(
                title = name,
                subtitle = null,
                artworkUrl = art,
                browseId = null,
                ms = (m * 60_000.0).toLong(),
                plays = p
            )
        } ?: emptyList()

        val albums = mc["top_albums"]?.jsonArray?.mapNotNull { el ->
            val obj = el.jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val artist = obj["artist"]?.jsonPrimitive?.contentOrNull
            val art = obj["image"]?.jsonPrimitive?.contentOrNull
                ?: obj["cover_url"]?.jsonPrimitive?.contentOrNull
            val m = obj["minutes"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val p = obj["plays"]?.jsonPrimitive?.intOrNull ?: 0

            RankedEntry(
                title = title,
                subtitle = artist,
                artworkUrl = art,
                browseId = null,
                ms = (m * 60_000.0).toLong(),
                plays = p
            )
        } ?: emptyList()

        val genres = mc["top_genres"]?.jsonArray?.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val m = obj["minutes"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val p = obj["plays"]?.jsonPrimitive?.intOrNull ?: 0

            RankedEntry(
                title = name,
                subtitle = null,
                artworkUrl = null,
                browseId = null,
                ms = (m * 60_000.0).toLong(),
                plays = p
            )
        } ?: emptyList()

        // Phân phối theo giờ (0..23) từ weekday_hour_chart_data.music
        val hourOfDay = LongArray(24)
        stats["weekday_hour_chart_data"]?.jsonObject?.get("music")?.jsonArray?.forEach { dayArr ->
            dayArr.jsonArray.forEachIndexed { h, cntEl ->
                val cnt = cntEl.jsonPrimitive.longOrNull ?: 0L
                if (h in 0..23) {
                    hourOfDay[h] += cnt * 180_000L
                }
            }
        }

        // Busiest day từ calendar_weeks
        var busiestDay: String? = null
        var maxDayCount = 0L
        stats["activity_data"]?.jsonObject?.get("calendar_weeks")?.jsonArray?.forEach { weekEl ->
            weekEl.jsonArray.forEach { dayEl ->
                val dayObj = dayEl.jsonObject
                val dateStr = dayObj["date"]?.jsonPrimitive?.contentOrNull
                val musicCnt = dayObj["by_type"]?.jsonObject?.get("music")?.jsonPrimitive?.longOrNull ?: 0L
                if (musicCnt > maxDayCount) {
                    maxDayCount = musicCnt
                    busiestDay = dateStr
                }
            }
        }

        val today = LocalDate.now()
        val label = when (period) {
            ReplayPeriod.ALL_TIME -> "All time (Floppy Sync)"
            else -> period.label(today)
        }

        return ReplaySummary(
            period = period,
            label = label,
            totalMs = if (totalMs > 0L) totalMs else songs.sumOf { it.ms },
            totalPlays = if (playsCount > 0) playsCount else songs.sumOf { it.plays },
            songs = songs,
            artists = artists,
            albums = albums,
            genres = genres,
            hourOfDay = hourOfDay.toList(),
            busiestDay = busiestDay,
            busiestDayMs = maxDayCount * 180_000L,
            distinctSongs = songs.size,
            distinctArtists = artists.size,
            distinctAlbums = albums.size,
            since = "2025-10"
        )
    }

    /**
     * Lấy summary hợp lệ: ưu tiên Floppy nếu kết nối được và có dữ liệu; fallback về ListeningStats cục bộ.
     */
    suspend fun getEffectiveSummary(period: ReplayPeriod = ReplayPeriod.THIS_YEAR): ReplaySummary {
        val floppy = summary(period)
        if (floppy != null && (floppy.totalMs > 0L || floppy.songs.isNotEmpty())) {
            return floppy
        }
        return ListeningStats.summary(period)
    }
}
