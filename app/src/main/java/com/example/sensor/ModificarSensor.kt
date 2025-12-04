package com.example.sensor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class ModificarSensor : AppCompatActivity() {

    // --- Vistas ---
    private lateinit var etCodigo: EditText
    private lateinit var tvDepartamentoFijo: TextView
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var tvEstadoActual: TextView
    private lateinit var btnModificar: Button
    private lateinit var btnDesactivar: Button
    private lateinit var btnPerdido: Button
    private lateinit var btnActivar: Button

    // --- API y Diálogos ---
    private lateinit var api: UsuarioApiService
    private var loadingDlg: SweetAlertDialog? = null

    // --- Datos ---
    private var sensorId: Int = -1
    private var estadoActual: String = ""
    private var nombreDeptoOriginal: String = ""
    private var nombreUsuarioOriginal: String = ""
    private var usuarios: List<Usuario> = emptyList()
    private var usuarioSeleccionadoId: Int = 0

    // ¡LA CLAVE! Obtenemos el ID del depto de la sesión, igual que en IngresarSensor.
    private var idDeptoUsuario: Int = -1
    private var esCargaInicialUsuario: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_sensor)

        // Obtenemos el ID del depto de la sesión. ¡Nuestra fuente de verdad!
        idDeptoUsuario = SessionManager.getDepartamentoId(this)

        inicializarVistas()

        if (!obtenerDatosDelIntent()) {
            alertError("Error de Datos", "No se pudo cargar la información del sensor.") { finish() }
            return
        }

        // La configuración ahora es simple y directa.
        configurarPantalla()

        configurarListeners()
    }

    private fun inicializarVistas() {
        api = UsuarioApiService(this)
        etCodigo = findViewById(R.id.txt_codigo_sensor)
        tvDepartamentoFijo = findViewById(R.id.tv_departamento_fijo)
        spinnerUsuario = findViewById(R.id.spinner_usuario)
        rgTipo = findViewById(R.id.rg_tipo)
        tvEstadoActual = findViewById(R.id.tv_estado_actual)
        btnModificar = findViewById(R.id.btn_modificar)
        btnDesactivar = findViewById(R.id.btn_desactivar)
        btnPerdido = findViewById(R.id.btn_perdido)
        btnActivar = findViewById(R.id.btn_activar)
    }

    private fun obtenerDatosDelIntent(): Boolean {
        sensorId = intent.getIntExtra("sensor_id", -1)
        if (sensorId == -1) return false

        etCodigo.setText(intent.getStringExtra("sensor_codigo") ?: "")
        estadoActual = intent.getStringExtra("sensor_estado") ?: "activo"
        nombreDeptoOriginal = intent.getStringExtra("sensor_depto") ?: "Sin Departamento"
        nombreUsuarioOriginal = intent.getStringExtra("sensor_usuario") ?: "Sin Asignar"

        val tipo = intent.getStringExtra("sensor_tipo") ?: "llavero"
        if (tipo.equals("tarjeta", ignoreCase = true)) rgTipo.check(R.id.rb_tarjeta)
        else rgTipo.check(R.id.rb_llavero)

        return true
    }

    // Lógica simplificada: usa los datos que ya tiene.
    private fun configurarPantalla() {
        actualizarBotonesEstado()
        tvDepartamentoFijo.text = nombreDeptoOriginal // Mostramos el nombre que viene del Intent.

        // Verificamos el ID de la sesión.
        if (idDeptoUsuario > 0) {
            // Usamos el ID de la sesión para cargar los usuarios, igual que en IngresarSensor.
            cargarUsuariosDelDepto(idDeptoUsuario)
        } else {
            alertWarn("Error de Sesión", "No se pudo identificar tu departamento.")
            limpiarUsuarios()
        }
    }

    // Esta función ahora es idéntica a la de IngresarSensor.
    private fun cargarUsuariosDelDepto(idDepto: Int) {
        val loadingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Cargando..."))
        spinnerUsuario.adapter = loadingAdapter
        spinnerUsuario.isEnabled = false

        api.listarUsuariosPorDepto(idDepto,
            onSuccess = { lista ->
                usuarios = lista
                spinnerUsuario.isEnabled = true

                val nombres = mutableListOf("Sin asignar")
                nombres.addAll(lista.map { "${it.nombre} ${it.apellido}" })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                // Preseleccionar el usuario original del sensor
                if (esCargaInicialUsuario && nombreUsuarioOriginal != "Sin Asignar") {
                    val index = usuarios.indexOfFirst { "${it.nombre} ${it.apellido}".equals(nombreUsuarioOriginal, ignoreCase = true) }
                    if (index != -1) {
                        spinnerUsuario.setSelection(index + 1)
                    }
                }
                esCargaInicialUsuario = false

                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
                alertWarn("Atención", "No se pudieron cargar los usuarios de este departamento.")
            }
        )
    }

    private fun limpiarUsuarios() {
        spinnerUsuario.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("No disponible"))
        spinnerUsuario.isEnabled = false
        usuarioSeleccionadoId = 0
    }

    private fun guardarCambios() {
        val codigo = etCodigo.text.toString().trim()
        if (codigo.isEmpty()) { alertWarn("Faltan datos", "El código no puede estar vacío"); return }

        // Usamos el ID del depto de la sesión.
        if (idDeptoUsuario == -1) { alertError("Error", "No se ha podido identificar tu departamento."); return }

        val tipo = if (rgTipo.checkedRadioButtonId == R.id.rb_tarjeta) "tarjeta" else "llavero"

        showLoading("Guardando", "Actualizando información...")
        api.modificarSensor(
            sensorId, codigo, tipo, estadoActual, idDeptoUsuario, usuarioSeleccionadoId,
            onSuccess = {
                dismissLoading()
                alertOk("¡Éxito!", "Sensor actualizado correctamente.") { finish() }
            },
            onError = { msg ->
                dismissLoading()
                alertError("Error al Guardar", msg)
            }
        )
    }

    // --- El resto de funciones auxiliares no necesitan cambios ---
    private fun configurarListeners() {
        btnModificar.setOnClickListener { guardarCambios() }
        btnDesactivar.setOnClickListener { cambiarEstado("inactivo") }
        btnPerdido.setOnClickListener { cambiarEstado("perdido") }
        btnActivar.setOnClickListener { cambiarEstado("activo") }
    }

    private fun actualizarBotonesEstado() {
        tvEstadoActual.text = "Estado Actual: ${estadoActual.uppercase()}"
        btnDesactivar.visibility = View.GONE
        btnPerdido.visibility = View.GONE
        btnActivar.visibility = View.GONE
        if (estadoActual == "activo") {
            btnDesactivar.visibility = View.VISIBLE
            btnPerdido.visibility = View.VISIBLE
        } else {
            btnActivar.visibility = View.VISIBLE
        }
    }

    private fun cambiarEstado(nuevoEstado: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Cambiar estado?")
            .setContentText("Nuevo estado: ${nuevoEstado.uppercase()}")
            .setConfirmText("Sí, cambiar")
            .setCancelText("Cancelar")
            .setConfirmClickListener { dlg ->
                dlg.dismissWithAnimation()
                showLoading("Procesando", "Cambiando estado...")
                api.cambiarEstadoSensor(sensorId, nuevoEstado,
                    onSuccess = {
                        dismissLoading()
                        estadoActual = nuevoEstado
                        actualizarBotonesEstado()
                        alertOk("Actualizado", "El sensor ahora está $nuevoEstado")
                    },
                    onError = { msg ->
                        dismissLoading()
                        alertError("Error", msg)
                    }
                )
            }.show()
    }

    private fun showLoading(t: String, c: String) {
        dismissLoading()
        loadingDlg = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).setTitleText(t).setContentText(c)
        loadingDlg?.setCancelable(false)
        loadingDlg?.show()
    }
    private fun dismissLoading() { loadingDlg?.dismissWithAnimation(); loadingDlg = null }
    private fun alertWarn(t: String, c: String) = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText(t).setContentText(c).show()
    private fun alertError(t: String, c: String, onOk: (() -> Unit)? = null) = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setTitleText(t).setContentText(c).setConfirmClickListener { it.dismissWithAnimation(); onOk?.invoke() }.show()
    private fun alertOk(t: String, c: String, onOk: (() -> Unit)? = null) = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE).setTitleText(t).setContentText(c).setConfirmClickListener { it.dismissWithAnimation(); onOk?.invoke() }.show()
}