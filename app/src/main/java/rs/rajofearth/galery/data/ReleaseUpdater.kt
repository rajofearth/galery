package rs.rajofearth.galery.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

internal const val GALERY_SOURCE_URL = "https://github.com/rajofearth/galery"

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/rajofearth/galery/releases/latest"
private const val MAX_APK_BYTES = 8 * 1024 * 1024
private const val MAX_METADATA_BYTES = 128 * 1024
private const val STALE_APK_MS = 60 * 60 * 1000L

internal data class GaleryRelease(val version: String, val url: String, val size: Int, val sha256: String)

class GaleryUpdateFiles : FileProvider()

internal class ReleaseUpdater(private val context: Context) {
    private val apk = File(context.cacheDir, "updates/galery.apk")
    private val pm get() = context.packageManager
    val versionName: String get() = installed().versionName.orEmpty()

    suspend fun latest(): GaleryRelease? = withContext(Dispatchers.IO) {
        connect(LATEST_RELEASE_URL) { response ->
            if (response.responseCode == 404) return@connect null
            response.requireSuccess()
            val bytes = response.inputStream.use { it.readNBytesCompat(MAX_METADATA_BYTES + 1) }
            if (bytes.size > MAX_METADATA_BYTES) throw IOException("Release metadata is too large")
            val release = JSONObject(bytes.toString(Charsets.UTF_8))
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@connect null
            val version = release.getString("tag_name").removePrefix("v")
            if (!isNewerRelease(version, versionName)) return@connect null
            val assets = release.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = "galery-$version.apk"
                if (asset.optString("state") != "uploaded" || asset.optString("name") != name) continue
                val url = "$GALERY_SOURCE_URL/releases/download/${release.getString("tag_name")}/$name"
                val size = asset.optLong("size")
                val sha256 = asset.optString("digest").removePrefix("sha256:")
                if (asset.optString("browser_download_url") != url ||
                    size !in 1L..MAX_APK_BYTES.toLong() || !sha256.matches(Regex("[0-9a-fA-F]{64}"))
                ) throw IOException("Invalid release asset")
                return@connect GaleryRelease(version, url, size.toInt(), sha256)
            }
            throw IOException("Release has no Galery APK")
        }
    }

    suspend fun download(release: GaleryRelease) = withContext(Dispatchers.IO) {
        var ready = false
        try {
            apk.parentFile?.mkdirs()
            val digest = MessageDigest.getInstance("SHA-256")
            val job = coroutineContext
            connect(release.url) { response ->
                response.requireSuccess()
                response.inputStream.use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var remaining = release.size
                        while (true) {
                            job.ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            remaining -= count
                            if (remaining < 0) throw IOException("APK is larger than advertised")
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                        }
                        if (remaining != 0) throw IOException("Incomplete APK")
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(release.sha256, ignoreCase = true)) throw IOException("APK checksum differs")
            verifyPackage(release.version)
            FileProvider.getUriForFile(context, "${context.packageName}.updates", apk).also { ready = true }
        } finally {
            if (!ready) deleteDownload()
        }
    }

    fun deleteDownload() {
        apk.delete()
    }

    fun discardStaleDownload() {
        if (!apk.isFile) return
        if (System.currentTimeMillis() - apk.lastModified() > STALE_APK_MS ||
            pm.getPackageArchiveInfo(apk.path, 0)?.versionName == versionName
        ) deleteDownload()
    }

    @Suppress("DEPRECATION")
    private fun installed(): PackageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun verifyPackage(version: String) {
        val candidate = pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNATURES)
            ?: throw IOException("Unreadable APK")
        val current = installed()
        val signatures = current.signatures.orEmpty().toSet()
        if (candidate.packageName != context.packageName || candidate.versionName != version ||
            candidate.versionCode <= current.versionCode || signatures.isEmpty() ||
            candidate.signatures.orEmpty().toSet() != signatures
        ) throw IOException("APK does not match the installed Galery")
    }
}

internal fun isNewerRelease(candidate: String, installed: String): Boolean {
    val pattern = Regex("[0-9]+\\.[0-9]+\\.[0-9]+")
    if (!candidate.matches(pattern) || !installed.matches(pattern)) return false
    val newer = candidate.split('.').map { it.toIntOrNull() ?: return false }
    val current = installed.split('.').map { it.toIntOrNull() ?: return false }
    for (i in 0..2) if (newer[i] != current[i]) return newer[i] > current[i]
    return false
}

private fun <T> connect(address: String, read: (HttpsURLConnection) -> T): T {
    var next = URL(address)
    repeat(6) {
        if (next.protocol != "https" || next.host !in setOf(
                "api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com",
            )
        ) throw IOException("Unexpected update host")
        val connection = next.openConnection() as HttpsURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "Galery-Android")
        try {
            if (connection.responseCode in 300..399) {
                next = URL(next, connection.getHeaderField("Location") ?: throw IOException("Missing redirect"))
            } else {
                return read(connection)
            }
        } finally {
            connection.disconnect()
        }
    }
    throw IOException("Too many update redirects")
}

private fun HttpsURLConnection.requireSuccess() {
    if (responseCode != 200) throw IOException("GitHub returned HTTP $responseCode")
}

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val bytes = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (bytes.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - bytes.size()))
        if (count == -1) break
        bytes.write(buffer, 0, count)
    }
    return bytes.toByteArray()
}
