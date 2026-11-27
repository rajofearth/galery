package rs.rajofearth.galery.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rs.rajofearth.galery.data.GaleryRelease
import rs.rajofearth.galery.data.ReleaseUpdater
import rs.rajofearth.galery.data.UpdateDownloadProgress

internal class GalleryUpdates(context: Context) {
    private val prefs = context.getSharedPreferences("galery-updates", Context.MODE_PRIVATE)
    val updater = ReleaseUpdater(context)
    val version by lazy { updater.versionName }
    var automatic by mutableStateOf(prefs.getBoolean("automatic", true))
        private set
    var checking by mutableStateOf(false)
        private set
    var checked by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var release by mutableStateOf<GaleryRelease?>(null)
        private set
    var offeredRelease by mutableStateOf<GaleryRelease?>(null)
        private set
    private var offeredVersion: String? = null
    private var nextAutomaticCheck = 0L

    fun timeUntilAutomaticCheck(): Long = nextAutomaticCheck - SystemClock.elapsedRealtime()

    fun toggleAutomatic() {
        automatic = !automatic
        prefs.edit { putBoolean("automatic", automatic) }
    }

    fun openOffer() {
        offeredRelease = release
        offeredVersion = release?.version
    }

    fun offerAutomatically() {
        if (automatic && offeredRelease == null && release?.version != offeredVersion) openOffer()
    }

    fun dismissOffer() {
        offeredVersion = offeredRelease?.version
        offeredRelease = null
    }

    fun resetReminder() {
        offeredVersion = offeredRelease?.version
    }

    suspend fun check() {
        if (checking) return
        checking = true
        failed = false
        try {
            release = updater.latest()
            checked = true
            nextAutomaticCheck = SystemClock.elapsedRealtime() + 24 * 60 * 60 * 1000L
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
            nextAutomaticCheck = SystemClock.elapsedRealtime() + 15 * 60 * 1000L
        } finally {
            checking = false
        }
    }
}

@Composable
internal fun rememberGalleryUpdates(foreground: Boolean): GalleryUpdates {
    val context = LocalContext.current.applicationContext
    val updates = remember(context) { GalleryUpdates(context) }
    LaunchedEffect(updates) { withContext(Dispatchers.IO) { updates.updater.discardStaleDownload() } }
    val automatic = updates.automatic
    LaunchedEffect(foreground, automatic) {
        if (!foreground || !automatic) return@LaunchedEffect
        while (true) {
            delay(updates.timeUntilAutomaticCheck().coerceAtLeast(5_000))
            if (updates.timeUntilAutomaticCheck() <= 0L) updates.check()
        }
    }
    return updates
}

internal sealed interface UpdatePhase {
    data object Offer : UpdatePhase
    data class Downloading(val progress: UpdateDownloadProgress) : UpdatePhase
    data object Stopping : UpdatePhase
    data object Verifying : UpdatePhase
    data object Installing : UpdatePhase
    data object Failed : UpdatePhase
    data object PermissionRequired : UpdatePhase
    data object InstallCanceled : UpdatePhase
}

@Composable
internal fun GalleryUpdateHost(
    updates: GalleryUpdates,
    foreground: Boolean,
    canOffer: Boolean,
    frameAtDraw: (() -> GlassFrame?)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf<UpdatePhase>(UpdatePhase.Offer) }
    var transfer by remember { mutableStateOf<Job?>(null) }
    var openingPermission by remember { mutableStateOf(false) }
    val automatic = updates.automatic
    val available = updates.release?.version
    LaunchedEffect(foreground, canOffer, automatic, available) {
        if (!foreground) updates.resetReminder()
        else if (canOffer) updates.offerAutomatically()
    }

    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        updates.updater.deleteDownload()
        if (result.resultCode == Activity.RESULT_OK) updates.dismissOffer()
        phase = if (result.resultCode == Activity.RESULT_OK) UpdatePhase.Offer else UpdatePhase.InstallCanceled
    }
    fun download(release: GaleryRelease) {
        if (transfer?.isCompleted == false || phase == UpdatePhase.Installing) return
        phase = UpdatePhase.Downloading(UpdateDownloadProgress(0))
        transfer = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var handedOff = false
            try {
                val uri = withTimeout(120_000) {
                    updates.updater.download(
                        release,
                        onProgress = { if (phase != UpdatePhase.Stopping) phase = UpdatePhase.Downloading(it) },
                        onVerifying = { phase = UpdatePhase.Verifying },
                    )
                }
                phase = UpdatePhase.Installing
                @Suppress("DEPRECATION")
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                installer.launch(intent)
                handedOff = true
            } catch (_: TimeoutCancellationException) {
                phase = UpdatePhase.Failed
            } catch (error: CancellationException) {
                phase = UpdatePhase.Offer
                throw error
            } catch (_: Exception) {
                phase = UpdatePhase.Failed
            } finally {
                if (!handedOff) updates.updater.deleteDownload()
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        openingPermission = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls()) {
            updates.offeredRelease?.let(::download)
        } else {
            phase = UpdatePhase.PermissionRequired
        }
    }
    val release = updates.offeredRelease ?: return
    UpdateSheet(
        installedVersion = updates.version,
        release = release,
        phase = phase,
        onDismiss = {
            if (phase != UpdatePhase.Installing) {
                transfer?.cancel()
                phase = UpdatePhase.Offer
            }
            updates.dismissOffer()
        },
        onInstall = install@{
            if (openingPermission) return@install
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                try {
                    openingPermission = true
                    phase = UpdatePhase.PermissionRequired
                    permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()))
                } catch (_: RuntimeException) {
                    openingPermission = false
                    phase = UpdatePhase.Failed
                }
            } else {
                download(release)
            }
        },
        onCancel = {
            phase = UpdatePhase.Stopping
            transfer?.cancel()
        },
        frameAtDraw = frameAtDraw,
    )
}
