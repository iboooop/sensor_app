package com.example.sensor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OpcionesCrudSensor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opciones_crudsensor)

        // Configurar botón INGRESAR SENSOR
        findViewById<MaterialButton>(R.id.btn_ingresar_sensor).setOnClickListener {
            // Cuando tengas IngresarSensor, descomenta esta línea:
             startActivity(Intent(this, IngresarSensor::class.java))
        }

        // Configurar botón LISTAR SENSORES
        findViewById<MaterialButton>(R.id.btn_listar_sensores).setOnClickListener {
            startActivity(Intent(this, ListarSensor::class.java))
        }
    }
}