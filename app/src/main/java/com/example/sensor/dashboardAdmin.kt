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
    private lateinit var btnDeveloper: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private val mHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboardadmin) // Asegúrate que este sea el nombre correcto de tu XML (dashboard.xml o activity_dashboard.xml)

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
        btnDeveloper = findViewById(R.id.btn_developer)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun cargarDatosUsuario() {
        val nombreDirecto = intent.getStringExtra("usuario_nombre")?.takeIf { it.isNotBlank() }
        val nombresExtra = intent.getStringExtra("nombres")?.trim().orEmpty()
        val apellidosExtra = intent.getStringExtra("apellidos")?.trim().orEmpty()
        val nombreCombinadoExtras = "$nombresExtra $apellidosExtra".trim().takeIf { it.isNotBlank() }
        val nombreSesion = SessionManager.getFullName(this)?.takeIf { it.isNotBlank() }
        val nombreParaMostrar = nombreDirecto ?: nombreCombinadoExtras ?: nombreSesion ?: "Usuario"
        tvNombreUsuario.text = nombreParaMostrar
    }

    private fun setupClickListeners() {
        // Botón Gestión de Usuarios
        btnCrudUser.setOnClickListener {
            // Asegúrate de usar el nombre correcto de la clase (revisamos que sea con mayúscula si la cambiaste)
            startActivity(Intent(this, opciones_crudusuario::class.java))
        }

        // Botón Gestión de Sensores -> Ahora va a OpcionesCrudSensor
        btnSensor.setOnClickListener {
            startActivity(Intent(this, OpcionesCrudSensor::class.java))
        }

        // Botón Desarrollador
        btnDeveloper.setOnClickListener {
            startActivity(Intent(this, EventosOpciones::class.java))
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