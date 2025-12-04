package com.example.sensor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog

// Implementamos la interfaz del Adapter para escuchar los clics
class HistorialAccesoActivity : AppCompatActivity(), EventoAdapter.OnEventoAdminActionsListener {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var adapter: EventoAdapter
    private lateinit var api: UsuarioApiService
    private var idUsuarioActual: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_acceso)

        api = UsuarioApiService(this)
        idUsuarioActual = SessionManager.getUserId(this)
        val esAdmin = SessionManager.getRole(this).equals("admin", ignoreCase = true)

        rvHistorial = findViewById(R.id.rv_historial)
        rvHistorial.layoutManager = LinearLayoutManager(this)

        // Pasamos el rol y el listener al crear el adaptador
        adapter = EventoAdapter(esAdmin, this)
        rvHistorial.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val rol = SessionManager.getRole(this)
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Cargando..."
            setCancelable(false)
            show()
        }

        val onSuccess: (List<Evento>) -> Unit = { lista ->
            pDialog.dismissWithAnimation()
            if (lista.isEmpty()) {
                SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                    .setTitleText("Sin Registros")
                    .setContentText("No hay eventos de acceso para mostrar.")
                    .show()
            } else {
                adapter.setItems(lista)
            }
        }

        val onError: (String) -> Unit = { msg ->
            pDialog.dismissWithAnimation()
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error")
                .setContentText(msg)
                .show()
        }

        if (rol.equals("admin", ignoreCase = true)) {
            // Admin ve todos los eventos
            api.listarEventos(0, onSuccess, onError)
        } else {
            // Operador ve solo los de su depto
            val idDepto = SessionManager.getDepartamentoId(this)
            api.listarEventosPorDepartamento(idDepto, onSuccess, onError)
        }
    }

    // --- MANEJO DE ACCIONES DEL ADMIN ---

    override fun onAceptarAcceso(evento: Evento) {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Abriendo barrera..."
            setCancelable(false)
            show()
        }
        // Usamos la función que ya creamos para el control manual
        api.enviarComandoBarrera("ABRIR", idUsuarioActual,
            onSuccess = {
                pDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                pDialog.titleText = "¡Acceso concedido!"
                pDialog.setContentText("La orden para abrir la barrera ha sido enviada.")
                pDialog.confirmText = "OK"
                pDialog.setConfirmClickListener { it.dismissWithAnimation() }
                // Podríamos llamar a un script para actualizar el evento a "APROBADO"
            },
            onError = { msg ->
                pDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                pDialog.titleText = "Error"
                pDialog.setContentText(msg)
                pDialog.confirmText = "OK"
                pDialog.setConfirmClickListener { it.dismissWithAnimation() }
            }
        )
    }

    override fun onRechazarAcceso(evento: Evento) {
        // Aquí podrías llamar a una función de la API para marcar el evento como "revisado".
        // Por ahora, solo mostramos una confirmación.
        SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
            .setTitleText("Acceso Rechazado")
            .setContentText("El acceso para '${evento.usuarioNombre}' ha sido denegado.")
            .show()
    }
}