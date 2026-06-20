package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun XhsPublishTopBar(
    title: String,
    actionLabel: String,
    onBack: () -> Unit,
    onAction: () -> Unit,
    actionEnabled: Boolean,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = XhsTextPrimary,
        )
        TextButton(
            onClick = onAction,
            enabled = actionEnabled && !isSubmitting,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = XhsRed,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    actionLabel,
                    color = if (actionEnabled) XhsRed else XhsTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun XhsPublishSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = XhsTextPrimary,
    )
}

@Composable
fun XhsPublishFieldCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.5.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun XhsPublishTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp, color = XhsTextPrimary),
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(XhsRed),
        minLines = minLines,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = XhsTextSecondary, style = textStyle)
                }
                inner()
            }
        },
    )
}

@Composable
fun XhsPublishDateTimeField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayValue = formatActivityDateTimeForDisplay(value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = XhsTextSecondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = displayValue.ifBlank { placeholder },
                fontSize = 16.sp,
                color = if (displayValue.isBlank()) XhsTextSecondary else XhsTextPrimary,
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = XhsTextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun XhsPublishLocationPicker(
    value: String,
    placeholder: String,
    isLoading: Boolean,
    onOpenSearch: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF7F7F7))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenSearch,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = XhsRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value.ifBlank { placeholder },
                fontSize = 15.sp,
                color = if (value.isBlank()) XhsTextSecondary else XhsTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = XhsTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                color = XhsRed,
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(onClick = onLocate) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "定位当前位置",
                    tint = XhsRed,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun XhsPublishLocationField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isLoading: Boolean,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = XhsTextPrimary),
            cursorBrush = SolidColor(XhsRed),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, color = XhsTextSecondary, fontSize = 16.sp)
                    }
                    inner()
                }
            },
        )
        if (isLoading) {
            CircularProgressIndicator(
                color = XhsRed,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(onClick = onLocate) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "高德定位",
                    tint = XhsRed,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun XhsPublishScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(XhsBackground),
    ) {
        content()
    }
}
