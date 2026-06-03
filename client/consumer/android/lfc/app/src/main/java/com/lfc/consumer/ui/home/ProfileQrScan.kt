package com.lfc.consumer.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun rememberProfileQrScanner(
    onProfileScanned: (ProfileScanTarget) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@rememberLauncherForActivityResult
        val target = ProfileShareHelper.parseProfileScanTarget(content)
        if (target != null) {
            onProfileScanned(target)
        } else {
            onError("无法识别该二维码，请扫描莲峰校园用户主页码")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scanLauncher.launch(buildProfileScanOptions())
        } else {
            onError("需要相机权限才能扫码")
        }
    }

    return remember(scanLauncher, permissionLauncher) {
        {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                    scanLauncher.launch(buildProfileScanOptions())
                }
                else -> permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun buildProfileScanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt("对准用户主页二维码")
    setBeepEnabled(false)
    setBarcodeImageEnabled(false)
    setOrientationLocked(false)
}
