package com.lfc.consumer.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary

private enum class OwnerContentConfirmAction {
    DELETE,
    OFF_SHELF,
}

@Composable
fun OwnerContentManageButton(
    showShelfActions: Boolean,
    isOffShelf: Boolean,
    contentLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOffShelf: () -> Unit,
    onOnShelf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<OwnerContentConfirmAction?>(null) }

    IconButton(onClick = { showMenu = true }, modifier = modifier.size(40.dp)) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "管理$contentLabel",
            tint = XhsTextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
            text = { Text("编辑") },
            onClick = {
                showMenu = false
                onEdit()
            },
        )
        if (showShelfActions) {
            if (isOffShelf) {
                DropdownMenuItem(
                    text = { Text("上架") },
                    onClick = {
                        showMenu = false
                        onOnShelf()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("下架") },
                    onClick = {
                        showMenu = false
                        pendingConfirm = OwnerContentConfirmAction.OFF_SHELF
                    },
                )
            }
        }
        DropdownMenuItem(
            text = { Text("删除") },
            onClick = {
                showMenu = false
                pendingConfirm = OwnerContentConfirmAction.DELETE
            },
        )
    }

    pendingConfirm?.let { action ->
        val (title, message, confirmLabel) = when (action) {
            OwnerContentConfirmAction.DELETE -> Triple(
                "确认删除",
                "删除后不可恢复，确定要删除这条$contentLabel 吗？",
                "删除",
            )
            OwnerContentConfirmAction.OFF_SHELF -> Triple(
                "确认下架",
                "下架后其他用户将无法在公域看到这条$contentLabel，你可以随时重新上架。",
                "下架",
            )
        }
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConfirm = null
                        when (action) {
                            OwnerContentConfirmAction.DELETE -> onDelete()
                            OwnerContentConfirmAction.OFF_SHELF -> onOffShelf()
                        }
                    },
                ) {
                    Text(confirmLabel, color = XhsRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) {
                    Text("取消")
                }
            },
        )
    }
}
