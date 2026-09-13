package com.handpicked.gpscamera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        setContent { GPSCameraScreen() }
    }
}

private data class GpsInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val address: String
)

@Composable
fun GPSCameraScreen() {
    val context = LocalContext.current
    var gpsInfo by remember { mutableStateOf<GpsInfo?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val address = try {
                        if (Geocoder.isPresent()) {
                            @Suppress("DEPRECATION")
                            Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                                ?.firstOrNull()?.getAddressLine(0) ?: "Address unavailable"
                        } else "Address unavailable"
                    } catch (_: Exception) { "Address unavailable" }
                    gpsInfo = GpsInfo(loc.latitude, loc.longitude, loc.accuracy, address)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { view ->
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                            val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                            imageCapture = capture
                            provider.unbindAll()
                            provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                Modifier.fillMaxSize()
            )

            Column(
                Modifier.align(Alignment.BottomStart)
                    .padding(14.dp)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = .70f))
                    .padding(12.dp)
            ) {
                Text("HANDPICKED GPS CAMERA", color = Color.White, style = MaterialTheme.typography.titleMedium)
                gpsInfo?.let {
                    Text("📍 ${it.address}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("Lat: %.6f   Lon: %.6f   ±%.0fm".format(it.latitude, it.longitude, it.accuracy), color = Color.White, style = MaterialTheme.typography.bodySmall)
                } ?: Text("GPS location acquiring…", color = Color.White)
                Text(SimpleDateFormat("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()).format(Date()), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            Button(
                enabled = !busy,
                onClick = {
                    val capture = imageCapture ?: return@Button
                    busy = true
                    val file = File.createTempFile("hp_gps_", ".jpg", context.cacheDir)
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) {
                            busy = false
                            Toast.makeText(context, "Photo failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val info = gpsInfo
                            if (info == null) {
                                busy = false
                                Toast.makeText(context, "GPS fix not available yet", Toast.LENGTH_SHORT).show()
                                file.delete()
                                return
                            }
                            Thread {
                                val stamped = stampBitmap(BitmapFactory.decodeFile(file.absolutePath), info)
                                val uri = saveToGallery(context, stamped)
                                file.delete()
                                (context as ComponentActivity).runOnUiThread {
                                    busy = false
                                    Toast.makeText(context, if (uri != null) "Saved to Handpicked GPS Camera" else "Could not save photo", Toast.LENGTH_SHORT).show()
                                }
                            }.start()
                        }
                    })
                },
                modifier = Modifier.size(82.dp),
                shape = CircleShape
            ) { Text(if (busy) "…" else "●", style = MaterialTheme.typography.headlineMedium) }
        }
    }
}

private fun stampBitmap(source: Bitmap, info: GpsInfo): Bitmap {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    val scale = (bitmap.width / 1080f).coerceAtLeast(.75f)
    val padding = (28 * scale).toInt()
    val textSize = (30 * scale).coerceAtLeast(22f)
    val lineHeight = (textSize * 1.45f).toInt()
    val lines = listOf(
        "HANDPICKED GPS CAMERA",
        info.address,
        "Lat %.6f  |  Lon %.6f".format(info.latitude, info.longitude),
        "Accuracy ±%.0fm  |  %s".format(info.accuracy, SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date()))
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val boxTop = bitmap.height - padding - lineHeight * lines.size - padding
    val boxBottom = bitmap.height - padding
    val boxPaint = Paint().apply { color = android.graphics.Color.argb(185, 0, 0, 0) }
    canvas.drawRect(0f, boxTop.toFloat(), bitmap.width.toFloat(), boxBottom.toFloat(), boxPaint)
    lines.forEachIndexed { index, line ->
        val y = boxTop + padding + lineHeight * (index + 1) - (lineHeight - textSize).toInt() / 2
        canvas.drawText(line.take(110), padding.toFloat(), y.toFloat(), paint)
    }
    return bitmap
}

private fun saveToGallery(context: android.content.Context, bitmap: Bitmap): Uri? {
    val filename = "Handpicked_GPS_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Handpicked GPS Camera")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}
