package com.adsweb.proxismart

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    var subScreen by remember { mutableStateOf("home") }
    var selectedCat by remember { mutableStateOf("") }
    var selectedAdForDetail by remember { mutableStateOf<Offer?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // --- MAPA DE LAS 20 CATEGORÍAS SEGÚN TU PDF ---
    val categoryMap = mapOf(
        "Verduleria" to listOf("Orgánico", "Frutas", "Vegetales", "Frutos Secos"),
        "Restaurante" to listOf("Vegano", "Pastas", "Carnes", "Sushi", "Minutas", "Sabores Argentinos"),
        "Ferretería" to listOf("Herramientas", "Pinturas", "Electricidad", "Plomería"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños", "Accesorios"),
        "Electronica" to listOf("Celulares", "Computación", "Gaming", "Audio"),
        "Farmacia" to listOf("Medicamentos", "Cosmética", "Higiene"),
        "Kiosco" to listOf("Golosinas", "Bebidas", "Tabaco"),
        "Carnicería" to listOf("Vacuno", "Pollo/Granja", "Cerdo"),
        "Supermercado" to listOf("Almacén", "Limpieza", "Express"),
        "Mascotas" to listOf("Alimento", "Peluquería", "Veterinaria"),
        "Juguetería" to listOf("Didácticos", "Juegos Mesa", "Muñecos"),
        "Deportes" to listOf("Fútbol", "Running", "Padel"),
        "Joyeria" to listOf("Relojes", "Anillos", "Diseño"),
        "Florería" to listOf("Plantas", "Arreglos", "Eventos"),
        "Calzado" to listOf("Zapatillas", "Zapatos", "Botas"),
        "Cafetería" to listOf("Especialidad", "Pastelería", "Desayunos"),
        "Automotor" to listOf("Repuestos", "Taller", "Gomería"),
        "Gimnasio" to listOf("Musculación", "Yoga", "Crossfit"),
        "Peluquería" to listOf("Barbería", "Color", "Estética"),
        "Servicios" to listOf("Electricista", "Plomero", "Gas")
    )

    Box(Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreHomeView(
                db = db,
                onNew = { subScreen = "cats" },
                onDetail = { selectedAdForDetail = it; subScreen = "detail" },
                onPay = { subScreen = "payment" }
            )

            "cats" -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { subScreen = "home" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text("Seleccionar Rubro", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn {
                    items(categoryMap.keys.toList()) { cat ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            selectedCat = cat; subScreen = "subcats"
                        }) { ListItem(headlineContent = { Text(cat) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }) }
                    }
                }
            }

            "subcats" -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { subScreen = "cats" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text("Sub-rubro de $selectedCat", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn {
                    items(categoryMap[selectedCat] ?: emptyList()) { sub ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            scope.launch {
                                val ad = Offer(storeName = profile.name, title = "Oportunidad en $sub", category = selectedCat, subCategory = sub, latitude = profile.lat, longitude = profile.lng, whatsapp = profile.email)
                                db.offerDao().insertOffer(ad)
                                subScreen = "home"
                                Toast.makeText(context, "Anuncio Lanzado!", Toast.LENGTH_SHORT).show()
                            }
                        }) { ListItem(headlineContent = { Text(sub) }) }
                    }
                }
            }

            "payment" -> Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Default.WorkspacePremium, null, Modifier.size(100.dp), tint = OrangeAds)
                Text("MEJORA A ADS PRO", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Desbloquea 300m y visibilidad permanente.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Button(onClick = { subScreen = "home" }, Modifier.padding(top = 40.dp).fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                    Text("ACTIVAR POR $9.99/MES")
                }
                TextButton(onClick = { subScreen = "home" }) { Text("Volver") }
            }

            "detail" -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                IconButton(onClick = { subScreen = "home" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text("RENDIMIENTO PRO", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
                Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
                    Column(Modifier.padding(24.dp)) {
                        Text(selectedAdForDetail?.title ?: "", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Vencimiento: ${selectedAdForDetail?.getExpirationDate()}", color = Color.White.copy(0.6f))
                        Spacer(Modifier.height(30.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            DetailMetricColumn("VISTAS", selectedAdForDetail?.views.toString())
                            DetailMetricColumn("CLICKS", selectedAdForDetail?.clicks.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreHomeView(db: AppDatabase, onNew: () -> Unit, onDetail: (Offer) -> Unit, onPay: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Campaign, null) }, label = { Text("Ads") })
                NavigationBarItem(selected = false, onClick = onPay, icon = { Icon(Icons.Default.Verified, null, tint = OrangeAds) }, label = { Text("Pro") })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNew, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("PUBLICAR OFERTA AHORA", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Text("HISTORIAL DE CAMPAÑAS", fontWeight = FontWeight.Bold, color = Color.Gray)
            LazyColumn {
                items(myAds) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDetail(ad) }) {
                        ListItem(headlineContent = { Text(ad.title) }, trailingContent = { Icon(Icons.Default.BarChart, null, tint = OrangeAds) })
                    }
                }
            }
        }
    }
}

@Composable
fun DetailMetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 34.sp, color = OrangeAds, fontWeight = FontWeight.Black)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}