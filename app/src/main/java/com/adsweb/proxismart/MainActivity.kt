package com.adsweb.proxismart

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                var profileState by remember { mutableStateOf<LocalProfile?>(null) }
                var screenState by remember { mutableStateOf("splash") }
                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    profileState = db.offerDao().getLocalProfile()
                    delay(2500)
                    screenState = if (profileState == null) "selection" else "main_app"
                }

                Scaffold(
                    topBar = {
                        if (screenState == "main_app" && profileState != null) {
                            CenterAlignedTopAppBar(
                                title = { Text("ADSGO", fontWeight = FontWeight.Black, color = OrangeAds) },
                                navigationIcon = {
                                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = DeepBlueAds, modifier = Modifier.size(32.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(profileState!!.name.take(12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) { Icon(Icons.Default.SwitchAccount, null) }
                                }
                            )
                        }
                    }
                ) { p ->
                    Box(Modifier.padding(p).fillMaxSize()) {
                        if (showAR) {
                            ARScreen(arTitle, onOrderSent = { showAR = false }, onBack = { showAR = false })
                        } else {
                            when (screenState) {
                                "splash" -> SplashScreen()
                                "selection" -> SelectionScreen { role -> screenState = "setup_$role" }
                                "setup_CLIENTE", "setup_TIENDA" -> {
                                    val r = if(screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"
                                    ProfileSetupScreen(r) { newProf ->
                                        scope.launch { db.offerDao().saveLocalProfile(newProf); profileState = newProf; screenState = "main_app" }
                                    }
                                }
                                "main_app" -> {
                                    if (profileState?.role == "CLIENTE") ClientScreen(onBack = { screenState = "selection" }, onOpenAR = { arTitle = it; showAR = true })
                                    else StoreScreen(profileState!!, onBack = { screenState = "selection" })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionScreen(onSelected: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("ADSGO", fontSize = 54.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Spacer(Modifier.height(40.dp))
        Button(onClick = { onSelected("CLIENTE") }, Modifier.fillMaxWidth().height(64.dp)) { Text("MODO CLIENTE", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onSelected("TIENDA") }, Modifier.fillMaxWidth().height(64.dp)) { Text("MODO COMERCIO", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ProfileSetupScreen(role: String, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Registro ADSGO: $role", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepBlueAds)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre o Local") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = extra, onValueChange = { extra = it }, label = { Text(if(role=="CLIENTE") "Tus gustos" else "Dirección") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role = role, name = name, email = email, address = if(role=="TIENDA") extra else "", interests = if(role=="CLIENTE") extra else "")) }, Modifier.fillMaxWidth().height(56.dp)) {
            Text("CREAR PERFIL PERMANENTE")
        }
    }
}