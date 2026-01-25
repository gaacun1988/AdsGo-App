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
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // --- SISTEMA DE VENTANAS ADSGO (FLUJO TIPO RAPPI) ---
    var currentSubScreen by remember { mutableStateOf("home") }
    var selectedRubro by remember { mutableStateOf("") }
    var selectedSubRubro by remember { mutableStateOf("") }
    var selectedAdForDetail by remember { mutableStateOf<Offer?>(null) }

    // --- MAPA MAESTRO DE 20 CATEGORÍAS SEGÚN EL PDF ---
    val categoryMap = mapOf(
        "Verduleria" to listOf("Frutas de estación", "Vegetales", "Orgánico", "Frutos Secos"),
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Sabores Argentinos", "Sushi", "Minutas"),
        "Kiosco" to listOf("Golosinas", "Bebidas Frías", "MaxiKiosco", "Regalería"),
        "Ferreteria" to listOf("Herramientas Hogar", "Pintura", "Electricidad", "Plomería"),
        "Panaderia" to listOf("Facturas", "Panes Artesanales", "Masas", "Tortas"),
        "Farmacia" to listOf("Recetados", "Cosmética", "Higiene", "Primeros Auxilios"),
        "Ropa" to listOf("Masculina", "Femenina", "Infantil", "Accesorios"),
        "Calzado" to listOf("Deportivo", "Urbano", "Gala", "Zapatillas"),
        "Electronica" to listOf("Celulares", "Gaming", "Audio", "Smart Home"),
        "Supermercado" to listOf("Almacén", "Limpieza", "Fiambrería", "Ofertas Góndola"),
        "Jugueteria" to listOf("Didácticos", "Juegos de Mesa", "Peluches", "Action Figures"),
        "Mascotas" to listOf("Alimento Perros/Gatos", "Peluquería", "Accesorios"),
        "Gimnasio" to listOf("Crossfit", "Yoga", "Musculación", "Funcional"),
        "Peluqueria" to listOf("Barbería", "Coloristas", "Tratamientos"),
        "Libreria" to listOf("Escolar", "Técnica", "Arte y Dibujo", "Novedades"),
        "Joyeria" to listOf("Relojes Pro", "Platería", "Oro", "Joyas Diseño"),
        "Floreria" to listOf("Ramos para Regalo", "Plantas Interior", "Eventos"),
        "Deportes" to listOf("Futbol/Basket", "Tenis/Padel", "Suplementos"),
        "Cafeteria" to listOf("Café de especialidad", "Brunch", "Desayunos"),
        "Carniceria" to listOf("Cortes Vacunos", "Granja/Pollo", "Cerdo", "Chacinados")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentSubScreen) {
            "home" -> StoreMainHome(
                profile = profile,
                db = db,
                onNewAd = { currentSubScreen = "select_rubro" },
                onViewDetail = { ad -> selectedAdForDetail = ad; currentSubScreen = "stats" },
                onUpgradePremium = { currentSubScreen = "premium_tunnel" }
            )

            "select_rubro" -> GenericListSelectionWindow(
                title = "1. Seleccione Rubro",
                items = categoryMap.keys.toList(),
                onBack = { currentSubScreen = "home" },
                onSelect = { selectedRubro = it; currentSubScreen = "select_subrubro" }
            )

            "select_subrubro" -> GenericListSelectionWindow(
                title = "2. $selectedRubro - Tipo",
                items = categoryMap[selectedRubro] ?: emptyList(),
                onBack = { currentSubScreen = "select_rubro" },
                onSelect = { selectedSubRubro = it; currentSubScreen = "publish_form" }
            )

            "publish_form" -> AdsGoPublishForm(
                profile = profile,
                rubro = selectedRubro,
                subRubro = selectedSubRubro,
                onBack = { currentSubScreen = "select_subrubro" },
                onComplete = { newOffer ->
                    scope.launch {
                        db.offerDao().insertOffer(newOffer)
                        currentSubScreen = "home"
                        Toast.makeText(context, "Anuncio Lanzado a ${newOffer.radius}m!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            "stats" -> AdMetricsDeepWindow(
                offer = selectedAdForDetail,
                onBack = { currentSubScreen = "home" },
                onDelete = {
                    scope.launch { /* Lógica de borrado */ }
                    currentSubScreen = "home"
                }
            )

            "premium_tunnel" -> PremiumPayTunnelWindow(
                onBack = { currentSubScreen = "home" },
                onSuccess = { currentSubScreen = "home" }
            )
        }
    }
}

// --- VENTANA 1: HOME DE LA TIENDA ---
@Composable
fun StoreMainHome(profile: LocalProfile, db: AppDatabase, onNewAd: () -> Unit, onViewDetail: (Offer) -> Unit, onUpgradePremium: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Storefront, null, tint = Color.White) }, label = { Text("Muro Ads") })
                NavigationBarItem(selected = false, onClick = onUpgradePremium, icon = { Icon(Icons.Default.Verified, null, tint = Color.White.copy(0.6f)) }, label = { Text("Hazte PRO") })
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text("Centro de Control: ${profile.name}", fontSize = 18.sp, color = DeepBlueAds, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onNewAd,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
            ) {
                Icon(Icons.Default.Campaign, null)
                Spacer(Modifier.width(8.dp))
                Text("PUBLICAR OFERTA AHORA", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(20.dp))
            Text("TUS CAMPAÑAS ACTIVAS", fontSize = 14.sp, fontWeight = FontWeight.Black)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(myAds) { ad ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onViewDetail(ad) }, elevation = CardDefaults.cardElevation(4.dp)) {
                        ListItem(
                            headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Vence: ${ad.getExpirationDate()} - ${ad.subCategory}") },
                            trailingContent = { Icon(Icons.Default.BarChart, null, tint = OrangeAds) }
                        )
                    }
                }
            }
        }
    }
}

