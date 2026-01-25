package com.adsweb.proxismart

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(profile: LocalProfile, onBack: () -> Unit) {
    var subScreen by remember { mutableStateOf("home") }
    var selectedCat by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }
    val db = remember { AppDatabase.getDatabase(LocalContext.current) }
    val scope = rememberCoroutineScope()

    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Bodega", "Sushi"),
        "Verduleria" to listOf("Orgánico", "Frutas", "Verduras", "Frutos Secos"),
        "Ferretería" to listOf("Herramientas", "Pintura", "Electricidad", "Plomería"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños", "Deportivo"),
        "Electronica" to listOf("Celulares", "Computación", "Gaming", "Audio")
    )

    Box(Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreHomeLayout(
                db = db,
                onNew = { subScreen = "cats" },
                onDetail = { selectedAd = it; subScreen = "detail" },
                onPremium = { subScreen = "pay" }
            )
            "cats" -> CategorySelectionWindow(categoryMap, onBack = { subScreen = "home" }) { c, s ->
                scope.launch {
                    val ad = Offer(storeName = profile.name, title = "Promo $s", category = c, subCategory = s, latitude = profile.lat, longitude = profile.lng, isPremium = profile.isPremium)
                    db.offerDao().insertOffer(ad)
                    subScreen = "home"
                    Toast.makeText(null, "Anuncio en línea!", Toast.LENGTH_SHORT).show()
                }
            }
            "pay" -> PremiumPayTunnel { subScreen = "home" }
            "detail" -> AdStatsWindow(selectedAd) { subScreen = "home" }
        }
    }
}

@Composable
fun StoreHomeLayout(db: AppDatabase, onNew: () -> Unit, onDetail: (Offer) -> Unit, onPremium: () -> Unit) {
    var myAds by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { myAds = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.AddBusiness, null, tint = Color.White) }, label = { Text("Lanzar", color = Color.White) })
                NavigationBarItem(selected = false, onClick = onPremium, icon = { Icon(Icons.Default.WorkspacePremium, null, tint = Color.White) }, label = { Text("PRO", color = Color.White) })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNew, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
                Text("PUBLICAR OFERTA ADSGO", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("ANUNCIOS ACTIVOS", fontWeight = FontWeight.Black, fontSize = 14.sp)
            LazyColumn {
                items(myAds) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDetail(ad) }) {
                        ListItem(
                            headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Categoría: ${ad.subCategory}") },
                            trailingContent = { Icon(Icons.Default.QueryStats, null, tint = OrangeAds) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectionWindow(map: Map<String, List<String>>, onBack: () -> Unit, onSelect: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var sel by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = if(step==1) onBack else {{step=1}}) { Icon(Icons.Default.ArrowBack, null) }
        Text(if(step==1) "Elige Rubro" else "Sub-rubro de $sel", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.padding(top = 16.dp)) {
            val list = if(step == 1) map.keys.toList() else map[sel] ?: emptyList()
            items(list) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    if(step==1) { sel=item; step=2 } else { onSelect(sel, item) }
                }) { ListItem(headlineContent = { Text(item) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }) }
            }
        }
    }
}

@Composable
fun AdStatsWindow(ad: Offer?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("RENDIMIENTO PRO", fontWeight = FontWeight.Black, fontSize = 24.sp, color = DeepBlueAds)
        Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("ANUNCIO: ${ad?.title}", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${ad?.views}", color = OrangeAds, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text("Vistas", color = Color.White, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${ad?.clicks}", color = OrangeAds, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text("Pedidos", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPayTunnel(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.Verified, null, Modifier.size(80.dp), tint = OrangeAds)
        Text("ADSGO PREMIUM", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Radio de 300m y reportes avanzados activados por email.", Modifier.padding(top = 12.dp))
        Button(onClick = onDone, Modifier.fillMaxWidth().padding(top = 40.dp).height(56.dp)) { Text("ACTIVAR YA") }
    }
}