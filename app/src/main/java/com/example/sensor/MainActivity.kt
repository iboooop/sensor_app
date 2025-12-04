package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import cn.pedant.SweetAlert.SweetAlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var etUser: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var api: UsuarioApiService

    private var lastWarnAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_layout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Si ya hay sesión, ir directo al dashboard
        if (SessionManager.hasActiveSession(this)) {
            startActivity(Intent(this, dashboardAdmin::class.java))
            finish()
            return
        }

        api = UsuarioApiService(this)
        etUser = findViewById(R.id.etUser)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener { intentarLogin() }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, RecuperarContraseniaActivity::class.java))
        }

        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin()
                true
            } else {
                false
            }
        }
    }

    private fun intentarLogin() {
        val correo = etUser.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (correo.isEmpty() || pass.isEmpty()) {
            showWarnOnce("Faltan datos", "Ingresa correo y contraseña")
            return
        }

        setLoading(true)
        api.login(
            correo = correo,
            password = pass,
            // ===== CORRECCIÓN PRINCIPAL AQUÍ =====
            // Ahora recibimos 'departamentoId' desde la API
            onSuccess = { id, nombres, apellidos, email, rol, estado, departamentoId ->

                // Y lo usamos para guardar la sesión completa en SessionManager
                SessionManager.saveUser(this, id, nombres, apellidos, email, rol, estado, departamentoId)
                // ===================================

                val mensaje = when (rol) {
                    "admin" -> "Bienvenido Administrador"
                    "operador" -> "Bienvenido Operador"
                    else -> "Bienvenido"
                }

                val i = Intent(this, dashboardAdmin::class.java).apply {
                    putExtra("usuario_nombre", "$nombres $apellidos")
                    putExtra("usuario_rol", rol)
                    putExtra("usuario_estado", estado)
                }

                setLoading(false)

                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText(mensaje)
                    .setContentText("$nombres $apellidos")
                    .setConfirmText("Continuar")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        startActivity(i)
                        finish()
                    }
                    .show()
            },
            onError = { msg ->
                setLoading(false)
                val (titulo, contenido) = when {
                    msg.contains("inactivo", ignoreCase = true) ->
                        "Usuario Inactivo" to "Tu cuenta está desactivada. Contacta al administrador."
                    msg.contains("contraseña", ignoreCase = true) || msg.contains("incorrecta", ignoreCase = true) ->
                        "Contraseña Incorrecta" to "Verifica tu contraseña e intenta nuevamente."
                    msg.contains("no encontrado", ignoreCase = true) ->
                        "Usuario no encontrado" to "El correo ingresado no está registrado."
                    msg.contains("credenciales", ignoreCase = true) ->
                        "Credenciales Incorrectas" to "Usuario o contraseña incorrectos."
                    else ->
                        "Error de inicio de sesión" to msg
                }
                showError(titulo, contenido)
            }
        )
    }

    private fun setLoading(b: Boolean) {
        btnLogin.isEnabled = !b
        etUser.isEnabled = !b
        etPassword.isEnabled = !b
    }

    private fun showWarnOnce(title: String, content: String) {
        val now = System.currentTimeMillis()
        if (now - lastWarnAt < 1500) return
        lastWarnAt = now
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .show()
    }

    private fun showError(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .show()
    }
}