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

        // ✅ Solo continuar si hay sesión activa y usuario activo
        if (SessionManager.hasActiveSession(this) && SessionManager.isActivo(this)) {
            val sessionRole = SessionManager.getRol(this)
            redirigirSegunRol(sessionRole)
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
            } else false
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
            // MODIFICADO PARA RECIBIR EL ID_DEPTO
            onSuccess = { id, nombres, apellidos, email, rol, estado, idDepto ->
                // GUARDAMOS EL USUARIO CON SU DEPTO
                SessionManager.saveUser(this, id, nombres, apellidos, email, rol, estado, idDepto)
                setLoading(false)

                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Bienvenido")
                    .setContentText("$nombres $apellidos")
                    .setConfirmText("Ingresar")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        redirigirSegunRol(rol)
                    }
                    .show()
            },
            onError = { msg ->
                setLoading(false)
                showError("Error", msg)
            }
        )
    }

    private fun redirigirSegunRol(rol: String) {
        val intent: Intent = if (rol.equals("admin", ignoreCase = true)) {
            Intent(this, dashboardAdmin::class.java)
        } else {
            Intent(this, UserDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
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
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText(title).setContentText(content).show()
    }

    private fun showError(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setTitleText(title).setContentText(content).show()
    }
}