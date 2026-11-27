package rs.rajofearth.galery

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.rajofearth.galery.data.GaleryRelease
import rs.rajofearth.galery.data.UpdateDownloadProgress
import rs.rajofearth.galery.ui.GalleryDesign
import rs.rajofearth.galery.ui.GalleryPillAction
import rs.rajofearth.galery.ui.UpdatePhase
import rs.rajofearth.galery.ui.UpdateSheet
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryTypography

class UpdatePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("scenario") ?: "retry"
        setContent { UpdatePreview(scenario) }
    }
}

private val exampleRelease = GaleryRelease(
    version = "0.3.0",
    url = "",
    size = 1_982_464,
    sha256 = "",
    notes = """
        ## What's new

        - Move photos and videos between albums, or create an album while moving them.
        - Touch and hold an album to rename or delete it.
        - Tap a video to hide or show the Viewer controls, even when paused.
        - See the download size and release notes before installing an update.
    """.trimIndent(),
)

@Composable
private fun UpdatePreview(scenario: String) {
    val scope = rememberCoroutineScope()
    var mockTransfer by remember { mutableStateOf<Job?>(null) }
    var showSheet by remember { mutableStateOf(true) }
    var phase by remember { mutableStateOf<UpdatePhase>(UpdatePhase.Offer) }
    var failureShown by remember { mutableStateOf(false) }
    var permissionShown by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    fun start() {
        if (mockTransfer?.isActive == true) return
        if (scenario == "permission" && !permissionShown) {
            permissionShown = true
            phase = UpdatePhase.PermissionRequired
            return
        }
        phase = UpdatePhase.Downloading(UpdateDownloadProgress(0))
        mockTransfer = scope.launch {
            delay(900)
            val steps = 20
            for (step in 1..steps) {
                delay(400)
                phase = UpdatePhase.Downloading(
                    UpdateDownloadProgress(
                        bytes = exampleRelease.size * step / steps,
                        bytesPerSecond = exampleRelease.size * 1_000L / (steps * 400L),
                    ),
                )
                if (scenario == "retry" && step == 7 && !failureShown) {
                    delay(450)
                    failureShown = true
                    phase = UpdatePhase.Failed
                    return@launch
                }
            }
            phase = UpdatePhase.Verifying
            delay(1_100)
            phase = UpdatePhase.Installing
            delay(1_500)
            if (scenario == "installer-cancel") {
                phase = UpdatePhase.InstallCanceled
            } else {
                completed = true
                showSheet = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(GalleryPalette.background)
            .safeDrawingPadding().padding(GalleryDesign.inset),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
    ) {
        GalleryText("Update preview", style = GalleryTypography.headlineMedium)
        GalleryText(
            if (completed) "Mock installation complete" else "Simulated download and installation",
            color = GalleryPalette.onSurfaceVariant,
        )
        GalleryPillAction(if (completed) "Replay preview" else "View update") {
            phase = UpdatePhase.Offer
            completed = false
            showSheet = true
        }
    }

    if (showSheet) {
        UpdateSheet(
            installedVersion = "0.2.0",
            release = exampleRelease,
            phase = phase,
            onDismiss = {
                mockTransfer?.cancel()
                showSheet = false
            },
            onInstall = ::start,
            onCancel = {
                mockTransfer?.cancel()
                phase = UpdatePhase.Stopping
                mockTransfer = scope.launch {
                    delay(if (scenario == "slow-cancel") 8_000 else 350)
                    phase = UpdatePhase.Offer
                }
            },
        )
    }
}
