package com.lfc.consumer.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.ui.theme.XhsRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishActivityScreen(
    initial: ActivityDto? = null,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        location: String,
        startTime: String,
        endTime: String,
        maxParticipants: Int,
    ) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var startTime by remember { mutableStateOf(initial?.startTime?.take(16)?.replace(" ", "T") ?: "") }
    var endTime by remember { mutableStateOf(initial?.endTime?.take(16)?.replace(" ", "T") ?: "") }
    var maxParticipants by remember { mutableStateOf((initial?.maxParticipants ?: 0).toString()) }

    val isValid = title.isNotBlank() && description.isNotBlank() &&
        location.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        if (initial == null) "发布活动" else "编辑活动",
                        fontWeight = FontWeight.Bold,
                        color = XhsRed,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                "填写活动信息，提交后将由管理员审核",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("活动标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("活动描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("活动地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("开始时间") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2026-06-10T14:00:00") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text("结束时间") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2026-06-10T16:00:00") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = maxParticipants,
                onValueChange = { maxParticipants = it },
                label = { Text("人数上限（0 表示不限）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    onSubmit(
                        title.trim(),
                        description.trim(),
                        location.trim(),
                        startTime.trim(),
                        endTime.trim(),
                        maxParticipants.toIntOrNull() ?: 0,
                    )
                },
                enabled = isValid && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (initial == null) "提交审核" else "保存修改")
                }
            }
        }
    }
}
