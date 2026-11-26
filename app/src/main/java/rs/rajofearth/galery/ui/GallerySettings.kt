package rs.rajofearth.galery.ui

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.GALERY_SOURCE_URL
import rs.rajofearth.galery.data.GaleryRelease
import rs.rajofearth.galery.data.ReleaseUpdater
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryTypography

internal class GalleryUpdates(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("galery-updates", 0)
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
    var downloading by mutableStateOf(false)
    private var nextAutomaticCheck = 0L

    fun timeUntilAutomaticCheck(): Long = nextAutomaticCheck - SystemClock.elapsedRealtime()

    fun toggleAutomatic() {
        automatic = !automatic
        prefs.edit { putBoolean("automatic", automatic) }
    }

    suspend fun check(): Boolean {
        if (checking) return false
        checking = true
        failed = false
        return try {
            val latest = updater.latest()
            val changed = latest != null && latest != release
            release = latest
            checked = true
            nextAutomaticCheck = SystemClock.elapsedRealtime() + 24 * 60 * 60 * 1000L
            changed
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
            nextAutomaticCheck = SystemClock.elapsedRealtime() + 15 * 60 * 1000L
            false
        } finally {
            checking = false
        }
    }
}

@Composable
internal fun rememberGalleryUpdates(foreground: Boolean): GalleryUpdates {
    val context = LocalContext.current
    val updates = remember(context.applicationContext) { GalleryUpdates(context.applicationContext) }
    LaunchedEffect(updates) { withContext(Dispatchers.IO) { updates.updater.discardStaleDownload() } }
    val automatic = updates.automatic
    LaunchedEffect(foreground, automatic) {
        if (!foreground || !automatic) return@LaunchedEffect
        while (true) {
            delay(updates.timeUntilAutomaticCheck().coerceAtLeast(5_000))
            if (updates.timeUntilAutomaticCheck() <= 0L && updates.check()) {
                Toast.makeText(context, R.string.update_available_notice, Toast.LENGTH_SHORT).show()
            }
        }
    }
    return updates
}

private enum class SettingsPage(val nameRes: Int) {
    Settings(R.string.settings), About(R.string.about), Privacy(R.string.privacy_policy), Terms(R.string.terms_of_use),
}

@Composable
internal fun GallerySettings(updates: GalleryUpdates, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageState = rememberSaveableStateHolder()
    var page by rememberSaveable { mutableStateOf(SettingsPage.Settings) }
    var installing by rememberSaveable { mutableStateOf(false) }
    var installPermissionNeeded by rememberSaveable { mutableStateOf(false) }
    var installFailed by remember { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        installing = false
        updates.updater.deleteDownload()
    }

    fun goBack() {
        if (page == SettingsPage.Settings) onClose() else page = SettingsPage.Settings
    }

    fun install(release: GaleryRelease) {
        if (updates.downloading || installing) return
        installFailed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()))
                installPermissionNeeded = true
            } catch (_: RuntimeException) {
                installFailed = true
            }
            return
        }
        installPermissionNeeded = false
        updates.downloading = true
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var handedOff = false
            try {
                val uri = updates.updater.download(release)
                installing = true
                @Suppress("DEPRECATION")
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                installer.launch(intent)
                handedOff = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                installFailed = true
                installing = false
            } finally {
                if (!handedOff) updates.updater.deleteDownload()
                updates.downloading = false
            }
        }
    }

    BackHandler(onBack = ::goBack)
    Column(
        Modifier.fillMaxSize().background(GalleryPalette.background).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
        ) {
            GalleryIconButton(
                Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.go_back), ::goBack,
                Modifier.galleryPill(), size = GalleryDesign.button,
            )
            GalleryText(
                stringResource(page.nameRes),
                modifier = Modifier.semantics { heading() },
                style = GalleryTypography.headlineMedium,
            )
        }
        pageState.SaveableStateProvider(page.name) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f).galleryEntrance(page)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.inset),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                when (page) {
                    SettingsPage.Settings -> {
                        val release = updates.release
                        val status = when {
                            updates.downloading -> stringResource(R.string.update_downloading)
                            installing -> stringResource(R.string.update_installing)
                            installFailed -> stringResource(R.string.update_install_failed)
                            updates.checking -> stringResource(R.string.update_checking)
                            updates.failed && release == null -> stringResource(R.string.update_check_failed)
                            installPermissionNeeded && release != null -> stringResource(R.string.update_install_permission)
                            release != null -> stringResource(R.string.update_available, release.version)
                            updates.checked -> stringResource(R.string.update_up_to_date)
                            else -> stringResource(R.string.update_using_github)
                        }
                        AppIdentity(updates.version)
                        SettingsGroup(R.string.update_section) {
                            val busy = updates.checking || updates.downloading || installing
                            UpdateAction(
                                status = status,
                                warning = !busy && (installFailed || (updates.failed && release == null)),
                                label = stringResource(if (release == null) R.string.update_check_now else R.string.update_download),
                                enabled = !busy,
                                onClick = {
                                    if (release == null) {
                                        installFailed = false
                                        scope.launch { updates.check() }
                                    } else {
                                        install(release)
                                    }
                                },
                            )
                            SettingsDivider()
                            AutomaticUpdateRow(updates.automatic, updates::toggleAutomatic)
                        }
                        SettingsGroup(R.string.settings_information) {
                            SettingsRow(R.string.about) { page = SettingsPage.About }
                            SettingsDivider()
                            SettingsRow(R.string.privacy_policy) { page = SettingsPage.Privacy }
                            SettingsDivider()
                            SettingsRow(R.string.terms_of_use) { page = SettingsPage.Terms }
                        }
                    }
                    SettingsPage.About -> {
                        AppIdentity(updates.version)
                        GalleryText(
                            stringResource(R.string.about_galery),
                            modifier = Modifier.padding(horizontal = GalleryDesign.gap),
                            color = GalleryPalette.onSurfaceVariant,
                        )
                        SettingsGroup {
                            SettingsRow(R.string.view_source) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, GALERY_SOURCE_URL.toUri()))
                                } catch (_: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, R.string.browser_unavailable, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    SettingsPage.Privacy -> {
                        PolicySection(R.string.privacy_media, R.string.privacy_media_body)
                        PolicySection(R.string.privacy_updates, R.string.privacy_updates_body)
                        PolicySection(R.string.privacy_backup, R.string.privacy_backup_body)
                    }
                    SettingsPage.Terms -> {
                        PolicySection(R.string.terms_media, R.string.terms_media_body)
                        PolicySection(R.string.update_section, R.string.terms_updates_body)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIdentity(version: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.gap),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(GalleryDesign.dock).clip(GalleryDesign.cover).background(GalleryPalette.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                null,
                Modifier.requiredSize(84.dp),
            )
        }
        Column {
            GalleryText(stringResource(R.string.app_name), style = GalleryTypography.titleLarge)
            GalleryText(
                stringResource(R.string.app_version, version),
                color = GalleryPalette.onSurfaceVariant,
                style = GalleryTypography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: Int? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GalleryDesign.gap)) {
        if (title != null) {
            GalleryText(
                stringResource(title),
                Modifier.padding(horizontal = GalleryDesign.gap).semantics { heading() },
                style = GalleryTypography.titleMedium,
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(GalleryDesign.panel).background(GalleryPalette.surfaceContainer),
        ) { content() }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.inset).height(1.dp)
            .background(GalleryPalette.outlineVariant),
    )
}

