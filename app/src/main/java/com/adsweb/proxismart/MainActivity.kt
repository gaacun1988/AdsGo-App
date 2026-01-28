package com.adsweb.proxismart

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsweb.proxismart.ui.theme.DeepBlueAds
import com.adsweb.proxismart.ui.theme.OrangeAds
import com.adsweb.proxismart.ui.theme.ProxiSmartTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual(onLocationReady: (Double, Double) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            } else {
                onLocationReady(-34.6037, -58.3816) // Fallback CABA
            }
        }.addOnFailureListener {
            onLocationReady(-34.6037, -58.3816)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        setContent {
            ProxiSmartTheme {
                val db = remember { AppDatabase.getDatabase(this) }
                val scope = rememberCoroutineScope()
                var currentProfile by remember { mutableStateOf<LocalProfile?>(null) }
                var screenState by remember { mutableStateOf("splash") }
                var isLoading by remember { mutableStateOf(false) }
                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    val saved = db.localProfileDao().getActiveProfile()
                    delay(2000)
                    if (saved != null) {
                        currentProfile = saved
                        screenState = "main_app"
                    } else {
                        screenState = "selection"
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showAR) {
                        ARScreen(arTitle, onOrderSent = { showAR = false }, onBack = { showAR = false })
                    } else {
                        when (screenState) {
                            "splash" -> SplashScreenLayout()

                            "selection" -> SelectionLayout { screenState = "setup_$it" }

                            "setup_CLIENTE", "setup_TIENDA" -> {
                                if (isLoading) {
                                    SplashScreenLayout()
                                } else {
                                    ProfileSetupLayout(stateKey = screenState) { name, email, phone, rubroId ->
                                        isLoading = true
                                        obtenerUbicacionActual { latReal, lngReal ->
                                            scope.launch {
                                                val role = if (screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"

                                                // Registrar en la nube
                                                val cloudResult = AdsGoNetwork.registerStoreWithProfile(
                                                    email = email,
                                                    storeName = name,
                                                    idCategory = rubroId,
                                                    lat = latReal,
                                                    lng = lngReal
                                                    // Nota: Tu función de red debe estar preparada para recibir el phone también
                                                )

                                                cloudResult.onSuccess { cloudUuid ->
                                                    val newProfile = LocalProfile(
                                                        id_perfil = cloudUuid,
                                                        name = name,
                                                        email = email,
                                                        phone = phone, // Se guarda el WhatsApp capturado
                                                        role = role,
                                                        category = rubroId.toString(),
                                                        lat = latReal,
                                                        lng = lngReal,
                                                        isLogged = true
                                                    )
                                                    db.localProfileDao().insertProfile(newProfile)
                                                    currentProfile = newProfile
                                                    screenState = "main_app"
                                                }.onFailure { error ->
                                                    println("Error en nube: ${error.message}")
                                                }
                                                isLoading = false
                                            }
                                        }
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
                                    if (currentProfile?.role == "CLIENTE") {
                                        ClientScreen(onBack = { screenState = "selection" }, onOpenAR = { arTitle = it; showAR = true })
                                    } else {
                                        StoreScreen(profile = currentProfile!!, onBack = { screenState = "selection" })
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
fun ProfileSetupLayout(stateKey: String, onComplete: (String, String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val role = if(stateKey.contains("CLIENTE")) "CLIENTE" else "TIENDA"

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Perfil $role", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (Opcional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("WhatsApp (Ej: 54911...)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if(name.isNotBlank() && phone.isNotBlank()) onComplete(name, email, phone, 1)
            },
            Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
        ) {
            Text("ACTIVAR MI RADAR", fontWeight = FontWeight.Bold)
        }
    }
}