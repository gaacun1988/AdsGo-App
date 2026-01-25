package com.adsweb.proxismart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.adsweb.proxismart.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProxiSmartTheme {
                val db = remember { AppDatabase.getDatabase(this) }
                val scope = rememberCoroutineScope()
                var currentProfile by remember { mutableStateOf<LocalProfile?>(null) }
                var screenState by remember { mutableStateOf("splash") }

                LaunchedEffect(Unit) {
                    val profiles = db.offerDao().getAllLocalProfiles()
                    delay(2000)
                    if (profiles.isNotEmpty()) {
                        currentProfile = profiles.first()
                        screenState = "main_app"
                    } else { screenState = "selection" }
                }

                Surface(Modifier.fillMaxSize()) {
                    when (screenState) {
                        "splash" -> Box(Modifier.fillMaxSize().background(DeepBlueAds), contentAlignment = Alignment.Center) {
                            Text("ADSGO", fontSize = 60.sp, fontWeight = FontWeight.Black, color = OrangeAds)
                        }
                        "selection" -> Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center) {
                            Text("ADSGO", fontSize = 50.sp, fontWeight = FontWeight.Black, color = OrangeAds)
                            Spacer(Modifier.height(40.dp))
                            Button(onClick = { screenState = "setup_CLIENTE" }, Modifier.fillMaxWidth().height(60.dp)) { Text("MODO CLIENTE") }
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { screenState = "setup_TIENDA" }, Modifier.fillMaxWidth().height(60.dp)) { Text("MODO COMERCIO") }
                        }
                        "setup_CLIENTE", "setup_TIENDA" -> {
                            ProfileSetup(screenState) { newProf ->
                                scope.launch {
                                    db.offerDao().saveLocalProfile(newProf)
                                    currentProfile = newProf
                                    screenState = "main_app"
                                }
                            }
                        }
                        "main_app" -> Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("ADSGO", fontWeight = FontWeight.Black, color = OrangeAds) },
                                    navigationIcon = { Icon(Icons.Default.AccountCircle, null, Modifier.padding(8.dp), tint = DeepBlueAds) },
                                    actions = { Text(currentProfile?.name ?: "", Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold) }
                                )
                            }
                        ) { p ->
                            Box(Modifier.padding(p)) {
                                if (currentProfile?.role == "CLIENTE") ClientScreen(onBack = { screenState = "selection" }, onOpenAR = {})
                                else StoreScreen(profile = currentProfile!!, onBack = { screenState = "selection" })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetup(roleKey: String, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val role = if(roleKey.contains("CLIENTE")) "CLIENTE" else "TIENDA"
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Crear Perfil de $role", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email de Contacto") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role = role, name = name, email = email)) }, Modifier.fillMaxWidth().height(56.dp)) {
            Text("CONTINUAR")
        }
    }
}