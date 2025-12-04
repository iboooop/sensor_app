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
    // ⚠️ VERIFICA TU IP SIEMPRE. Si usas emulador suele ser 10.0.2.2, si es dispositivo físico usa la IP de tu PC
    private val baseUrl = "http://100.25.170.26"
    private val TAG = "UsuarioApi"

    // =================================================================
    // SECCIÓN: AUTENTICACIÓN
    // =================================================================

    fun login(
        correo: String,
        password: String,
        onSuccess: (id: Int, nombres: String, apellidos: String, email: String, rol: String, estado: String, idDepto: Int) -> Unit,
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
                            val idDepto = u.optInt("id_departamento", 0)

                            if (estado == "inactivo") {
                                onError("Usuario inactivo. Contacte al administrador")
                            } else {
                                onSuccess(id, nombres, apellidos, email, rol, estado, idDepto)
                            }
                        } else onError("Datos incompletos")
                    } else onError(r.optString("message", "Credenciales incorrectas"))
                } catch (e: Exception) { onError("Error procesando respuesta") }
            },
            { error -> onError("Error de conexión") }
        ).apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: GESTIÓN DE USUARIOS
    // =================================================================

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
    // SECCIÓN: GESTIÓN DE SENSORES
    // =================================================================

    fun listarUsuariosPorDepto(idDepto: Int, onSuccess: (List<Usuario>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar_usuarios_por_depto.php?id_departamento=$idDepto"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val arr = r.optJSONArray("usuarios") ?: org.json.JSONArray()
                    val out = ArrayList<Usuario>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        out.add(
                            Usuario(
                                id = o.optInt("id", 0),
                                nombre = o.optString("nombres", ""),
                                apellido = o.optString("apellidos", ""),
                                email = "", rol = "", estado = "activo", departamentoNombre = ""
                            )
                        )
                    }
                    onSuccess(out)
                } catch (e: Exception) { onError("Error al filtrar usuarios: ${e.message}") }
            },
            { onError("Error de conexión al filtrar usuarios") }
        )
        q.add(req)
    }

    /**
     * Lista los sensores.
     * Si se provee un 'idDepto', filtra por ese departamento.
     * Si 'idDepto' es nulo o 0, lista todos los sensores.
     */
    fun listarSensores(
        idDepto: Int? = null,
        onSuccess: (List<Sensor>) -> Unit,
        onError: (String) -> Unit
    ) {
        var url = "$baseUrl/listar_sensores.php"
        if (idDepto != null && idDepto > 0) {
            url += "?id_departamento=$idDepto"
        }

        val req = StringRequest(Request.Method.GET, url,
            { response ->
                Log.d(TAG, "Respuesta listarSensores (URL: $url): $response")
                try {
                    val r = JSONObject(response)
                    if (!r.optBoolean("success", true)) {
                        val msg = r.optString("message", "Error desconocido del servidor")
                        onError("Server Error: $msg")
                        return@StringRequest
                    }
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
                                departamentoNombre = o.optString("departamento", ""),
                                usuarioNombre = o.optString("usuario", "Sin Asignar")
                            )
                        )
                    }
                    onSuccess(out)
                } catch (e: Exception) { onError("Error parseando JSON: ${e.message}") }
            },
            { error -> onError("Error de Red: ${error.message}") }
        )
        q.add(req)
    }

    fun ingresarSensor(
        codigoSensor: String, tipo: String, estado: String, idDepartamento: Int, idUsuario: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val url = "$baseUrl/ingresar_sensor.php"
        val jsonBody = JSONObject().apply {
            put("codigo_sensor", codigoSensor)
            put("tipo", tipo)
            put("estado", estado)
            put("id_departamento", idDepartamento)
            put("id_usuario", idUsuario)
        }

        val req = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { r ->
                try {
                    if (r.optString("status") == "success" || r.optBoolean("success", false)) {
                        onSuccess()
                    } else {
                        onError(r.optString("message", "Error al registrar sensor"))
                    }
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            { error -> onError("Error Red: ${error.message}") }
        )
        q.add(req)
    }

    fun modificarSensor(
        idSensor: Int, codigo: String, tipo: String, estado: String, idDepartamento: Int, idUsuario: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val url = "$baseUrl/actualizar_sensor.php"
        val jsonBody = JSONObject().apply {
            put("id_sensor", idSensor)
            put("codigo_sensor", codigo)
            put("tipo", tipo)
            put("estado", estado)
            put("id_departamento", idDepartamento)
            put("id_usuario", idUsuario)
        }

        val req = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { r ->
                try {
                    if (r.optString("status") == "success" || r.optBoolean("success", false)) {
                        onSuccess()
                    } else {
                        onError(r.optString("message", "Error al actualizar"))
                    }
                } catch (e: Exception) { onError("Error respuesta servidor") }
            },
            { error -> onError("Error de conexión: ${error.networkResponse?.statusCode ?: 0}") }
        )
        q.add(req)
    }

    fun cambiarEstadoSensor(
        idSensor: Int, nuevoEstado: String, onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val url = "$baseUrl/actualizar_estado_sensor.php"
        val jsonBody = JSONObject().apply {
            put("id_sensor", idSensor)
            put("estado", nuevoEstado)
        }

        val req = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { r ->
                if (r.optString("status") == "success") onSuccess()
                else onError(r.optString("message", "No se pudo cambiar el estado"))
            },
            { error ->
                val code = error.networkResponse?.statusCode ?: 0
                onError("Error de conexión: $code")
            }
        )
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: HISTORIAL DE EVENTOS (CON MANEJO DE ERRORES MEJORADO)
    // =================================================================

    fun listarEventos(idUsuario: Int = 0, onSuccess: (List<Evento>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar_eventos_acceso.php?id_usuario=$idUsuario"

        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                if (r.has("status") && r.getString("status") == "error") {
                    onError(r.optString("message", "Error desconocido del servidor"))
                    return@JsonObjectRequest
                }

                try {
                    val arr = r.optJSONArray("eventos") ?: org.json.JSONArray()
                    val lista = ArrayList<Evento>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        lista.add(
                            Evento(
                                id = o.optInt("id"),
                                tipo = o.optString("tipo"),
                                resultado = o.optString("resultado"),
                                fecha = o.optString("fecha"),
                                usuarioNombre = o.optString("usuario"),
                                sensorCodigo = o.optString("sensor")
                            )
                        )
                    }
                    onSuccess(lista)
                } catch (e: Exception) {
                    onError("Error parseando JSON: ${e.message}")
                }
            },
            { error ->
                val response = error.networkResponse
                if (response != null && response.data != null) {
                    try {
                        val errorString = String(response.data, Charsets.UTF_8)
                        if (errorString.contains("Error SQL")) {
                            onError("Error SQL Sever: $errorString")
                        } else {
                            onError("Error ${response.statusCode}. Revise el log.")
                            Log.e(TAG, "Error Body: $errorString")
                        }
                    } catch (e: Exception) {
                        onError("Error ${response.statusCode}")
                    }
                } else {
                    onError("Error de red: ${error.message ?: "Sin respuesta"}")
                }
            }
        )
        q.add(req)
    }

    fun listarEventosPorDepartamento(idDepto: Int, onSuccess: (List<Evento>) -> Unit, onError: (String) -> Unit) {
        val url = "$baseUrl/listar_eventos_acceso.php?id_departamento=$idDepto"

        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                if (r.has("status") && r.getString("status") == "error") {
                    onError(r.optString("message", "Error BD"))
                    return@JsonObjectRequest
                }

                try {
                    val arr = r.optJSONArray("eventos") ?: org.json.JSONArray()
                    val lista = ArrayList<Evento>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        lista.add(
                            Evento(
                                id = o.optInt("id"),
                                tipo = o.optString("tipo"),
                                resultado = o.optString("resultado"),
                                fecha = o.optString("fecha"),
                                usuarioNombre = o.optString("usuario"),
                                sensorCodigo = o.optString("sensor")
                            )
                        )
                    }
                    onSuccess(lista)
                } catch (e: Exception) { onError("Error parseando JSON: ${e.message}") }
            },
            { error ->
                val response = error.networkResponse
                if (response != null && response.data != null) {
                    try {
                        val errorString = String(response.data, Charsets.UTF_8)
                        onError("Error ${response.statusCode}: $errorString")
                        Log.e(TAG, "SERVER ERROR BODY: $errorString")
                    } catch (e: Exception) {
                        onError("Error HTTP ${response.statusCode}")
                    }
                } else {
                    onError("Error de conexión (Sin respuesta)")
                }
            }
        )
        q.add(req)
    }

    // =================================================================
    // SECCIÓN: AUXILIARES (DEPARTAMENTOS)
    // =================================================================

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