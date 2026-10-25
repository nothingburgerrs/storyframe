package com.fakegram.profile.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The plain iOS-ish colours shared by Notes, Photos and Messages. */
class PhoneColors(val bg: Color, val card: Color, val text: Color, val sub: Color, val line: Color, val field: Color)

@Composable
fun phoneColors(): PhoneColors =
    if (LocalStoryframeColors.current.isDark) {
        PhoneColors(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFFF5F5F7), Color(0xFF8E8E93), Color(0xFF38383A), Color(0xFF1C1C1E))
    } else {
        PhoneColors(Color(0xFFF2F2F7), Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFF8E8E93), Color(0xFFD1D1D6), Color(0xFFE5E5EA))
    }

fun formatTime(millis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
fun formatDay(millis: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

/** Rounded search box. Searching is something you do on a phone, so it works in View mode too. */
@Composable
fun PhoneSearchField(query: String, onChange: (String) -> Unit, placeholder: String, colors: PhoneColors, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.field).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = colors.sub, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 17.sp), cursorBrush = SolidColor(colors.text),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text(placeholder, style = TextStyle(color = colors.sub, fontSize = 17.sp))
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(Icons.Filled.Close, "Clear", tint = colors.sub, modifier = Modifier.size(20.dp).clickable { onChange("") })
        }
    }
}

/** A small box to type comma-separated words into; used for naming a new contact or tagging pictures. */
@Composable
fun PhoneTextDialog(title: String, hint: String, initial: String, confirm: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text(hint) }, minLines = 1) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
