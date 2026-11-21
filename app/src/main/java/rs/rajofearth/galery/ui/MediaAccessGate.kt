package rs.rajofearth.galery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rs.rajofearth.galery.R

@Composable
fun MediaAccessGate(
    state: MediaAccessState,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (state) {
        MediaAccessState.Granted -> content()

        MediaAccessState.RequestFirstTime,
        MediaAccessState.Denied,
        -> MediaAccessScreen(
            denied = state == MediaAccessState.Denied,
            onRequestAccess = onRequestAccess,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun MediaAccessScreen(
    denied: Boolean,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.media_access_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.media_access_explanation),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestAccess) {
                Text(
                    stringResource(
                        if (denied) R.string.media_access_retry else R.string.media_access_grant,
                    ),
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.media_access_open_settings))
            }
        }
    }
}
