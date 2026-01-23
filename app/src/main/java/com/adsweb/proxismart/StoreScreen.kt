package com.adsweb.proxismart

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    // ESTADO DE NAVEGACIÓN INTERNA (Ventanas tipo Rappi/PedidosYa)
    var currentSubScreen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // MAPA DE CATEGORÍAS PROFESIONALES (20 rubros comerciales)
    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Bodega", "Árabe", "Sushi"),
        "Verduleria" to listOf("Orgánico", "Frutas", "Verduras", "Frutos Secos"),
        "Ferretería" to listOf("Herramientas", "Pintura", "Electricidad", "Plomería", "Construcción"),
        "Kiosco" to listOf("Golosinas", "Bebidas", "Cigarrillos", "Regalos"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños", "Deportivo", "Accesorios"),
        "Farmacia" to listOf("Medicamentos", "Perfumería", "Cuidado Personal"),
        "Electronica" to listOf("Celulares", "Computación", "Audio", "Gaming"),
        "Gimnasio" to listOf("Crossfit", "Musculación", "Yoga", "Natación"),
        "Peluqueria" to listOf("Barbería", "Estética", "Unisex"),
        "Libreria" to listOf("Escolar", "Técnica", "Libros", "Arte"),
        "Supermercado" to listOf("Almacén", "Limpieza", "Bebidas", "Fiambrería"),
        "Mascotas" to listOf("Alimento", "Veterinaria", "Juguetes"),
        "Jugueteria" to listOf("Primera Infancia", "Juegos de Mesa", "Muñecos"),
        "Deportes" to listOf("Fútbol", "Tenis", "Running", "Ciclismo"),
        "Joyeria" to listOf("Relojes", "Anillos", "Reparaciones"),
        "Floreria" to listOf("Arreglos", "Plantas de interior", "Eventos"),
        "Calzado" to listOf("Urbano", "Zapatillas", "Zapatos"),
        "Cafeteria" to listOf("Especialidad", "Pastelería", "Desayunos"),
        "Carniceria" to listOf("Vacuno", "Pollo", "Cerdo", "Achuras"),
        "Servicios" to listOf("Plomería", "Gas", "Electricista", "Fletes")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentSubScreen) {
            "home" -> StoreHome(
                db = db,
                onNewAd = { currentSubScreen = "categories" },
                onViewDetail = { ad -> selectedAd = ad; currentSubScreen = "detail" },
                onGoPremium = { currentSubScreen = "payment" }
            )
            "categories" -> SelectionListWindow(
                title = "Elegir Rubro",
                items = categoryMap.keys.toList(),
                onBack = { currentSubScreen = "home" },
                onSelect = { selectedCategory = it; currentSubScreen = "subcategories" }
            )
            "subcategories" -> SelectionListWindow(
                title = "Sub-tipo de $selectedCategory",
                items = categoryMap[selectedCategory] ?: emptyList(),
                onBack = { currentSubScreen = "categories" },
                onSelect = { sub ->
                    // Simulación de publicación exitosa
                    scope.launch {
                        val newAd = Offer(
                            storeName = profile.name,
                            title = "Promo en $sub",
                            price = "Ver en local",
                            category = selectedCategory,
                            subCategory = sub,
                            latitude = profile.lat,
                            longitude = profile.lng,
                            isPremium = profile.isPremium
                        )
                        db.offerDao().insertOffer(newAd)
                        currentSubScreen = "home"
                        Toast.makeText(context, "¡Anuncio ADSGO lanzado!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            "payment" -> PremiumPaymentWindow(
                onBack = { currentSubScreen = "home" },
                onSuccess = { currentSubScreen = "home" }
            )
            "detail" -> AdDetailWindow(
                ad = selectedAd,
                onBack = { currentSubScreen = "home" }
            )
        }
    }
}

@Composable
fun StoreHome(db: AppDatabase, onNewAd: () -> Unit, onViewDetail: (Offer) -> Unit, onGoPremium: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("MIS CAMPAÑAS ADSGO", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Spacer(Modifier.height(16.dp))

        Button(onClick = onNewAd, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
            Icon(Icons.Default.Add, null)
            Text(" LANZAR NUEVA OFERTA")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(onClick = onGoPremium, Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Star, null, tint = OrangeAds)
            Text(" ACTIVAR LICENCIA PREMIUM")
        }

        Spacer(Modifier.height(24.dp))
        Text("Historial de Actividad", fontWeight = FontWeight.Bold, color = Color.Gray)

        LazyColumn(Modifier.weight(1f)) {
            items(myAds) { ad ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onViewDetail(ad) },
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Expira: ${ad.getExpirationDate()}") },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = OrangeAds) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionListWindow(title: String, items: List<String>, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        }
        LazyColumn(Modifier.padding(top = 16.dp)) {
            items(items) { item ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(item) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(0.5f))
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdDetailWindow(ad: Offer?, onBack: () -> Unit) {
    if (ad == null) return
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Estadísticas del Anuncio", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Card(Modifier.fillMaxWidth().padding(top = 24.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("ANUNCIO ID: #ADS-${ad.id}", color = Color.White.copy(0.6f), fontSize = 12.sp)
                Text(ad.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    DetailMetricItem("Vistas", ad.views.toString())
                    DetailMetricItem("Clicks", ad.clicks.toString())
                }
            }
        }

        Button(onClick = { }, Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp)) {
            Icon(Icons.Default.Edit, null)
            Text(" EDITAR CONTENIDO")
        }
    }
}

@Composable
fun DetailMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 32.sp, color = OrangeAds, fontWeight = FontWeight.Black)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun PremiumPaymentWindow(onBack: () -> Unit, onSuccess: (String) -> Unit) {
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (loading) {
            CircularProgressIndicator(color = OrangeAds)
            Text("Verificando transacción...", Modifier.padding(top = 16.dp))
        } else {
            Icon(Icons.Default.WorkspacePremium, null, modifier = Modifier.size(120.dp), tint = OrangeAds)
            Text("ADSGO PREMIUM", fontSize = 30.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
            Text("Desbloquea el radar de 300m y métricas PRO", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)

            Spacer(Modifier.height(40.dp))
            Button(onClick = {
                loading = true
                Toast.makeText(context, "ID Premium enviado a tu email", Toast.LENGTH_LONG).show()
                onSuccess("ADS-PREM-OK")
            }, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("PAGAR SUSCRIPCIÓN", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) { Text("Cancelar y volver") }
        }
    }
}