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
    private lateinit var imagenLinternaOn: ImageView
    private lateinit var imagenLinternaOff: ImageView
    private lateinit var requestQueue: RequestQueue

    private lateinit var cameraManager: CameraManager
    private var cameraId: String = ""

    private val apiUrl = "https://www.pnk.cl/muestra_datos.php"
    private val mHandler = Handler(Looper.getMainLooper())

    private var linternaEncendida = false
    private var tienePermisoLinterna = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        tienePermisoLinterna = isGranted
        if (isGranted) {
            mostrarAlertaExito("Permiso de linterna concedido")
        } else {
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

        // Inicializar CameraManager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.first { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        initializeViews()
        requestQueue = Volley.newRequestQueue(this)
        configurarLinterna()
        mHandler.post(refrescar)
    }

    override fun onStart() {
        super.onStart()
        // 🔁 Pedir permiso de linterna cada vez que se abre la app
        solicitarPermisoLinterna()
    }

    private fun initializeViews() {
        fecha = findViewById(R.id.txt_fecha)
        temp = findViewById(R.id.txt_temp)
        hum = findViewById(R.id.txt_humedad)
        imagenTemp = findViewById(R.id.imagen_temp)
        imagenLinternaOn = findViewById(R.id.imagen_linterna_on)
        imagenLinternaOff = findViewById(R.id.imagen_linterna_off)

        linternaEncendida = getSharedPreferences("sensor_prefs", MODE_PRIVATE)
            .getBoolean("linterna_encendida", false)
    }

    private fun solicitarPermisoLinterna() {
        // 🔁 Siempre pedimos el permiso al abrir la app
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun configurarLinterna() {
        imagenLinternaOn.setOnClickListener {
            if (!tienePermisoLinterna) {
                mostrarAlertaError("Permiso de cámara no concedido")
                return@setOnClickListener
            }
            linternaEncendida = true
            actualizarLinterna()
        }

        imagenLinternaOff.setOnClickListener {
            if (!tienePermisoLinterna) {
                mostrarAlertaError("Permiso de cámara no concedido")
                return@setOnClickListener
            }
            linternaEncendida = false
            actualizarLinterna()
        }
    }

    private fun actualizarLinterna() {
        try {
            cameraManager.setTorchMode(cameraId, linternaEncendida)
            val estado = if (linternaEncendida) "Encendida" else "Apagada"
            mostrarAlertaExito("Linterna $estado")

            getSharedPreferences("sensor_prefs", MODE_PRIVATE).edit()
                .putBoolean("linterna_encendida", linternaEncendida)
                .apply()
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

                    val valor = temperatura.toFloat()
                    cambiarImagenTemperatura(valor)
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
        imagenTemp.setImageResource(
            if (valor >= 20) R.drawable.temperaturados else R.drawable.tempbaja
        )
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
