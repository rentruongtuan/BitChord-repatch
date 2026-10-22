package com.music.bitchord.data.scrobbling

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.music.bitchord.data.Http

object ListenBrainzManager {
    private const val TAG = "ListenBrainzManager"
    
    // Mặc định trỏ về server Floppy của bạn
    var apiUrl: String = "https://floppy.chutun.xyz/apis/listenbrainz/1/submit-listens"
    var token: String = "mcfP9mwncsQaWghEmcvgPtGjdShcra0N"
    var isEnabled: Boolean = true

    suspend fun submitPlayingNow(
        userToken: String? = null,
        song: Song?,
        positionMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean {
        val effectiveToken = userToken?.takeIf { it.isNotBlank() } ?: token
        if (!isEnabled || effectiveToken.isBlank() || song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val durationMs = durationMsOverride ?: parseDurationMs(song.durationText)
                val durationPart = if (durationMs > 0) "\"duration_ms\":$durationMs," else ""
                val releaseName = song.albumName.orEmpty()
                val releasePart = if (releaseName.isBlank()) "" else "\"release_name\":\"${escapeJson(releaseName)}\","
                val artist = if (primaryArtistOnly) song.artist.primaryArtist() else song.artist
                
                // Đẩy cover_url, artists và origin_url vào additional_info
                val coverPart = song.thumbnailUrl?.takeIf { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }?.let { "\"cover_url\":\"${escapeJson(it)}\"," }.orEmpty()
                val artistsPart = if (song.artists.isNotEmpty()) {
                    val names = song.artists.joinToString(",") { "\"${escapeJson(it.name)}\"" }
                    "\"artists\":[$names],"
                } else ""
                val originUrlPart = if (song.videoId.isNotBlank()) {
                    "\"origin_url\":\"https://www.youtube.com/watch?v=${escapeJson(song.videoId)}\","
                } else ""

                val trackMetadata = """{"track_metadata":{"artist_name":"${escapeJson(artist)}","track_name":"${escapeJson(song.title)}",$releasePart"additional_info":{${coverPart}${artistsPart}${originUrlPart}${durationPart}"position_ms":$positionMs,"submission_client":"BitChord"}}}"""
                val bodyJson = "{\"listen_type\":\"playing_now\",\"payload\":[$trackMetadata]}"
                Log.d(TAG, "submitPlayingNow: $bodyJson")
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request =
                    Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Token $effectiveToken")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) BitChord/1.8.1")
                        .build()

                Http.client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "playing_now submitted for ${song.title}")
                        true
                    } else {
                        val bodyText = try { resp.body?.string() ?: "" } catch (_: Exception) { "" }
                        Log.w(TAG, "playing_now submit failed: ${resp.code} - $bodyText")
                        false
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "submitPlayingNow failed", ex)
                false
            }
        }
    }

    suspend fun submitPlayingNow(
        song: Song?,
        positionMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean = submitPlayingNow(null, song, positionMs, durationMsOverride, primaryArtistOnly)

    suspend fun submitFinished(
        userToken: String? = null,
        song: Song?,
        startMs: Long,
        endMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean {
        val effectiveToken = userToken?.takeIf { it.isNotBlank() } ?: token
        if (!isEnabled || effectiveToken.isBlank() || song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val durationMs = durationMsOverride ?: parseDurationMs(song.durationText)
                val durationPart = if (durationMs > 0) "\"duration_ms\":$durationMs," else ""
                val releaseName = song.albumName.orEmpty()
                val releasePart = if (releaseName.isBlank()) "" else "\"release_name\":\"${escapeJson(releaseName)}\","
                var listenedAtStart = startMs / 1000L
                val MIN_LISTEN_TS = 1033430400L
                if (listenedAtStart < MIN_LISTEN_TS) {
                    listenedAtStart = System.currentTimeMillis() / 1000L
                }
                val artist = if (primaryArtistOnly) song.artist.primaryArtist() else song.artist

                // Đẩy cover_url, artists và origin_url vào additional_info
                val coverPart = song.thumbnailUrl?.takeIf { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }?.let { "\"cover_url\":\"${escapeJson(it)}\"," }.orEmpty()
                val artistsPart = if (song.artists.isNotEmpty()) {
                    val names = song.artists.joinToString(",") { "\"${escapeJson(it.name)}\"" }
                    "\"artists\":[$names],"
                } else ""
                val originUrlPart = if (song.videoId.isNotBlank()) {
                    "\"origin_url\":\"https://www.youtube.com/watch?v=${escapeJson(song.videoId)}\","
                } else ""

                val trackMetadata = """{"listened_at":$listenedAtStart,"track_metadata":{"artist_name":"${escapeJson(artist)}","track_name":"${escapeJson(song.title)}",$releasePart"additional_info":{${coverPart}${artistsPart}${originUrlPart}${durationPart}"start_ms":$startMs,"end_ms":$endMs,"submission_client":"BitChord"}}}"""
                val bodyJson = "{\"listen_type\":\"single\",\"payload\":[$trackMetadata]}"
                Log.d(TAG, "submitFinished: $bodyJson")
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request =
                    Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Token $effectiveToken")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) BitChord/1.8.1")
                        .build()

                Http.client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "finished listen submitted for ${song.title}")
                        true
                    } else {
                        val bodyText = try { resp.body?.string() ?: "" } catch (_: Exception) { "" }
                        Log.w(TAG, "finished listen submit failed: ${resp.code} - $bodyText")
                        false
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "submitFinished failed", ex)
                false
            }
        }
    }

    suspend fun submitFinished(
        song: Song?,
        startMs: Long,
        endMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean = submitFinished(null, song, startMs, endMs, durationMsOverride, primaryArtistOnly)

    private fun parseDurationMs(text: String?): Long {
        if (text == null) return 0L
        val parts = text.split(":")
        if (parts.size != 2) return 0L
        val minutes = parts[0].toLongOrNull() ?: return 0L
        val seconds = parts[1].toLongOrNull() ?: return 0L
        return (minutes * 60 + seconds) * 1000
    }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}

