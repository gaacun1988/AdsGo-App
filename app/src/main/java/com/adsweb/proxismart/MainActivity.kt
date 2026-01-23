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
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.*
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
                var allProfiles by remember { mutableStateOf<List<LocalProfile>>(emptyList()) }
                var screenState by remember { mutableStateOf("loading") }

                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                // Cargar perfiles al iniciar (Persistencia Blindada)
                LaunchedEffect(Unit) {
                    allProfiles = db.offerDao().getAllLocalProfiles()
                    if (allProfiles.isNotEmpty()) {
                        currentProfile = allProfiles.first()
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
                                        Icon(Icons.Default.AccountCircle, null, tint = DeepBlueAds)
                                        Spacer(Modifier.width(4.dp))
                                        Text(currentProfile!!.name.take(10), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) {
                                        Icon(Icons.Default.SwitchAccount, "Cambiar Perfil")
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
                                "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = OrangeAds) }
                                "selection" -> SelectionScreen(
                                    onRoleSelected = { role -> screenState = "setup_$role" },
                                    onBack = { if (currentProfile != null) screenState = "main_app" }
                                )
                                "setup_CLIENTE", "setup_TIENDA" -> {
                                    val role = if(screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"
                                    ProfileSetupScreen(role, onBack = { screenState = "selection" }) { newProf ->
                                        scope.launch {
                                            db.offerDao().saveLocalProfile(newProf)
                                            allProfiles = db.offerDao().getAllLocalProfiles()
                                            currentProfile = newProf
                                            screenState = "main_app"
                                        }
                                    }
                                }
                                "main_app" -> {
                                    if (currentProfile?.role == "CLIENTE") {
                                        ClientScreen(
                                            onBack = { screenState = "selection" },
                                            onOpenAR = { arTitle = it; showAR = true }
                                        )
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

@Composable
fun SelectionScreen(onRoleSelected: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ADSGO", fontSize = 54.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Spacer(Modifier.height(40.dp))
        Button(onClick = { onRoleSelected("CLIENTE") }, Modifier.fillMaxWidth().height(64.dp)) { Text("BUSCAR OFERTAS") }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onRoleSelected("TIENDA") }, Modifier.fillMaxWidth().height(64.dp)) { Text("SOY COMERCIO") }
        TextButton(onClick = onBack, Modifier.padding(top = 20.dp)) { Text("VOLVER A MI PERFIL ACTIVO") }
    }
}

@Composable
fun ProfileSetupScreen(role: String, onBack: () -> Unit, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("Crear Perfil $role", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepBlueAds)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo o Local") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email de contacto") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role = role, name = name, email = email)) }, Modifier.fillMaxWidth().height(60.dp)) {
            Text("GUARDAR Y ENTRAR")
        }
    }
}