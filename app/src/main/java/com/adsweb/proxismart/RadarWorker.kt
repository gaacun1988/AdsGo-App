package com.adsweb.proxismart

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadarWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener el cliente de ubicación
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // 2. Obtener la última ubicación conocida (Sincrónico para el Worker)
            val locationTask = fusedLocationClient.lastLocation
            val location = Tasks.await(locationTask)

            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                // 3. Consultar Supabase (Llamada a la lógica ST_DWithin de tu PDF)
                // Usamos Postgrest para buscar en la tabla de notificaciones vigentes
                val response = SupabaseManager.client.postgrest["notificacion_geo"]
                    .select {
                        filter {
                            // Aquí es donde entra tu query de cercanía
                            // Nota: Para una query compleja como ST_DWithin lo ideal es usar una RPC
                            Log.d("AdsGoRadar", "Buscando ofertas para: $lat, $lng")
                        }
                    }

                // 4. Si hay ofertas cerca, podríamos disparar una notificación local
                Log.d("AdsGoRadar", "Radar ejecutado exitosamente")
                Result.success()
            } else {
                Log.e("AdsGoRadar", "No se pudo obtener la ubicación actual")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("AdsGoRadar", "Error en el radar: ${e.message}")
            Result.failure()
        }
    }
}