package com.adsweb.proxismart

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.OrangeAds
import com.adsweb.proxismart.ui.theme.DeepBlueAds

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlueAds), // Fondo corporativo azul profundo
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // BRANDING ADSGO
            Text(
                text = "ADSGO",
                color = OrangeAds,
                fontSize = 60.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp
            )
            Text(
                text = "PROXIMIDAD INTELIGENTE",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(50.dp))

            // ANIMACIÓN DE CARGA (Para que se vea "pesada" y pro)
            CircularProgressIndicator(
                color = OrangeAds,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = "Cargando motor de base de datos...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}