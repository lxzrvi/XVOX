package com.xvox.music.core.design.kit

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import io.github.neoruaa.glasmio.ui.GlasmioEditableText

/**
 * The app's single bare text input: a drop-in for `BasicTextField(value: String, ...)` that is
 * backed by Glasmio's editable text. Under a `GlasmioTextSelectionHost` (mounted at the root by
 * [XvoxDesignEngine] and inside every popup window that hosts a field) it gets Glasmio's glass
 * caret loupe, selection handles and liquid Edit Menu; without a host it behaves like a normal
 * Foundation field.
 *
 * The caller keeps owning a plain `String`; selection and IME composition are kept here so the
 * caret never jumps while typing, and a caller that rejects or truncates a change (length caps)
 * simply sees its own text re-applied.
 */
@Composable
fun XvoxTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    var owned by remember { mutableStateOf(TextFieldValue(value)) }
    // The caller's text always wins; the field only remembers selection and composition.
    val current = owned.copy(text = value)
    SideEffect {
        if (owned.selection != current.selection || owned.composition != current.composition) owned = current
    }
    GlasmioEditableText(
        value = current,
        onValueChange = { next ->
            owned = next
            if (next.text != value) onValueChange(next.text)
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}

/**
 * `TextFieldValue` flavour of [XvoxTextInput], for callers that already own selection and
 * composition themselves. Same Glasmio-backed behaviour as the `String` overload.
 */
@Composable
fun XvoxTextInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    GlasmioEditableText(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}
