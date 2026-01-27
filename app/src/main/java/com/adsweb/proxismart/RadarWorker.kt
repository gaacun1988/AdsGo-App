package com.adsweb.proxismart

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadarWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // VERIFICACIÓN DE PERMISOS (Fixes the warning)
            val hasLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasLocation) {
                Log.e("ADSGO", "Radar falló: No hay permisos de ubicación")
                return@withContext Result.failure()
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = Tasks.await(fusedLocationClient.lastLocation)

            if (location != null) {
                // LLAMADA AL SAAS
                val response: HttpResponse = AdsGoNetwork.httpClient.post(AdsGoNetwork.ENDPOINT) {
                    // Import the extension from our new object
                    with(AdsGoNetwork) {
                        hasuraAuth()
                    }

                    // Sending the coordinates as JSON body
                    setBody(mapOf(
                        "lat" to location.latitude,
                        "lng" to location.longitude
                    ))
                }

                if (response.status.value in 200..299) {
                    Log.d("ADSGO", "Radar procesado: ${response.status}")
                    Result.success()
                } else {
                    Log.e("ADSGO", "Error de servidor: ${response.status}")
                    Result.retry()
                }
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ADSGO", "Radar falló: ${e.message}")
            Result.failure()
        }
    }
}