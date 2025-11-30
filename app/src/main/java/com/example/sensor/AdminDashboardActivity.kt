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

class AdminDashboardActivity : AppCompatActivity() {

    // Vistas del Admin Dashboard
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvDepartamentoInfo: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var btnGestionarUsuarios: MaterialButton
    private lateinit var btnGestionarSensores: MaterialButton
    private lateinit var btnHistorialAccesos: MaterialButton
    private lateinit var btnControlManual: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private val mHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cargar el layout del dashboard de ADMIN
        setContentView(R.layout.activity_admin_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin_dashboard)) { v, insets ->
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
        tvDepartamentoInfo = findViewById(R.id.tv_departamento_info) // Vista para info depto
        tvDateTime = findViewById(R.id.tv_datetime)

        // Referenciar los botones del layout de admin
        btnGestionarUsuarios = findViewById(R.id.btn_gestionar_usuarios)
        btnGestionarSensores = findViewById(R.id.btn_gestionar_sensores)
        btnHistorialAccesos = findViewById(R.id.btn_historial_accesos)
        btnControlManual = findViewById(R.id.btn_control_manual)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun cargarDatosUsuario() {
        // La lógica para obtener el nombre de usuario es la misma
        val nombreSesion = SessionManager.getFullName(this) ?: "Admin"
        tvNombreUsuario.text = nombreSesion

        // Aquí podrías cargar la info del departamento desde la sesión en el futuro
        // Por ahora, usamos un texto de ejemplo.
        tvDepartamentoInfo.text = "Departamento: 101-A"
    }

    private fun setupClickListeners() {
        // Asignar acciones a los nuevos botones
        btnGestionarUsuarios.setOnClickListener { startActivity(Intent(this, opciones_crud::class.java)) }
        btnGestionarSensores.setOnClickListener { startActivity(Intent(this, sensor::class.java)) }

        // TODO: Crear y enlazar las actividades para Historial y Control Manual
        btnHistorialAccesos.setOnClickListener {
            // startActivity(Intent(this, HistorialAccesosActivity::class.java))
        }
        btnControlManual.setOnClickListener {
            // startActivity(Intent(this, ControlManualActivity::class.java))
        }

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
                // Usamos el método centralizado de SessionManager
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