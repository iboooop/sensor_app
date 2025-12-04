package com.example.sensor

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import android.widget.AdapterView // Asegúrate de que este import esté presente

class IngresarSensor : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var tvDepartamentoFijo: TextView
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    private var usuarios: List<Usuario> = emptyList()
    private var idDeptoUsuario: Int = -1
    private var usuarioSeleccionadoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingresar_sensor)

        api = UsuarioApiService(this)
        idDeptoUsuario = SessionManager.getDepartamentoId(this)

        etCodigo = findViewById(R.id.txt_codigo_sensor)
        tvDepartamentoFijo = findViewById(R.id.tv_departamento_fijo)
        spinnerUsuario = findViewById(R.id.spinner_usuario_sensor)
        rgTipo = findViewById(R.id.rg_tipo_sensor)
        btnRegistrar = findViewById(R.id.btn_registro_sensor)

        configurarDepartamentoYUsuarios()
        btnRegistrar.setOnClickListener { registrarSensor() }
    }

    private fun configurarDepartamentoYUsuarios() {
        if (idDeptoUsuario == -1) {
            warn("Error de Sesión", "No se pudo obtener tu departamento. Inicia sesión de nuevo.")
            tvDepartamentoFijo.text = "Error: Sin departamento"
            return
        }

        api.listarDepartamentos(
            onSuccess = { listaDepartamentos ->
                val deptoActual = listaDepartamentos.find { it.id == idDeptoUsuario }
                tvDepartamentoFijo.text = deptoActual?.nombre ?: "Departamento no encontrado"
            },
            onError = { tvDepartamentoFijo.text = "Error al cargar depto." }
        )

        cargarUsuariosDelDepto(idDeptoUsuario)
    }

    private fun cargarUsuariosDelDepto(idDepto: Int) {
        val loadingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Cargando usuarios..."))
        spinnerUsuario.adapter = loadingAdapter
        spinnerUsuario.isEnabled = false

        api.listarUsuariosPorDepto(idDepto,
            onSuccess = { lista ->
                usuarios = lista
                spinnerUsuario.isEnabled = true

                val nombres = mutableListOf("Sin asignar (Disponible)")
                if (lista.isEmpty()) {
                    nombres.add("(No hay usuarios en este depto)")
                } else {
                    nombres.addAll(lista.map { "${it.nombre} ${it.apellido}" })
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                // ===== CORRECCIÓN AQUÍ =====
                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    // El primer parámetro debe ser de tipo AdapterView<*>?, no AdapterView.OnItemSelectedListener
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        usuarioSeleccionadoId = if (position > 0 && position - 1 < usuarios.size) {
                            usuarios[position - 1].id
                        } else {
                            0
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                // ============================
            },
            onError = {
                spinnerUsuario.isEnabled = true
                warn("Atención", "No se pudieron cargar los usuarios de tu departamento.")
            }
        )
    }

    private fun registrarSensor() {
        val codigo = etCodigo.text.toString().trim()
        if (codigo.isEmpty()) {
            warn("Faltan datos", "Ingresa el código del sensor")
            return
        }

        if (idDeptoUsuario == -1) {
            warn("Error de Sesión", "No se pudo identificar tu departamento.")
            return
        }

        val selectedTipoId = rgTipo.checkedRadioButtonId
        val tipo = if (selectedTipoId == R.id.rb_llavero) "llavero" else "tarjeta"

        api.ingresarSensor(
            codigoSensor = codigo,
            tipo = tipo,
            estado = "activo",
            idDepartamento = idDeptoUsuario,
            idUsuario = usuarioSeleccionadoId,
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Éxito!")
                    .setContentText("Sensor guardado correctamente.")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        finish()
                    }
                    .show()
            },
            onError = { msg -> warn("Error", msg) }
        )
    }

    private fun warn(t: String, c: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText(t).setContentText(c).show()
    }
}