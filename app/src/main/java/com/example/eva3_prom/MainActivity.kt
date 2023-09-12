package com.example.eva3_prom

import android.R.attr.contentDescription
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime


enum class Pantalla {
    FORM,
    FOTO
}

class CamAppViewModel : ViewModel() {
    val pantalla = mutableStateOf(Pantalla.FORM)

    //call back
    var PermisoOnCamaraOk: () -> Unit = {}
    var PermisoOnUbicacionOk: () -> Unit = {}

    //Lanzador permisos
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null

    fun cambiarPantFoto() {
        pantalla.value = Pantalla.FOTO
    }

    fun cambiarPantForm() {
        pantalla.value = Pantalla.FORM
    }
}


class RecepcionViewModelForm : ViewModel() {
    val receptor = mutableStateOf("")
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val fotoRecepcion = mutableStateOf<Uri?>(null)
}




class MainActivity : ComponentActivity() {
    val cameraAppVm: CamAppViewModel by viewModels()
    lateinit var cameraController: LifecycleCameraController
    val Permisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            when {
                (it[android.Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION]
                    ?: false) -> {
                    Log.v("callback ResquestMultiplePermissions", "permiso ubicacion granted")
                    cameraAppVm.PermisoOnUbicacionOk()
                }

                (it[android.Manifest.permission.CAMERA] ?: false) -> {
                    Log.v("Callback RequestMultiplePermissions", "permiso camara Granted")
                    cameraAppVm.PermisoOnCamaraOk()
                }

                else -> {
                }
            }
        }
    private fun CamaraSetup() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        cameraAppVm.lanzadorPermisos = Permisos
      CamaraSetup()

        super.onCreate(savedInstanceState)
        setContent {
            AppUI(cameraController)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun NombrePorFechaSeg(): String =
    LocalDateTime.now().toString().toString()
        .replace(Regex("[T:.-]"), "").substring(0, 14)


fun uriImageBitmap(uri: Uri, contexto: Context) = BitmapFactory.decodeStream(
    contexto.contentResolver.openInputStream(uri)
).asImageBitmap()


fun ArchivoImagenPrivado(contexto: Context): File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${NombrePorFechaSeg()}.jpg"
)

/*  fun crearArchivoImagenPublico(contexto: Context): File {
  val contexto = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
  val archivoImagen = File(contexto
      , "${contexto}.jpg")
  return archivoImagen

 // codigo para guardar las fotos publicas

}*/

fun FotoCapturada(
    cameraController: CameraController,
    archivo: File,
    contexto: Context,
    imagenGuardadaOk: (uri: Uri) -> Unit
) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()

    cameraController.takePicture(
        outputFileOptions, ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also { uri ->
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${uri.toString()}")
                    imagenGuardadaOk(uri)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        }
    )
}

class SinPermisoException(mensaje: String) : Exception(mensaje)

fun UbicacionG(contexto: Context, onUbicacionOk: (location: Location) -> Unit) {

    try {
        val servicio = LocationServices.getFusedLocationProviderClient(contexto)
        val tarea = servicio.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        tarea.addOnSuccessListener {
            onUbicacionOk(it)
        }
    } catch (e: SecurityException) {
        throw SinPermisoException(
            e.message ?: "No tiene permisos para conseguir la ubicacion"
        )
    }
}

@Composable
fun AppUI(cameraController: CameraController) {
    val contexto = LocalContext.current

    val cameraAppViewModel: CamAppViewModel = viewModel()
    val formRecepcionVm: RecepcionViewModelForm = viewModel()

    when (cameraAppViewModel.pantalla.value) {
        Pantalla.FORM -> {
            PantallaUI(
                formRecepcionVm,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(
                        arrayOf(android.Manifest.permission.CAMERA)
                    )
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.PermisoOnUbicacionOk = {
                        UbicacionG(contexto) {
                            formRecepcionVm.latitud.value = it.latitude
                            formRecepcionVm.longitud.value = it.longitude
                        }
                    }
                    cameraAppViewModel.lanzadorPermisos?.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
        Pantalla.FOTO -> {
            PantFotoUI(formRecepcionVm, cameraAppViewModel, cameraController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun PantallaUI(
    formRecepcionVm: RecepcionViewModelForm,
    tomarFotoOnClick: () -> Unit = {},
    actualizarUbicacionOnClick: () -> Unit = {}
) {
    val contexto = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            TextField(
                label = { Text("Lugar de la fotografia") },
                value = formRecepcionVm.receptor.value,
                onValueChange = { formRecepcionVm.receptor.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Button(
                onClick = {
                    tomarFotoOnClick()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Capturar Momento")
            }

            formRecepcionVm.fotoRecepcion.value?.also {
                Box(Modifier.size(200.dp, 100.dp)) {
                    Image(
                        painter = BitmapPainter(
                            uriImageBitmap(
                                it,
                                contexto

                            )
                        ),
                        contentDescription = "Imagen recepcionada ${formRecepcionVm.receptor.value}"
                    )
                }

                Text(
                    "La ubicación es: lat: ${formRecepcionVm.latitud.value} y long: ${formRecepcionVm.longitud.value}",
                    modifier = Modifier.padding(top = 16.dp)
                )

                Button(
                    onClick = {
                        actualizarUbicacionOnClick()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Actualizar Ubicación")
                }

                Spacer(modifier = Modifier.height(100.dp))
                MapOsmUI(formRecepcionVm.latitud.value, formRecepcionVm.longitud.value)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
@Composable
fun PantFotoUI(
    formRecepcionVm: RecepcionViewModelForm,
    appViewModel: CamAppViewModel,
    cameraController: CameraController
) {
    val contexto = LocalContext.current

    AndroidView(
        factory = {
            PreviewView(it).apply { controller = cameraController }
        },

        modifier = Modifier.fillMaxSize()
    )
    Button(onClick = {
        FotoCapturada(
            cameraController,
            ArchivoImagenPrivado
            /*crearArchivoImagenPublico*/(contexto),
            contexto
        ) {
            formRecepcionVm.fotoRecepcion.value = it
            appViewModel.cambiarPantForm()
        }
    }) {
        Text("Tomar Foto")
    }
}



@Composable
fun MapOsmUI(latitud: Double, longitud: Double) {
    val contexto = LocalContext.current

    AndroidView(
        factory = {
            MapView(it).also {
                it.setTileSource(TileSourceFactory.MAPNIK)
                Configuration.getInstance().userAgentValue =
                    contexto.packageName

            }
        },  update = {
            it.overlays.removeIf { true }
            it.invalidate()

            it.controller.setZoom(18.0)
            val geoPoint = GeoPoint(latitud, longitud)
            it.controller.animateTo(geoPoint)

            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.overlays.add(marcador)

        }
    )
}

