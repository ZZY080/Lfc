package com.lfc.consumer.ui.legal

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun LegalAgreementCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenDocument: (LegalDocumentId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = XhsRed),
        )
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = XhsTextSecondary, fontSize = 13.sp)) {
                append("我已阅读并同意 ")
            }
            pushStringAnnotation(tag = "doc", annotation = LegalDocumentId.USER_AGREEMENT.routeKey)
            withStyle(SpanStyle(color = XhsRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)) {
                append("《用户服务协议》")
            }
            pop()
            withStyle(SpanStyle(color = XhsTextSecondary, fontSize = 13.sp)) {
                append("、")
            }
            pushStringAnnotation(tag = "doc", annotation = LegalDocumentId.PRIVACY_POLICY.routeKey)
            withStyle(SpanStyle(color = XhsRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)) {
                append("《隐私政策》")
            }
            pop()
            withStyle(SpanStyle(color = XhsTextSecondary, fontSize = 13.sp)) {
                append(" 及 ")
            }
            pushStringAnnotation(tag = "doc", annotation = LegalDocumentId.COMMUNITY_GUIDELINES.routeKey)
            withStyle(SpanStyle(color = XhsRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)) {
                append("《社区规范》")
            }
            pop()
        }
        ClickableText(
            text = annotated,
            modifier = Modifier.weight(1f),
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "doc", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { annotation ->
                        LegalDocumentId.fromRouteKey(annotation.item)?.let(onOpenDocument)
                    }
            },
        )
    }
}
