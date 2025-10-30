package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.button.MaterialButton

class RecuperarContraseniaActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnRecuperar: MaterialButton
    private lateinit var etC1: EditText
    private lateinit var etC2: EditText
    private lateinit var etC3: EditText
    private lateinit var etC4: EditText
    private lateinit var etC5: EditText
    private lateinit var btnReenviar: MaterialButton
    private lateinit var tvTimer: TextView
    private lateinit var tvIngrese: TextView

    private lateinit var api: UsuarioApiService
    private var timer: CountDownTimer? = null

    // ⏱️ Guardamos el tiempo del último código enviado
    private var codigoExpiraEn: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasenia)

        api = UsuarioApiService(this)

        etEmail = findViewById(R.id.etEmail)
        btnRecuperar = findViewById(R.id.btnRecuperar)
        etC1 = findViewById(R.id.etC1)
        etC2 = findViewById(R.id.etC2)
        etC3 = findViewById(R.id.etC3)
        etC4 = findViewById(R.id.etC4)
        etC5 = findViewById(R.id.etC5)
        btnReenviar = findViewById(R.id.btnReenviar)
        tvTimer = findViewById(R.id.tvTimer)
        tvIngrese = findViewById(R.id.tv_ingrese_codigo)

        btnReenviar.isEnabled = false
        setupOtpInputs()

        btnRecuperar.setOnClickListener { solicitarCodigo() }

        btnReenviar.setOnClickListener {
            limpiarCode()
            solicitarCodigo() // reenviamos nuevo código
        }
    }

    private fun setupOtpInputs() {
        val boxes = arrayOf(etC1, etC2, etC3, etC4, etC5)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                when {
                    etC1.text.length == 1 && etC2.text.isEmpty() -> etC2.requestFocus()
                    etC2.text.length == 1 && etC3.text.isEmpty() -> etC3.requestFocus()
                    etC3.text.length == 1 && etC4.text.isEmpty() -> etC4.requestFocus()
                    etC4.text.length == 1 && etC5.text.isEmpty() -> etC5.requestFocus()
                }
                if (getCode().length == 5) verificarCodigo()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        boxes.forEach { it.addTextChangedListener(watcher) }

        val back = View.OnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                when (v.id) {
                    R.id.etC2 -> if (etC2.text.isEmpty()) { etC1.requestFocus(); etC1.text = null }
                    R.id.etC3 -> if (etC3.text.isEmpty()) { etC2.requestFocus(); etC2.text = null }
                    R.id.etC4 -> if (etC4.text.isEmpty()) { etC3.requestFocus(); etC3.text = null }
                    R.id.etC5 -> if (etC5.text.isEmpty()) { etC4.requestFocus(); etC4.text = null }
                }
            }
            false
        }

        etC2.setOnKeyListener(back)
        etC3.setOnKeyListener(back)
        etC4.setOnKeyListener(back)
        etC5.setOnKeyListener(back)
    }

    private fun solicitarCodigo() {
        val email = etEmail.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            warn("Correo inválido", "Ingresa un correo válido")
            return
        }

        setUiEnabled(false)
        api.enviarCodigoRecuperacion(
            email = email,
            onSuccess = {
                ok("Código enviado", "Te hemos enviado un nuevo código de 5 dígitos. Revisa tu correo.")
                mostrarSeccionCodigo()
                iniciarTimer()
                setUiEnabled(true)
            },
            onError = { msg ->
                setUiEnabled(true)
                error("No se pudo enviar", msg)
            }
        )
    }

    private fun verificarCodigo() {
        val email = etEmail.text.toString().trim()
        val code = getCode()
        if (code.length != 5) return

        // ⏰ Validar si el código expiró
        if (System.currentTimeMillis() > codigoExpiraEn) {
            error("Código expirado", "El código que ingresaste ya expiró. Solicita uno nuevo.")
            limpiarCode()
            return
        }

        setUiEnabled(false)
        api.verificarCodigo(
            email = email,
            codigo = code,
            onSuccess = {
                setUiEnabled(true)
                val i = Intent(this, CrearContraseniaActivity::class.java).apply {
                    putExtra("correo", email)
                    putExtra("codigo", code)
                }
                startActivity(i)
                finish()
            },
            onError = { msg ->
                setUiEnabled(true)
                error("Código inválido", msg)
                limpiarCode()
            }
        )
    }

    private fun getCode(): String =
        "${etC1.text}${etC2.text}${etC3.text}${etC4.text}${etC5.text}".trim()

    private fun limpiarCode() {
        etC1.text = null
        etC2.text = null
        etC3.text = null
        etC4.text = null
        etC5.text = null
        etC1.requestFocus()
    }

    private fun mostrarSeccionCodigo() {
        findViewById<View>(R.id.code_container).visibility = View.VISIBLE
        tvTimer.visibility = View.VISIBLE
        tvIngrese.visibility = View.VISIBLE
        btnReenviar.visibility = View.VISIBLE
        etC1.requestFocus()
    }

    private fun iniciarTimer() {
        timer?.cancel()
        btnReenviar.isEnabled = false
        tvTimer.text = "60 segundos"

        // 🔒 El código expira en 60 segundos desde ahora
        codigoExpiraEn = System.currentTimeMillis() + 60_000

        timer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(ms: Long) {
                tvTimer.text = "${(ms / 1000).toInt()} segundos"
            }

            override fun onFinish() {
                tvTimer.text = "Puedes reenviar el código"
                btnReenviar.isEnabled = true
            }
        }.start()
    }

    private fun setUiEnabled(enabled: Boolean) {
        btnRecuperar.isEnabled = enabled
        etEmail.isEnabled = enabled
        btnReenviar.isEnabled = enabled && tvTimer.text == "Puedes reenviar el código"
        etC1.isEnabled = enabled
        etC2.isEnabled = enabled
        etC3.isEnabled = enabled
        etC4.isEnabled = enabled
        etC5.isEnabled = enabled
    }

    private fun warn(t: String, c: String) =
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(t)
            .setContentText(c)
            .setConfirmText("OK")
            .show()

    private fun error(t: String, c: String) =
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(t)
            .setContentText(c)
            .setConfirmText("OK")
            .show()

    private fun ok(t: String, c: String) =
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(t)
            .setContentText(c)
            .setConfirmText("OK")
            .show()

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
