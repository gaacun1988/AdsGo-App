package com.adsweb.proxismart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// CHANGE: Remove the wildcard import and use specific ones
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
// Ensure this specific Marker is imported if not using wildcard
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSetupWindow(onComplete: (LocalProfile) -> Unit) {
    var storeName by remember { mutableStateOf("") }
    var locationPin by remember { mutableStateOf(LatLng(-34.6037, -58.3816)) }
    var selectedCat by remember { mutableStateOf("Restaurante") }
    var expanded by remember { mutableStateOf(false) }

    val cats = listOf("Restaurante", "Verduleria", "Ferretería", "Ropa", "Farmacia")

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Configura tu Negocio", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Nombre del Local") }, modifier = Modifier.fillMaxWidth())

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedCat, onValueChange = {}, readOnly = true, label = { Text("Rubro") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cats.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { selectedCat = c; expanded = false }) }
            }
        }

        Box(Modifier.height(300.dp).padding(top = 16.dp)) {
            GoogleMap(
                cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(locationPin, 15f) },
                onMapClick = { locationPin = it }
            ) { Marker(state=MarkerState(locationPin)) }
        }

        Button(
            onClick = {
                // LLAMADA CORREGIDA
                onComplete(LocalProfile(
                    role = "TIENDA",
                    name = storeName,
                    category = selectedCat,
                    lat = locationPin.latitude,
                    lng = locationPin.longitude
                ))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
        ) {
            Text("GUARDAR MI COMERCIO")
        }
    }
}