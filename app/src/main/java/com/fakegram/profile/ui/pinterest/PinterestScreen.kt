package com.fakegram.profile.ui.pinterest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.PinChip
import com.fakegram.profile.data.PinChipColors
import com.fakegram.profile.data.PinEntity
import com.fakegram.profile.data.PinScreen
import com.fakegram.profile.data.toPinChips
import com.fakegram.profile.data.toPinTabs
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.InlineField
import com.fakegram.profile.ui.components.rememberCropPicker
import com.fakegram.profile.ui.theme.LocalStoryframeColors

private val PinRed = Color(0xFFE60023)

/**
 * A fake Pinterest. Home = category tabs over a masonry feed; Search = a search bar, pills and results.
 * Every word, picture, tab and pill is edited right where it is; the + below adds pins.
 */
@Composable
fun PinterestScreen(pageId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val vm: PinterestViewModel = viewModel(key = "pin$pageId", factory = PinterestViewModel.factory(app, app.repository, pageId))
    val page by vm.page.collectAsState()
    val pins by vm.pins.collectAsState()
    val dark = LocalStoryframeColors.current.isDark
    val bg = if (dark) Color.Black else Color.White
    val ink = if (dark) Color.White else Color(0xFF111111)
    val sub = if (dark) Color(0xFFAAAAAA) else Color(0xFF767676)

    var screen by rememberSaveable { mutableStateOf(PinScreen.HOME) }
    var tabId by rememberSaveable { mutableStateOf(0L) }
    var addMenu by remember { mutableStateOf(false) }

    val p = page
    if (p == null) {
        Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) { Text("Loading...", color = sub) }
        return
    }
    val tabs = p.tabs.toPinTabs()
    val activeTab = tabs.firstOrNull { it.id == tabId } ?: tabs.firstOrNull()
    val shown = pins.filter { it.screen == screen && (screen == PinScreen.SEARCH || it.tabId == (activeTab?.id ?: 0L)) }

    val addPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) vm.addPins(uris, screen, if (screen == PinScreen.SEARCH) 0L else activeTab?.id ?: 0L)
    }
    val pickAvatar = rememberCropPicker(CropShape.CIRCLE) { path -> vm.updatePage { it.copy(avatarUri = path) } }

    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().imePadding()) {
        if (screen == PinScreen.HOME) {
            // category tabs
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, top = 22.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                tabs.forEach { t ->
                    val selected = t.id == activeTab?.id
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Column(Modifier.combinedClickableTab(onClick = { tabId = t.id }, onLongClick = { menu = true })) {
                            val style = TextStyle(color = if (selected) ink else sub, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            if (selected) InlineField(t.label, { vm.renameTab(t.id, it.trim()) }, style, placeholder = "Tab", singleLine = true, wrap = true, cursorColor = ink)
                            else Text(t.label, style = style, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) ink else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Delete tab") }, onClick = { menu = false; vm.deleteTab(t.id) })
                        }
                    }
                }
                Icon(Icons.Filled.Add, "Add tab", tint = sub, modifier = Modifier.size(26.dp).clickable { vm.addTab { tabId = it } })
            }
        } else {
            // search bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ink, modifier = Modifier.size(28.dp).clickable { screen = PinScreen.HOME })
                Spacer(Modifier.width(14.dp))
                Row(
                    Modifier.weight(1f).height(56.dp).border(2.dp, if (dark) Color(0xFF555555) else Color(0xFF8E8E8E), RoundedCornerShape(28.dp)).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineField(
                        p.searchQuery, { v -> vm.updatePage { it.copy(searchQuery = v.trim()) } },
                        TextStyle(color = ink, fontSize = 20.sp), Modifier.weight(1f), placeholder = "Search for ideas", singleLine = true, cursorColor = ink
                    )
                }
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Filled.Tune, null, tint = ink, modifier = Modifier.size(32.dp))
            }
            // pills
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                p.chips.toPinChips().forEachIndexed { i, chip -> ChipPill(chip, ink, onChange = { vm.updateChip(i, it) }, onDelete = { vm.deleteChip(i) }) }
                Icon(Icons.Filled.Add, "Add pill", tint = sub, modifier = Modifier.size(28.dp).clickable { vm.addChip() })
            }
            Spacer(Modifier.height(8.dp))
        }

        // the feed
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
            if (shown.isEmpty()) {
                Text("No pins here yet. Tap + below to add pictures.", color = sub, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
            } else {
                Masonry(shown, ink, sub, vm)
            }
            Spacer(Modifier.height(24.dp))
        }

        // bottom bar
        Row(Modifier.fillMaxWidth().background(bg).navigationBarsPadding().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).clickable { screen = PinScreen.HOME }, contentAlignment = Alignment.Center) {
                Icon(if (screen == PinScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home, "Home", tint = ink, modifier = Modifier.size(32.dp))
            }
            Box(Modifier.weight(1f).clickable { screen = PinScreen.SEARCH }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Search, "Search", tint = ink, modifier = Modifier.size(if (screen == PinScreen.SEARCH) 34.dp else 30.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, "Add", tint = ink, modifier = Modifier.size(34.dp).clickable { addMenu = true })
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(text = { Text("Add pictures") }, onClick = { addMenu = false; addPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    DropdownMenuItem(text = { Text("Add a sponsored pin") }, onClick = {
                        addMenu = false
                        vm.addEmptyPin(screen, if (screen == PinScreen.SEARCH) 0L else activeTab?.id ?: 0L)
                    })
                }
            }
            Box(Modifier.weight(1f).clickable { vm.updatePage { it.copy(showDot = !it.showDot) } }, contentAlignment = Alignment.Center) {
                Box {
                    Icon(Icons.Outlined.ChatBubbleOutline, "Messages", tint = ink, modifier = Modifier.size(32.dp))
                    if (p.showDot) Box(Modifier.align(Alignment.TopEnd).size(11.dp).background(PinRed, CircleShape))
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF3A3A3A)).clickable(onClick = pickAvatar), contentAlignment = Alignment.Center) {
                    if (p.avatarUri != null) AsyncImage(p.avatarUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableTab(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = onLongClick)

/** A search pill: optional picture, an editable label, a pastel colour. Hold for photo / colour / delete. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChipPill(chip: PinChip, ink: Color, onChange: (PinChip) -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val pick = rememberCropPicker(CropShape.RECT_4_5) { onChange(chip.copy(imageUri = it)) }
    Box {
        Row(
            Modifier.height(52.dp).clip(RoundedCornerShape(18.dp)).background(Color(chip.color)).combinedClickable(onClick = { if (chip.imageUri == null) pick() }, onLongClick = { menu = true }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chip.imageUri != null) {
                AsyncImage(chip.imageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.size(width = 54.dp, height = 52.dp).clickable(onClick = pick))
            }
            InlineField(
                chip.label, { onChange(chip.copy(label = it.trim())) }, TextStyle(color = Color(0xFF111111), fontSize = 18.sp, fontWeight = FontWeight.Medium),
                Modifier.padding(horizontal = 18.dp), placeholder = "Pill", singleLine = true, wrap = true, cursorColor = Color(0xFF111111), placeholderColor = Color(0x80000000)
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(if (chip.imageUri == null) "Add photo" else "Change photo") }, onClick = { menu = false; pick() })
            DropdownMenuItem(text = { Text("Next colour") }, onClick = {
                menu = false
                val next = PinChipColors[(PinChipColors.indexOf(chip.color) + 1).mod(PinChipColors.size)]
                onChange(chip.copy(color = next))
            })
            DropdownMenuItem(text = { Text("Delete pill") }, onClick = { menu = false; onDelete() })
        }
    }
}

/** Two columns, each pin dropped into the shorter one, like Pinterest. */
@Composable
private fun Masonry(pins: List<PinEntity>, ink: Color, sub: Color, vm: PinterestViewModel) {
    val left = mutableListOf<PinEntity>()
    val right = mutableListOf<PinEntity>()
    var hl = 0f
    var hr = 0f
    for (pin in pins) {
        val h = 1f / pin.aspect + if (pin.sponsored) 0.35f else 0.12f
        if (hl <= hr) { left += pin; hl += h } else { right += pin; hr += h }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { left.forEach { PinCard(it, ink, sub, vm) } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { right.forEach { PinCard(it, ink, sub, vm) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinCard(pin: PinEntity, ink: Color, sub: Color, vm: PinterestViewModel) {
    var menu by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.setPinImage(pin, uri) }
    val choose = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Column(Modifier.fillMaxWidth()) {
        if (pin.sponsored) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                InlineField(pin.title, { vm.savePin(pin.copy(title = it.trim())) }, TextStyle(color = ink, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium), Modifier.weight(1f), placeholder = "Ad title", cursorColor = ink)
                Icon(Icons.Filled.MoreHoriz, "More", tint = ink, modifier = Modifier.padding(start = 6.dp, top = 4.dp, end = 4.dp).size(26.dp).clickable { menu = true })
            }
            Row(Modifier.padding(start = 6.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFF6A1F)), contentAlignment = Alignment.Center) {
                    Text(pin.sponsorName.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                InlineField(pin.sponsorName, { vm.savePin(pin.copy(sponsorName = it.trim())) }, TextStyle(color = sub, fontSize = 20.sp), placeholder = "Sponsored", singleLine = true, cursorColor = ink)
            }
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(pin.aspect).clip(RoundedCornerShape(24.dp)).background(if (ink == Color.White) Color(0xFF222222) else Color(0xFFEFEFEF))
                .combinedClickable(onClick = { if (pin.imageUri == null) choose() }, onLongClick = { menu = true }),
            contentAlignment = Alignment.Center
        ) {
            if (pin.imageUri != null) AsyncImage(pin.imageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text("Tap to add a picture", color = sub, fontSize = 13.sp)
        }
        if (!pin.sponsored) {
            Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MoreHoriz, "More", tint = ink, modifier = Modifier.padding(end = 8.dp).size(24.dp).clickable { menu = true })
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Change picture") }, onClick = { menu = false; choose() })
            DropdownMenuItem(text = { Text(if (pin.sponsored) "Make normal pin" else "Make sponsored") }, onClick = { menu = false; vm.savePin(pin.copy(sponsored = !pin.sponsored)) })
            DropdownMenuItem(text = { Text("Delete pin") }, onClick = { menu = false; vm.deletePin(pin.id) })
        }
    }
}
