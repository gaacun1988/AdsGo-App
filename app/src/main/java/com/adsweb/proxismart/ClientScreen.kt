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
import kotlinx.coroutines.launch

@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbLocal = remember { AppDatabase.getDatabase(context) }

    // ESTADOS DEL RADAR
    var storesNearBy by remember { mutableStateOf<List<RemoteStore>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<RemoteStore?>(null) }
    var currentClient by remember { mutableStateOf<LocalProfile?>(null) }
    var userLocation by remember { mutableStateOf(LatLng(-12.0463, -77.0427)) } // Default
    var tab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 16f)
    }

    // --- CEREBRO DEL RADAR: Captura ubicación y busca en PostGIS ---
    LaunchedEffect(Unit) {
        // 1. Obtener perfil del cliente (v8 engine)
        currentClient = dbLocal.localProfileDao().getActiveProfile()

        // 2. Simular/Obtener ubicación actual para el radar
        // En una implementación final, aquí usaríamos FusedLocationProvider
        val lat = currentClient?.lat ?: -12.0463
        val lng = currentClient?.lng ?: -77.0427
        userLocation = LatLng(lat, lng)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation, 16f)

        // 3. Consulta a Neon.tech vía Ktor
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
                    icon = { Icon(Icons.Default.History, null, tint = Color.White) },
                    label = { Text("Cerca", color = Color.White) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) {
                // --- MAPA DEL RADAR ---
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    // Círculo visual del Radar (Alcance Básico 100m)
                    Circle(
                        center = userLocation,
                        radius = 150.0,
                        fillColor = OrangeAds.copy(alpha = 0.1f),
                        strokeColor = OrangeAds,
                        strokeWidth = 2f
                    )

                    // Marcador del Usuario
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Tu Ubicación",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // Tiendas detectadas por ST_DWithin
                    storesNearBy.forEach { store ->
                        // Extraer LatLng del formato GeoJSON que envía PostGIS
                        val storeLatLng = parseGeoJson(store.ubicacion)

                        Marker(
                            state = MarkerState(position = storeLatLng),
                            title = store.nombre,
                            snippet = store.categoria,
                            icon = if (store.id_plan > 1)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE) // Premium
                            else
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE), // Básico
                            onClick = {
                                selectedStore = store
                                true
                            }
                        )
                    }
                }
            } else {
                // LISTA TIPO HISTORIAL
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

            // CARGANDO...
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = OrangeAds)
            }

            // DETALLE DEL LOCAL SELECCIONADO (FOLLETO)
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
                        Text("¡Hola! Esta tienda tiene una oferta para ti.", fontSize = 14.sp)

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    // Generación de ID ÚNICO ADS-XXXX
                                    val adsId = "ADS-${store.id.toString().padStart(4, '0')}"
                                    val msg = "Hola ${store.nombre}, vi tu local en el radar ADSGO ($adsId). Me gustaría más info."
                                    // Aquí el correo se usa como placeholder de teléfono o link WA
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/5491100000000?text=${Uri.encode(msg)}")))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
                            ) {
                                // Cambia Icons.Default.WhatsApp por Icons.Default.Send
                                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("PEDIR")
                            }
                        }
                        TextButton(onClick = { selectedStore = null }, modifier = Modifier.fillMaxWidth()) { Text("VOLVER AL MAPA") }
                    }
                }
            }
        }
    }
}

// Helper para convertir la respuesta de PostGIS a LatLng
private fun parseGeoJson(geoJson: String): LatLng {
    return try {
        // Formato simple esperado: "POINT(lng lat)" o JSON
        // Esto dependerá de cómo lo envíe tu Hasura (GeoJSON o Text)
        if (geoJson.contains("[")) {
            val coords = geoJson.substringAfter("[").substringBefore("]").split(",")
            LatLng(coords[1].toDouble(), coords[0].toDouble())
        } else {
            // Fallback si es formato POINT(lng lat)
            val parts = geoJson.replace("POINT(", "").replace(")", "").split(" ")
            LatLng(parts[1].toDouble(), parts[0].toDouble())
        }
    } catch (e: Exception) {
        LatLng(-34.6037, -58.3816)
    }
}