package com.example.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import cn.pedant.SweetAlert.SweetAlertDialog
import org.json.JSONException
import org.json.JSONObject

class sensor : AppCompatActivity() {

    private lateinit var fecha: TextView
    private lateinit var temp: TextView
    private lateinit var hum: TextView
    private lateinit var imagenTemp: ImageView
    private lateinit var imagenAmpolleta: ImageView
    private lateinit var imagenLinterna: ImageView
    private lateinit var requestQueue: RequestQueue

    private lateinit var cameraManager: CameraManager
    private var cameraId: String = ""

    private val apiUrl = "https://www.pnk.cl/muestra_datos.php"
    private val mHandler = Handler(Looper.getMainLooper())

    private var linternaEncendida = false
    private var ampolletaEncendida = false
    private var tienePermisoLinterna = false

    private val PREFS_NAME = "sensor_prefs"
    private val AMPOLLETA_KEY = "ampolleta_encendida"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        tienePermisoLinterna = isGranted
        if (!isGranted) {
            mostrarAlertaError("Permiso de linterna denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fecha = findViewById(R.id.txt_fecha)
        temp = findViewById(R.id.txt_temp)
        hum = findViewById(R.id.txt_humedad)
        imagenTemp = findViewById(R.id.imagen_temp)
        imagenAmpolleta = findViewById(R.id.imagen_ampolleta)
        imagenLinterna = findViewById(R.id.imagen_linterna)

        // Inicializar CameraManager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.first { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        // Recuperar estado de ampolleta para la sesión
        ampolletaEncendida = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(AMPOLLETA_KEY, false)
        if (ampolletaEncendida) {
            imagenAmpolleta.setImageResource(R.drawable.ampolletaon)
        } else {
            imagenAmpolleta.setImageResource(R.drawable.ampolletaoff)
        }

        imagenLinterna.setImageResource(R.drawable.off)
        linternaEncendida = false

        requestQueue = Volley.newRequestQueue(this)
        configurarAmpolleta()
        configurarLinterna()
        mHandler.post(refrescar)
    }

    override fun onStart() {
        super.onStart()
        solicitarPermisoLinterna()
    }

    private fun solicitarPermisoLinterna() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            tienePermisoLinterna = true
        }
    }

    private fun configurarAmpolleta() {
        imagenAmpolleta.setOnClickListener {
            ampolletaEncendida = !ampolletaEncendida
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            prefs.putBoolean(AMPOLLETA_KEY, ampolletaEncendida).apply()

            if (ampolletaEncendida) {
                imagenAmpolleta.setImageResource(R.drawable.ampolletaon)
                mostrarAlertaExito("Ampolleta encendida")
            } else {
                imagenAmpolleta.setImageResource(R.drawable.ampolletaoff)
                mostrarAlertaExito("Ampolleta apagada")
            }
        }
    }

    private fun configurarLinterna() {
        imagenLinterna.setOnClickListener {
            if (!tienePermisoLinterna) {
                mostrarAlertaError("Permiso de cámara no concedido")
                return@setOnClickListener
            }
            linternaEncendida = !linternaEncendida
            if (linternaEncendida) {
                imagenLinterna.setImageResource(R.drawable.on)
                actualizarLinterna(true)
                mostrarAlertaExito("Linterna encendida")
            } else {
                imagenLinterna.setImageResource(R.drawable.off)
                actualizarLinterna(false)
                mostrarAlertaExito("Linterna apagada")
            }
        }
    }

    private fun actualizarLinterna(encender: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId, encender)
        } catch (e: Exception) {
            mostrarAlertaError("No se pudo cambiar la linterna: ${e.message}")
        }
    }

    private val refrescar = object : Runnable {
        override fun run() {
            fecha.text = obtenerFechaHora()
            obtenerDatos()
            mHandler.postDelayed(this, 1000)
        }
    }

    private fun obtenerFechaHora(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM YYYY, hh:mm:ss a")
        return dateFormat.format(calendar.time)
    }

    private fun obtenerDatos() {
        val request = JsonObjectRequest(
            com.android.volley.Request.Method.GET,
            apiUrl,
            null,
            { response: JSONObject ->
                try {
                    val temperatura = response.getString("temperatura")
                    val humedad = response.getString("humedad")

                    temp.text = "Temperatura: $temperatura °C"
                    hum.text = "Humedad: $humedad %"

                    cambiarImagenTemperatura(temperatura.toFloat())
                } catch (e: JSONException) {
                    mostrarAlertaError("Error al procesar datos: ${e.message}")
                }
            },
            { error: VolleyError ->
                mostrarAlertaError("Error de conexión: ${error.message}")
            }
        )
        requestQueue.add(request)
    }

    private fun cambiarImagenTemperatura(valor: Float) {
        if (valor > 20f) {
            imagenTemp.setImageResource(R.drawable.temperaturados) // alta
        } else {
            imagenTemp.setImageResource(R.drawable.tempbaja) // baja
        }
    }

    private fun mostrarAlertaError(mensaje: String) {
        runOnUiThread {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error")
                .setContentText(mensaje)
                .setConfirmText("Aceptar")
                .setConfirmClickListener { dialog -> dialog.dismissWithAnimation() }
                .show()
        }
    }

    private fun mostrarAlertaExito(mensaje: String) {
        runOnUiThread {
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Éxito")
                .setContentText(mensaje)
                .setConfirmText("Aceptar")
                .setConfirmClickListener { dialog -> dialog.dismissWithAnimation() }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(refrescar)
        try {
            cameraManager.setTorchMode(cameraId, false)
        } catch (_: Exception) {}
    }
}