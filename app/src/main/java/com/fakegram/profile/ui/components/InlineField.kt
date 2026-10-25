package com.fakegram.profile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.fakegram.profile.ui.shell.LocalEditMode

/**
 * Text you type straight into a screen instead of through a dialog. It keeps its own text while you type and
 * saves when you tap away. [wrap] makes it only as wide as its text (for fields sitting in a row).
 */
@Composable
fun InlineField(
    value: String,
    onCommit: (String) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    placeholderColor: Color = Color(0x80808080),
    cursorColor: Color = style.color,
    singleLine: Boolean = false,
    wrap: Boolean = false,
    onBlur: () -> Unit = {}
) {
    val editable = LocalEditMode.current
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = text, onValueChange = { text = it }, readOnly = !editable, textStyle = style, singleLine = singleLine,
        cursorBrush = SolidColor(if (cursorColor == Color.Unspecified) Color.Gray else cursorColor),
        modifier = modifier.then(if (wrap) Modifier.width(IntrinsicSize.Min) else Modifier).onFocusChanged {
            if (focused && !it.isFocused) { if (text != value) onCommit(text); onBlur() }
            focused = it.isFocused
        },
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) Text(placeholder, style = style.copy(color = placeholderColor))
                inner()
            }
        }
    )
}
