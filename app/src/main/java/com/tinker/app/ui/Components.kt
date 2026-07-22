package com.tinker.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tinker.app.ui.theme.AppTheme

@Composable
fun AppText(
    text: String,
    style: TextStyle = AppTheme.type.body,
    color: Color = AppTheme.colors.ink,
    modifier: Modifier = Modifier,
) = BasicText(text, modifier, style.merge(TextStyle(color = color)))

@Composable
fun Overline(text: String, color: Color = AppTheme.colors.mute) =
    AppText(text.uppercase(), AppTheme.type.overline, color)

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.radius.md))
            .background(c.surface)
            .border(1.dp, c.hairline, RoundedCornerShape(AppTheme.radius.md))
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
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AppTheme.radius.sm)
    Column(Modifier.fillMaxWidth()) {
        Overline(label)
        Spacer(Modifier.height(AppTheme.space.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface)
                .border(if (focused) 2.dp else 1.dp, if (focused) c.primary else c.hairline, shape)
                .padding(horizontal = 14.dp, vertical = 13.dp)
                .heightIn(min = minHeight)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = AppTheme.type.body.merge(TextStyle(color = c.ink)),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) AppText(placeholder, color = c.mute)
                    inner()
                },
            )
        }
    }
}

/** Primary CTA with a tactile "key" that presses down onto its darker edge. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(AppTheme.radius.md)
    val drop by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp, label = "press")
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) c.primaryEdge else c.hairline)
            .clickable(interaction, indication = null, enabled = enabled) { onClick() }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = drop)
                .clip(shape)
                .background(if (enabled) c.primary else c.surfaceSoft),
            contentAlignment = Alignment.Center,
        ) { AppText(text, AppTheme.type.label, if (enabled) c.onPrimary else c.mute) }
    }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.sm)
    Box(
        Modifier
            .size(46.dp)
            .clip(shape)
            .background(c.surfaceSoft)
            .border(1.dp, c.hairline, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { AppText(text, AppTheme.type.heading, c.ink) }
}

@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    val knob by animateDpAsState(if (checked) 22.dp else 0.dp, label = "knob")
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(if (checked) c.live else c.surfaceSoft)
            .border(1.dp, if (checked) c.liveEdge else c.hairline, CircleShape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Box(Modifier.offset(x = knob).size(24.dp).clip(CircleShape).background(Color.White)) }
}
