package com.example.sensor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

// Asumimos que tienes estas Data Class o similares
// data class Usuario(val id_usuario: Int, val nombre: String, val apellido: String)
// data class Departamento(val id: Int, val nombre: String)

class IngresarSensor : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    // Listas para los spinners
    private var departamentos: List<Departamento> = emptyList()
    private var usuarios: List<Usuario> = emptyList()

    // IDs seleccionados
    private var departamentoSeleccionadoId: Int = 0
    private var usuarioSeleccionadoId: Int = 0 // 0 significa sin asignar o NULL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingresar_sensor) // Asegúrate que el nombre del layout coincida

        api = UsuarioApiService(this)

        // Vincular vistas
        etCodigo = findViewById(R.id.txt_codigo_sensor)
        spinnerDepartamento = findViewById(R.id.spinner_departamento_sensor)
        spinnerUsuario = findViewById(R.id.spinner_usuario_sensor)
        rgTipo = findViewById(R.id.rg_tipo_sensor)
        btnRegistrar = findViewById(R.id.btn_registro_sensor)

        // Cargar datos iniciales
        cargarDepartamentos()
        cargarUsuarios() // Necesario para el nuevo Spinner

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
                        departamentoSeleccionadoId = if (position == 0) 0 else departamentos[position - 1].id
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = {
                // Opcional: Mostrar error o dejar vacío si falla
                warn("Error", "No se cargaron departamentos")
            }
        )
    }

    private fun cargarUsuarios() {
        // ASUMIENDO que agregaste listarUsuarios en tu API Service
        api.listarUsuarios(
            onSuccess = { lista ->
                usuarios = lista
                // Opción por defecto para no asignar usuario
                val nombres = mutableListOf("Sin asignar (Disponible)")
                // Formato "Juan Perez" en el spinner
                nombres.addAll(lista.map { "${it.nombre} ${it.apellido}" })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        usuarioSeleccionadoId = if (position == 0) {
                            0 // Sin asignar
                        } else {
                            usuarios[position - 1].id // Asegúrate que tu modelo tenga este campo
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = {
                warn("Atención", "No se pudo cargar la lista de usuarios")
            }
        )
    }

    private fun registrarSensor() {
        val codigo = etCodigo.text.toString().trim()

        // Validar Código
        if (codigo.isEmpty()) {
            warn("Faltan datos", "Debe ingresar un código para el sensor")
            return
        }

        // Validar Departamento
        if (departamentoSeleccionadoId == 0) {
            warn("Ubicación", "Debe seleccionar un departamento")
            return
        }

        // Nota: usuarioSeleccionadoId puede ser 0, eso es válido (sensor libre)

        // Obtener Tipo
        val selectedTipoId = rgTipo.checkedRadioButtonId
        val tipo = if (selectedTipoId == R.id.rb_llavero) "llavero" else "tarjeta"

        // Llamada a API
        api.ingresarSensor(
            codigoSensor = codigo,
            tipo = tipo,
            estado = "activo", // Por defecto al crear
            idDepartamento = departamentoSeleccionadoId,
            idUsuario = usuarioSeleccionadoId, // Envíamos el ID (0 o valor)
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Registrado!")
                    .setContentText("Sensor agregado correctamente")
                    .setConfirmText("OK")
                    .setConfirmClickListener { dlg ->
                        dlg.dismissWithAnimation()
                        finish() // Cerrar actividad
                    }.show()
            },
            onError = { msg ->
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText(msg)
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