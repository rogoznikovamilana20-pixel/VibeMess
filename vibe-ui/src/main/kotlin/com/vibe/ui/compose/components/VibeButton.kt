package com.vibe.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class VibeButtonVariant {
    PRIMARY, SECONDARY, GHOST, OUTLINE
}

enum class VibeButtonSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun VibeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: VibeButtonVariant = VibeButtonVariant.PRIMARY,
    size: VibeButtonSize = VibeButtonSize.MEDIUM,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    val shape = RoundedCornerShape(
        when (size) {
            VibeButtonSize.SMALL -> 12.dp
            VibeButtonSize.MEDIUM -> 16.dp
            VibeButtonSize.LARGE -> 20.dp
        }
    )

    val contentPadding = when (size) {
        VibeButtonSize.SMALL -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        VibeButtonSize.MEDIUM -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        VibeButtonSize.LARGE -> PaddingValues(horizontal = 32.dp, vertical = 16.dp)
    }

    val textStyle = when (size) {
        VibeButtonSize.SMALL -> MaterialTheme.typography.labelLarge
        VibeButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        VibeButtonSize.LARGE -> MaterialTheme.typography.titleSmall
    }

    val buttonModifier = if (fullWidth) modifier.height(
        when (size) {
            VibeButtonSize.SMALL -> 36.dp
            VibeButtonSize.MEDIUM -> 48.dp
            VibeButtonSize.LARGE -> 56.dp
        }
    ) else modifier

    val colors = when (variant) {
        VibeButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        VibeButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        VibeButtonVariant.GHOST -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        VibeButtonVariant.OUTLINE -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }

    val buttonContent: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(
                        when (size) {
                            VibeButtonSize.SMALL -> 16.dp
                            VibeButtonSize.MEDIUM -> 20.dp
                            VibeButtonSize.LARGE -> 24.dp
                        }
                    ),
                    color = when (variant) {
                        VibeButtonVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
                        VibeButtonVariant.SECONDARY -> MaterialTheme.colorScheme.onSurface
                        VibeButtonVariant.GHOST -> MaterialTheme.colorScheme.primary
                        VibeButtonVariant.OUTLINE -> MaterialTheme.colorScheme.primary
                    },
                    strokeWidth = 2.dp
                )
            }
            AnimatedVisibility(visible = !isLoading) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(
                                when (size) {
                                    VibeButtonSize.SMALL -> 16.dp
                                    VibeButtonSize.MEDIUM -> 20.dp
                                    VibeButtonSize.LARGE -> 24.dp
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = textStyle,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    when (variant) {
        VibeButtonVariant.PRIMARY, VibeButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = contentPadding
            ) {
                buttonContent()
            }
        }
        VibeButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = contentPadding
            ) {
                buttonContent()
            }
        }
        VibeButtonVariant.OUTLINE -> {
            androidx.compose.material3.OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = contentPadding
            ) {
                buttonContent()
            }
        }
    }
}
