package com.adsweb.proxismart

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adsweb.proxismart.ui.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    var selectedOffer by remember { mutableStateOf<Offer?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 15f) }
        ) {
            // Aquí se dibujan los marcadores (simulados por ahora)
        }

        if (selectedOffer != null) {
            OfferFlyer(selectedOffer!!) { selectedOffer = null }
        }
    }
}

@Composable
fun OfferFlyer(offer: Offer, onClose: () -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = offer.storeLogo, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape))
                Spacer(Modifier.width(16.dp))
                Text(offer.storeName, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(offer.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("$${offer.price}", color = OrangeAds, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            var note by remember { mutableStateOf("") }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Comentario (ej: Sin cebolla)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val id = (1000..9999).random()
                    val msg = "NUEVO PEDIDO ADSGO #$id\nProducto: ${offer.title}\nNota: $note"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${offer.whatsapp}?text=${Uri.encode(msg)}")))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
            ) { Text("PEDIR POR WHATSAPP") }

            TextButton(onClick = onClose, Modifier.fillMaxWidth()) { Text("CERRAR") }
        }
    }
}