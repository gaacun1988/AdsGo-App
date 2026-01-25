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
import androidx.compose.ui.platform.LocalContext // IMPORT CRÍTICO
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    // 1. OBTENER CONTEXTO CORRECTAMENTE
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // 2. ESTADOS DE NAVEGACIÓN (Ventanas Profundas)
    var subScreen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }

    // 3. MAPA DE 20 CATEGORÍAS (Según el flujo del PDF)
    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Bodega", "Árabe", "Sushi"),
        "Verduleria" to listOf("Orgánico", "Frutas", "Verduras", "Frutos Secos"),
        "Ferretería" to listOf("Herramientas", "Pintura", "Electricidad", "Plomería"),
        "Kiosco" to listOf("Golosinas", "Bebidas", "Cigarrillos"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños", "Deportivo"),
        "Farmacia" to listOf("Medicamentos", "Perfumería", "Cuidado Personal"),
        "Electronica" to listOf("Celulares", "Computación", "Audio", "Gaming"),
        "Gimnasio" to listOf("Crossfit", "Musculación", "Yoga", "Natación"),
        "Peluqueria" to listOf("Barbería", "Estética", "Unisex"),
        "Libreria" to listOf("Escolar", "Técnica", "Libros", "Arte"),
        "Supermercado" to listOf("Almacén", "Limpieza", "Bebidas", "Fiambrería"),
        "Mascotas" to listOf("Alimento", "Veterinaria", "Juguetes"),
        "Jugueteria" to listOf("Mesa", "Muñecos", "Primera Infancia"),
        "Deportes" to listOf("Fútbol", "Tenis", "Running"),
        "Joyeria" to listOf("Relojes", "Anillos", "Joyas"),
        "Floreria" to listOf("Arreglos", "Plantas", "Eventos"),
        "Calzado" to listOf("Urbano", "Zapatillas", "Zapatos"),
        "Cafetería" to listOf("Especialidad", "Pastelería", "Desayunos"),
        "Carniceria" to listOf("Vacuno", "Pollo", "Cerdo"),
        "Servicios" to listOf("Plomería", "Gas", "Electricista")
    )

    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        when (subScreen) {
            "home" -> StoreHomeContent(
                db = db,
                profile = profile,
                onNewAd = { subScreen = "categories" },
                onViewDetail = { ad -> selectedAd = ad; subScreen = "detail" },
                onGoPremium = { subScreen = "payment" }
            )
            "categories" -> SelectionWindow(
                title = "Seleccionar Rubro",
                items = categoryMap.keys.toList(),
                onBack = { subScreen = "home" },
                onSelect = { selectedCategory = it; subScreen = "subcategories" }
            )
            "subcategories" -> SelectionWindow(
                title = "Sub-rubro de $selectedCategory",
                items = categoryMap[selectedCategory] ?: emptyList(),
                onBack = { subScreen = "categories" },
                onSelect = { sub ->
                    scope.launch {
                        val ad = Offer(
                            storeName = profile.name,
                            title = "Promo $sub",
                            price = "Ver en tienda",
                            category = selectedCategory,
                            subCategory = sub,
                            latitude = profile.lat,
                            longitude = profile.lng,
                            isPremium = profile.isPremium
                        )
                        db.offerDao().insertOffer(ad)
                        subScreen = "home"
                        Toast.makeText(context, "ADSGO: ¡Oferta lanzada!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            "payment" -> PremiumPortal(
                onBack = { subScreen = "home" },
                onSuccess = { subScreen = "home" }
            )
            "detail" -> AdMetricsWindow(
                ad = selectedAd,
                onBack = { subScreen = "home" }
            )
        }
    }
}

@Composable
fun StoreHomeContent(db: AppDatabase, profile: LocalProfile, onNewAd: () -> Unit, onViewDetail: (Offer) -> Unit, onGoPremium: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.AddBusiness, null, tint = Color.White) }, label = { Text("Publicar", color = Color.White) })
                NavigationBarItem(selected = false, onClick = onGoPremium, icon = { Icon(Icons.Default.WorkspacePremium, null, tint = Color.White) }, label = { Text("Premium", color = Color.White) })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNewAd, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("CREAR NUEVA NOTIFICACIÓN", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Text("HISTORIAL DE CAMPAÑAS", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            LazyColumn {
                items(myAds) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onViewDetail(ad) }, elevation = CardDefaults.cardElevation(2.dp)) {
                        ListItem(
                            headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Expira: ${ad.getExpirationDate()}") },
                            trailingContent = { Icon(Icons.Default.QueryStats, null, tint = OrangeAds) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionWindow(title: String, items: List<String>, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        }
        LazyColumn(Modifier.padding(top = 16.dp)) {
            items(items) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(item) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    ListItem(headlineContent = { Text(item) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                }
            }
        }
    }
}

@Composable
fun AdMetricsWindow(ad: Offer?, onBack: () -> Unit) {
    if (ad == null) return
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("GESTIÓN DE MÉTRICAS", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Card(Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("ID: #ADS-${ad.id}", color = Color.White.copy(0.6f))
                Text(ad.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    MetricUnit("Vistas", ad.views.toString())
                    MetricUnit("Clicks", ad.clicks.toString())
                }
            }
        }
        Button(onClick = { }, Modifier.fillMaxWidth().padding(top = 30.dp).height(56.dp)) { Text("EDITAR CONTENIDO") }
    }
}

@Composable
fun MetricUnit(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 32.sp, color = OrangeAds, fontWeight = FontWeight.Black)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun PremiumPortal(onBack: () -> Unit, onSuccess: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.WorkspacePremium, null, Modifier.size(100.dp), tint = OrangeAds)
        Text("ADSGO PREMIUM", fontSize = 30.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Text("Radar de 300m e icono con salto.", Modifier.padding(top = 8.dp), color = Color.Gray)
        Spacer(Modifier.height(40.dp))
        Button(onClick = {
            Toast.makeText(context, "ID Premium enviado a tu email", Toast.LENGTH_LONG).show()
            onSuccess("ADS-OK")
        }, Modifier.fillMaxWidth().height(60.dp)) {
            Text("ACTIVAR POR $9.99")
        }
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}