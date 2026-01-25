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
                val db = remember { AppDatabase.getDatabase(this) }
                val scope = rememberCoroutineScope()
                var currentProfile by remember { mutableStateOf<LocalProfile?>(null) }
                var screenState by remember { mutableStateOf("splash") }
                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                // MOTOR DE PERSISTENCIA Y SPLASH
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    val profiles = db.offerDao().getAllLocalProfiles()
                    delay(2500) // Pantalla de marca AdsGo
                    if (profiles.isNotEmpty()) {
                        currentProfile = profiles.first()
                        screenState = "main_app"
                    } else { screenState = "selection" }
                }

                Scaffold(
                    topBar = {
                        if (screenState == "main_app" && currentProfile != null) {
                            TopAppBar(
                                title = { Text("ADSGO", fontWeight = FontWeight.Black, color = OrangeAds) },
                                navigationIcon = {
                                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = DeepBlueAds, Modifier.size(32.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(currentProfile!!.name.take(10), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) {
                                        Icon(Icons.Default.SwitchAccount, null, tint = Color.Gray)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        if (showAR) {
                            ARScreen(arTitle, onOrderSent = { showAR = false }, onBack = { showAR = false })
                        } else {
                            when (screenState) {
                                "splash" -> SplashScreen()
                                "selection" -> SelectionScreen(
                                    onRoleSelected = { role -> screenState = "setup_$role" },
                                    onBack = { if (currentProfile != null) screenState = "main_app" }
                                )
                                "setup_CLIENTE", "setup_TIENDA" -> {
                                    val r = if(screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"
                                    if (r == "CLIENTE") {
                                        ClientSetupWindow { scope.launch { db.offerDao().saveLocalProfile(it); currentProfile = it; screenState = "main_app" } }
                                    } else {
                                        StoreSetupWindow { scope.launch { db.offerDao().saveLocalProfile(it); currentProfile = it; screenState = "main_app" } }
                                    }
                                }
                                "main_app" -> {
                                    if (currentProfile?.role == "CLIENTE") {
                                        ClientScreen(onBack = { screenState = "selection" }, onOpenAR = { arTitle = it; showAR = true })
                                    } else {
                                        StoreScreen(currentProfile!!, onBack = { screenState = "selection" })
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