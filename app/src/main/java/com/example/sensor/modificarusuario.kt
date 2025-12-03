package com.example.sensor

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

// Utilidades de filtro
class LettersAndSpacesInputFilter : InputFilter {
    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        if (source == null) return null
        val out = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            if (c.isLetter() || c == ' ') out.append(c)
        }
        val incoming = source.subSequence(start, end).toString()
        return if (out.toString() == incoming) null else out.toString()
    }
}

private fun normalizeSpaces(value: String): String = value.trim().replace(Regex("\\s+"), " ")
private fun isValidName(value: String): Boolean = normalizeSpaces(value).matches(Regex("^[\\p{L}]+(?: [\\p{L}]+)*$"))

// NOTA: He cambiado el nombre de la clase a ModificarUsuario (con Mayúscula).
// Asegúrate de que en tu AndroidManifest.xml diga: android:name=".ModificarUsuario"
class ModificarUsuario : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etRut: EditText
    private lateinit var etTelefono: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var rgRol: RadioGroup
    private lateinit var tvEstadoActual: TextView

    private lateinit var btnModificar: Button
    private lateinit var btnEliminar: Button // Botón para DESACTIVAR
    private lateinit var btnActivar: Button  // Botón para ACTIVAR

    private lateinit var api: UsuarioApiService

    private var userId: Int = -1
    private var loadingDlg: SweetAlertDialog? = null

    private var departamentos: List<Departamento> = emptyList()
    private var departamentoSeleccionadoId: Int = 0
    private var usuarioCargadoIdDepto: Int = 0

    // Variable para recordar el estado (ya que quitamos el RadioGroup del XML)
    private var estadoActualUsuario: String = "activo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificarusuario)

        api = UsuarioApiService(this)

        // Vincular vistas
        etNombres = findViewById(R.id.txtnombre)
        etApellidos = findViewById(R.id.txtapellido)
        etEmail = findViewById(R.id.txtemail)
        etRut = findViewById(R.id.txtrut)
        etTelefono = findViewById(R.id.txttelefono)
        spinnerDepartamento = findViewById(R.id.spinner_departamento)
        rgRol = findViewById(R.id.rg_rol)
        tvEstadoActual = findViewById(R.id.tv_estado_actual)

        btnModificar = findViewById(R.id.btn_modificar)
        btnEliminar = findViewById(R.id.btn_eliminar)
        btnActivar = findViewById(R.id.btn_activar)

        // Filtros
        etNombres.filters = arrayOf(LettersAndSpacesInputFilter())
        etApellidos.filters = arrayOf(LettersAndSpacesInputFilter())

        userId = intent.getIntExtra("usuario_id", -1)
        if (userId <= 0) {
            alertError("ID inválido", "No hay identificador válido") { finish() }
            return
        }

        // Cargar datos
        cargarDepartamentos()

        // Listeners
        btnModificar.setOnClickListener { onGuardarCambios() }
        btnEliminar.setOnClickListener { onCambiarEstado("inactivo") } // Desactivar
        btnActivar.setOnClickListener { onCambiarEstado("activo") }     // Activar
    }

    private fun cargarDepartamentos() {
        showLoading("Cargando", "Obteniendo departamentos...")
        api.listarDepartamentos(
            onSuccess = { lista ->
                departamentos = lista
                val nombres = mutableListOf("Seleccione departamento")
                nombres.addAll(lista.map { it.nombre })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDepartamento.adapter = adapter

                spinnerDepartamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        departamentoSeleccionadoId = if (position == 0) 0 else departamentos[position - 1].id
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) { departamentoSeleccionadoId = 0 }
                }

                cargarUsuario()
            },
            onError = { msg ->
                dismissLoading()
                alertError("Error", "No se cargaron departamentos: $msg") { finish() }
            }
        )
    }

    private fun cargarUsuario() {
        api.getUsuarioPorId(
            userId,
            onSuccess = { u ->
                dismissLoading()
                etNombres.setText(u.nombre)
                etApellidos.setText(u.apellido)
                etEmail.setText(u.email)
                etRut.setText(u.rut)
                etTelefono.setText(u.telefono)

                usuarioCargadoIdDepto = u.idDepartamento
                val index = departamentos.indexOfFirst { it.id == usuarioCargadoIdDepto }
                if (index >= 0) spinnerDepartamento.setSelection(index + 1)

                if (u.rol.equals("admin", true)) rgRol.check(R.id.rb_admin)
                else rgRol.check(R.id.rb_operador)

                // Guardamos el estado en la variable y actualizamos botones
                estadoActualUsuario = u.estado
                actualizarBotonesEstado()
            },
            onError = { msg ->
                dismissLoading()
                alertError("Error", "Fallo al cargar usuario: $msg") { finish() }
            }
        )
    }

    private fun actualizarBotonesEstado() {
        tvEstadoActual.text = "Estado Actual: ${estadoActualUsuario.uppercase()}"

        if (estadoActualUsuario.equals("activo", true)) {
            // Si es activo: mostrar botón desactivar, ocultar botón activar
            btnEliminar.visibility = View.VISIBLE
            btnActivar.visibility = View.GONE
        } else {
            // Si es inactivo: mostrar botón activar, ocultar botón desactivar
            btnEliminar.visibility = View.GONE
            btnActivar.visibility = View.VISIBLE
        }
    }

    private fun onGuardarCambios() {
        val nom = normalizeSpaces(etNombres.text.toString())
        val ape = normalizeSpaces(etApellidos.text.toString())
        val mail = etEmail.text.toString().trim()
        val rut = etRut.text.toString().trim()
        val tel = etTelefono.text.toString().trim()

        if (nom.isEmpty() || ape.isEmpty() || mail.isEmpty() || rut.isEmpty() || tel.isEmpty()) {
            alertWarn("Faltan datos", "Completa todos los campos.")
            return
        }
        if (departamentoSeleccionadoId == 0) {
            alertWarn("Departamento", "Selecciona un departamento.")
            return
        }
        if (!isValidName(nom) || !isValidName(ape)) {
            alertWarn("Nombres", "Solo letras y espacios.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            alertWarn("Correo", "Email inválido.")
            return
        }

        val rol = if (rgRol.checkedRadioButtonId == R.id.rb_admin) "admin" else "operador"

        setUiEnabled(false)
        showLoading("Guardando", "Actualizando datos...")

        // Usamos estadoActualUsuario para mantener el estado que tenga actualmente
        api.modificarUsuario(
            userId, nom, ape, mail, tel, rut, rol, estadoActualUsuario, departamentoSeleccionadoId,
            onSuccess = {
                dismissLoading()
                setUiEnabled(true)
                alertOk("Éxito", "Usuario actualizado correctamente.") { /* Opcional: finish() */ }
            },
            onError = { msg ->
                dismissLoading()
                setUiEnabled(true)
                alertError("Error", msg)
            }
        )
    }

    private fun onCambiarEstado(nuevoEstado: String) {
        val accion = if (nuevoEstado == "activo") "Activar" else "Desactivar"
        val color = if (nuevoEstado == "activo") SweetAlertDialog.SUCCESS_TYPE else SweetAlertDialog.WARNING_TYPE

        SweetAlertDialog(this, color)
            .setTitleText("¿$accion Usuario?")
            .setContentText("El usuario pasará a estado: ${nuevoEstado.uppercase()}.")
            .setCancelText("Cancelar")
            .setConfirmText("Sí, $accion")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()

                // Recopilamos datos actuales para no perder cambios en los textos
                val nom = normalizeSpaces(etNombres.text.toString())
                val ape = normalizeSpaces(etApellidos.text.toString())
                val mail = etEmail.text.toString().trim()
                val rut = etRut.text.toString().trim()
                val tel = etTelefono.text.toString().trim()
                val rol = if (rgRol.checkedRadioButtonId == R.id.rb_admin) "admin" else "operador"
                val deptoId = if (departamentoSeleccionadoId == 0) usuarioCargadoIdDepto else departamentoSeleccionadoId

                setUiEnabled(false)
                showLoading("Procesando", "Cambiando estado a $nuevoEstado...")

                api.modificarUsuario(
                    userId, nom, ape, mail, tel, rut, rol, nuevoEstado, deptoId,
                    onSuccess = {
                        dismissLoading()
                        setUiEnabled(true)
                        estadoActualUsuario = nuevoEstado
                        actualizarBotonesEstado()
                        alertOk("Actualizado", "El usuario ahora está $nuevoEstado.")
                    },
                    onError = { msg ->
                        dismissLoading()
                        setUiEnabled(true)
                        alertError("Error", msg)
                    }
                )
            }
            .setCancelClickListener { it.dismissWithAnimation() }
            .show()
    }

    private fun setUiEnabled(enabled: Boolean) {
        btnModificar.isEnabled = enabled
        btnEliminar.isEnabled = enabled
        btnActivar.isEnabled = enabled
        etNombres.isEnabled = enabled
        // Habilitar/deshabilitar otros campos si gustas
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