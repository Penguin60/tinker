package com.perimeter.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perimeter.app.ui.theme.AppTheme

@Composable
fun AppText(
    text: String,
    style: TextStyle = AppTheme.type.body,
    color: Color = AppTheme.colors.onSurface,
    modifier: Modifier = Modifier,
) = BasicText(text, modifier, style.merge(TextStyle(color = color)))

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(AppTheme.space.md),
        content = content,
    )
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Dp = 0.dp,
) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        AppText(label, AppTheme.type.label, c.muted)
        Spacer(Modifier.height(AppTheme.space.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .heightIn(min = minHeight)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = AppTheme.type.body.merge(TextStyle(color = c.onSurface)),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) AppText(placeholder, color = c.muted)
                    inner()
                },
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) c.accent else c.border)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { AppText(text, AppTheme.type.label, if (enabled) c.onAccent else c.muted) }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { AppText(text, AppTheme.type.title, c.onSurface) }
}

@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(if (checked) c.accent else c.border)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White)) }
}
