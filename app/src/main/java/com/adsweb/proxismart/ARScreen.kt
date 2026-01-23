package com.adsweb.proxismart

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.adsweb.proxismart.ui.theme.OrangeAds
import com.adsweb.proxismart.ui.theme.DeepBlueAds
import java.util.concurrent.Executor

@Composable
fun ARScreen(offerTitle: String, onOrderSent: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. FONDO DE CÁMARA REAL
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor: Executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    } catch (e: Exception) {
                        Log.e("ADSGO_AR", "Fallo al iniciar cámara: ${e.message}")
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. FOLLETO INTERACTIVO ADSGO
        Card(
            modifier = Modifier
                .size(320.dp, 480.dp)
                .align(Alignment.Center)
                .clickable { onOrderSent() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(OrangeAds)) {
                    Text("ADSGO EXCLUSIVE", modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Black)
                }
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("OFERTA DETECTADA", color = DeepBlueAds, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(offerTitle, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Spacer(Modifier.height(12.dp))
                    Text("Toca el folleto para enviar tu pedido con ID único al comercio.", fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                Surface(modifier = Modifier.fillMaxWidth().height(60.dp), color = DeepBlueAds) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("COMPRAR AHORA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Botón salir
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) { Text("CERRAR CÁMARA") }
    }
}