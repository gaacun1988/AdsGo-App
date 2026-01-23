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
                var screenState by remember { mutableStateOf("loading") } // loading, selection, setup, main_app

                // Cargar perfiles al iniciar
                LaunchedEffect(Unit) {
                    allProfiles = db.offerDao().getAllLocalProfiles()
                    if (allProfiles.isNotEmpty()) {
                        currentProfile = allProfiles.first()
                        screenState = "main_app"
                    } else {
                        screenState = "selection"
                    }
                }

                Scaffold(
                    topBar = {
                        if (screenState == "main_app" && currentProfile != null) {
                            CenterAlignedTopAppBar(
                                title = { Text("ADSGO", fontWeight = FontWeight.Black) },
                                navigationIcon = {
                                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = OrangeAds)
                                        Spacer(Modifier.width(4.dp))
                                        Text(currentProfile!!.name.take(8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { screenState = "selection" }) {
                                        Icon(Icons.Default.SwitchAccount, "Cambiar Perfil")
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when (screenState) {
                            "loading" -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                            "selection" -> SelectionScreen(
                                onRoleSelected = { role -> screenState = "setup_$role" },
                                onBack = { if(allProfiles.isNotEmpty()) screenState = "main_app" }
                            )

                            "setup_CLIENTE", "setup_TIENDA" -> {
                                val role = if(screenState == "setup_CLIENTE") "CLIENTE" else "TIENDA"
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
                                    ClientScreen(onBack = { screenState = "selection" }, onOpenAR = { })
                                } else {
                                    // LLAMADA CORREGIDA A STORESCREEN
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

@Composable
fun SelectionScreen(onRoleSelected: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ADSGO", fontSize = 54.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Spacer(Modifier.height(40.dp))
        Button(onClick = { onRoleSelected("CLIENTE") }, Modifier.fillMaxWidth().height(60.dp)) { Text("MODO CLIENTE") }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onRoleSelected("TIENDA") }, Modifier.fillMaxWidth().height(60.dp)) { Text("MODO COMERCIO") }
        TextButton(onClick = onBack, Modifier.padding(top = 20.dp)) { Text("VOLVER ATRÁS") }
    }
}

@Composable
fun ProfileSetupScreen(role: String, onBack: () -> Unit, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        Text("Perfil $role", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (Se asociará al perfil)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role = role, name = name, email = email)) }, Modifier.fillMaxWidth().height(56.dp)) {
            Text("GUARDAR PERFIL")
        }
    }
}