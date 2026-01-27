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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
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

    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual(onLocationReady: (Double, Double) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            } else {
                onLocationReady(-12.0463, -77.0427) // Fallback Lima
            }
        }.addOnFailureListener {
            onLocationReady(-12.0463, -77.0427)
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
                var isLoading by remember { mutableStateOf(false) } // <--- Corregido: Variable faltante
                var showAR by remember { mutableStateOf(false) }
                var arTitle by remember { mutableStateOf("") }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
                    // Corregido: Usamos localProfileDao en vez de offerDao
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
                                    ProfileSetupLayout(stateKey = screenState) { name, email, rubroId ->
                                        isLoading = true
                                        obtenerUbicacionActual { latReal, lngReal ->
                                            scope.launch {
                                                val role = if (screenState.contains("CLIENTE")) "CLIENTE" else "TIENDA"

                                                val cloudResult = AdsGoNetwork.registerStoreWithProfile(
                                                    email = email,
                                                    storeName = name,
                                                    idCategory = rubroId,
                                                    lat = latReal,
                                                    lng = lngReal
                                                )

                                                cloudResult.onSuccess { cloudUuid ->
                                                    val newProfile = LocalProfile(
                                                        id_perfil = cloudUuid,
                                                        name = name,
                                                        email = email,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupLayout(stateKey: String, onComplete: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val rubros = listOf(
        "Gastronomía" to 1, "Moda" to 2, "Salud" to 3, "Belleza" to 4,
        "Bodega/Súper" to 5, "Deportes" to 6, "Tecnología" to 12
    )

    var selectedRubroName by remember { mutableStateOf(rubros[0].first) }
    var selectedRubroId by remember { mutableStateOf(rubros[0].second) }
    val role = if(stateKey.contains("CLIENTE")) "CLIENTE" else "TIENDA"

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Registro de $role", fontSize = 26.sp, fontWeight = FontWeight.Black, color = DeepBlueAds)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del comercio") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (BI ADSGO)") }, modifier = Modifier.fillMaxWidth())

        if (role == "TIENDA") {
            Spacer(Modifier.height(20.dp))
            Text("Categoría del Negocio:", fontWeight = FontWeight.Bold, color = DeepBlueAds)

            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedRubroName)
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    rubros.forEach { (nombre, id) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(nombre) },
                            onClick = {
                                selectedRubroName = nombre
                                selectedRubroId = id
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        fun generarAdsId(): String {
            // Tomamos los últimos 4 caracteres del ID de perfil para el ADS-XXXX
            val suffix = id_perfil.takeLast(4).uppercase()
            return "ADS-$suffix"
        }
        Spacer(Modifier.weight(1f))

        Button(
            onClick = { if(name.isNotBlank() && email.isNotBlank()) onComplete(name, email, selectedRubroId) },
            Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAds)
        ) {
            Text("VINCULAR CON ADSGO CLOUD")
        }
    }
}