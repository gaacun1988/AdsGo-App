package com.adsweb.proxismart

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // --- ESTADOS DE NAVEGACIÓN INTERNA (Flujo PDF / Rappi Style) ---
    var subScreen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }

    // --- LAS 20 CATEGORÍAS PROFESIONALES DEL FEEDBACK ---
    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Sabores Argentinos", "Bodega", "Sushi"),
        "Verduleria" to listOf("Frutas de Estación", "Verduras", "Orgánicos", "Frutos Secos"),
        "Ferretería" to listOf("Pinturas", "Herramientas", "Electricidad", "Plomería", "Cerrajería"),
        "Kiosco" to listOf("Maxikiosco", "Bebidas", "Tabaco", "Golosinas"),
        "Comercio Gral" to listOf("Bazar", "Limpieza", "Librería", "Regalería"),
        "Farmacia" to listOf("Medicamentos", "Perfumería", "Cuidado Bebé"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños", "Accesorios"),
        "Calzado" to listOf("Deportivo", "Casual", "Fiesta"),
        "Electrónica" to listOf("Celulares", "Gaming", "Computación", "Cámaras"),
        "Mascotas" to listOf("Alimento", "Accesorios", "Clínica Veterinaria"),
        "Gimnasio" to listOf("Funcional", "Crossfit", "Musculación", "Boxeo"),
        "Peluquería" to listOf("Barbería", "Color y Corte", "Manicura"),
        "Panadería" to listOf("Facturas/Masas", "Pan Galletas", "Pastas Frescas"),
        "Joyería" to listOf("Relojería", "Plata", "Joyas de Diseño"),
        "Florería" to listOf("Ramos Preparados", "Plantas", "Servicio Eventos"),
        "Deportes" to listOf("Fútbol", "Padel", "Tennis", "Suplementos"),
        "Cafetería" to listOf("De Especialidad", "Patisserie", "Brunch"),
        "Carnicería" to listOf("Vacunos", "Granja", "Embutidos"),
        "Supermercado" to listOf("Mayorista", "Market Express"),
        "Automotor" to listOf("Lubricentro", "Repuestos", "Accesorios Vehículo")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreHomeView(
                db = db,
                onNewAd = { subScreen = "cats" },
                onViewDetail = { ad -> selectedAd = ad; subScreen = "detail" },
                onGoPremium = { subScreen = "pay" }
            )
            "cats" -> RubroWindow(
                map = categoryMap,
                onBack = { subScreen = "home" },
                onComplete = { cat, sub ->
                    scope.launch {
                        val ad = Offer(
                            storeName = profile.name,
                            title = "Promo en $sub",
                            price = "Consultar en el Local",
                            category = cat,
                            subCategory = sub,
                            latitude = profile.lat,
                            longitude = profile.lng,
                            isPremium = profile.isPremium,
                            whatsapp = profile.email
                        )
                        db.offerDao().insertOffer(ad)
                        subScreen = "home"
                        Toast.makeText(context, "ADSGO: Publicación Lanzada con Éxito", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            "pay" -> PremiumTunnelWindow(
                onBack = { subScreen = "home" },
                onDone = { subScreen = "home" }
            )
            "detail" -> AdMetricsWindow(
                ad = selectedAd,
                onBack = { subScreen = "home" }
            )
        }
    }
}

@Composable
fun StoreHomeView(db: AppDatabase, onNewAd: () -> Unit, onViewDetail: (Offer) -> Unit, onGoPremium: () -> Unit) {
    var list by remember { mutableStateOf(emptyList<Offer>()) }
    LaunchedEffect(Unit) { list = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Dashboard, null, tint = Color.White) }, label = { Text("Muro Ads") })
                NavigationBarItem(selected = false, onClick = onGoPremium, icon = { Icon(Icons.Default.Verified, null, tint = Color.LightGray) }, label = { Text("Plus Pro") })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNewAd, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("PUBLICAR OFERTA AHORA", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Text("CAMPAÑAS ACTIVAS (VIGENCIA PDF)", fontWeight = FontWeight.Bold, color = Color.Gray)
            LazyColumn {
                items(list) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onViewDetail(ad) }) {
                        ListItem(
                            headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${ad.subCategory} • Vence: ${ad.getExpirationDate()}") },
                            trailingContent = { Icon(Icons.Default.QueryStats, null, tint = OrangeAds) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RubroWindow(map: Map<String, List<String>>, onBack: () -> Unit, onComplete: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var selCat by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = if(step == 1) onBack else {{ step = 1 }}) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Text(if(step == 1) "Selecciona tu Rubro" else "Elegir Especialidad", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            val currentList = if(step == 1) map.keys.toList() else map[selCat] ?: emptyList()
            items(currentList) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    if(step == 1) { selCat = item; step = 2 }
                    else { onComplete(selCat, item) }
                }) {
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
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text("CONVERSIÓN ADSGO", fontWeight = FontWeight.Black, fontSize = 24.sp, color = DeepBlueAds)
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("ANUNCIO ID: #ADS-${ad.id}", color = Color.White.copy(alpha = 0.6f))
                Text(ad.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    MetricDetail("Vistas", ad.views.toString())
                    MetricDetail("Pedidos WA", ad.clicks.toString())
                }
            }
        }
    }
}

@Composable
fun MetricDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 34.sp, color = OrangeAds, fontWeight = FontWeight.Black)
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun PremiumTunnelWindow(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) {
            CircularProgressIndicator(color = OrangeAds)
            Text("Procesando suscripción ADSGO Pro...", Modifier.padding(top = 16.dp))
        } else {
            Icon(Icons.Default.WorkspacePremium, null, Modifier.size(100.dp), tint = OrangeAds)
            Text("MODO PREMIUM", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
            Text("Desbloquea 300m de alcance y métricas en tiempo real.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(8.dp))
            Spacer(Modifier.height(40.dp))
            Button(onClick = {
                loading = true
                Toast.makeText(context, "ID de acceso Premium enviado a tu correo.", Toast.LENGTH_LONG).show()
                onDone()
            }, Modifier.fillMaxWidth().height(60.dp)) {
                Text("ADQUIRIR LICENCIA")
            }
            TextButton(onClick = onBack) { Text("Ahora no, prefiero Plan Base") }
        }
    }
}