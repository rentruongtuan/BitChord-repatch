package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.util.Locale

/**
 * Nạp dữ liệu thống kê Replay đồng bộ từ máy chủ cá nhân Floppy.
 * Thay thế hoặc bổ sung cho thống kê cục bộ chỉ tính riêng trên từng thiết bị.
 */
object DesktopFloppyStats {
    private const val DEFAULT_TOKEN = "mcfP9mwncsQaWghEmcvgPtGjdShcra0N"
    private const val CACHE_MS = 60_000L

    private var cachedJson: JsonObject? = null
    private var lastFetchTime = 0L
    private val lock = Any()

    private fun getApiEndpoint(): String {
        val endpoint = DesktopScrobbleSettings.listenBrainzEndpoint.value.trim()
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
        val token = DesktopScrobbleSettings.listenBrainzToken.value.trim()
        return token.ifBlank { DEFAULT_TOKEN }
    }

    /**
     * Tải JSON thống kê từ Floppy server (có cache 60s).
     */
    fun fetchOverview(forceRefresh: Boolean = false): JsonObject? {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (!forceRefresh && cachedJson != null && (now - lastFetchTime) < CACHE_MS) {
                return cachedJson
            }
        }

        return try {
            val url = URI(getApiEndpoint()).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("Authorization", "Token ${getToken()}")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 BitChord/1.8")
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = Json.parseToJsonElement(text).jsonObject
                synchronized(lock) {
                    cachedJson = parsed
                    lastFetchTime = now
                }
                parsed
            } else {
                val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                println("Floppy response error HTTP $code: $errText")
                null
            }
        } catch (e: Exception) {
            println("Floppy fetch exception: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Chuyển đổi dữ liệu Floppy thành DesktopReplaySummary.
     */
    fun summary(period: DesktopReplayPeriod = DesktopReplayPeriod.ALL_TIME): DesktopReplaySummary? {
        val root = fetchOverview() ?: return null
        val stats = root["statistics"]?.jsonObject ?: return null
        val mc = stats["music_consumption"]?.jsonObject ?: return null

        val minObj = mc["minutes"]?.jsonObject
        val playsObj = mc["plays"]?.jsonObject

        val (mins, playsCount) = when (period) {
            DesktopReplayPeriod.THIS_MONTH -> {
                val m = minObj?.get("this_month")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("this_month")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
            DesktopReplayPeriod.THIS_YEAR -> {
                val m = minObj?.get("this_year")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val p = playsObj?.get("this_year")?.jsonPrimitive?.intOrNull ?: 0
                m to p
            }
            DesktopReplayPeriod.ALL_TIME -> {
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

            DesktopReplayEntry(
                song = Song(
                    videoId = id,
                    title = title,
                    artist = artist,
                    thumbnailUrl = art,
                    albumName = album
                ),
                listenedMs = (m * 60_000.0).toLong(),
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

            DesktopRankedEntry(
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

            DesktopRankedEntry(
                title = title,
                subtitle = artist,
                artworkUrl = art,
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
                    hourOfDay[h] += cnt * 180_000L // Giả định trung bình 3 phút / bài để ra ms
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
            DesktopReplayPeriod.THIS_MONTH ->
                today.month.name.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)
            DesktopReplayPeriod.THIS_YEAR -> today.year.toString()
            DesktopReplayPeriod.ALL_TIME -> "All time (Floppy Sync)"
        }

        return DesktopReplaySummary(
            period = period,
            label = label,
            totalMs = if (totalMs > 0L) totalMs else songs.sumOf { it.listenedMs },
            totalPlays = if (playsCount > 0) playsCount else songs.sumOf { it.plays },
            songs = songs,
            artists = artists,
            albums = albums,
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
     * Ưu tiên số liệu đồng bộ từ máy chủ Floppy; nếu offline hoặc chưa có số liệu thì dùng cục bộ.
     */
    fun getEffectiveSummary(period: DesktopReplayPeriod = DesktopReplayPeriod.ALL_TIME): DesktopReplaySummary {
        val floppy = summary(period)
        if (floppy != null && (floppy.totalMs > 0L || floppy.songs.isNotEmpty())) {
            return floppy
        }
        return DesktopListeningStats.summary(period)
    }
}
