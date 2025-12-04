package com.example.sensor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog

class HistorialAccesoActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var adapter: EventoAdapter
    private lateinit var api: UsuarioApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_acceso)

        api = UsuarioApiService(this)
        rvHistorial = findViewById(R.id.rv_historial)
        rvHistorial.layoutManager = LinearLayoutManager(this)
        adapter = EventoAdapter()
        rvHistorial.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val rol = SessionManager.getRole(this)
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.titleText = "Cargando..."
        pDialog.setCancelable(false)
        pDialog.show()

        // Cambia aquí la lógica:
        if (rol.equals("admin", ignoreCase = true)) {
            // Admin: consulta completa
            api.listarEventos(0,
                onSuccess = { lista ->
                    pDialog.dismissWithAnimation()
                    if (lista.isEmpty()) {
                        SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                            .setTitleText("Sin Registros")
                            .setContentText("No hay eventos de acceso registrados.")
                            .show()
                    } else {
                        adapter.setItems(lista)
                    }
                },
                onError = { msg ->
                    pDialog.dismissWithAnimation()
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText(msg)
                        .show()
                }
            )
        } else {
            // Usuario normal: eventos de su departamento
            val idDepto = SessionManager.getDeptId(this)
            api.listarEventosPorDepartamento(idDepto,
                onSuccess = { lista ->
                    pDialog.dismissWithAnimation()
                    if (lista.isEmpty()) {
                        SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                            .setTitleText("Sin Registros")
                            .setContentText("No hay eventos de acceso registrados para tu departamento.")
                            .show()
                    } else {
                        adapter.setItems(lista)
                    }
                },
                onError = { msg ->
                    pDialog.dismissWithAnimation()
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText(msg)
                        .show()
                }
            )
        }
    }
}