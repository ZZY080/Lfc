package com.lfc.consumer.ui.legal

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.local.LegalConsentStore
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun LegalConsentScreen(
    onAgreed: () -> Unit,
    onOpenDocument: (LegalDocumentId) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = "欢迎使用莲峰校园",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = XhsTextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "为向您提供笔记分享、活动组织、闲置交易等服务，我们需要收集并使用必要的信息。请阅读并同意以下协议：",
                fontSize = 14.sp,
                color = XhsTextSecondary,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF7F7F7),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LegalConsentLinkRow(
                        title = "《用户服务协议》",
                        onClick = { onOpenDocument(LegalDocumentId.USER_AGREEMENT) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalConsentLinkRow(
                        title = "《隐私政策》",
                        onClick = { onOpenDocument(LegalDocumentId.PRIVACY_POLICY) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalConsentLinkRow(
                        title = "《社区规范》",
                        onClick = { onOpenDocument(LegalDocumentId.COMMUNITY_GUIDELINES) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalConsentLinkRow(
                        title = "《C2C 退货售后规则》",
                        onClick = { onOpenDocument(LegalDocumentId.C2C_AFTER_SALES) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalConsentLinkRow(
                        title = "《支付与分账说明》",
                        onClick = { onOpenDocument(LegalDocumentId.PAYMENT_TERMS) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalConsentLinkRow(
                        title = "《第三方 SDK 说明》",
                        onClick = { onOpenDocument(LegalDocumentId.THIRD_PARTY_SDK) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "我们将严格保护您的个人信息安全。您可在「设置 - 法律与隐私」中随时查阅上述文件。",
                fontSize = 12.sp,
                color = XhsTextSecondary,
                lineHeight = 18.sp,
            )
            Text(
                text = "协议版本 v${LegalConsentStore.CURRENT_VERSION}.0",
                fontSize = 11.sp,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = onAgreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("同意并继续", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("不同意并退出", color = XhsTextSecondary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LegalConsentLinkRow(
    title: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(
            text = title,
            color = XhsRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
