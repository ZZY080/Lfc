package com.lfc.consumer.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun LegalDocumentScreen(
    documentId: LegalDocumentId,
    onBack: () -> Unit,
) {
    val document = legalDocumentOf(documentId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = document.title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = XhsTextPrimary,
            )
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "更新日期：${document.updatedAt}",
                fontSize = 12.sp,
                color = XhsTextSecondary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            document.sections.forEach { section ->
                Text(
                    text = section.heading,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = XhsTextPrimary,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                section.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        fontSize = 14.sp,
                        color = XhsTextPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
