package com.fakegram.profile.ui.phone

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.PhotoEntity
import com.fakegram.profile.data.matchesQuery
import com.fakegram.profile.data.normalizeTags
import com.fakegram.profile.data.tagList
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.shell.LocalEditMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PhotosBlue = Color(0xFF0A84FF)

/** A fake Photos library. Edit mode adds, tags and deletes pictures; View mode browses and searches by tag. */
@Composable
fun PhotosScreen(characterId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val dao = app.repository.photos
    val photos by remember(characterId) { dao.observe(characterId) }.collectAsState(initial = emptyList())
    val edit = LocalEditMode.current
    val c = phoneColors()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var viewerIndex by rememberSaveable { mutableStateOf(-1) }
    var pending by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris ->
        if (uris.isNotEmpty()) pending = uris
    }

    val q = query.trim()
    val shown = if (q.isEmpty()) photos else photos.filter { it.matchesQuery(q) }

    BackHandler(enabled = viewerIndex in shown.indices) { viewerIndex = -1 }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PhotosBlue, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
                Spacer(Modifier.weight(1f))
                if (edit) Icon(Icons.Filled.Add, "Add photos", tint = PhotosBlue, modifier = Modifier.size(44.dp).clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }.padding(8.dp))
            }
            Text("Library", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            PhoneSearchField(query, { query = it }, "Search (try a tag)", c, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
            if (shown.isEmpty()) {
                Text(
                    if (q.isNotEmpty()) "No results for “$q”" else if (edit) "No photos yet. Tap + to add some." else "No photos",
                    color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), modifier = Modifier.weight(1f).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(shown, key = { it.id }) { p ->
                        Box(Modifier.aspectRatio(1f).clickable { viewerIndex = shown.indexOf(p) }) {
                            AsyncImage(p.imageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        if (viewerIndex in shown.indices) {
            PhotoViewer(
                photos = shown, startIndex = viewerIndex, edit = edit, colors = c,
                onClose = { viewerIndex = -1 },
                onSaveTags = { p, raw -> scope.launch { dao.update(p.copy(tags = normalizeTags(raw))) } },
                onDelete = { p -> scope.launch { dao.deleteById(p.id) } }
            )
        }
    }

    if (pending.isNotEmpty()) {
        PhoneTextDialog(
            title = "Tag these photos", hint = "airi, selfie, dorm", initial = "", confirm = "Add",
            onSave = { raw ->
                val uris = pending
                pending = emptyList()
                val tags = normalizeTags(raw)
                scope.launch {
                    val now = System.currentTimeMillis()
                    uris.forEachIndexed { i, uri ->
                        val path = withContext(Dispatchers.IO) { MediaStorage.copyToInternal(context, uri) } ?: return@forEachIndexed
                        dao.insert(PhotoEntity(characterId = characterId, imageUri = path, tags = tags, takenAt = now - i))
                    }
                }
            },
            onDismiss = { pending = emptyList() }
        )
    }
}

@Composable
private fun PhotoViewer(
    photos: List<PhotoEntity>, startIndex: Int, edit: Boolean, colors: PhoneColors,
    onClose: () -> Unit, onSaveTags: (PhotoEntity, String) -> Unit, onDelete: (PhotoEntity) -> Unit
) {
    // The page count must follow the list when a photo is deleted, so it reads the latest list.
    val latest by rememberUpdatedState(photos)
    val pagerState = rememberPagerState(initialPage = startIndex) { latest.size }
    var tagDialog by remember { mutableStateOf(false) }
    val current = photos.getOrNull(pagerState.currentPage)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            photos.getOrNull(page)?.let { AsyncImage(it.imageUri, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
        }
        Icon(
            Icons.Filled.Close, "Close", tint = Color.White,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp).size(44.dp).clickable(onClick = onClose).padding(10.dp)
        )
        if (current != null) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x99000000)).navigationBarsPadding().padding(16.dp)) {
                if (current.tags.isNotBlank()) {
                    Text(current.tags.tagList().joinToString(" ") { "#$it" }, color = Color.White, fontSize = 14.sp)
                }
                if (edit) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Edit tags", color = PhotosBlue, fontSize = 16.sp, modifier = Modifier.clickable { tagDialog = true })
                        Icon(Icons.Filled.Delete, "Delete photo", tint = Color(0xFFFF453A), modifier = Modifier.size(26.dp).clickable { onDelete(current) })
                    }
                }
            }
        }
    }
    if (tagDialog && current != null) {
        PhoneTextDialog(
            title = "Tags", hint = "airi, selfie, dorm", initial = current.tags.tagList().joinToString(", "), confirm = "Save",
            onSave = { raw -> onSaveTags(current, raw); tagDialog = false },
            onDismiss = { tagDialog = false }
        )
    }
}
