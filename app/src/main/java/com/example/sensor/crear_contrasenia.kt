package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.button.MaterialButton

class CrearContraseniaActivity : AppCompatActivity() {

    private lateinit var etPass1: EditText
    private lateinit var etPass2: EditText
    private lateinit var btnCrear: MaterialButton
    private lateinit var api: UsuarioApiService

    private var correo: String = ""
    private var codigo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usa tu layout crear_contrasenia (el nombre del archivo en res/layout)
        setContentView(R.layout.activity_crear_contrasenia)

        api = UsuarioApiService(this)

        etPass1 = findViewById(R.id.etPass1)
        etPass2 = findViewById(R.id.etPass2)
        btnCrear = findViewById(R.id.btnCrear)

        // Recibe de RecuperarContraseniaActivity
        correo = intent.getStringExtra("correo").orEmpty()
        codigo = intent.getStringExtra("codigo").orEmpty()

        if (correo.isBlank() || codigo.isBlank()) {
            alertError("Datos inválidos", "Faltan correo o código. Vuelve a solicitar el código.") {
                finish()
            }
            return
        }

        btnCrear.setOnClickListener { crear() }

        etPass2.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                crear(); true
            } else false
        }
    }

    private fun crear() {
        val p1 = etPass1.text.toString().trim()
        val p2 = etPass2.text.toString().trim()

        if (p1.isEmpty() || p2.isEmpty()) { alertWarn("Faltan datos", "Completa ambas contraseñas"); return }
        if (p1 != p2) { alertWarn("Contraseña", "Las contraseñas no coinciden"); return }

        val strengthMsg = passwordStrengthMessage(p1)
        if (strengthMsg != null) {
            alertWarn("Contraseña débil", strengthMsg); return
        }

        btnCrear.isEnabled = false
        api.crearContrasenia(
            email = correo,
            codigo = codigo,
            nueva = p1,
            onSuccess = {
                btnCrear.isEnabled = true
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Contraseña actualizada")
                    .setContentText("Ya puedes iniciar sesión")
                    .setConfirmText("Ir al login")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .show()
            },
            onError = { msg ->
                btnCrear.isEnabled = true
                alertError("No se pudo actualizar", msg)
            }
        )
    }

    // Reglas: mínimo 8, mayúscula, minúscula, número y símbolo
    private fun passwordStrengthMessage(p: String): String? {
        val missing = mutableListOf<String>()
        if (p.length < 8) missing += "al menos 8 caracteres"
        if (!p.any { it.isUpperCase() }) missing += "una mayúscula"
        if (!p.any { it.isLowerCase() }) missing += "una minúscula"
        if (!p.any { it.isDigit() }) missing += "un número"
        if (!p.any { !it.isLetterOrDigit() }) missing += "un símbolo"
        return if (missing.isEmpty()) null
        else "La contraseña debe contener ${missing.joinToString(", ")}."
    }

    private fun alertWarn(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title).setContentText(content).setConfirmText("OK").show()
    }
    private fun alertError(title: String, content: String, onOk: (() -> Unit)? = null) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title).setContentText(content).setConfirmText("OK")
            .setConfirmClickListener { it.dismissWithAnimation(); onOk?.invoke() }
            .show()
    }
}