package com.adsweb.proxismart

import android.Manifest
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val scope = rememberCoroutineScope()

                // --- VARIABLES DE ESTADO ---
                var currentProfile by remember { mutableStateOf<LocalProfile?>(null) }
                var screenState by remember { mutableStateOf("splash") }
                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                // --- MOTOR DE PERSISTENCIA (No borra perfil) ---
                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    val profiles = db.offerDao().getAllLocalProfiles()
                    delay(2000) // Splash profesional
                    if (profiles.isNotEmpty()) {
                        currentProfile = profiles.first()
                        screenState = "main_app"
                    } else {
                        screenState = "selection"
                    }
                }

                Scaffold(
                    topBar = {
                        if (screenState == "main_app" && currentProfile != null) {
                            CenterAlignedTopAppBar(
                                title = { Text("ADSGO", fontWeight = FontWeight.Black, color = OrangeAds) },
                                navigationIcon = {
                                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = DeepBlueAds)
                                        Spacer(Modifier.width(5.dp))
                                        Text(currentProfile!!.name.take(10), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) {
                                        Icon(Icons.Default.SwitchAccount, null)
                                    }
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

                                "selection" -> SelectionScreen { role ->
                                    screenState = "setup_$role"
                                }

                                "setup_CLIENTE" -> ClientSetupWindow { newProf ->
                                    scope.launch {
                                        db.offerDao().saveLocalProfile(newProf)
                                        currentProfile = newProf
                                        screenState = "main_app"
                                    }
                                }

                                "setup_TIENDA" -> StoreSetupWindow { newProf ->
                                    scope.launch {
                                        db.offerDao().saveLocalProfile(newProf)
                                        currentProfile = newProf
                                        screenState = "main_app"
                                    }
                                }

                                "main_app" -> {
                                    if (currentProfile?.role == "CLIENTE") {
                                        ClientScreen(
                                            onBack = { screenState = "selection" },
                                            onOpenAR = { arTitle = it; showAR = true }
                                        )
                                    } else if (currentProfile != null) {
                                        StoreScreen(
                                            profile = currentProfile!!,
                                            onBack = { screenState = "selection" }
                                        )
                                    }
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
        Text("BUSINESS & SMART SHOPPING", fontSize = 10.sp, color = DeepBlueAds, letterSpacing = 2.sp)
        Spacer(Modifier.height(50.dp))
        Button(onClick = { onSelected("CLIENTE") }, Modifier.fillMaxWidth().height(64.dp)) {
            Text("SOY CLIENTE", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onSelected("TIENDA") }, Modifier.fillMaxWidth().height(64.dp)) {
            Text("SOY COMERCIO", fontWeight = FontWeight.Bold)
        }
    }
}