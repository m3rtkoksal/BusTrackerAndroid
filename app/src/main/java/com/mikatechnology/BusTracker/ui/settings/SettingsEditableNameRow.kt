package com.mikatechnology.BusTracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun SettingsEditableNameRow(
    title: String,
    value: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(value) }
    var hasFocusedOnce by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun saveAndClose() {
        val trimmed = editText.trim()
        if (trimmed.isNotEmpty() && trimmed != value) {
            onSave(trimmed)
        }
        isEditing = false
        hasFocusedOnce = false
        focusManager.clearFocus()
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                color = if (isEditing) NeonTheme.Secondary else NeonTheme.Outline.copy(alpha = 0.3f),
                shape = SettingsCardShape
            )
            .clickable {
                if (isEditing) {
                    saveAndClose()
                } else {
                    editText = value
                    isEditing = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = { editText = it },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.OnSurface,
                    textAlign = TextAlign.End
                ),
                singleLine = true,
                cursorBrush = SolidColor(NeonTheme.Secondary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { saveAndClose() }),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            hasFocusedOnce = true
                        } else if (hasFocusedOnce && isEditing) {
                            saveAndClose()
                        }
                    }
            )
        } else {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface
            )
        }

        Icon(
            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
            contentDescription = null,
            tint = NeonTheme.Secondary,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(16.dp)
        )
    }
}
