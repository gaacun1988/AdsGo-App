package com.adsweb.proxismart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.adsweb.proxismart.ui.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSetupWindow(onComplete: (LocalProfile) -> Unit) {
    var storeName by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Restaurante") }
    var locationPin by remember { mutableStateOf(LatLng(-34.6037, -58.3816)) }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf("Verduleria", "Restaurante", "Kiosco", "Ferreteria", "Panaderia", "Farmacia", "Ropa", "Calzado", "Electronica", "Supermercado", "Jugueteria", "Mascotas", "Gimnasio", "Peluqueria", "Libreria", "Joyeria", "Floreria", "Deportes", "Cafeteria", "Carniceria")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Perfil de Comercio", style = MaterialTheme.typography.headlineMedium, color = DeepBlueAds)

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Nombre de la Tienda") }, modifier = Modifier.fillMaxWidth())

        // Selector Desplegable de 20 Categorías
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.padding(top = 16.dp)) {
            OutlinedTextField(value = selectedCat, onValueChange = {}, readOnly = true, label = { Text("Rubro") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCat = cat; expanded = false })
                }
            }
        }

        Text("Fija tu ubicación exacta:", modifier = Modifier.padding(top = 20.dp))

        // Mapa con estado persistente (No saldrá en blanco)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(locationPin, 15f)
        }

        Box(modifier = Modifier.height(250.dp).fillMaxWidth().padding(top = 8.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { locationPin = it }
            ) {
                Marker(state = MarkerState(position = locationPin), title = "Mi Tienda")
            }
        }

        Button(
            onClick = { onComplete(LocalProfile(rol = "TIENDA", nombre = storeName, email = "", categoria = selectedCat, lat = locationPin.latitude, lng = locationPin.longitude)) },
            modifier = Modifier.fillMaxWidth().padding(top = 30.dp).height(56.dp)
        ) {
            Text("GUARDAR MI COMERCIO")
        }
    }
}