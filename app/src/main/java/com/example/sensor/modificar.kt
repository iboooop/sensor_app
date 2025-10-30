package com.example.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Patterns
import cn.pedant.SweetAlert.SweetAlertDialog

class LettersAndSpacesInputFilter : InputFilter {
    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        if (source == null) return null
        val before = dest?.toString() ?: ""
        val out = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            val isLetter = c.isLetter()
            val isSpace = c == ' '
            if (!isLetter && !isSpace) continue
            if (isSpace && dstart == 0 && out.isEmpty()) continue
            val leftChar = when {
                out.isNotEmpty() -> out.last()
                dstart > 0 && before.isNotEmpty() -> before[dstart - 1]
                else -> null
            }
            if (isSpace && leftChar == ' ') continue
            out.append(c)
        }
        val incoming = source.subSequence(start, end).toString()
        return if (out.toString() == incoming) null else out.toString()
    }
}

private fun normalizeSpaces(value: String): String = value.trim().replace(Regex("\\s+"), " ")
private fun isValidName(value: String): Boolean = normalizeSpaces(value).matches(Regex("^[\\p{L}]+(?: [\\p{L}]+)*$"))

class modificar : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnModificar: Button
    private lateinit var btnEliminar: Button
    private lateinit var api: UsuarioApiService

    private var userId: Int = -1
    private var loadingDlg: SweetAlertDialog? = null
    private var currentUserId: Int = -1

    private val CAMERA_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar)

        api = UsuarioApiService(this)

        etNombres = findViewById(R.id.txtnombre)
        etApellidos = findViewById(R.id.txtapellido)
        etEmail = findViewById(R.id.txtemail)
        btnModificar = findViewById(R.id.btn_modificar)
        btnEliminar = findViewById(R.id.btn_eliminar)

        solicitarPermisoCamara()

        // Filtro solo letras y espacios
        appendFilter(etNombres, LettersAndSpacesInputFilter())
        appendFilter(etApellidos, LettersAndSpacesInputFilter())

        userId = intent.getIntExtra("usuario_id", -1)
        currentUserId = SessionManager.getUserId(this)

        if (userId <= 0) {
            alertError("ID inválido", "No hay identificador válido") { finish() }
            return
        }

        cargarUsuario()

        btnModificar.setOnClickListener { onModificar() }
        btnEliminar.setOnClickListener { onEliminar() }
    }

    private fun solicitarPermisoCamara() {
        val permiso = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Permiso requerido")
                .setContentText("Esta aplicación necesita acceso a la cámara para usar la linterna.")
                .setConfirmText("Permitir")
                .setCancelText("Cancelar")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    ActivityCompat.requestPermissions(this, arrayOf(permiso), CAMERA_REQUEST_CODE)
                }
                .setCancelClickListener { it.dismissWithAnimation() }
                .show()
        }
    }

    private fun appendFilter(editText: EditText, filter: InputFilter) {
        val current = editText.filters?.toMutableList() ?: mutableListOf()
        current.add(filter)
        editText.filters = current.toTypedArray()
    }

    private fun cargarUsuario() {
        showLoading("Cargando", "Obteniendo datos del usuario…")
        api.getUsuarioPorId(
            userId,
            onSuccess = { u ->
                dismissLoading()
                etNombres.setText(u.nombre)
                etApellidos.setText(u.apellido)
                etEmail.setText(u.email)
            },
            onError = { msg ->
                dismissLoading()
                alertError("Error", msg) { finish() }
            }
        )
    }

    private fun onModificar() {
        val nom = normalizeSpaces(etNombres.text.toString())
        val ape = normalizeSpaces(etApellidos.text.toString())
        val mail = etEmail.text.toString().trim()

        if (nom.isEmpty() || ape.isEmpty() || mail.isEmpty()) {
            alertWarn("Faltan datos", "Completa nombres, apellidos y correo.")
            return
        }
        if (nom.length < 3 || ape.length < 3) {
            alertWarn("Nombre inválido", "Nombre y apellido deben tener al menos 3 caracteres.")
            return
        }
        if (!isValidName(nom)) { etNombres.error = "Solo letras y espacios"; etNombres.requestFocus(); return }
        if (!isValidName(ape)) { etApellidos.error = "Solo letras y espacios"; etApellidos.requestFocus(); return }
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            etEmail.error = "Correo inválido"; etEmail.requestFocus(); return
        }

        setUiEnabled(false)
        showLoading("Actualizando", "Enviando cambios…")
        api.modificarUsuario(
            userId, nom, ape, mail,
            onSuccess = {
                dismissLoading()
                setUiEnabled(true)
                alertOk("Actualizado", "Datos modificados correctamente.") { finish() }
            },
            onError = { msg ->
                dismissLoading()
                setUiEnabled(true)
                alertError("No se pudo modificar", msg)
            }
        )
    }

    private fun onEliminar() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Eliminar usuario")
            .setContentText("¿Seguro que deseas eliminar a este usuario?")
            .setCancelText("Cancelar")
            .setConfirmText("Eliminar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                setUiEnabled(false)
                showLoading("Eliminando", "Procesando solicitud…")
                api.eliminarUsuario(
                    userId,
                    onSuccess = {
                        dismissLoading()
                        setUiEnabled(true)
                        if (userId == currentUserId) {
                            alertOk("Sesión cerrada", "El usuario eliminado era el actual.") {
                                SessionManager.logoutAndGoToLogin(this)
                            }
                        } else {
                            alertOk("Eliminado", "Usuario eliminado correctamente.") { finish() }
                        }
                    },
                    onError = { msg ->
                        dismissLoading()
                        setUiEnabled(true)
                        alertError("No se pudo eliminar", msg)
                    }
                )
            }
            .setCancelClickListener { it.dismissWithAnimation() }
            .show()
    }

    private fun setUiEnabled(enabled: Boolean) {
        btnModificar.isEnabled = enabled
        btnEliminar.isEnabled = enabled
        etNombres.isEnabled = enabled
        etApellidos.isEnabled = enabled
        etEmail.isEnabled = enabled
    }

    private fun showLoading(title: String, content: String) {
        dismissLoading()
        loadingDlg = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            .setTitleText(title)
            .setContentText(content)
        loadingDlg?.setCancelable(false)
        loadingDlg?.show()
    }

    private fun dismissLoading() {
        loadingDlg?.dismissWithAnimation()
        loadingDlg = null
    }

    private fun alertWarn(title: String, content: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title).setContentText(content).setConfirmText("OK").show()
    }

    private fun alertError(title: String, content: String, onOk: (() -> Unit)? = null) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .setConfirmClickListener { it.dismissWithAnimation(); onOk?.invoke() }
            .show()
    }

    private fun alertOk(title: String, content: String, onOk: (() -> Unit)? = null) {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("OK")
            .setConfirmClickListener { it.dismissWithAnimation(); onOk?.invoke() }
            .show()
    }
}