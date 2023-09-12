package com.example.eva3_prom

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.example.eva3_prom.ui.theme.Eva3_ProMTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AppVM: ViewModel() {
    val latitud   = mutableStateOf( 0.0)
    val longitud  = mutableStateOf( 0.0)

    var permisoUbicacionOk:() -> Unit = {}
}


class Mapa : ComponentActivity() {
    val appVM:AppVM by viewModels()

    val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (
            (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false) or
            (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
        ) {
            appVM.permisoUbicacionOk()
        } else {
            Log.v("lanzador permisos callback", "se denegaron los permisos")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppUI(appVM, lanzadorPermisos)
            }
        }
    }


class FaltaPermisosException(mensaje:String): Exception(mensaje)

fun conseguirUbicacion(contexto: Context, onSuccess:(ubicacion: Location) -> Unit) {
    try {
        val servicio = LocationServices.getFusedLocationProviderClient(contexto)
        val tarea = servicio.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
        tarea.addOnSuccessListener {
            onSuccess(it)
        }
    } catch (se:SecurityException) {
        throw  FaltaPermisosException("permiso de ubicacion")

    }
}
@Composable
fun AppUI(appVM: AppVM, lanzadorPermisos: ActivityResultLauncher<Array<String>>) {
    val contexto = LocalContext.current

    Column() {


        Button(onClick = {
            appVM.permisoUbicacionOk = {
                conseguirUbicacion(contexto) {
                    appVM.latitud.value = it.latitude
                    appVM.longitud.value = it.longitude
                }
            }

            lanzadorPermisos.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )


        }) {
            Text("conseguir ubicacion")
        }
        Text("Lat: ${appVM.latitud.value} Long: ${appVM.longitud.value}")
        Spacer(Modifier.height(100.dp))
        AndroidView(
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    Configuration.getInstance().userAgentValue = contexto.packageName
                    controller.setZoom(15.0)
                }
            } , update = {
                it.overlays.removeIf { true }
                it.invalidate()

                val geoPoint = GeoPoint(appVM.latitud.value, appVM.longitud.value)
                it.controller.animateTo(geoPoint)

                val marcador = Marker(it)
                marcador.position = geoPoint
                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                it.overlays.add(marcador)
            }
        )
    }
}

