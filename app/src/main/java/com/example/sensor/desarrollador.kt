package com.example.sensor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class desarrollador : AppCompatActivity() {

    // Desarrollador 1
    private lateinit var tvNombreDev1: TextView
    private lateinit var tvCargoDev1: TextView
    private lateinit var tvCorreoDev1: TextView
    private lateinit var tvCarreraDev1: TextView
    private lateinit var iconGithubDev1: ImageView
    private lateinit var iconLinkedinDev1: ImageView

    // Desarrollador 2
    private lateinit var tvNombreDev2: TextView
    private lateinit var tvCargoDev2: TextView
    private lateinit var tvCorreoDev2: TextView
    private lateinit var tvCarreraDev2: TextView
    private lateinit var iconGithubDev2: ImageView
    private lateinit var iconLinkedinDev2: ImageView

    private val dev1 = mapOf(
        "nombre" to "TAÍS CRUZ CEPEDA",
        "cargo" to "Desarrolladora Full Stack",
        "correo" to "tais.cruz@ejemplo.com",
        "carrera" to "Ingeniería en Informática - INACAP",
        "github" to "https://github.com/iboooop",
        "linkedin" to "https://linkedin.com/"
    )

    private val dev2 = mapOf(
        "nombre" to "CATALINA ARANCIBIA PIZARRO",
        "cargo" to "QA Y PM",
        "correo" to "catalina.arancibia@ejemplo.com",
        "carrera" to "Ingeniería en Informática - INACAP",
        "github" to "https://github.com/CatalinaArancibia",
        "linkedin" to "https://linkedin.com/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_desarrollador)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        cargarDatos()
        setupClickListeners()
    }

    private fun initializeViews() {
        // Desarrollador 1
        tvNombreDev1 = findViewById(R.id.tv_nombre_dev1)
        tvCargoDev1 = findViewById(R.id.tv_cargo_dev1)
        tvCorreoDev1 = findViewById(R.id.tv_correo_dev1)
        tvCarreraDev1 = findViewById(R.id.tv_carrera_dev1)
        iconGithubDev1 = findViewById(R.id.icon_github_dev1)
        iconLinkedinDev1 = findViewById(R.id.icon_linkedin_dev1)

        // Desarrollador 2
        tvNombreDev2 = findViewById(R.id.tv_nombre_dev2)
        tvCargoDev2 = findViewById(R.id.tv_cargo_dev2)
        tvCorreoDev2 = findViewById(R.id.tv_correo_dev2)
        tvCarreraDev2 = findViewById(R.id.tv_carrera_dev2)
        iconGithubDev2 = findViewById(R.id.icon_github_dev2)
        iconLinkedinDev2 = findViewById(R.id.icon_linkedin_dev2)
    }

    private fun cargarDatos() {
        // Dev 1
        tvNombreDev1.text = dev1["nombre"]
        tvCargoDev1.text = dev1["cargo"]
        tvCorreoDev1.text = dev1["correo"]
        tvCarreraDev1.text = dev1["carrera"]

        // Dev 2
        tvNombreDev2.text = dev2["nombre"]
        tvCargoDev2.text = dev2["cargo"]
        tvCorreoDev2.text = dev2["correo"]
        tvCarreraDev2.text = dev2["carrera"]
    }

    private fun setupClickListeners() {
        iconGithubDev1.setOnClickListener { abrirEnlace(dev1["github"] ?: "") }
        iconLinkedinDev1.setOnClickListener { abrirEnlace(dev1["linkedin"] ?: "") }

        iconGithubDev2.setOnClickListener { abrirEnlace(dev2["github"] ?: "") }
        iconLinkedinDev2.setOnClickListener { abrirEnlace(dev2["linkedin"] ?: "") }
    }

    private fun abrirEnlace(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "Enlace no disponible", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No hay app para abrir el enlace", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }
}