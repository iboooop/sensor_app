package com.example.sensor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

/** Normaliza espacios: quita extremos y múltiples espacios */
private fun normalizeSpaces(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

/** Valida que solo haya letras y espacios (acentos incluidos), sin números ni símbolos */
private fun isValidName(value: String): Boolean {
    val normalized = normalizeSpaces(value)
    return normalized.matches(Regex("^[\\p{L}]+(?: [\\p{L}]+)*$"))
}

class IngresarUsuario : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etClave: EditText
    private lateinit var etClaveRep: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ingresarusuario)

        api = UsuarioApiService(this)

        etNombres = findViewById(R.id.txtnombre)
        etApellidos = findViewById(R.id.txtapellido)
        etEmail = findViewById(R.id.txtemail)
        etClave = findViewById(R.id.txtclave)
        etClaveRep = findViewById(R.id.txtrepitaclave)
        btnRegistrar = findViewById(R.id.btn_registro)

        btnRegistrar.setOnClickListener { registrarUsuarioAdmin() }
    }

    private fun registrarUsuarioAdmin() {
        val nom = normalizeSpaces(etNombres.text.toString())
        val ape = normalizeSpaces(etApellidos.text.toString())
        val mail = etEmail.text.toString().trim()
        val pass = etClave.text.toString().trim()
        val pass2 = etClaveRep.text.toString().trim()

        // Validaciones básicas
        if (nom.isEmpty() || ape.isEmpty() || mail.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            warn("Faltan datos", "Completa todos los campos")
            return
        }

        // Validar que solo letras y espacios
        if (!isValidName(nom)) {
            warn("Nombre inválido", "El nombre solo puede contener letras y espacios")
            return
        }
        if (!isValidName(ape)) {
            warn("Apellido inválido", "El apellido solo puede contener letras y espacios")
            return
        }

        // Validar correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            warn("Correo inválido", "Ingresa un correo válido")
            return
        }

        // Validar contraseña
        if (pass != pass2) {
            warn("Contraseña", "Las contraseñas no coinciden")
            return
        }
        if (pass.length < 8) {
            warn("Contraseña débil", "La contraseña debe tener mínimo 8 caracteres")
            return
        }

        // Registrar usuario
        api.ingresarUsuario(
            nombre = nom,
            apellido = ape,
            email = mail,
            password = pass,
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Usuario creado")
                    .setContentText("El usuario se registró correctamente")
                    .setConfirmText("OK")
                    .setConfirmClickListener { dlg ->
                        dlg.dismissWithAnimation()
                        finish() // vuelve a la pantalla anterior sin iniciar sesión
                    }.show()
            },
            onError = { msg ->
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText(msg)
                    .setConfirmText("OK")
                    .show()
            }
        )
    }

    private fun warn(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .show()
    }
}
