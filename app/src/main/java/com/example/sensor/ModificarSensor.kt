package com.example.sensor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class ModificarSensor : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var spinnerUsuario: Spinner
    private lateinit var rgTipo: RadioGroup
    private lateinit var tvEstadoActual: TextView

    private lateinit var btnModificar: Button
    private lateinit var btnDesactivar: Button
    private lateinit var btnPerdido: Button
    private lateinit var btnActivar: Button

    private lateinit var api: UsuarioApiService
    private var loadingDlg: SweetAlertDialog? = null

    private var sensorId: Int = -1
    private var estadoActual: String = ""

    private var nombreDeptoOriginal: String = ""
    private var nombreUsuarioOriginal: String = ""
    private var esCargaInicialDepto: Boolean = true
    private var esCargaInicialUsuario: Boolean = true

    private var departamentos: List<Departamento> = emptyList()
    private var usuarios: List<Usuario> = emptyList()

    private var departamentoSeleccionadoId: Int = 0
    private var usuarioSeleccionadoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_sensor)

        api = UsuarioApiService(this)

        etCodigo = findViewById(R.id.txt_codigo_sensor)
        spinnerDepartamento = findViewById(R.id.spinner_departamento)
        spinnerUsuario = findViewById(R.id.spinner_usuario)
        rgTipo = findViewById(R.id.rg_tipo)
        tvEstadoActual = findViewById(R.id.tv_estado_actual)

        btnModificar = findViewById(R.id.btn_modificar)
        btnDesactivar = findViewById(R.id.btn_desactivar)
        btnPerdido = findViewById(R.id.btn_perdido)
        btnActivar = findViewById(R.id.btn_activar)

        sensorId = intent.getIntExtra("sensor_id", -1)
        val codigo = intent.getStringExtra("sensor_codigo") ?: ""
        val tipo = intent.getStringExtra("sensor_tipo") ?: "llavero"
        estadoActual = intent.getStringExtra("sensor_estado") ?: "activo"
        nombreDeptoOriginal = intent.getStringExtra("sensor_depto") ?: ""
        nombreUsuarioOriginal = intent.getStringExtra("sensor_usuario") ?: ""

        if (sensorId == -1) {
            alertError("Error", "ID inválido") { finish() }
            return
        }

        etCodigo.setText(codigo)
        actualizarBotonesEstado()

        if (tipo.equals("tarjeta", ignoreCase = true)) rgTipo.check(R.id.rb_tarjeta)
        else rgTipo.check(R.id.rb_llavero)

        cargarDepartamentos()

        btnModificar.setOnClickListener { guardarCambios() }
        btnDesactivar.setOnClickListener { cambiarEstado("inactivo") }
        btnPerdido.setOnClickListener { cambiarEstado("perdido") }
        btnActivar.setOnClickListener { cambiarEstado("activo") }
    }

    private fun cargarDepartamentos() {
        showLoading("Cargando", "Obteniendo departamentos...")
        api.listarDepartamentos(
            onSuccess = { lista ->
                departamentos = lista
                val nombres = mutableListOf("Seleccione ubicación...")
                nombres.addAll(lista.map { it.nombre })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDepartamento.adapter = adapter

                if (esCargaInicialDepto && nombreDeptoOriginal.isNotEmpty()) {
                    val index = departamentos.indexOfFirst { it.nombre.equals(nombreDeptoOriginal, ignoreCase = true) }
                    if (index >= 0) spinnerDepartamento.setSelection(index + 1)
                    esCargaInicialDepto = false
                }

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
                if (departamentoSeleccionadoId == 0) dismissLoading()
            },
            onError = {
                dismissLoading()
                alertWarn("Error", "No se cargaron departamentos")
            }
        )
    }

    private fun cargarUsuariosDelDepto(idDepto: Int) {
        val loadingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Cargando..."))
        spinnerUsuario.adapter = loadingAdapter
        spinnerUsuario.isEnabled = false

        api.listarUsuariosPorDepto(idDepto,
            onSuccess = { lista ->
                dismissLoading()
                usuarios = lista
                spinnerUsuario.isEnabled = true

                val nombres = mutableListOf("Sin asignar")
                nombres.addAll(lista.map { "${it.nombre} ${it.apellido}" })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                if (esCargaInicialUsuario && nombreUsuarioOriginal.isNotEmpty() && nombreUsuarioOriginal != "Sin Asignar") {
                    val index = usuarios.indexOfFirst { "${it.nombre} ${it.apellido}".equals(nombreUsuarioOriginal, ignoreCase = true) }
                    if (index >= 0) spinnerUsuario.setSelection(index + 1)
                    esCargaInicialUsuario = false
                }

                spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        usuarioSeleccionadoId = if (position == 0 || usuarios.isEmpty()) 0
                        else usuarios[position - 1].id
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            onError = {
                dismissLoading()
                spinnerUsuario.isEnabled = true
                alertWarn("Error", "Error cargando usuarios")
            }
        )
    }

    private fun limpiarUsuarios() {
        spinnerUsuario.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("-- Seleccione Depto --"))
        spinnerUsuario.isEnabled = false
        usuarioSeleccionadoId = 0
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

    private fun guardarCambios() {
        val codigo = etCodigo.text.toString().trim()
        if (codigo.isEmpty()) { alertWarn("Faltan datos", "Ingrese código"); return }
        if (departamentoSeleccionadoId == 0) { alertWarn("Ubicación", "Seleccione departamento"); return }

        val tipo = if (rgTipo.checkedRadioButtonId == R.id.rb_tarjeta) "tarjeta" else "llavero"

        showLoading("Guardando", "Actualizando sensor...")
        // Aquí usamos la función genérica para modificar TODO
        api.modificarSensor(
            sensorId, codigo, tipo, estadoActual, departamentoSeleccionadoId, usuarioSeleccionadoId,
            onSuccess = {
                dismissLoading()
                alertOk("¡Éxito!", "Sensor actualizado") { finish() }
            },
            onError = { msg ->
                dismissLoading()
                alertError("Error", msg)
            }
        )
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

                // USAMOS LA FUNCIÓN ESPECÍFICA DE ESTADO
                api.cambiarEstadoSensor(
                    sensorId,
                    nuevoEstado,
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