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
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    var subScreen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Bodega", "Sushi"),
        "Verduleria" to listOf("Orgánico", "Frutas", "Verduras", "Frutos Secos"),
        "Ferretería" to listOf("Herramientas", "Pintura", "Electricidad", "Plomería"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños"),
        "Cafetería" to listOf("Especialidad", "Pastelería", "Desayunos")
    )

    Box(Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreHome(
                db = db,
                onNewAd = { subScreen = "categories" },
                onViewDetail = { ad -> selectedAd = ad; subScreen = "detail" },
                onGoPremium = { subScreen = "payment" }
            )
            "categories" -> CategorySelectionWindow(
                categoryMap = categoryMap,
                onBack = { subScreen = "home" },
                onComplete = { cat, sub ->
                    scope.launch {
                        val newOffer = Offer(
                            storeName = profile.name,
                            title = "Promo de $sub",
                            price = "0.0",
                            category = cat,
                            subCategory = sub,
                            latitude = profile.lat,
                            longitude = profile.lng,
                            views = (50..200).random(), // Datos de ejemplo pro
                            clicks = (5..20).random()
                        )
                        db.offerDao().insertOffer(newOffer)
                        subScreen = "home"
                        Toast.makeText(context, "¡Anuncio AdsGo lanzado!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            "payment" -> PremiumPaymentWindow(
                onBack = { subScreen = "home" },
                onSuccess = { subScreen = "home" }
            )
            "detail" -> AdDetailWindow(ad = selectedAd, onBack = { subScreen = "home" })
        }
    }
}

@Composable
fun AdDetailWindow(ad: Offer?, onBack: () -> Unit) {
    if (ad == null) return
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("MÉTRICAS DEL ANUNCIO", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)

        Card(Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("ID: #ADS-${ad.id}", color = Color.White.copy(0.6f))
                Text(ad.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MetricBoxItem("Vistas", ad.views.toString())
                    MetricBoxItem("Clicks", ad.clicks.toString())
                }
            }
        }
        Button(onClick = { }, Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp)) {
            Text("EDITAR PUBLICACIÓN")
        }
    }
}

@Composable
fun MetricBoxItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 32.sp, color = OrangeAds, fontWeight = FontWeight.Black)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun StoreHome(db: AppDatabase, onNewAd: () -> Unit, onViewDetail: (Offer) -> Unit, onGoPremium: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.AddBusiness, null) }, label = { Text("Anunciar") })
                NavigationBarItem(selected = false, onClick = onGoPremium, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Premium") })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNewAd, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("LANZAR NUEVA OFERTA", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            Text("TUS ANUNCIOS ACTIVOS", fontWeight = FontWeight.Black, fontSize = 14.sp)
            LazyColumn(Modifier.fillMaxSize()) {
                items(myAds) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onViewDetail(ad) }) {
                        ListItem(
                            headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Vence: ${ad.getExpirationDate()}") },
                            trailingContent = { Icon(Icons.Default.QueryStats, null, tint = OrangeAds) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectionWindow(categoryMap: Map<String, List<String>>, onBack: () -> Unit, onComplete: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var selCat by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = if(step == 1) onBack else { { step = 1 } }) { Icon(Icons.Default.ArrowBack, null) }
        Text(if(step == 1) "Seleccionar Rubro" else "Subcategoría de $selCat", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            val list = if(step == 1) categoryMap.keys.toList() else categoryMap[selCat] ?: emptyList()
            items(list) { item ->
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
fun PremiumPaymentWindow(onBack: () -> Unit, onSuccess: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.WorkspacePremium, null, modifier = Modifier.size(100.dp), tint = OrangeAds)
        Text("ADSGO PREMIUM", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Alcance 300m + Métricas en tiempo real", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { onSuccess("TOKEN-ADS") }, Modifier.fillMaxWidth().padding(top = 40.dp).height(60.dp)) {
            Text("ACTIVAR SUSCRIPCIÓN")
        }
        TextButton(onClick = onBack) { Text("Volver") }
    }
}