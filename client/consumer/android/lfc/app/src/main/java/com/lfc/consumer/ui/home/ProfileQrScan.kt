package com.lfc.consumer.ui.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

private val scanFrameSize = 260.dp
private val scanOverlayColor = Color.Black.copy(alpha = 0.58f)

@Composable
fun ProfileQrScanScreen(
    onBack: () -> Unit,
    onProfileScanned: (ProfileScanTarget) -> Unit,
    onInvalidCode: (String) -> Unit,
    onShowMyQr: (() -> Unit)? = null,
) {
    ImmersiveScanSystemBars()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var scanHandled by remember { mutableStateOf(false) }
    var lastInvalidToastAt by remember { mutableLongStateOf(0L) }
    val barcodeViewState = remember { mutableStateOf<DecoratedBarcodeView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onInvalidCode("需要相机权限才能扫码")
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val scanner = barcodeViewState.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (hasCameraPermission && !scanHandled) scanner.resume()
                Lifecycle.Event.ON_PAUSE -> scanner.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            barcodeViewState.value?.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    DecoratedBarcodeView(ctx).apply {
                        barcodeView.decoderFactory = DefaultDecoderFactory(
                            listOf(BarcodeFormat.QR_CODE),
                        )
                        viewFinder.visibility = View.GONE
                        statusView.visibility = View.GONE
                        decodeContinuous(object : BarcodeCallback {
                            override fun barcodeResult(result: BarcodeResult?) {
                                if (scanHandled || result?.text.isNullOrBlank()) return
                                val target = ProfileShareHelper.parseProfileScanTarget(result.text)
                                if (target != null) {
                                    scanHandled = true
                                    pause()
                                    onProfileScanned(target)
                                } else {
                                    val now = System.currentTimeMillis()
                                    if (now - lastInvalidToastAt > 2000L) {
                                        lastInvalidToastAt = now
                                        onInvalidCode("无法识别该二维码，请扫描莲峰校园用户主页码")
                                    }
                                }
                            }
                        })
                        barcodeViewState.value = this
                        resume()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "正在请求相机权限…",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                )
            }
        }

        ProfileQrScanCenterArea(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        ProfileQrScanTopBar(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.65f),
                        ),
                    ),
                ),
        )

        if (onShowMyQr != null) {
            Text(
                text = "我的二维码",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
                    .clickable(onClick = onShowMyQr)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ImmersiveScanSystemBars() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    DisposableEffect(view, darkTheme) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        onDispose {
            controller.show(WindowInsetsCompat.Type.navigationBars())
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
private fun ProfileQrScanCenterArea(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val framePx = with(density) { scanFrameSize.toPx() }
        val frameLeft = (constraints.maxWidth - framePx) / 2f
        val frameTop = (constraints.maxHeight - framePx) / 2f

        ProfileQrScanDimOverlay(
            frameLeft = frameLeft,
            frameTop = frameTop,
            frameSize = framePx,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(scanFrameSize),
        ) {
            ProfileQrScanFrame()
        }

        Text(
            text = "对准用户主页二维码",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = scanFrameSize / 2 + 18.dp),
        )
    }
}

@Composable
private fun ProfileQrScanFrame() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "frameEnterScale",
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "frameEnterAlpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "scanFrame")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanLineProgress",
    )
    val cornerPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cornerPulse",
    )
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = enterAlpha
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cornerLen = 24.dp.toPx()
            val stroke = 4.dp.toPx()
            val cornerColor = Color.White.copy(alpha = cornerPulse)

            drawCornerBracket(0f, 0f, cornerLen, stroke, cornerColor, 1f, 1f)
            drawCornerBracket(width, 0f, cornerLen, stroke, cornerColor, -1f, 1f)
            drawCornerBracket(0f, height, cornerLen, stroke, cornerColor, 1f, -1f)
            drawCornerBracket(width, height, cornerLen, stroke, cornerColor, -1f, -1f)

            drawRoundRect(
                color = Color.White.copy(alpha = borderPulse),
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx()),
            )

            val padding = 14.dp.toPx()
            val scanTop = padding
            val scanBottom = height - padding
            val lineY = scanTop + (scanBottom - scanTop) * scanLineProgress
            val lineStart = padding + 4.dp.toPx()
            val lineEnd = width - padding - 4.dp.toPx()

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.95f),
                        Color.Transparent,
                    ),
                    startY = lineY - 10.dp.toPx(),
                    endY = lineY + 10.dp.toPx(),
                ),
                start = Offset(lineStart, lineY),
                end = Offset(lineEnd, lineY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF5EEAD4).copy(alpha = 0.35f),
                start = Offset(lineStart, lineY),
                end = Offset(lineEnd, lineY),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ProfileQrScanDimOverlay(
    frameLeft: Float,
    frameTop: Float,
    frameSize: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val right = frameLeft + frameSize
        val bottom = frameTop + frameSize

        drawRect(scanOverlayColor, size = Size(size.width, frameTop))
        drawRect(
            scanOverlayColor,
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom),
        )
        drawRect(
            scanOverlayColor,
            topLeft = Offset(0f, frameTop),
            size = Size(frameLeft, frameSize),
        )
        drawRect(
            scanOverlayColor,
            topLeft = Offset(right, frameTop),
            size = Size(size.width - right, frameSize),
        )
    }
}

@Composable
private fun ProfileQrScanTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "扫一扫",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBracket(
    x: Float,
    y: Float,
    length: Float,
    stroke: Float,
    color: Color,
    horizontalDir: Float,
    verticalDir: Float,
) {
    drawLine(
        color = color,
        start = Offset(x, y),
        end = Offset(x + length * horizontalDir, y),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(x, y),
        end = Offset(x, y + length * verticalDir),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}
