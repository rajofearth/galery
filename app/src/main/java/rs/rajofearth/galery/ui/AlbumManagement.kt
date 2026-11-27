package rs.rajofearth.galery.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.AlbumFolder
import rs.rajofearth.galery.data.MediaAlbum
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.albumName
import rs.rajofearth.galery.data.canRename
import rs.rajofearth.galery.data.folder
import rs.rajofearth.galery.data.moveAlbumMedia
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryTypography

internal class AlbumManagement(
    val isBusy: Boolean,
    val manage: ((MediaAlbum) -> Unit)?,
    val move: ((List<MediaItem>) -> Unit)?,
)

@Composable
internal fun rememberAlbumManagement(
    albums: () -> List<MediaAlbum>,
    frameAtDraw: () -> GlassFrame?,
    onTrash: (List<MediaItem>) -> Unit,
    onMediaChanged: () -> Unit,
): AlbumManagement {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return AlbumManagement(false, null, null)
    val context = LocalContext.current
    val resources = LocalResources.current
    val refresh by rememberUpdatedState(onMediaChanged)
    var sheet by remember { mutableStateOf<AlbumSheet?>(null) }
    var operation by remember { mutableStateOf<AlbumMove?>(null) }
    var writeApproval by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        writeApproval?.complete(it.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(operation) {
        val move = operation ?: return@LaunchedEffect
        val result = moveAlbumMedia(context.contentResolver, move.items, move.destination) { uris ->
            val approval = CompletableDeferred<Boolean>()
            writeApproval = approval
            try {
                launcher.launch(albumWriteRequest(context, uris))
                approval.await()
            } catch (_: RuntimeException) {
                Toast.makeText(context, R.string.album_move_permission_failed, Toast.LENGTH_LONG).show()
                false
            } finally {
                writeApproval = null
            }
        }
        operation = null
        if (result.moved > 0) refresh()
        val text = when {
            result.cancelled && result.moved == 0 -> null
            result.moved == 0 -> resources.getString(R.string.album_move_failed)
            result.moved < move.items.size -> resources.getString(R.string.album_move_partial, result.moved, move.items.size)
            move.rename -> resources.getString(R.string.album_renamed, move.destination.name)
            else -> resources.getQuantityString(
                R.plurals.album_move_done, result.moved, result.moved, move.destination.name,
            )
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun startMove(items: List<MediaItem>, destination: AlbumFolder, rename: Boolean = false) {
        val changed = items.filter { it.albumKey != destination.key }
        sheet = null
        if (changed.isNotEmpty()) operation = AlbumMove(changed, destination, rename)
    }

    sheet?.let { active ->
        GallerySheet({ sheet = null }, frameAtDraw) {
            when (active) {
                is AlbumSheet.Actions -> {
                    val folder = active.album.folder
                    AlbumActionList(
                        active.album,
                        onRename = if (folder.canRename && folder.accepts(active.album.items)) {
                            { sheet = AlbumSheet.Name(active.album.items, folder) }
                        } else null,
                        onTrash = { sheet = null; onTrash(active.album.items) },
                    )
                }
                is AlbumSheet.MoveTo -> AlbumMovePicker(
                    active.items,
                    albums(),
                    onCreate = { sheet = AlbumSheet.Name(active.items, original = null) },
                    onPick = { startMove(active.items, it) },
                )
                is AlbumSheet.Name -> {
                    val parent = active.original?.path?.substringBeforeLast('/') ?: "Pictures"
                    val existing = remember(active) { albums().map { it.folder }.toSet() }
                    AlbumNameForm(
                        original = active.original,
                        folderForName = { AlbumFolder(active.items.first().volumeName, "$parent/$it") },
                        exists = { candidate ->
                            existing.any { it.volume == candidate.volume && it.path.equals(candidate.path, ignoreCase = true) }
                        },
                        onCancel = {
                            sheet = if (active.original == null) AlbumSheet.MoveTo(active.items) else null
                        },
                        onSave = { startMove(active.items, it, rename = active.original != null) },
                    )
                }
            }
        }
    }

    return AlbumManagement(
        isBusy = operation != null,
        manage = { if (operation == null) sheet = AlbumSheet.Actions(it) },
        move = { items ->
            if (operation == null && items.isNotEmpty()) {
                if (items.any { it.volumeName != items.first().volumeName }) {
                    Toast.makeText(context, R.string.album_move_same_storage, Toast.LENGTH_LONG).show()
                } else {
                    sheet = AlbumSheet.MoveTo(items)
                }
            }
        },
    )
}

private sealed interface AlbumSheet {
    data class Actions(val album: MediaAlbum) : AlbumSheet
    data class MoveTo(val items: List<MediaItem>) : AlbumSheet
    data class Name(val items: List<MediaItem>, val original: AlbumFolder?) : AlbumSheet
}

private data class AlbumMove(val items: List<MediaItem>, val destination: AlbumFolder, val rename: Boolean)

@RequiresApi(Build.VERSION_CODES.R)
private fun albumWriteRequest(context: Context, uris: List<Uri>): IntentSenderRequest =
    IntentSenderRequest.Builder(MediaStore.createWriteRequest(context.contentResolver, uris).intentSender).build()

@Composable
private fun AlbumActionList(album: MediaAlbum, onRename: (() -> Unit)?, onTrash: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.inset).padding(bottom = GalleryDesign.inset)) {
        GalleryText(album.name.ifBlank { stringResource(R.string.device_storage) }, style = GalleryTypography.titleLarge)
        if (onRename != null) {
            AlbumActionRow(R.string.album_rename, Icons.Filled.Edit, onClick = onRename)
        }
        AlbumActionRow(R.string.album_delete, Icons.Filled.DeleteOutline, destructive = true, onClick = onTrash)
    }
}

@Composable
private fun AlbumMovePicker(
    items: List<MediaItem>,
    albums: List<MediaAlbum>,
    onCreate: () -> Unit,
    onPick: (AlbumFolder) -> Unit,
) {
    val options = remember(albums, items) {
        val current = items.mapTo(HashSet()) { it.albumKey }
        albums.filter { album ->
            (current.size > 1 || album.key !in current) && album.folder.accepts(items)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.inset).padding(bottom = GalleryDesign.inset)) {
        GalleryText(stringResource(R.string.album_move_title), style = GalleryTypography.titleLarge)
        GalleryText(
            pluralStringResource(R.plurals.album_move_prompt, items.size, items.size),
            color = GalleryPalette.onSurfaceVariant,
            style = GalleryTypography.bodyMedium,
        )
        AlbumActionRow(R.string.album_create, Icons.Filled.CreateNewFolder, onClick = onCreate)
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(options, key = { it.key }) { album ->
                Column(
                    Modifier.fillMaxWidth().clip(GalleryDesign.cover).clickable(role = Role.Button) {
                        onPick(album.folder)
                    }.heightIn(min = GalleryDesign.target).padding(vertical = GalleryDesign.gap),
                ) {
                    GalleryText(album.name, style = GalleryTypography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    GalleryText(
                        album.folder.path,
                        color = GalleryPalette.onSurfaceVariant,
                        style = GalleryTypography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumActionRow(label: Int, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) GalleryPalette.error else GalleryPalette.onSurface
    Row(
        Modifier.fillMaxWidth().clip(GalleryDesign.cover).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = GalleryDesign.target),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryIcon(icon, null, tint = tint)
        GalleryText(stringResource(label), color = tint, style = GalleryTypography.titleMedium)
    }
}

@Composable
private fun AlbumNameForm(
    original: AlbumFolder?,
    folderForName: (String) -> AlbumFolder,
    exists: (AlbumFolder) -> Boolean,
    onCancel: () -> Unit,
    onSave: (AlbumFolder) -> Unit,
) {
    var input by remember(original) { mutableStateOf(original?.name.orEmpty()) }
    val destination = albumName(input)?.let(folderForName)
    val nameTaken = destination != null && destination != original && exists(destination)
    Column(
        Modifier.fillMaxWidth().imePadding().padding(horizontal = GalleryDesign.inset).padding(bottom = GalleryDesign.inset),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
    ) {
        GalleryText(stringResource(if (original == null) R.string.album_create else R.string.album_rename), style = GalleryTypography.titleLarge)
        GalleryText(stringResource(R.string.album_name), style = GalleryTypography.labelLarge)
        BasicTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().clip(GalleryDesign.cover).background(GalleryPalette.surfaceContainer)
                .padding(GalleryDesign.inset),
            textStyle = GalleryTypography.bodyLarge.copy(color = GalleryPalette.onSurface),
            cursorBrush = SolidColor(GalleryPalette.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        if (nameTaken || (input.isNotBlank() && destination == null)) {
            GalleryText(
                stringResource(if (nameTaken) R.string.album_name_used else R.string.album_name_invalid),
                color = GalleryPalette.error,
                style = GalleryTypography.bodyMedium,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap, Alignment.End)) {
            GalleryPillAction(stringResource(R.string.album_cancel), onClick = onCancel)
            GalleryPillAction(
                stringResource(if (original == null) R.string.album_create_move else R.string.album_rename),
                primary = true,
                enabled = destination != null && destination != original && !nameTaken,
            ) { if (destination != null) onSave(destination) }
        }
    }
}