// --- VENTANA 2: SELECTORES GENÉRICOS (RUBROS Y SUBRUBROS) ---
@Composable
fun GenericListSelectionWindow(title: String, items: List<String>, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(item) }) {
                    ListItem(headlineContent = { Text(item) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                }
            }
        }
    }
}

// --- VENTANA 3: FORMULARIO DE PUBLICACIÓN ADSGO ---
@Composable
fun AdsGoPublishForm(profile: LocalProfile, rubro: String, subRubro: String, onBack: () -> Unit, onComplete: (Offer) -> Unit) {
    var offerTitle by remember { mutableStateOf("") }
    var offerPrice by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text("DETALLE DE TU ADSGO", fontSize = 24.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Text("Se publicará en $rubro / $subRubro", color = Color.Gray)

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = offerTitle, onValueChange = { offerTitle = it }, label = { Text("Título de la Oferta") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = offerPrice, onValueChange = { offerPrice = it }, label = { Text("Precio Final $") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("Tu WhatsApp de ventas") }, modifier = Modifier.fillMaxWidth())

        Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = if(profile.isPremium) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))) {
            Column(Modifier.padding(16.dp)) {
                Text(if(profile.isPremium) "Satus: PREMIUM PRO" else "Status: PLAN BASE", fontWeight = FontWeight.Bold)
                Text(if(profile.isPremium) "• Alcance: 300m\n• Tiempo: 7 días" else "• Alcance: 100m\n• Tiempo: 24h", fontSize = 12.sp)
            }
        }

        Button(
            onClick = {
                onComplete(Offer(
                    storeName = profile.name,
                    title = offerTitle,
                    price = offerPrice,
                    whatsapp = whatsapp,
                    category = rubro,
                    subCategory = subRubro,
                    latitude = profile.lat,
                    longitude = profile.lng,
                    isPremium = profile.isPremium,
                    radius = if(profile.isPremium) 300f else 100f
                ))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 30.dp).height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
        ) { Text("LANZAR NOTIFICACIÓN", fontWeight = FontWeight.Black) }
    }
}

// --- VENTANA 4: MÉTRICAS Y EDICIÓN ---
@Composable
fun AdMetricsDeepWindow(offer: Offer?, onBack: () -> Unit, onDelete: () -> Unit) {
    if (offer == null) return
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("DETALLE DE RENDIMIENTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(30.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text(offer.title, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Black)
                Text("Anuncio ID: #ADS-${offer.id}", color = Color.White.copy(0.6f))
                Spacer(Modifier.height(40.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${offer.views}", color = OrangeAds, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("Vistas", color = Color.White, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${offer.clicks}", color = OrangeAds, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("Ventas WA", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(top = 40.dp), border = BorderStroke(1.dp, Color.Red)) {
            Text("DAR DE BAJA PUBLICACIÓN", color = Color.Red)
        }
    }
}

// --- VENTANA 5: TÚNEL DE PAGO PREMIUM ---
@Composable
fun PremiumPayTunnelWindow(onBack: () -> Unit, onSuccess: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Stars, null, Modifier.size(100.dp), tint = OrangeAds)
        Text("ADSGO PRO+", fontSize = 30.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Text("Llega antes que tu competencia", modifier = Modifier.padding(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Text("Membresía Mensual: $14.99", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
        }
        Button(onClick = { onSuccess("ADS-TOKEN-GOLD") }, modifier = Modifier.padding(top = 40.dp).fillMaxWidth().height(60.dp)) {
            Text("VINCULAR TARJETA Y ACTIVAR")
        }
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}