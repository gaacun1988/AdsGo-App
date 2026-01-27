package com.adsweb.proxismart

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    val dbCloud = FirebaseFirestore.getInstance()
    val dbLocal = remember { AppDatabase.getDatabase(context) }

    var offers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var selectedOffer by remember { mutableStateOf<Offer?>(null) }
    var currentClient by remember { mutableStateOf<LocalProfile?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val profiles = dbLocal.offerDao().getAllLocalProfiles()
        if (profiles.isNotEmpty()) currentClient = profiles.first()

        dbCloud.collection("ofertas").get().addOnSuccessListener { result ->
            offers = result.toObjects<Offer>()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.LocationOn, null, tint = Color.White) }, label = { Text("Radar") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.History, null, tint = Color.White) }, label = { Text("Historial") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 15f) }
                ) {
                    offers.forEach { offer ->
                        Marker(
                            state = MarkerState(position = LatLng(offer.latitude, offer.longitude)),
                            title = offer.storeName,
                            // AQUI SE USA EL isPremium YA DEFINIDO EN OFFER.KT
                            icon = if (offer.isPremium) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            else BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = { selectedOffer = offer; true }
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
                    item { Text("HISTORIAL DE OFERTAS", fontWeight = FontWeight.Black, fontSize = 20.sp) }
                    items(offers) { ad ->
                        ListItem(headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) }, supportingContent = { Text("De: ${ad.storeName}") })
                        HorizontalDivider()
                    }
                }
            }

            selectedOffer?.let { offer ->
                Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(20.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // CARGA DE LOGO SEGURA USANDO storeLogo DEFINIDO EN OFFER.KT
                            val logo: Any = if(offer.storeLogo.isNotEmpty()) offer.storeLogo else "https://via.placeholder.com/150"
                            AsyncImage(model = logo, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(16.dp))
                            Text(offer.storeName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = DeepBlueAds)
                        }
                        Text(offer.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("$${offer.price}", fontSize = 26.sp, color = OrangeAds, fontWeight = FontWeight.ExtraBold)

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenAR(offer.title) }, modifier = Modifier.weight(1f)) { Text("VER EN AR") }

                            Button(onClick = {
                                val id = (1000..9999).random()
                                val msg = "*PEDIDO ADSGO #$id*\nQuiero: ${offer.title}\nDe: ${currentClient?.name ?: "Cliente"}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${offer.whatsapp}?text=${Uri.encode(msg)}")))
                            },
                                modifier = Modifier.weight(1.3f), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
                            ) { Text("PEDIR POR WA") }
                        }
                        TextButton(onClick = { selectedOffer = null }, modifier = Modifier.fillMaxWidth()) { Text("CERRAR") }
                    }
                }
            }
        }
    }
}