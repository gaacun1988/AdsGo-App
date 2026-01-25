package com.adsweb.proxismart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                var screenState by remember { mutableStateOf("splash") }
                var profile by remember { mutableStateOf<LocalProfile?>(null) }

                LaunchedEffect(Unit) {
                    profile = db.offerDao().getLocalProfile()
                    delay(2500) // Pantalla de presentación
                    screenState = if (profile == null) "selection" else "main_app"
                }

                Scaffold(
                    topBar = {
                        if (screenState == "main_app" && profile != null) {
                            TopAppBar(
                                title = { Text("ADSGO", fontWeight = FontWeight.Black, color = OrangeAds) },
                                navigationIcon = {
                                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = DeepBlueAds)
                                        Spacer(Modifier.width(8.dp))
                                        Text(profile!!.name.take(12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) {
                                        Icon(Icons.Default.SwitchAccount, "Cambiar")
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when (screenState) {
                            "splash" -> SplashScreen()
                            "selection" -> SelectionScreen(onRoleSelected = { role -> screenState = "setup_$role" })
                            "setup_CLIENTE", "setup_TIENDA" -> {
                                val role = if(screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"
                                ProfileSetupScreen(role) { newProf ->
                                    scope.launch {
                                        db.offerDao().saveLocalProfile(newProf)
                                        profile = newProf
                                        screenState = "main_app"
                                    }
                                }
                            }
                            "main_app" -> {
                                if (profile?.role == "CLIENTE") ClientScreen(onBack = { screenState = "selection" }, onOpenAR = {})
                                else StoreScreen(profile!!, onBack = { screenState = "selection" })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(DeepBlueAds), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ADSGO", color = OrangeAds, fontSize = 50.sp, fontWeight = FontWeight.Black)
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun SelectionScreen(onRoleSelected: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ADSGO", fontSize = 54.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Spacer(Modifier.height(40.dp))
        Button(onClick = { onRoleSelected("CLIENTE") }, Modifier.fillMaxWidth().height(64.dp)) { Text("SOY CLIENTE") }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onRoleSelected("TIENDA") }, Modifier.fillMaxWidth().height(64.dp)) { Text("SOY COMERCIO") }
    }
}

@Composable
fun ProfileSetupScreen(role: String, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Perfil $role", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepBlueAds)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo o Razón Social") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email de Registro") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = extra, onValueChange = { extra = it }, label = { Text(if(role=="CLIENTE") "Intereses (Ropa, Pasta...)" else "Dirección") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role=role, name=name, email=email, address=if(role=="TIENDA") extra else "", interests=if(role=="CLIENTE") extra else "")) },
            Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
            Text("GUARDAR PERFIL")
        }
    }
}