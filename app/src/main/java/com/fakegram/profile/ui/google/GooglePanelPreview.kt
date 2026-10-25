@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fakegram.profile.ui.google

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.fakegram.profile.ui.components.InlineField
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.R
import com.fakegram.profile.data.PanelFactEntity
import com.fakegram.profile.data.PanelItemEntity
import com.fakegram.profile.data.PanelItemKind
import com.fakegram.profile.data.PanelUiState

/** Google Sans (OFL, variable) for titles / card headings; Roboto (system default) for body text. */
private val GoogleSans = FontFamily(
    Font(R.font.google_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.google_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.google_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
)

/** Colours sampled from Chrome's dark results page; light follows Google's light theme. */
private class GooglePalette(
    val background: Color,
    val card: Color,
    val searchPill: Color,
    val title: Color,
    val text: Color,
    val subtitle: Color,
    val tabIdle: Color,
    val link: Color,
    val divider: Color
)

private val DarkPalette = GooglePalette(
    background = Color(0xFF22222A),
    card = Color(0xFF35353F),
    searchPill = Color(0xFF4C5054),
    title = Color(0xFFF6F6FE),
    text = Color(0xFFE3E3EB),
    subtitle = Color(0xFFCFCFD9),
    tabIdle = Color(0xFFACACB4),
    link = Color(0xFF9DB9F5),
    divider = Color(0xFF3F3F49)
)

private val LightPalette = GooglePalette(
    background = Color(0xFFFFFFFF),
    card = Color(0xFFF1F3F8),
    searchPill = Color(0xFFF1F3F8),
    title = Color(0xFF1F1F1F),
    text = Color(0xFF1F1F1F),
    subtitle = Color(0xFF444746),
    tabIdle = Color(0xFF5F6368),
    link = Color(0xFF0B57D0),
    divider = Color(0xFFDADCE0)
)

private val SectionOrder = listOf(PanelItemKind.SONGS, PanelItemKind.MEMBERS)

private fun sectionTitle(kind: String) = if (kind == PanelItemKind.SONGS) "Songs" else "Members"

private val CardShape = RoundedCornerShape(20.dp)

/** A card in the two-column tile grid: either an info fact or an expandable section. */
private sealed interface Tile {
    data class Fact(val fact: PanelFactEntity) : Tile
    data class Section(val kind: String, val items: List<PanelItemEntity>) : Tile
    data object AddFact : Tile
}

/**
 * Renders a knowledge panel the way Google's mobile search results show it. Songs / Members cards
 * and the overview expand when tapped, like the real thing.
 */
@Composable
fun GooglePanelPreview(state: PanelUiState, isDark: Boolean, modifier: Modifier = Modifier, edit: PanelViewModel? = null) {
    val palette = if (isDark) DarkPalette else LightPalette
    val panel = state.panel ?: return

    // Both sections start collapsed side by side, exactly like a fresh results page.
    var expandedKinds by remember { mutableStateOf(emptySet<String>()) }
    var overviewExpanded by remember { mutableStateOf(false) }
    // The first composition must not animate; only cards the user toggled do.
    var hasToggled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
    ) {
        GoogleHeader(palette, avatarUri = state.images.minByOrNull { it.position }?.uri)
        SearchBar(query = if (edit != null) panel.query else panel.query.ifBlank { panel.title }, palette = palette, edit = edit)
        SearchTabs(palette)

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleStyle = TextStyle(fontFamily = GoogleSans, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, color = palette.title)
                if (edit != null) {
                    InlineField(panel.title, { v -> edit.updatePanel { it.copy(title = v.trim()) } }, titleStyle, Modifier.fillMaxWidth(), placeholder = "Name", placeholderColor = palette.tabIdle)
                    InlineField(
                        panel.subtitle, { v -> edit.updatePanel { it.copy(subtitle = v.trim()) } }, TextStyle(color = palette.subtitle, fontSize = 14.sp),
                        Modifier.fillMaxWidth().padding(top = 2.dp), placeholder = "Description (South Korean singer)", placeholderColor = palette.tabIdle
                    )
                } else {
                    Text(panel.title, color = palette.title, style = titleStyle)
                    if (panel.subtitle.isNotBlank()) {
                        Text(panel.subtitle, color = palette.subtitle, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Icon(
                Icons.Filled.MoreVert, null, tint = palette.text,
                modifier = Modifier.padding(top = 6.dp).size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.size(36.dp).border(1.dp, palette.divider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Share, null, tint = palette.text, modifier = Modifier.size(17.dp))
            }
        }

        if (state.images.isNotEmpty() || edit != null) {
            Spacer(Modifier.height(14.dp))
            val images = state.images.sortedBy { it.position }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
                    // Only the outer ends of the strip carry Google's large rounded corners.
                    val big = 16.dp
                    val small = 3.dp
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) big else small,
                        bottomStart = if (index == 0) big else small,
                        topEnd = if (index == images.lastIndex) big else small,
                        bottomEnd = if (index == images.lastIndex) big else small
                    )
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier.height(206.dp).clip(shape).background(palette.card)
                                .then(if (edit != null) Modifier.combinedClickable(onClick = {}, onLongClick = { menu = true }) else Modifier)
                        )
                        if (edit != null) DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Move left") }, onClick = { menu = false; edit.moveImage(image, -1) })
                            DropdownMenuItem(text = { Text("Move right") }, onClick = { menu = false; edit.moveImage(image, 1) })
                            DropdownMenuItem(text = { Text("Remove photo") }, onClick = { menu = false; edit.deleteImage(image.id) })
                        }
                    }
                }
                if (edit != null) item(key = "add-photo") { AddPhotoTile(edit, palette) }
            }
        }

        Spacer(Modifier.height(12.dp))
        val tiles: List<Tile> = state.facts.sortedBy { it.position }.map { Tile.Fact(it) } +
            SectionOrder.mapNotNull { kind ->
                val list = state.items(kind)
                // While editing, Songs and Members always show so there is somewhere to add the first one.
                if (list.isEmpty() && edit == null) null else Tile.Section(kind, list)
            } + (if (edit != null) listOf(Tile.AddFact) else emptyList())
        TileGrid(
            tiles = tiles,
            expandedKinds = expandedKinds,
            animate = hasToggled,
            palette = palette,
            edit = edit,
            onToggle = { kind ->
                hasToggled = true
                expandedKinds = if (kind in expandedKinds) expandedKinds - kind else expandedKinds + kind
            }
        )

        if (edit != null) {
            EditableOverviewCard(panel.overview, panel.sourceLabel, palette, edit)
        } else if (panel.overview.isNotBlank()) {
            OverviewCard(
                overview = panel.overview,
                source = panel.sourceLabel,
                expanded = overviewExpanded,
                palette = palette,
                onToggle = { overviewExpanded = !overviewExpanded }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GoogleHeader(palette: GooglePalette, avatarUri: String?) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
        Text(
            "Google",
            color = palette.title,
            style = TextStyle(
                fontFamily = GoogleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                letterSpacing = (-0.8).sp
            ),
            modifier = Modifier.align(Alignment.Center)
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box {
                Icon(Icons.Filled.Notifications, null, tint = palette.text, modifier = Modifier.size(22.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(Color(0xFFE53935), CircleShape)
                )
            }
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(palette.card),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(model = avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Person, null, tint = palette.tabIdle, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, palette: GooglePalette, edit: PanelViewModel? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.searchPill)
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = palette.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        if (edit != null) {
            InlineField(
                query, { v -> edit.updatePanel { it.copy(query = v.trim()) } }, TextStyle(color = palette.title, fontSize = 16.sp),
                Modifier.weight(1f), placeholder = "Search", singleLine = true, placeholderColor = palette.tabIdle
            )
        } else {
            Text(
                query,
                color = palette.title,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Icon(Icons.Filled.Close, null, tint = palette.title, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(1.dp).height(24.dp).background(palette.tabIdle.copy(alpha = 0.35f)))
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.Mic, null, tint = palette.title, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SearchTabs(palette: GooglePalette) {
    val tabs = listOf("AI Mode", "All", "Images", "Videos", "Short videos", "Shopping", "News")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        tabs.forEach { label ->
            val selected = label == "All"
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Text(
                    label,
                    color = if (selected) palette.title else palette.tabIdle,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (selected) palette.title else Color.Transparent,
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
}

/** Fades freshly swapped card content in; only after the user toggled something. */
@Composable
private fun FadeIn(animate: Boolean, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(260, easing = LinearOutSlowInEasing)) }
    Box(Modifier.graphicsLayer { this.alpha = alpha.value }) { content() }
}

@Composable
private fun TileGrid(
    tiles: List<Tile>,
    expandedKinds: Set<String>,
    animate: Boolean,
    palette: GooglePalette,
    edit: PanelViewModel?,
    onToggle: (String) -> Unit
) {
    // The lookahead scope lets each card morph from its old bounds to its new ones: the tapped
    // card grows toward the opposite side while its neighbour slides underneath.
    LookaheadScope {
        val boundsForFlow = rememberBoundsAnimation()
        TileFlow(
            modifier = Modifier.padding(horizontal = 12.dp).animateBounds(boundsForFlow)
        ) {
            for (tile in tiles) {
                when (tile) {
                    is Tile.Fact -> key("fact-${tile.fact.id}") {
                        val bounds = rememberBoundsAnimation()
                        Box(Modifier.tileExpanded(false).animateBounds(bounds)) { FactCard(tile.fact, palette, edit) }
                    }
                    Tile.AddFact -> key("add-fact") {
                        Box(Modifier.tileExpanded(false)) { AddCard("Add info card", palette) { edit?.addFact() } }
                    }
                    is Tile.Section -> key(tile.kind) {
                        val bounds = rememberBoundsAnimation()
                        val expanded = tile.kind in expandedKinds
                        val members = tile.kind == PanelItemKind.MEMBERS
                        val background by animateColorAsState(
                            if (expanded && members) palette.card.copy(alpha = 0f) else palette.card,
                            tween(300), label = "tileBackground"
                        )
                        Box(
                            Modifier
                                .tileExpanded(expanded)
                                .animateBounds(bounds)
                                .clip(CardShape)
                                .background(background)
                        ) {
                            key(expanded) {
                                FadeIn(animate) {
                                    if (expanded) ExpandedSection(tile, palette, edit) { onToggle(tile.kind) }
                                    else CollapsedSection(tile, palette) { onToggle(tile.kind) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val CardHeading = TextStyle(fontFamily = GoogleSans, fontWeight = FontWeight.Medium, fontSize = 14.sp)

@Composable
private fun FactCard(fact: PanelFactEntity, palette: GooglePalette, edit: PanelViewModel? = null) {
    var menu by remember { mutableStateOf(false) }
    val valueStyle = TextStyle(fontFamily = GoogleSans, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = palette.title)
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(palette.card)
                .then(if (edit != null) Modifier.combinedClickable(onClick = {}, onLongClick = { menu = true }) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (edit != null) {
                InlineField(fact.label, { v -> edit.updateFact(fact.copy(label = v.trim())) }, CardHeading.copy(color = palette.text), Modifier.fillMaxWidth(), placeholder = "Label", placeholderColor = palette.tabIdle)
                Spacer(Modifier.height(8.dp))
                InlineField(fact.value, { v -> edit.updateFact(fact.copy(value = v.trim())) }, valueStyle, Modifier.fillMaxWidth(), placeholder = "Value", placeholderColor = palette.tabIdle)
                InlineField(fact.detail, { v -> edit.updateFact(fact.copy(detail = v.trim())) }, TextStyle(color = palette.subtitle, fontSize = 12.sp), Modifier.fillMaxWidth(), placeholder = "Detail", placeholderColor = palette.tabIdle)
            } else {
                Text(fact.label, color = palette.text, style = CardHeading)
                Spacer(Modifier.height(8.dp))
                Text(fact.value, color = palette.title, style = valueStyle)
                if (fact.detail.isNotBlank()) {
                    Text(fact.detail, color = palette.subtitle, fontSize = 12.sp)
                }
            }
        }
        if (edit != null) DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Delete card") }, onClick = { menu = false; edit.deleteFact(fact.id) })
        }
    }
}

/** A dashed-feeling "+" card that adds something while editing. */
@Composable
private fun AddCard(label: String, palette: GooglePalette, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(palette.card.copy(alpha = 0.55f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Add, null, tint = palette.text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = palette.tabIdle, fontSize = 13.sp)
    }
}

@Composable
private fun AddPhotoTile(edit: PanelViewModel, palette: GooglePalette) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris -> if (uris.isNotEmpty()) edit.addImages(uris) }
    Box(
        Modifier.padding(start = 3.dp).height(206.dp).width(120.dp).clip(RoundedCornerShape(16.dp)).background(palette.card)
            .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Add, null, tint = palette.text, modifier = Modifier.size(28.dp))
            Text("Add photos", color = palette.tabIdle, fontSize = 12.sp)
        }
    }
}

/** A song / member picture. While editing, tap it to pick one and hold it to delete the row. */
@Composable
private fun PickThumb(item: PanelItemEntity, palette: GooglePalette, modifier: Modifier, shape: Shape, edit: PanelViewModel?) {
    if (edit == null) { Thumb(item.imageUri, palette, modifier, shape); return }
    var menu by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) edit.setItemImage(item, uri) }
    Box(modifier.combinedClickable(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onLongClick = { menu = true }
    )) {
        Thumb(item.imageUri, palette, Modifier.fillMaxSize(), shape)
        if (item.imageUri == null) Icon(Icons.Filled.Add, null, tint = palette.tabIdle, modifier = Modifier.align(Alignment.Center).size(22.dp))
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Change picture") }, onClick = { menu = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; edit.deleteItem(item.id) })
        }
    }
}

@Composable
private fun EditableOverviewCard(overview: String, source: String, palette: GooglePalette, edit: PanelViewModel) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.card)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text("Overview", color = palette.text, style = CardHeading)
        Spacer(Modifier.height(8.dp))
        InlineField(
            overview, { v -> edit.updatePanel { it.copy(overview = v.trim()) } }, TextStyle(color = palette.text, fontSize = 14.sp, lineHeight = 20.sp),
            Modifier.fillMaxWidth(), placeholder = "Overview text", placeholderColor = palette.tabIdle
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            InlineField(
                source, { v -> edit.updatePanel { it.copy(sourceLabel = v.trim()) } }, TextStyle(color = palette.link, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                placeholder = "Source", singleLine = true, wrap = true, placeholderColor = palette.tabIdle
            )
            Text(" >", color = palette.link, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CollapsedSection(tile: Tile.Section, palette: GooglePalette, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            Text(sectionTitle(tile.kind), color = palette.text, style = CardHeading, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = palette.text, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val shown = tile.items.take(3)
            shown.forEach { item ->
                Thumb(item.imageUri, palette, Modifier.weight(1f).aspectRatio(1f), RoundedCornerShape(10.dp))
            }
            // Keep thumbnails the same width when fewer than three exist.
            repeat(3 - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ExpandedSection(tile: Tile.Section, palette: GooglePalette, edit: PanelViewModel?, onClick: () -> Unit) {
    val members = tile.kind == PanelItemKind.MEMBERS
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                sectionTitle(tile.kind),
                color = palette.title,
                style = TextStyle(fontFamily = GoogleSans, fontWeight = FontWeight.Normal, fontSize = 22.sp),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Box(
                modifier = Modifier.size(36.dp).background(palette.card, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = palette.text, modifier = Modifier.size(22.dp))
            }
        }
        if (members) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)
            ) {
                tile.items.forEach { member ->
                    Column(modifier = Modifier.width(104.dp)) {
                        PickThumb(member, palette, Modifier.size(104.dp), RoundedCornerShape(14.dp), edit)
                        if (edit != null) {
                            InlineField(
                                member.title, { v -> edit.updateItem(member.copy(title = v.trim())) }, TextStyle(color = palette.text, fontSize = 14.sp, lineHeight = 18.sp),
                                Modifier.padding(top = 8.dp, start = 4.dp, end = 14.dp), placeholder = "Name", placeholderColor = palette.tabIdle
                            )
                        } else {
                            Text(
                                member.title,
                                color = palette.text,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 14.dp)
                            )
                        }
                    }
                }
                if (edit != null) {
                    Box(
                        Modifier.size(104.dp).clip(RoundedCornerShape(14.dp)).background(palette.card).clickable { edit.addItem(PanelItemKind.MEMBERS) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, "Add member", tint = palette.text, modifier = Modifier.size(28.dp)) }
                }
            }
            Spacer(Modifier.height(4.dp))
        } else {
            tile.items.forEachIndexed { index, song ->
                if (index > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(palette.divider))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PickThumb(song, palette, Modifier.width(72.dp).height(44.dp), RoundedCornerShape(8.dp), edit)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        if (edit != null) {
                            InlineField(song.title, { v -> edit.updateItem(song.copy(title = v.trim())) }, TextStyle(color = palette.title, fontSize = 15.sp), Modifier.fillMaxWidth(), placeholder = "Song title", singleLine = true, placeholderColor = palette.tabIdle)
                            InlineField(song.subtitle, { v -> edit.updateItem(song.copy(subtitle = v.trim())) }, TextStyle(color = palette.tabIdle, fontSize = 11.sp), Modifier.fillMaxWidth(), placeholder = "Artist \u00B7 Year", singleLine = true, placeholderColor = palette.tabIdle)
                        } else {
                            Text(song.title, color = palette.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (song.subtitle.isNotBlank()) {
                                Text(song.subtitle, color = palette.tabIdle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (edit != null) {
                Row(
                    Modifier.fillMaxWidth().clickable { edit.addItem(PanelItemKind.SONGS) }.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = palette.text, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add song", color = palette.tabIdle, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun Thumb(uri: String?, palette: GooglePalette, modifier: Modifier, shape: Shape) {
    Box(modifier = modifier.clip(shape).background(palette.divider)) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun OverviewCard(overview: String, source: String, expanded: Boolean, palette: GooglePalette, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.card)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .animateContentSize(tween(250))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Overview", color = palette.text, style = CardHeading, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null, tint = palette.text, modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append(overview)
                if (source.isNotBlank()) {
                    append(" ")
                    withStyle(SpanStyle(color = palette.link, fontWeight = FontWeight.Medium)) { append("$source >") }
                }
            },
            color = palette.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}
