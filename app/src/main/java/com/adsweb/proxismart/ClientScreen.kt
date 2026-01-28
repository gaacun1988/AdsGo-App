package com.adsweb.proxismart

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Looper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbLocal = remember { AppDatabase.getDatabase(context) }

    // --- SERVICIOS DE UBICACIÓN ---
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- ESTADOS ---
    var storesNearBy by remember { mutableStateOf<List<RemoteStore>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<RemoteStore?>(null) }
    var userLocation by remember { mutableStateOf(LatLng(-34.6037, -58.3816)) }
    var tab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 16f)
    }

    // --- 1. LÓGICA DE RADAR EN VIVO (GPS SENSOR) ---
    // Este bloque detecta si el usuario camina 10 metros y refresca todo
    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f) // Refrescar cada 10 metros
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    userLocation = newLatLng

                    // Solo en el primer fix, movemos la cámara
                    if (isLoading) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(newLatLng, 16f)
                    }

                    // BUSCAR EN NEON/HASURA CADA VEZ QUE SE MUEVE
                    scope.launch {
                        AdsGoNetwork.fetchNearbyStores(newLatLng.latitude, newLatLng.longitude)
                            .onSuccess { storesNearBy = it }
                    }
                }
                isLoading = false
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback) // Apagar GPS al salir
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.TrackChanges, null, tint = Color.White) }, // Icono de Radar estable
                    label = { Text("Radar", color = Color.White) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Storefront, null, tint = Color.White) },
                    label = { Text("Lista", color = Color.White) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) {
                // --- MODO MAPA DINÁMICO ---
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
                ) {
                    // Círculo del Radar: Visualización del radio de detección
                    Circle(
                        center = userLocation,
                        radius = 200.0,
                        fillColor = OrangeAds.copy(alpha = 0.08f),
                        strokeColor = OrangeAds,
                        strokeWidth = 2f
                    )

                    // Marcador del Cliente
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Estás aquí",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // Tiendas detectadas por ST_DWithin
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
                            onClick = { selectedStore = store; true }
                        )
                    }
                }
            } else {
                // --- MODO LISTA DE CERCANÍA ---
                LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(16.dp)) {
                    item { Text("OFERTAS EN TU RADIO", fontWeight = FontWeight.Black, fontSize = 22.sp, color = DeepBlueAds) }
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

            // --- FOLLETO DINÁMICO ---
            selectedStore?.let { store ->
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(20.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(OrangeAds), contentAlignment = Alignment.Center) {
                                Text(store.nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(store.nombre, fontWeight = FontWeight.Black, fontSize = 20.sp, color = DeepBlueAds)
                                Text(store.categoria, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("¡Oportunidad detectada! Pulsa el botón para pedir por WhatsApp.", fontSize = 14.sp)

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val adsId = "ADS-${store.id.toString().padStart(4, '0')}"
                                val msg = "Hola ${store.nombre}, vi tu local en ADSGO ($adsId) y quiero información."
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${store.whatsapp}?text=${Uri.encode(msg)}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
                        ) {
                            Icon(Icons.Default.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("CHATEAR CON EL LOCAL", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { selectedStore = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("VOLVER AL MAPA", color = Color.Gray)
                        }
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
        LatLng(-34.6037, -58.3816) // Argentina Fallback
    }
}