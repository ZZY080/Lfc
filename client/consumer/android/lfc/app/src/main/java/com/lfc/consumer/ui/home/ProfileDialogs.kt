package com.lfc.consumer.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.UserProfileDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ProfileQrDialog(
    profile: UserProfileDto,
    onDismiss: () -> Unit,
) {
    val link = remember(profile.lfcNo) { ProfileShareHelper.profileLink(profile.lfcNo) }
    val qrBitmap = remember(link) { ProfileShareHelper.generateQrBitmap(link, 480) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("我的二维码") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "主页二维码",
                    modifier = Modifier.size(220.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(profile.displayName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("莲峰号：${profile.lfcNo}", color = XhsTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(link, color = XhsTextSecondary, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = XhsRed)
            }
        },
    )
}
