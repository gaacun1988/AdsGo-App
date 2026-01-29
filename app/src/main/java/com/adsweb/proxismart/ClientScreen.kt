package com.adsweb.proxismart

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

@OptIn(ExperimentalMaterial3Api::class) // Necesario para FilterChip
@SuppressLint("MissingPermission")
@Composable
fun ClientScreen(onBack: () -> Unit, onOpenAR: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbLocal = remember { AppDatabase.getDatabase(context) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- ESTADOS GLOBALES ---
    var storesNearBy by remember { mutableStateOf<List<RemoteStore>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<RemoteStore?>(null) }
    var userLocation by remember { mutableStateOf(LatLng(-34.6037, -58.3816)) }
    var selectedCategoryId by remember { mutableStateOf(0) } // 0 = Todas
    var tab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val categories = listOf(
        "Todos" to 0, "Gastronomía" to 1, "Moda" to 2, "Salud" to 3, "Belleza" to 4,
        "Almacén" to 5, "Deportes" to 6, "Tecnología" to 12, "Mascotas" to 11
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    // --- LÓGICA DE RADAR (GPS SENSOR + FILTRO) ---
    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    userLocation = newLatLng

                    // Sincronizar radar con categoría actual
                    scope.launch {
                        AdsGoNetwork.fetchNearbyStores(newLatLng.latitude, newLatLng.longitude, selectedCategoryId)
                            .onSuccess { storesNearBy = it }
                    }
                }
                isLoading = false
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.TrackChanges, null, tint = Color.White) },
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
                // EL MAPA VA PRIMERO (Fondo)
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
                ) {
                    Circle(
                        center = userLocation,
                        radius = 200.0,
                        fillColor = OrangeAds.copy(alpha = 0.08f),
                        strokeColor = OrangeAds,
                        strokeWidth = 2f
                    )

                    Marker(state = MarkerState(position = userLocation), title = "Estás aquí")

                    storesNearBy.forEach { store ->
                        val storeLatLng = parseGeoJson(store.ubicacion)
                        Marker(
                            state = MarkerState(position = storeLatLng),
                            title = store.nombre,
                            icon = if (store.id_plan > 1)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            else
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = { selectedStore = store; true }
                        )
                    }
                }

                // LAS CATEGORÍAS VAN DESPUÉS (Encima del mapa)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (nombre, id) ->
                        FilterChip(
                            selected = selectedCategoryId == id,
                            onClick = {
                                selectedCategoryId = id
                                scope.launch {
                                    AdsGoNetwork.fetchNearbyStores(userLocation.latitude, userLocation.longitude, id)
                                        .onSuccess { storesNearBy = it }
                                }
                            },
                            label = { Text(nombre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeAds,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.9f)
                            ),
                            elevation = FilterChipDefaults.filterChipElevation(6.dp)
                        )
                    }
                }
            } else {
                // MODO LISTA
                LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(16.dp)) {
                    item { Text("OFERTAS EN TU RADIO", fontWeight = FontWeight.Black, fontSize = 22.sp, color = DeepBlueAds) }
                    items(storesNearBy) { store ->
                        ListItem(
                            headlineContent = { Text(store.nombre, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(store.categoria) },
                            trailingContent = { if(store.id_plan > 1) Icon(Icons.Default.WorkspacePremium, null, tint = OrangeAds) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // CARGANDO...
            if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = OrangeAds)

            // TARJETA DE TIENDA (Encima de todo al seleccionar)
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
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val adsId = "ADS-${store.id.toString().padStart(4, '0')}"
                                val msg = "Hola ${store.nombre}, vi tu local en ADSGO ($adsId) y quería hacer un pedido."
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${store.whatsapp}?text=${Uri.encode(msg)}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
                        ) {
                            Icon(Icons.Default.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("CHATEAR POR WHATSAPP")
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
    } catch (e: Exception) { LatLng(-34.6037, -58.3816) }
}