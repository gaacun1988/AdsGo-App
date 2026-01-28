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
import com.adsweb.proxismart.ui.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbLocal = remember { AppDatabase.getDatabase(context) }

    // ESTADOS DEL RADAR
    var storesNearBy by remember { mutableStateOf<List<RemoteStore>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<RemoteStore?>(null) }
    var currentClient by remember { mutableStateOf<LocalProfile?>(null) }
    var userLocation by remember { mutableStateOf(LatLng(-34.6037, -58.3816)) } // Fallback CABA
    var tab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 16f)
    }

    LaunchedEffect(Unit) {
        currentClient = dbLocal.localProfileDao().getActiveProfile()
        val lat = currentClient?.lat ?: -34.6037
        val lng = currentClient?.lng ?: -58.3816
        userLocation = LatLng(lat, lng)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation, 16f)

        AdsGoNetwork.fetchNearbyStores(lat, lng).onSuccess { stores ->
            storesNearBy = stores
        }
        isLoading = false
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Radar, null, tint = Color.White) },
                    label = { Text("Radar", color = Color.White) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Storefront, null, tint = Color.White) },
                    label = { Text("Cerca", color = Color.White) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    Circle(
                        center = userLocation,
                        radius = 200.0,
                        fillColor = OrangeAds.copy(alpha = 0.1f),
                        strokeColor = OrangeAds,
                        strokeWidth = 2f
                    )

                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Tu Ubicación",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    storesNearBy.forEach { store ->
                        val storeLatLng = parseGeoJson(store.ubicacion)
                        Marker(
                            state = MarkerState(position = storeLatLng),
                            title = store.nombre,
                            snippet = store.categoria,
                            icon = if (store.id_plan > 1)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            else
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = {
                                selectedStore = store
                                true
                            }
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
                    item { Text("LOCALES EN TU RADIO", fontWeight = FontWeight.Black, fontSize = 20.sp, color = DeepBlueAds) }
                    items(storesNearBy) { store ->
                        ListItem(
                            headlineContent = { Text(store.nombre, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(store.categoria) },
                            trailingContent = {
                                if(store.id_plan > 1) Icon(Icons.Default.WorkspacePremium, null, tint = OrangeAds)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = OrangeAds)
            }

            selectedStore?.let { store ->
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(45.dp).clip(CircleShape).background(OrangeAds), contentAlignment = Alignment.Center) {
                                Text(store.nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(store.nombre, fontWeight = FontWeight.Black, fontSize = 18.sp, color = DeepBlueAds)
                                Text(store.categoria, fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Esta tienda tiene una oportunidad cerca tuyo.", fontSize = 14.sp)

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val adsId = "ADS-${store.id.toString().padStart(4, '0')}"
                                    val msg = "Hola ${store.nombre}, vi tu local en el radar ADSGO ($adsId) y quería hacer un pedido."
                                    val tiendaPhone = store.whatsapp.ifEmpty { "5491100000000" } // Fallback
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$tiendaPhone?text=${Uri.encode(msg)}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
                            ) {
                                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CONSULTAR")
                            }
                        }
                        TextButton(onClick = { selectedStore = null }, modifier = Modifier.fillMaxWidth()) { Text("CERRAR") }
                    }
                }
            }
        }
    }
}

private fun parseGeoJson(geoJson: String): LatLng {
    return try {
        if (geoJson.contains("[")) {
            val coords = geoJson.substringAfter("[").substringBefore("]").split(",")
            LatLng(coords[1].toDouble(), coords[0].toDouble())
        } else {
            val parts = geoJson.replace("POINT(", "").replace(")", "").split(" ")
            LatLng(parts[1].toDouble(), parts[0].toDouble())
        }
    } catch (e: Exception) {
        LatLng(-34.6037, -58.3816)
    }
}