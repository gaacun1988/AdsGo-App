package com.adsweb.proxismart

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.DeepBlueAds
import com.adsweb.proxismart.ui.theme.OrangeAds
import com.adsweb.proxismart.ui.theme.ProxiSmartTheme
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

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    val saved = db.offerDao().getAllLocalProfiles()
                    delay(2000)
                    if (saved.isNotEmpty()) {
                        currentProfile = saved.first()
                        screenState = "main_app"
                    } else { screenState = "selection" }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showAR) {
                        ARScreen(arTitle, onOrderSent = { showAR = false }, onBack = { showAR = false })
                    } else {
                        when (screenState) {
                            "splash" -> SplashScreenLayout()
                            "selection" -> SelectionLayout { screenState = "setup_$it" }
                            "setup_CLIENTE", "setup_TIENDA" -> {
                                ProfileSetupLayout(screenState) { newProf ->
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
                                        navigationIcon = {
                                            Icon(Icons.Default.AccountCircle, null, Modifier.padding(start = 12.dp).size(30.dp), tint = DeepBlueAds)
                                        },
                                        actions = {
                                            Text(currentProfile?.name?.take(10) ?: "", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                                            IconButton(onClick = { screenState = "selection" }) { Icon(Icons.Default.SwapHoriz, null) }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                                    )
                                }
                            ) { padding ->
                                Box(modifier = Modifier.padding(padding)) {
                                    if (currentProfile?.role == "CLIENTE") ClientScreen(onBack = { screenState = "selection" }, onOpenAR = { arTitle = it; showAR = true })
                                    else StoreScreen(profile = currentProfile!!, onBack = { screenState = "selection" })
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
fun SplashScreenLayout() {
    Box(Modifier.fillMaxSize().background(DeepBlueAds), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ADSGO", fontSize = 60.sp, fontWeight = FontWeight.Black, color = OrangeAds)
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun SelectionLayout(onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("ADSGO", fontSize = 54.sp, fontWeight = FontWeight.Black, color = OrangeAds)
        Spacer(Modifier.height(50.dp))
        Button(onClick = { onSelect("CLIENTE") }, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
            Text("MODO CLIENTE", color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onSelect("TIENDA") }, Modifier.fillMaxWidth().height(60.dp)) {
            Text("MODO TIENDA")
        }
    }
}

@Composable
fun ProfileSetupLayout(stateKey: String, onComplete: (LocalProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val role = if(stateKey.contains("CLIENTE")) "CLIENTE" else "TIENDA"
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Perfil $role", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepBlueAds)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (BI ADSGO)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(onClick = { onComplete(LocalProfile(role = role, name = name, email = email)) }, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)) {
            Text("GUARDAR PERMANENTE")
        }
    }
}