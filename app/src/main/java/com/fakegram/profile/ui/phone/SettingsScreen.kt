package com.fakegram.profile.ui.phone

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.theme.AppFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Phone settings: wallpaper, dark mode, and the Edit / View switch. */
@Composable
fun SettingsScreen(editing: Boolean, onEditingChange: (Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val c = phoneColors()
    val scope = rememberCoroutineScope()
    val dark by app.themePreferences.isDarkMode.collectAsState(initial = true)
    val wallpaper by app.themePreferences.wallpaper.collectAsState(initial = null)
    val fontId by app.themePreferences.font.collectAsState(initial = AppFonts.DEFAULT_ID)

    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { MediaStorage.copyToInternal(context, uri) }
            if (path != null) app.themePreferences.setWallpaper(path)
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF0A84FF), modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
        }
        Text("Settings", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)) {
                SettingRow("Edit mode", "Off = use the phone like a real one", c) {
                    Switch(checked = editing, onCheckedChange = onEditingChange)
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = c.line)
                SettingRow("Dark mode", null, c) {
                    Switch(checked = dark, onCheckedChange = { scope.launch { app.themePreferences.setDarkMode(it) } })
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)) {
                Text(
                    "Choose wallpaper", color = Color(0xFF0A84FF), fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable { pickWallpaper.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(16.dp)
                )
                if (wallpaper != null) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = c.line)
                    Text(
                        "Remove wallpaper", color = Color(0xFFFF453A), fontSize = 17.sp,
                        modifier = Modifier.fillMaxWidth().clickable { scope.launch { app.themePreferences.setWallpaper(null) } }.padding(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Font", color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)) {
                Text(
                    "Hello 😀 ❤️ 👍🏽 🔥 🇰🇷 안녕", color = c.text, fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                AppFonts.options.forEach { o ->
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = c.line)
                    val family = remember(o.id) { AppFonts.previewFamily(context, o) }
                    Row(
                        Modifier.fillMaxWidth().clickable { scope.launch { app.themePreferences.setFont(o.id) } }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(o.label, color = c.text, fontSize = 19.sp, fontFamily = family, modifier = Modifier.weight(1f))
                        if (o.id == fontId) Icon(Icons.Filled.Check, "Selected", tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, c: PhoneColors, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = c.sub, fontSize = 13.sp)
        }
        trailing()
    }
}
