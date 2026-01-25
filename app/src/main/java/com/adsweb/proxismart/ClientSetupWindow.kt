package com.adsweb.proxismart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*

@Composable
fun ClientSetupWindow(onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val selectedInterests = remember { mutableStateListOf<String>() }

    val categoriasAdsGo = listOf("Restaurantes", "Verdulerías", "Moda", "Electrónica", "Ferretería", "Mascotas", "Deportes", "Cafetería")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Perfil de Cliente", fontSize = 26.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Principal") }, modifier = Modifier.fillMaxWidth())

        Text("¿Qué te interesa encontrar?", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(200.dp)) {
            items(categoriasAdsGo) { cat ->
                FilterChip(
                    selected = selectedInterests.contains(cat),
                    onClick = { if (selectedInterests.contains(cat)) selectedInterests.remove(cat) else selectedInterests.add(cat) },
                    label = { Text(cat) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                // LLAMADA CORREGIDA CON LOS NOMBRES EXACTOS DE Offer.kt
                onComplete(LocalProfile(
                    role = "CLIENTE",
                    name = name,
                    email = email,
                    interests = selectedInterests.joinToString(",")
                ))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GUARDAR Y BUSCAR OFERTAS", fontWeight = FontWeight.Bold)
        }
    }
}