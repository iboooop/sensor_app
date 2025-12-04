package com.example.sensor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class IngresarSensor : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    private var departamentos: List<Departamento> = emptyList()
    private var usuarios: List<Usuario> = emptyList()

    private var departamentoSeleccionadoId: Int = 0
    private var usuarioSeleccionadoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingresar_sensor)

        api = UsuarioApiService(this)

        etCodigo = findViewById(R.id.txt_codigo_sensor)
        spinnerDepartamento = findViewById(R.id.spinner_departamento_sensor)
        spinnerUsuario = findViewById(R.id.spinner_usuario_sensor)
        rgTipo = findViewById(R.id.rg_tipo_sensor)
        btnRegistrar = findViewById(R.id.btn_registro_sensor)

        cargarDepartamentos()
        btnRegistrar.setOnClickListener { registrarSensor() }
    }

    private fun cargarDepartamentos() {
        api.listarDepartamentos(
            onSuccess = { lista ->
                departamentos = lista
                val nombres = mutableListOf("Seleccione ubicación...")
                nombres.addAll(lista.map { it.nombre })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDepartamento.adapter = adapter

                spinnerDepartamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == 0) {
                            departamentoSeleccionadoId = 0
                            limpiarUsuarios()
                        } else {
                            departamentoSeleccionadoId = departamentos[position - 1].id
                            cargarUsuariosDelDepto(departamentoSeleccionadoId)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = { warn("Error", "No se cargaron departamentos") }
        )
    }

    private fun cargarUsuariosDelDepto(idDepto: Int) {
        val loadingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Cargando usuarios..."))
        spinnerUsuario.adapter = loadingAdapter
        spinnerUsuario.isEnabled = false

        // AHORA SÍ FUNCIONARÁ PORQUE LA AGREGAMOS EN EL APISERVICE
        api.listarUsuariosPorDepto(idDepto,
            onSuccess = { lista ->
                usuarios = lista
                spinnerUsuario.isEnabled = true

                val nombres = mutableListOf("Sin asignar (Disponible)")
                if (lista.isEmpty()) {
                    nombres.add("(No hay usuarios en este depto)")
                } else {
                    // Mapeamos usando las propiedades correctas de Data Class Usuario
                    nombres.addAll(lista.map { "${it.nombre} ${it.apellido}" })
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == 0 || lista.isEmpty()) {
                            usuarioSeleccionadoId = 0
                        } else {
                            // El índice 1 del spinner es el usuario 0 de la lista
                            if (position - 1 < usuarios.size) {
                                usuarioSeleccionadoId = usuarios[position - 1].id
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = {
                spinnerUsuario.isEnabled = true
                warn("Atención", "No se pudieron cargar los usuarios")
            }
        )
    }

    private fun limpiarUsuarios() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Seleccione primero un depto"))
        spinnerUsuario.adapter = adapter
        spinnerUsuario.isEnabled = false
        usuarioSeleccionadoId = 0
    }

    private fun registrarSensor() {
        val codigo = etCodigo.text.toString().trim()
        if (codigo.isEmpty()) { warn("Faltan datos", "Ingrese código"); return }
        if (departamentoSeleccionadoId == 0) { warn("Ubicación", "Seleccione departamento"); return }

        val selectedTipoId = rgTipo.checkedRadioButtonId
        val tipo = if (selectedTipoId == R.id.rb_llavero) "llavero" else "tarjeta"

        api.ingresarSensor(
            codigoSensor = codigo,
            tipo = tipo,
            estado = "activo",
            idDepartamento = departamentoSeleccionadoId,
            idUsuario = usuarioSeleccionadoId,
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Éxito!")
                    .setContentText("Sensor guardado")
                    .setConfirmClickListener { it.dismissWithAnimation(); finish() }
                    .show()
            },
            onError = { msg -> warn("Error", msg) }
        )
    }

    private fun warn(t: String, c: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText(t).setContentText(c).show()
    }
}