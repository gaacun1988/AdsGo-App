package com.adsweb.proxismart

import android.widget.Toast
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
    var subScreen by remember { mutableStateOf("home") }
    var stats by remember { mutableStateOf(TiendaStats()) }
    var selectedCat by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // Sincronizar estadísticas al entrar
    LaunchedEffect(Unit) {
        AdsGoNetwork.fetchStoreStats(profile.id_perfil).onSuccess { stats = it }
    }

    val categoryMap = mapOf(
        "Gastronomía" to listOf("Restaurante", "Cafetería", "Vegano"),
        "Moda" to listOf("Ropa Hombre", "Ropa Mujer", "Calzado"),
        "Salud" to listOf("Farmacia", "Óptica"),
        "Hogar" to listOf("Ferretería", "Decoración")
        // Puedes completar la lista aquí...
    )

    Box(Modifier.fillMaxSize()) {
        when (subScreen) {
            "home" -> StoreDashboardView(
                profile = profile,
                stats = stats,
                onNew = { subScreen = "cats" },
                onUpgrade = { subScreen = "payment" }
            )

            "cats" -> CategorySelectorView(
                categories = categoryMap.keys.toList(),
                onBack = { subScreen = "home" },
                onSelect = { selectedCat = it; subScreen = "subcats" }
            )

            "subcats" -> SubCategorySelectorView(
                category = selectedCat,
                subcategories = categoryMap[selectedCat] ?: emptyList(),
                onBack = { subScreen = "cats" },
                onPublish = { sub ->
                    scope.launch {
                        val ad = Offer(
                            storeName = profile.name,
                            title = "Promo en $sub",
                            category = selectedCat,
                            subCategory = sub,
                            latitude = profile.lat,
                            longitude = profile.lng
                        )
                        db.offerDao().insertOffer(ad)
                        subScreen = "home"
                        Toast.makeText(context, "ADSGO ACTIVADO!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            "payment" -> PremiumUpgradeView(onBack = { subScreen = "home" })
        }
    }
}

@Composable
fun StoreDashboardView(profile: LocalProfile, stats: TiendaStats, onNew: () -> Unit, onUpgrade: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MI TIENDA", fontWeight = FontWeight.Black, fontSize = 24.sp, color = DeepBlueAds)
            Surface(color = if(stats.is_premium) OrangeAds else Color.Gray, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = if(stats.is_premium) "PREMIUM" else "BÁSICO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            }
        }
        Text("ID: ${profile.id_perfil.takeLast(4).uppercase()}", color = OrangeAds, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
            StatCard("Vistas", stats.vistas.toString(), Modifier.weight(1f))
            StatCard("Clicks", stats.clics.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text("Radar Activo: ${stats.radius}m", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(if(stats.is_premium) 1f else 0.3f, Modifier.fillMaxWidth().padding(vertical = 8.dp), color = OrangeAds)
                if(!stats.is_premium) {
                    Button(onClick = onUpgrade, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAds)) {
                        Text("AMPILAR RADAR (Premium)")
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onNew, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
            Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(8.dp))
            Text("LANZAR OFERTA AL RADAR", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CategorySelectorView(categories: List<String>, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text("Elige un Rubro", fontSize = 24.sp, fontWeight = FontWeight.Black)
        LazyColumn {
            items(categories) { cat ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(cat) }) {
                    ListItem(headlineContent = { Text(cat) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                }
            }
        }
    }
}

@Composable
fun SubCategorySelectorView(category: String, subcategories: List<String>, onBack: () -> Unit, onPublish: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text(category, fontSize = 24.sp, fontWeight = FontWeight.Black)
        LazyColumn {
            items(subcategories) { sub ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPublish(sub) }) {
                    ListItem(headlineContent = { Text(sub) })
                }
            }
        }
    }
}

@Composable
fun PremiumUpgradeView(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.WorkspacePremium, null, Modifier.size(100.dp), tint = OrangeAds)
        Text("MEJORA A ADSGO PRO", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Tu radar pasará de 100m a 300m.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Button(onClick = onBack, Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("PAGAR S/ 9.90") }
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        }
    }
}