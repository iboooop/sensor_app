package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import cn.pedant.SweetAlert.SweetAlertDialog

class dashboardAdmin : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var btnCrudUser: MaterialButton
    private lateinit var btnSensor: MaterialButton
    private lateinit var btnHistorial: MaterialButton // <-- NUEVO
    private lateinit var btnControlManual: MaterialButton // <-- NUEVO
    private lateinit var btnDeveloper: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private val mHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboardadmin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_dashboard)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        cargarDatosUsuario()
        setupClickListeners()
        actualizarFechaHora()
    }

    private fun initializeViews() {
        tvBienvenida = findViewById(R.id.tv_bienvenida)
        tvNombreUsuario = findViewById(R.id.tv_nombre_usuario)
        tvDateTime = findViewById(R.id.tv_datetime)
        btnCrudUser = findViewById(R.id.btn_crud_user)
        btnSensor = findViewById(R.id.btn_sensor)
        btnHistorial = findViewById(R.id.btn_historial) // <-- NUEVO
        btnControlManual = findViewById(R.id.btn_control_manual) // <-- NUEVO
        btnDeveloper = findViewById(R.id.btn_developer)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun cargarDatosUsuario() {
        val nombreSesion = SessionManager.getFullName(this) ?: "Usuario"
        tvNombreUsuario.text = nombreSesion
    }

    private fun setupClickListeners() {
        // 1. Botón Gestión de Usuarios
        btnCrudUser.setOnClickListener {
            startActivity(Intent(this, opciones_crudusuario::class.java))
        }

        // 2. Botón Gestión de Sensores
        btnSensor.setOnClickListener {
            startActivity(Intent(this, OpcionesCrudSensor::class.java))
        }

        // 3. Botón Historial de Accesos (NUEVO)
        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialAccesoActivity::class.java))
        }

        // 4. Botón Control Manual (NUEVO)
        btnControlManual.setOnClickListener {
            // DEBES CREAR ESTA ACTIVIDAD: ControlManualActivity.kt y su layout
            // Y añadirla al AndroidManifest.xml
            startActivity(Intent(this, ControlManualActivity::class.java))
        }

        // 5. Botón Desarrollador
        btnDeveloper.setOnClickListener {
            startActivity(Intent(this, desarrollador::class.java))
        }

        // 6. Botón Salir
        btnLogout.setOnClickListener { confirmarLogout() }
    }

    private fun confirmarLogout() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Cerrar sesión")
            .setContentText("¿Deseas salir de tu cuenta?")
            .setCancelText("Cancelar")
            .setConfirmText("Cerrar sesión")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                SessionManager.clear(this)
                val i = Intent(this, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
                finish()
            }
            .setCancelClickListener { dialog -> dialog.dismissWithAnimation() }
            .show()
    }

    private fun actualizarFechaHora() {
        updateRunnable = object : Runnable {
            override fun run() {
                val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault())
                tvDateTime.text = "Fecha/Hora: ${sdf.format(Date())}"
                mHandler.postDelayed(this, 1000)
            }
        }
        updateRunnable?.let { mHandler.post(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { mHandler.removeCallbacks(it) }
    }
}