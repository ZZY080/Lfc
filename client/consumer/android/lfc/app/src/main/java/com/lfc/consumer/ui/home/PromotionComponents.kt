package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.PromotionMetaDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val promotionBadgeBg = Color(0xFFFFF4E8)
private val promotionBadgeBorder = Color(0xFFFFD6A8)
private val promotionBadgeText = Color(0xFFE07A00)

@Composable
fun XhsPromotionBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .background(promotionBadgeBg, RoundedCornerShape(6.dp))
            .border(0.5.dp, promotionBadgeBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = promotionBadgeText,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun PromotionOwnerActionCard(
    promotion: PromotionMetaDto?,
    actionLabel: String,
    activeHint: String,
    cooldownHint: String?,
    priceHint: String? = null,
    bidHint: String? = null,
    isSubmitting: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (promotion == null) return

    val isActive = promotion.isActive
    val canApply = promotion.canApply && !isSubmitting
    val buttonLabel = when {
        !priceHint.isNullOrBlank() && canApply -> "$actionLabel $priceHint"
        else -> actionLabel
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFFEFEFEF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "曝光提升",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = XhsTextPrimary,
                )
                if (isActive && !promotion.label.isNullOrBlank()) {
                    XhsPromotionBadge(label = promotion.label)
                }
            }
            Text(
                text = when {
                    isActive -> activeHint
                    !bidHint.isNullOrBlank() -> bidHint
                    !cooldownHint.isNullOrBlank() -> cooldownHint
                    !priceHint.isNullOrBlank() -> "支付后优先展示，推广内容将明确标注"
                    else -> "提升展示优先级，不影响其他用户正常使用"
                },
                fontSize = 12.sp,
                color = XhsTextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (canApply) {
            Button(
                onClick = onAction,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else if (isActive) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("进行中", fontSize = 13.sp)
            }
        }
    }
}