@Composable
private fun UpdateAction(status: String, warning: Boolean, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.padding(GalleryDesign.inset),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
    ) {
        GalleryText(
            status,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = if (warning) GalleryPalette.error else GalleryPalette.onSurface,
            style = GalleryTypography.titleMedium,
        )
        GalleryPillAction(label, primary = true, enabled = enabled, onClick = onClick)
    }
}

@Composable
private fun AutomaticUpdateRow(checked: Boolean, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = { toggle() })
            .heightIn(min = GalleryDesign.target).padding(GalleryDesign.inset),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            GalleryText(stringResource(R.string.update_automatic), style = GalleryTypography.titleMedium)
            GalleryText(
                stringResource(R.string.update_automatic_summary),
                color = GalleryPalette.onSurfaceVariant,
                style = GalleryTypography.bodyMedium,
            )
        }
        val track by animateColorAsState(
            if (checked) GalleryPalette.surfaceContainerHighest else GalleryPalette.surfaceContainerHigh,
            GalleryDesign.effects(),
            label = "Update switch track",
        )
        val thumb by animateColorAsState(
            if (checked) GalleryPalette.primary else GalleryPalette.onSurfaceVariant,
            GalleryDesign.effects(),
            label = "Update switch thumb",
        )
        val offset = animateDpAsState(
            if (checked) 24.dp else 4.dp,
            GalleryDesign.effects(),
            label = "Update switch position",
        )
        Box(
            Modifier.size(width = 48.dp, height = 28.dp).clip(CircleShape).background(track),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.offset { IntOffset(offset.value.roundToPx(), 0) }
                .size(20.dp).background(thumb, CircleShape))
        }
    }
}

@Composable
private fun SettingsRow(label: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = GalleryDesign.dock)
            .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
    ) {
        GalleryText(stringResource(label), Modifier.weight(1f), style = GalleryTypography.titleMedium)
        GalleryIcon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(GalleryDesign.icon),
            tint = GalleryPalette.onSurfaceVariant,
        )
    }
}

@Composable
private fun PolicySection(title: Int, body: Int) {
    Column(
        Modifier.padding(horizontal = GalleryDesign.gap),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
    ) {
        GalleryText(stringResource(title), Modifier.semantics { heading() }, style = GalleryTypography.titleMedium)
        GalleryText(stringResource(body), color = GalleryPalette.onSurfaceVariant)
    }
}
