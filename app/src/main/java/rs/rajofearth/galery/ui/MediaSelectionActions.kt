package rs.rajofearth.galery.ui

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryTypography

private const val ACTIONS_TAG = "MediaSelectionActions"
private const val MAX_SHARE_ITEMS = 100
private const val MAX_TRASH_REQUEST_SIZE = 2_000

internal class MediaSelectionActions(
    val isBusy: Boolean,
    val canTrash: Boolean,
    val share: (List<MediaItem>) -> Unit,
    val trash: (List<MediaItem>) -> Unit,
    val restore: (List<MediaItem>) -> Unit,
    val details: (List<MediaItem>, Set<String>) -> Unit,
)

@Composable
internal fun rememberMediaSelectionActions(
    frameAtDraw: () -> GlassFrame?,
    onMediaChanged: () -> Unit,
): MediaSelectionActions {
    val context = LocalContext.current
    val canTrash = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    var details by remember { mutableStateOf<MediaDetails?>(null) }
    var awaitingConfirmation by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var directChanges by rememberSaveable { mutableIntStateOf(0) }
    var trashUris by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var restoring by rememberSaveable { mutableStateOf(false) }
    var trashedCount by rememberSaveable { mutableIntStateOf(0) }
    var requestInFlight by rememberSaveable { mutableStateOf(false) }

    fun finishTrash() {
        val changed = directChanges > 0 || trashedCount > 0
        trashUris = emptyList()
        trashedCount = 0
        directChanges = 0
        requestInFlight = false
        if (changed) onMediaChanged()
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            trashedCount = (trashedCount + MAX_TRASH_REQUEST_SIZE).coerceAtMost(trashUris.size)
            if (trashedCount == trashUris.size) finishTrash()
            else requestInFlight = false
        } else {
            if (result.data?.hasExtra(
                    ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION,
                ) == true
            ) {
                Toast.makeText(context, R.string.media_trash_failed, Toast.LENGTH_LONG).show()
            } else if (trashedCount > 0 || directChanges > 0) {
                Toast.makeText(context, R.string.media_trash_partial, Toast.LENGTH_LONG).show()
            }
            finishTrash()
        }
    }

    LaunchedEffect(trashUris, trashedCount, requestInFlight) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || requestInFlight || trashUris.isEmpty()) {
            return@LaunchedEffect
        }
        val end = (trashedCount + MAX_TRASH_REQUEST_SIZE).coerceAtMost(trashUris.size)
        val uris = trashUris.subList(trashedCount, end).map(Uri::parse)
        try {
            val trash = !restoring
            val pending = withContext(Dispatchers.IO) { uris.filterNot { trashDirectly(context, it, trash) } }
            directChanges += uris.size - pending.size
            if (pending.isEmpty()) {
                trashedCount = end
                if (trashedCount == trashUris.size) finishTrash()
                return@LaunchedEffect
            }
            val request = withContext(Dispatchers.IO) { createTrashRequest(context, pending, trash) }
            requestInFlight = true
            trashLauncher.launch(request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: RuntimeException) {
            Log.w(ACTIONS_TAG, "Could not request system trash", exception)
            Toast.makeText(context, R.string.media_trash_failed, Toast.LENGTH_LONG).show()
            finishTrash()
        }
    }

    details?.let { selection ->
        MediaDetailsDialog(selection.items, selection.favoriteKeys, frameAtDraw) { details = null }
    }
    if (awaitingConfirmation.isNotEmpty()) {
        val count = awaitingConfirmation.size
        GallerySheet(onDismissRequest = { awaitingConfirmation = emptyList() }, frameAtDraw = frameAtDraw) {
            Column(
                Modifier.fillMaxWidth().padding(start = GalleryDesign.inset, end = GalleryDesign.inset, bottom = GalleryDesign.inset),
                verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
            ) {
                GalleryText(
                    pluralStringResource(R.plurals.media_trash_confirm, count, count),
                    style = GalleryTypography.titleLarge,
                )
                GalleryText(stringResource(R.string.media_trash_restorable), color = GalleryPalette.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap)) {
                    GalleryPillAction(stringResource(R.string.media_trash_cancel), Modifier.weight(1f)) {
                        awaitingConfirmation = emptyList()
                    }
                    GalleryPillAction(stringResource(R.string.trash_selection), Modifier.weight(1f), primary = true) {
                        restoring = false
                        trashUris = ArrayList(awaitingConfirmation.map { it.contentUri.toString() })
                        awaitingConfirmation = emptyList()
                    }
                }
            }
        }
    }

    return MediaSelectionActions(
        isBusy = trashUris.isNotEmpty(),
        canTrash = canTrash,
        share = { items -> if (items.isNotEmpty()) shareMedia(context, items) },
        trash = { items ->
            if (!canTrash) {
                Toast.makeText(context, R.string.media_trash_unavailable, Toast.LENGTH_LONG).show()
            } else if (items.isNotEmpty() && trashUris.isEmpty()) {
                awaitingConfirmation = items
            }
        },
        restore = { items ->
            if (canTrash && items.isNotEmpty() && trashUris.isEmpty()) {
                restoring = true
                trashUris = ArrayList(items.map { it.contentUri.toString() })
            }
        },
        details = { items, favoriteKeys ->
            if (items.isNotEmpty()) details = MediaDetails(items.toList(), favoriteKeys)
        },
    )
}

@RequiresApi(Build.VERSION_CODES.R)
private fun trashDirectly(context: Context, uri: Uri, trash: Boolean): Boolean = try {
    val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, if (trash) 1 else 0) }
    context.contentResolver.update(uri, values, null, null) > 0
} catch (_: SecurityException) {
    false
}

@RequiresApi(Build.VERSION_CODES.R)
private fun createTrashRequest(context: Context, uris: List<Uri>, trash: Boolean): IntentSenderRequest {
    val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, trash)
    return IntentSenderRequest.Builder(pendingIntent.intentSender).build()
}

private fun shareMedia(context: Context, items: List<MediaItem>) {
    if (items.size > MAX_SHARE_ITEMS) {
        Toast.makeText(context, context.getString(R.string.media_share_limit, MAX_SHARE_ITEMS), Toast.LENGTH_LONG).show()
        return
    }
    val uris = ArrayList(items.map { it.contentUri })
    val mimeTypes = items.map {
        it.mimeType?.takeIf(String::isNotBlank) ?: if (it.isVideo) "video/*" else "image/*"
    }.toSet()
    val intentType = when {
        mimeTypes.size == 1 -> mimeTypes.first()
        items.all { it.isVideo } -> "video/*"
        items.none { it.isVideo } -> "image/*"
        else -> "*/*"
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
    intent.type = intentType
    intent.clipData = ClipData(null, mimeTypes.toTypedArray(), ClipData.Item(uris.first())).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, null))
    } catch (exception: RuntimeException) {
        Log.w(ACTIONS_TAG, "Could not open Android Sharesheet", exception)
        Toast.makeText(context, R.string.media_share_failed, Toast.LENGTH_LONG).show()
    }
}

private data class MediaDetails(val items: List<MediaItem>, val favoriteKeys: Set<String>)
