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
import android.widget.AdapterView

class IngresarSensor : AppCompatActivity() {

    // --- Vistas de la UI ---
    private lateinit var etCodigo: EditText
    private lateinit var tvDepartamentoFijo: TextView
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var btnRegistrar: Button

    // --- API y Datos ---
    private lateinit var api: UsuarioApiService
    private var usuarios: List<Usuario> = emptyList()
    private var idDeptoUsuario: Int = -1
    private var usuarioSeleccionadoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingresar_sensor)

        // 1. Obtenemos el ID del departamento del usuario en sesión. Esta es nuestra fuente de verdad.
        idDeptoUsuario = SessionManager.getDepartamentoId(this)

        // 2. Inicializamos las vistas y la API.
        api = UsuarioApiService(this)
        etCodigo = findViewById(R.id.txt_codigo_sensor)
        tvDepartamentoFijo = findViewById(R.id.tv_departamento_fijo)
        spinnerUsuario = findViewById(R.id.spinner_usuario_sensor)
        rgTipo = findViewById(R.id.rg_tipo_sensor)
        btnRegistrar = findViewById(R.id.btn_registro_sensor)

        // 3. Configuramos la pantalla y los listeners.
        configurarPantalla()
        btnRegistrar.setOnClickListener { registrarSensor() }
    }

    /**
     * Configura la pantalla: muestra el nombre del departamento y carga los usuarios correspondientes.
     * Esta función es el punto de partida de la lógica de la pantalla.
     */
    private fun configurarPantalla() {
        if (idDeptoUsuario == -1) {
            warn("Error de Sesión", "No se pudo obtener tu departamento. Por favor, inicia sesión de nuevo.")
            tvDepartamentoFijo.text = "Error: Sin departamento"
            return
        }

        // Primero, obtenemos el nombre del departamento para mostrarlo en el TextView.
        api.listarDepartamentos(
            onSuccess = { listaDepartamentos ->
                val deptoActual = listaDepartamentos.find { it.id == idDeptoUsuario }
                tvDepartamentoFijo.text = deptoActual?.nombre ?: "Departamento no encontrado"
            },
            onError = { tvDepartamentoFijo.text = "Error al cargar depto." }
        )

        // Luego, cargamos la lista de usuarios que pertenecen a ese departamento.
        cargarUsuariosDelDepto(idDeptoUsuario)
    }

    /**
     * Carga los usuarios de un departamento específico en el Spinner.
     */
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

                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        // Si se selecciona la primera opción ("Sin asignar"), el ID es 0.
                        // Si no, se busca el ID del usuario en la lista (restando 1 a la posición).
                        usuarioSeleccionadoId = if (position > 0 && position - 1 < usuarios.size) {
                            usuarios[position - 1].id
                        } else {
                            0
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = {
                spinnerUsuario.isEnabled = true
                warn("Atención", "No se pudieron cargar los usuarios de tu departamento.")
            }
        )
    }

    /**
     * Valida los datos y llama a la API para registrar el nuevo sensor.
     */
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
            idDepartamento = idDeptoUsuario, // Usa el ID del depto de la sesión.
            idUsuario = usuarioSeleccionadoId, // Usa el ID seleccionado en el spinner.
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Éxito!")
                    .setContentText("Sensor guardado correctamente.")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        finish() // Cierra la actividad al confirmar.
                    }
                    .show()
            },
            onError = { msg -> warn("Error", msg) }
        )
    }

    /**
     * Helper para mostrar una alerta de advertencia.
     */
    private fun warn(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText(title).setContentText(content).show()
    }
}