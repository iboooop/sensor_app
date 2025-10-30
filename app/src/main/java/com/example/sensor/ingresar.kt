package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class ingresar : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etClave: EditText
    private lateinit var etClaveRep: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingresar)

        api = UsuarioApiService(this)
        etNombres = findViewById(R.id.txtnombre)
        etApellidos = findViewById(R.id.txtapellido)
        etEmail = findViewById(R.id.txtemail)
        etClave = findViewById(R.id.txtclave)
        etClaveRep = findViewById(R.id.txtrepitaclave)
        btnRegistrar = findViewById(R.id.btn_registro)

        btnRegistrar.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nom = etNombres.text.toString().trim()
        val ape = etApellidos.text.toString().trim()
        val mail = etEmail.text.toString().trim()
        val pass = etClave.text.toString().trim()
        val pass2 = etClaveRep.text.toString().trim()

        // Validaciones
        if (nom.isEmpty() || ape.isEmpty() || mail.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            warn("Faltan datos", "Completa todos los campos")
            return
        }

        if (nom.length < 3 || ape.length < 3) {
            warn("Nombre inválido", "Nombre y apellido deben tener al menos 3 caracteres")
            return
        }

        // Validar que solo tenga letras y espacios
        val nombreRegex = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+\$".toRegex()
        if (!nombreRegex.matches(nom) || !nombreRegex.matches(ape)) {
            warn("Nombre inválido", "Nombre y apellido solo pueden contener letras y espacios")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            warn("Correo inválido", "Ingresa un correo válido")
            return
        }

        if (pass != pass2) {
            warn("Contraseña", "Las contraseñas no coinciden")
            return
        }

        if (!isStrongPassword(pass)) {
            warn(
                "Contraseña débil",
                "Debe tener mínimo 8 caracteres, incluir mayúscula, minúscula, número y símbolo"
            )
            return
        }

        // Llamada al backend
        api.crearUsuario(
            nombre = nom,
            apellido = ape,
            email = mail,
            password = pass,
            onSuccess = { idCreado ->
                // Éxito: redirigir al login (MainActivity)
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Registro exitoso")
                    .setContentText("Usuario creado correctamente. Inicia sesión para continuar.")
                    .setConfirmText("Ir al login")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }.show()
            },
            onError = { msg ->
                // Error del backend (correo ya existe, etc.)
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("No se pudo registrar")
                    .setContentText(msg)
                    .setConfirmText("OK")
                    .show()
            }
        )
    }

    private fun isStrongPassword(p: String): Boolean {
        if (p.length < 8) return false
        val hasUpper = p.any { it.isUpperCase() }
        val hasLower = p.any { it.isLowerCase() }
        val hasDigit = p.any { it.isDigit() }
        val hasSymbol = p.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSymbol
    }

    private fun warn(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .show()
    }
}
