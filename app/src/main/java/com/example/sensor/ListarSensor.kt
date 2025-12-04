package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import java.util.Locale

class ListarSensor : AppCompatActivity() {

    private lateinit var rvSensores: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var api: UsuarioApiService
    private lateinit var adapter: SensorRvAdapter

    private var sensores = mutableListOf<Sensor>()
    private var visibles = mutableListOf<Sensor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_sensor)

        api = UsuarioApiService(this)
        rvSensores = findViewById(R.id.lista_sensores)
        etSearch = findViewById(R.id.search_sensores)

        rvSensores.layoutManager = LinearLayoutManager(this)

        // Clic en el item -> abrirModificar
        adapter = SensorRvAdapter { s -> abrirModificar(s) }
        rvSensores.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filtrar(s?.toString().orEmpty()) }
        })
    }

    override fun onStart() {
        super.onStart()
        cargar()
    }

    private fun cargar() {
        api.listarSensores(
            onSuccess = { data ->
                sensores = data.toMutableList()
                visibles = data.toMutableList()
                adapter.setItems(visibles)
            },
            onError = { msg ->
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText(msg)
                    .setConfirmText("OK").show()
            }
        )
    }

    private fun filtrar(q: String) {
        val needle = q.trim().lowercase(Locale.getDefault())
        visibles = if (needle.isEmpty()) sensores.toMutableList() else sensores.filter { s ->
            s.codigo.lowercase().contains(needle) ||
                    s.departamentoNombre.lowercase().contains(needle) ||
                    s.tipo.lowercase().contains(needle)
        }.toMutableList()
        adapter.setItems(visibles)
    }

    private fun abrirModificar(s: Sensor) {
        val i = Intent(this, ModificarSensor::class.java).apply {
            putExtra("sensor_id", s.id)
            putExtra("sensor_codigo", s.codigo)
            putExtra("sensor_tipo", s.tipo)
            putExtra("sensor_estado", s.estado)
            putExtra("sensor_depto", s.departamentoNombre)
            putExtra("sensor_usuario", s.usuarioNombre)
        }
        startActivity(i)
    }
}