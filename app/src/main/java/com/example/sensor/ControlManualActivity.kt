package com.example.sensor

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.button.MaterialButton

class ControlManualActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var btnAccion: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var api: UsuarioApiService

    private var idUsuario: Int = -1
    private var estadoActual: String = ""
    private var isLoading = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusChecker: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_manual)

        api = UsuarioApiService(this)
        idUsuario = SessionManager.getUserId(this)

        tvEstado = findViewById(R.id.tv_estado_barrera)
        btnAccion = findViewById(R.id.btn_accion_barrera)
        progressBar = findViewById(R.id.progress_bar)

        btnAccion.setOnClickListener { enviarComando() }

        // Runnable para verificar el estado periódicamente
        statusChecker = Runnable {
            if (!isLoading) {
                consultarEstado()
            }
            handler.postDelayed(statusChecker, 5000) // Revisa cada 5 segundos
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(statusChecker) // Inicia la verificación al mostrar la pantalla
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusChecker) // Detiene la verificación al ocultar la pantalla
    }

    private fun consultarEstado() {
        if (isLoading) return
        setLoading(true)

        api.consultarEstadoBarrera(
            onSuccess = { estado ->
                estadoActual = estado
                actualizarUI()
                setLoading(false)
            },
            onError = { errorMsg ->
                // No mostramos alerta en cada fallo para no molestar al usuario.
                // Podríamos poner un icono de "sin conexión".
                setLoading(false)
            }
        )
    }

    private fun actualizarUI() {
        if (estadoActual.equals("ABIERTA", ignoreCase = true)) {
            tvEstado.text = "ABIERTA"
            tvEstado.setTextColor(Color.parseColor("#4CAF50")) // Verde
            btnAccion.text = "CERRAR BARRERA"
            btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F")) // Rojo
        } else { // "CERRADA" o cualquier otro estado por defecto
            tvEstado.text = "CERRADA"
            tvEstado.setTextColor(Color.parseColor("#D32F2F")) // Rojo
            btnAccion.text = "ABRIR BARRERA"
            btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50")) // Verde
        }
    }

    private fun enviarComando() {
        if (idUsuario == -1) {
            alertError("Error de Sesión", "No se pudo identificar al usuario.")
            return
        }

        val comandoAEnviar = if (estadoActual.equals("ABIERTA", ignoreCase = true)) "CERRAR" else "ABRIR"
        setLoading(true)

        api.enviarComandoBarrera(comandoAEnviar, idUsuario,
            onSuccess = {
                // Comando enviado con éxito. Esperamos un poco para que el estado se actualice.
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Éxito!")
                    .setContentText("Comando '$comandoAEnviar' enviado.")
                    .hideConfirmButton()
                    .show()

                // Ocultar la alerta y refrescar el estado después de un momento
                Handler(Looper.getMainLooper()).postDelayed({
                    (window.decorView.rootView.findFocus() as? SweetAlertDialog)?.dismissWithAnimation()
                    consultarEstado()
                }, 2000)
            },
            onError = { errorMsg ->
                setLoading(false)
                alertError("Error", errorMsg)
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnAccion.isEnabled = !loading
    }

    private fun alertError(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .show()
    }
}