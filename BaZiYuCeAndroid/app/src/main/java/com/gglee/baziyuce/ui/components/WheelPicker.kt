package com.gglee.baziyuce.ui.components

import android.graphics.drawable.ColorDrawable
import android.widget.NumberPicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.gglee.baziyuce.ui.theme.Ink

/**
 * iOS-like wheel picker via [NumberPicker].
 * [values] are the underlying ints; [label] formats each for display.
 */
@Composable
fun WheelIntPicker(
    value: Int,
    values: List<Int>,
    modifier: Modifier = Modifier,
    label: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    val safeValues = values.ifEmpty { listOf(value) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                // Soften divider if available (API 29+ reflection fallback ignored)
                try {
                    val pickerFields = NumberPicker::class.java.declaredFields
                    for (field in pickerFields) {
                        if (field.name == "mSelectionDivider") {
                            field.isAccessible = true
                            field.set(this, ColorDrawable(android.graphics.Color.TRANSPARENT))
                        }
                    }
                } catch (_: Exception) {
                    // OEM NumberPicker internals vary; ignore
                }
                setTextColorCompat(Ink.toArgb())
            }
        },
        update = { picker ->
            val labels = safeValues.map(label).toTypedArray()
            val idx = safeValues.indexOf(value).coerceAtLeast(0)
            // Avoid recursive set when unchanged
            if (picker.maxValue != safeValues.lastIndex ||
                picker.displayedValues?.toList() != labels.toList()
            ) {
                picker.displayedValues = null
                picker.minValue = 0
                picker.maxValue = safeValues.lastIndex
                picker.displayedValues = labels
            }
            if (picker.value != idx) {
                picker.value = idx
            }
            picker.setOnValueChangedListener { _, _, newIndex ->
                val next = safeValues.getOrElse(newIndex) { safeValues.first() }
                if (next != value) onValueChange(next)
            }
        },
    )
}

private fun NumberPicker.setTextColorCompat(color: Int) {
    try {
        val method = NumberPicker::class.java.getMethod("setTextColor", Int::class.javaPrimitiveType)
        method.invoke(this, color)
    } catch (_: Exception) {
        // pre-API 29: leave default
    }
}
