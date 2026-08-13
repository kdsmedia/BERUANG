package com.altomedia.beruang.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Full-screen camera that scans a QR code and returns the decoded value via
 * [onScanned]. Uses CameraX + ML Kit Barcode scanning.
 */
@Composable
fun QrScannerScreen(
    onScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Guard so the analyzer (which runs on a background executor) delivers the
    // result exactly once, and always on the main thread.
    val scanned = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val mainExecutor = remember(context) { androidx.core.content.ContextCompat.getMainExecutor(context) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val scanner = BarcodeScanning.getClient(options)
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { img -> analyze(img, scanner, onScanned, scanned, mainExecutor) }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (_: Exception) { /* no camera on this device/emulator */ }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                }
            }
            Box(
                Modifier.align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Arahkan kamera ke QR tujuan", color = Color(0xFF262626), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Izin kamera diperlukan untuk memindai QR.", color = Color.White, modifier = Modifier.padding(16.dp))
                androidx.compose.material3.TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berikan izin", color = Color.White)
                }
                androidx.compose.material3.TextButton(onClick = onBack) { Text("Kembali", color = Color.White) }
            }
        }
    }
}

private fun analyze(
    image: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onScanned: (String) -> Unit,
    guard: java.util.concurrent.atomic.AtomicBoolean,
    mainExecutor: java.util.concurrent.Executor
) {
    val mediaImage = image.image
    if (mediaImage == null) { image.close(); return }
    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
    val task: com.google.android.gms.tasks.Task<List<Barcode>> = scanner.process(inputImage)
    task.addOnSuccessListener { barcodes: List<Barcode> ->
            barcodes.firstOrNull()?.rawValue?.let { value ->
                // Deliver exactly once, on the main thread (the analyzer runs on a
                // background executor; mutating Compose state off-main crashes).
                if (guard.compareAndSet(false, true)) {
                    mainExecutor.execute { onScanned(value) }
                }
            }
        }
        .addOnCompleteListener { image.close() }
}
