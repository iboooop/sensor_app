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

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvDepartamentoInfo: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var btnHistorialAccesos: MaterialButton
    private lateinit var btnControlManual: MaterialButton // <-- 1. Declaramos el botón nuevo
    private lateinit var btnLogout: MaterialButton

    private val mHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_usuario)

        // Ajustar padding para la barra de estado (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_user_dashboard)) { v, insets ->
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
        tvNombreUsuario = findViewById(R.id.tv_nombre_usuario)
        tvDepartamentoInfo = findViewById(R.id.tv_departamento_info)
        tvDateTime = findViewById(R.id.tv_datetime)
        btnHistorialAccesos = findViewById(R.id.btn_historial_accesos)
        btnControlManual = findViewById(R.id.btn_control_manual) // <-- 2. Vinculamos con el XML
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun cargarDatosUsuario() {
        // Obtenemos el nombre real de la sesión
        val nombreSesion = SessionManager.getFullName(this) ?: "Usuario"
        tvNombreUsuario.text = nombreSesion
        // Aquí podrías cargar el departamento real si lo tienes en SessionManager
        tvDepartamentoInfo.text = "Departamento: 101-A"
    }

    private fun setupClickListeners() {
        // Botón Historial
        btnHistorialAccesos.setOnClickListener {
            startActivity(Intent(this, HistorialAccesoActivity::class.java))
        }

        // Botón Control Manual (--- NUEVO ---)
        btnControlManual.setOnClickListener {
            // Redirige a la misma actividad de Control Manual que usa el Admin
            startActivity(Intent(this, ControlManualActivity::class.java))
        }

        // Botón Salir
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
                // Usamos tu método para limpiar sesión y volver al login
                SessionManager.logoutAndGoToLogin(this)
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