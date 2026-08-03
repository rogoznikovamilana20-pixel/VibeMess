package com.vibe.ui.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    // **bold**
                    text.startsWith("**", i) && text.indexOf("**", i + 2) > i -> {
                        val end = text.indexOf("**", i + 2)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    }
                    // *italic*
                    text.startsWith("*", i) && !text.startsWith("**", i) && text.indexOf("*", i + 1) > i -> {
                        val end = text.indexOf("*", i + 1)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    }
                    // __underline__
                    text.startsWith("__", i) && text.indexOf("__", i + 2) > i -> {
                        val end = text.indexOf("__", i + 2)
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color
    )
}
