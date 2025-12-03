package com.example.sensor

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class UsuarioApiService(ctx: Context) {

    private val q: RequestQueue = Volley.newRequestQueue(ctx)
    // ⚠️ RECUERDA VERIFICAR TU IP AQUÍ
    private val baseUrl = "http://100.25.170.26"
    private val TAG = "UsuarioApi"

    // =================================================================
    // SECCIÓN: AUTENTICACIÓN
    // =================================================================

    /** LOGIN */
    fun login(
        correo: String,
        password: String,
        onSuccess: (id: Int, nombres: String, apellidos: String, email: String, rol: String, estado: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/login.php"
        val body = JSONObject().apply {
            put("correo", correo)
            put("password", password)
        }

        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                try {
                    if (r.optString("status") == "success" || r.optBoolean("success", false)) {
                        val u = r.optJSONObject("usuario")
                        if (u != null) {
                            val id = parseId(u)
                            val nombres = u.optString("nombres", "")
                            val apellidos = u.optString("apellidos", "")
                            val email = u.optString("correo", "")
                            val rol = u.optString("rol", "operador")
                            val estado = u.optString("estado", "activo")

                            if (estado == "inactivo") {
                                onError("Usuario inactivo. Contacte al administrador")
                            } else {
                                onSuccess(id, nombres, apellidos, email, rol, estado)
                            }
                        } else onError("Datos incompletos")
                    } else onError(r.optString("message", "Credenciales incorrectas"))
                } catch (e: Exception) { onError("Error procesando respuesta") }
            },
            { onError("Error de conexión") }
        ).apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: GESTIÓN DE USUARIOS
    // =================================================================

    /** REGISTRAR USUARIO */
    fun ingresarUsuario(
        nombre: String, apellido: String, email: String, telefono: String,
        rut: String, password: String, rol: String, idDepartamento: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val url = "$baseUrl/registrar.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    val r = JSONObject(resp)
                    if (r.optBoolean("success", false) || r.optString("status") == "success") onSuccess()
                    else onError(r.optString("message", "Error al registrar"))
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            Response.ErrorListener { onError("Conexión fallida") }
        ) {
            override fun getParams() = hashMapOf(
                "nombres" to nombre,
                "apellidos" to apellido,
                "correo" to email,
                "telefono" to telefono,
                "rut" to rut,
                "password" to password,
                "rol" to rol,
                "id_departamento" to idDepartamento.toString()
            )
        }
        q.add(req)
    }

    /** MODIFICAR USUARIO (ACTUALIZAR DATOS Y ESTADO) */
    fun modificarUsuario(
        id: Int, nombre: String, apellido: String, email: String,
        telefono: String, rut: String, rol: String, estado: String, idDept: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val url = "$baseUrl/actualizar.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    val r = JSONObject(resp)
                    if (r.optBoolean("success", false) || r.optString("status") == "success") onSuccess()
                    else onError(r.optString("message", "Error actualizando"))
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            Response.ErrorListener { onError("Conexión fallida") }
        ) {
            override fun getParams() = hashMapOf(
                "id" to id.toString(),
                "nombres" to nombre,
                "apellidos" to apellido,
                "correo" to email,
                "telefono" to telefono,
                "rut" to rut,
                "rol" to rol,
                "estado" to estado,
                "id_departamento" to idDept.toString()
            )
        }
        q.add(req)
    }

    /** LISTAR USUARIOS */
    fun listarUsuarios(onSuccess: (List<Usuario>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar.php"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val arr = r.getJSONArray("usuarios")
                    val out = ArrayList<Usuario>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        out.add(
                            Usuario(
                                id = o.optInt("id", 0),
                                nombre = o.optString("nombres", ""),
                                apellido = o.optString("apellidos", ""),
                                email = o.optString("correo", ""),
                                rol = o.optString("rol", ""),
                                estado = o.optString("estado", ""),
                                departamentoNombre = o.optString("departamento", "")
                            )
                        )
                    }
                    onSuccess(out)
                } catch (e: Exception) { onError("Error parseando lista") }
            },
            { onError("Conexión fallida al listar") }
        )
        q.add(req)
    }

    /** OBTENER USUARIO POR ID */
    fun getUsuarioPorId(id: Int, onSuccess: (Usuario) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/getUsuarioPorId.php?id=$id"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    if (r.optBoolean("success", false)) {
                        val u = r.getJSONObject("usuario")
                        val usuario = Usuario(
                            id = u.optInt("id", 0),
                            nombre = u.optString("nombres", ""),
                            apellido = u.optString("apellidos", ""),
                            email = u.optString("correo", ""),
                            telefono = u.optString("telefono", ""),
                            rut = u.optString("rut", ""),
                            rol = u.optString("rol", "operador"),
                            estado = u.optString("estado", "activo"),
                            idDepartamento = u.optInt("id_departamento", 0)
                        )
                        onSuccess(usuario)
                    } else onError(r.optString("message", "No encontrado"))
                } catch (e: Exception) { onError("Error parseando usuario") }
            },
            { onError("Error de conexión") }
        )
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: GESTIÓN DE SENSORES (NUEVO)
    // =================================================================

    /** LISTAR SENSORES */
    fun listarSensores(onSuccess: (List<Sensor>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar_sensores.php"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val arr = r.getJSONArray("sensores")
                    val out = ArrayList<Sensor>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        out.add(
                            Sensor(
                                id = o.optInt("id", 0),
                                codigo = o.optString("codigo", ""),
                                estado = o.optString("estado", ""),
                                tipo = o.optString("tipo", ""),
                                fechaAlta = o.optString("fecha_alta", ""),
                                departamentoNombre = o.optString("departamento", "")
                            )
                        )
                    }
                    onSuccess(out)
                } catch (e: Exception) { onError("Error parseando sensores") }
            },
            { onError("Error de conexión sensores") }
        )
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: AUXILIARES (DEPARTAMENTOS)
    // =================================================================

    /** LISTAR DEPARTAMENTOS */
    fun listarDepartamentos(onSuccess: (List<Departamento>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar_departamentos.php"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val arr = r.getJSONArray("departamentos")
                    val out = ArrayList<Departamento>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        out.add(Departamento(o.optInt("id", 0), o.optString("nombre", "")))
                    }
                    onSuccess(out)
                } catch (e: Exception) { onError("Error parseando deptos") }
            },
            { onError("Conexión fallida deptos") }
        )
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: RECUPERACIÓN DE CONTRASEÑA
    // =================================================================

    /** 1. ENVIAR CÓDIGO */
    fun enviarCodigoRecuperacion(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/enviar_codigo.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    val r = JSONObject(resp)
                    if (r.optBoolean("success", false) || r.optString("status") == "success") onSuccess()
                    else onError(r.optString("message", "Error al enviar código"))
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            Response.ErrorListener { onError("Error de conexión") }
        ) {
            override fun getParams() = hashMapOf("correo" to email)
        }
        q.add(req)
    }

    /** 2. VERIFICAR CÓDIGO */
    fun verificarCodigo(email: String, codigo: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/verificar_codigo.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    val r = JSONObject(resp)
                    if (r.optBoolean("success", false) || r.optString("status") == "success") onSuccess()
                    else onError(r.optString("message", "Código inválido"))
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            Response.ErrorListener { onError("Error de conexión") }
        ) {
            override fun getParams() = hashMapOf("correo" to email, "codigo" to codigo)
        }
        q.add(req)
    }

    /** 3. CREAR NUEVA CONTRASEÑA */
    fun crearContrasenia(email: String, codigo: String, nueva: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/crear_contrasenia.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    val r = JSONObject(resp)
                    if (r.optBoolean("success", false) || r.optString("status") == "success") onSuccess()
                    else onError(r.optString("message", "Error al cambiar contraseña"))
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            Response.ErrorListener { onError("Error de conexión") }
        ) {
            override fun getParams() = hashMapOf(
                "correo" to email,
                "codigo" to codigo,
                "password" to nueva
            )
        }
        q.add(req)
    }

    // =================================================================
    // UTILIDADES INTERNAS
    // =================================================================

    private fun parseId(o: JSONObject?): Int {
        if (o == null) return 0
        val keys = arrayOf("id", "usuario_id", "id_usuario")
        for (k in keys) {
            val v = o.optInt(k, Int.MIN_VALUE)
            if (v != Int.MIN_VALUE && v > 0) return v
        }
        return 0
    }
}