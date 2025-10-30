package com.example.sensor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class opciones_crud : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opciones_crud)

        findViewById<MaterialButton>(R.id.btn_ingresar_usuarios).setOnClickListener {
            startActivity(Intent(this, IngresarUsuario::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_listar_usuarios).setOnClickListener {
            startActivity(Intent(this, listar::class.java))
        }
    }
}