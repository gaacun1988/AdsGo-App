package com.adsweb.proxismart

import android.widget.Toast
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
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAd by remember { mutableStateOf<Offer?>(null) }
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val categoryMap = mapOf(
        "Restaurante" to listOf("Vegano", "Pastas", "Pescados", "Carnes", "Bodega", "Sushi"),
        "Verduleria" to listOf("Orgánico", "Frutas", "Verduras"),
        "Ferretería" to listOf("Herramientas", "Pintura", "Electricidad"),
        "Ropa" to listOf("Hombre", "Mujer", "Niños"),
        "Cafetería" to listOf("Especialidad", "Pastelería")
    )

    Box(Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreHomeView(db, onNew = { subScreen = "cats" }, onDetail = { selectedAd = it; subScreen = "detail" }, onPremium = { subScreen = "pay" })
            "cats" -> CategoryWindow(categoryMap, onBack = { subScreen = "home" }) { cat, sub ->
                scope.launch {
                    val ad = Offer(storeName = profile.name, title = "Oferta $sub", category = cat, subCategory = sub, latitude = profile.lat, longitude = profile.lng)
                    db.offerDao().insertOffer(ad)
                    subScreen = "home"
                    Toast.makeText(context, "Anuncio lanzado!", Toast.LENGTH_SHORT).show()
                }
            }
            "pay" -> PremiumPayWindow(onBack = { subScreen = "home" }) { subScreen = "home" }
            "detail" -> AdDetailWindow(selectedAd) { subScreen = "home" }
        }
    }
}

@Composable
fun StoreHomeView(db: AppDatabase, onNew: () -> Unit, onDetail: (Offer) -> Unit, onPremium: () -> Unit) {
    var ads by remember { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(Unit) { ads = db.offerDao().getAllOffers() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DeepBlueAds) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.AddBusiness, null, tint = Color.White) }, label = { Text("Anunciar", color = Color.White) })
                NavigationBarItem(selected = false, onClick = onPremium, icon = { Icon(Icons.Default.Star, null, tint = Color.White) }, label = { Text("Premium", color = Color.White) })
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Button(onClick = onNew, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) { Text("LANZAR ANUNCIO") }
            Spacer(Modifier.height(24.dp))
            Text("HISTORIAL DE ADS", fontWeight = FontWeight.Black)
            LazyColumn {
                items(ads) { ad ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDetail(ad) }) {
                        ListItem(headlineContent = { Text(ad.title, fontWeight = FontWeight.Bold) }, supportingContent = { Text("Vence: ${ad.getExpirationDate()}") })
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryWindow(map: Map<String, List<String>>, onBack: () -> Unit, onComplete: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var selCat by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp)) {
        IconButton(onClick = if(step == 1) onBack else { { step = 1 } }) { Icon(Icons.Default.ArrowBack, null) }
        Text(if(step == 1) "Selecciona Rubro" else "Subcategoría de $selCat", fontSize = 22.sp, fontWeight = FontWeight.Black)
        LazyColumn(Modifier.padding(top = 16.dp)) {
            val list = if(step == 1) map.keys.toList() else map[selCat] ?: emptyList()
            items(list) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    if(step == 1) { selCat = item; step = 2 } else { onComplete(selCat, item) }
                }) { ListItem(headlineContent = { Text(item) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }) }
            }
        }
    }
}

@Composable
fun PremiumPayWindow(onBack: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        IconButton(onClick = onBack, Modifier.align(Alignment.Start)) { Icon(Icons.Default.ArrowBack, null) }
        Icon(Icons.Default.WorkspacePremium, null, Modifier.size(100.dp), tint = OrangeAds)
        Text("ADSGO PREMIUM", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Activa el alcance de 300m e ID temporal", Modifier.padding(top = 8.dp))
        Button(onClick = onDone, Modifier.fillMaxWidth().padding(top = 40.dp).height(60.dp)) { Text("PAGAR Y ACTIVAR") }
    }
}

@Composable
fun AdDetailWindow(ad: Offer?, onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("MÉTRICAS: ${ad?.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepBlueAds)
        Card(Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = DeepBlueAds)) {
            Column(Modifier.padding(24.dp)) {
                Text("VISTAS: ${ad?.views}", color = OrangeAds, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("CLICKS: ${ad?.clicks}", color = Color.White)
            }
        }
    }
}