package com.adsweb.proxismart

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*

@Composable
fun ClientSetupWindow(onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val selectedPrefs = remember { mutableStateListOf<String>() }

    // Las categorías del PDF pág 12
    val categoriasDisponibles = listOf("Restaurantes", "Verdulerías", "Kioscos", "Ferreterías", "Farmacias", "Moda", "Electrónica", "Mascotas", "Deportes", "Cafetería")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Personaliza tu ADSGO", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Text("¿Qué te gustaría encontrar cerca de ti?", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Principal") }, modifier = Modifier.fillMaxWidth())

        Text("Tus Preferencias:", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp), fontWeight = FontWeight.Bold)

        // Selección de burbujas amigables
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(250.dp)) {
            items(categoriasDisponibles) { cat ->
                val isSelected = selectedPrefs.contains(cat)
                FilterChip(
                    selected = isSelected,
                    onClick = { if (isSelected) selectedPrefs.remove(cat) else selectedPrefs.add(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeAds)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                onComplete(LocalProfile(rol = "CLIENTE", nombre = name, email = email, gustos = selectedPrefs.joinToString(", ")))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GUARDAR Y BUSCAR OFERTAS", fontWeight = FontWeight.Bold)
        }
    }
}