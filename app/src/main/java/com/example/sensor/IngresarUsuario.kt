package com.example.sensor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

/** Normaliza espacios */
private fun normalizeSpaces(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

/** Valida solo letras */
private fun isValidName(value: String): Boolean {
    val normalized = normalizeSpaces(value)
    return normalized.matches(Regex("^[\\p{L}]+(?: [\\p{L}]+)*$"))
}

/** Valida contraseña fuerte */
private fun isStrongPassword(p: String): Boolean {
    if (p.length < 8) return false
    val hasUpper = p.any { it.isUpperCase() }
    val hasLower = p.any { it.isLowerCase() }
    val hasDigit = p.any { it.isDigit() }
    val hasSymbol = p.any { !it.isLetterOrDigit() }
    return hasUpper && hasLower && hasDigit && hasSymbol
}

/** Valida RUT chileno */
private fun esRutValido(rut: String): Boolean {
    val rutLimpio = rut.replace("-", "").replace(".", "")
    if (rutLimpio.length < 2) return false

    val cuerpoRut = rutLimpio.substring(0, rutLimpio.length - 1)
    val dv = rutLimpio.last().uppercaseChar()

    if (!cuerpoRut.all { it.isDigit() }) return false

    var suma = 0
    var multiplicador = 2
    for (i in cuerpoRut.length - 1 downTo 0) {
        suma += (cuerpoRut[i].toString().toInt()) * multiplicador
        multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
    }

    val dvCalculado = 11 - (suma % 11)
    val dvCorrecto = when (dvCalculado) {
        11 -> '0'
        10 -> 'K'
        else -> dvCalculado.toString()[0]
    }
    return dv == dvCorrecto
}

/** Valida teléfono */
private fun esTelefonoValido(telefono: String): Boolean {
    return telefono.length == 9 && telefono.startsWith("9") && telefono.all { it.isDigit() }
}

class IngresarUsuario : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etRut: EditText
    private lateinit var etClave: EditText
    private lateinit var etClaveRep: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var rgRol: RadioGroup
    private lateinit var btnRegistrar: Button
    private lateinit var api: UsuarioApiService

    private var departamentos: List<Departamento> = emptyList()
    private var departamentoSeleccionadoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ingresarusuario)

        api = UsuarioApiService(this)

        etNombres = findViewById(R.id.txtnombre)
        etApellidos = findViewById(R.id.txtapellido)
        etEmail = findViewById(R.id.txtemail)
        etTelefono = findViewById(R.id.txttelefono)
        etRut = findViewById(R.id.txtrut)
        etClave = findViewById(R.id.txtclave)
        etClaveRep = findViewById(R.id.txtrepitaclave)
        spinnerDepartamento = findViewById(R.id.spinner_departamento)
        rgRol = findViewById(R.id.rg_rol)
        btnRegistrar = findViewById(R.id.btn_registro)

        cargarDepartamentos()

        btnRegistrar.setOnClickListener { registrarUsuarioAdmin() }
    }

    private fun cargarDepartamentos() {
        api.listarDepartamentos(
            onSuccess = { lista ->
                departamentos = lista

                if (lista.isEmpty()) {
                    warn("Sin departamentos", "No hay departamentos disponibles.")
                    btnRegistrar.isEnabled = false
                    return@listarDepartamentos
                }

                val nombres = mutableListOf("Seleccione un departamento")
                nombres.addAll(lista.map { it.nombre })

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    nombres
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDepartamento.adapter = adapter

                spinnerDepartamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        departamentoSeleccionadoId = if (position == 0) {
                            0
                        } else {
                            departamentos[position - 1].id
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        departamentoSeleccionadoId = 0
                    }
                }
            },
            onError = { msg ->
                warn("Error de Red", "No se pudieron cargar los departamentos.\nDetalle: $msg")
                btnRegistrar.isEnabled = false
            }
        )
    }

    private fun registrarUsuarioAdmin() {
        val nom = normalizeSpaces(etNombres.text.toString())
        val ape = normalizeSpaces(etApellidos.text.toString())
        val mail = etEmail.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val rut = etRut.text.toString().trim()
        val pass = etClave.text.toString().trim()
        val pass2 = etClaveRep.text.toString().trim()

        // Obtener Rol seleccionado
        val selectedRolId = rgRol.checkedRadioButtonId
        val rol = if (selectedRolId == R.id.rb_admin) "admin" else "operador"

        // Validaciones
        if (nom.isEmpty() || ape.isEmpty() || mail.isEmpty() || telefono.isEmpty() ||
            rut.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            warn("Faltan datos", "Completa todos los campos")
            return
        }

        if (departamentoSeleccionadoId == 0) {
            warn("Departamento", "Selecciona un departamento")
            return
        }

        if (!isValidName(nom) || !isValidName(ape)) {
            warn("Nombre/Apellido", "Solo letras y espacios")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            warn("Correo", "Formato inválido")
            return
        }

        if (!esTelefonoValido(telefono)) {
            warn("Teléfono", "Debe empezar con 9 y tener 9 dígitos")
            return
        }

        if (!esRutValido(rut)) {
            warn("RUT", "RUT inválido (ej: 12345678-9)")
            return
        }

        if (pass != pass2) {
            warn("Contraseña", "No coinciden")
            return
        }

        if (!isStrongPassword(pass)) {
            warn("Seguridad", "La clave debe tener 8+ caracteres, mayúscula, minúscula, número y símbolo")
            return
        }

        // Llamar a la API con todos los datos
        api.ingresarUsuario(
            nombre = nom,
            apellido = ape,
            email = mail,
            telefono = telefono,
            rut = rut,
            password = pass,
            rol = rol,
            idDepartamento = departamentoSeleccionadoId,
            onSuccess = {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Éxito!")
                    .setContentText("Usuario creado correctamente")
                    .setConfirmText("OK")
                    .setConfirmClickListener { dlg ->
                        dlg.dismissWithAnimation()
                        finish()
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