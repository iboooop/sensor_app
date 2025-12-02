package com.example.sensor

import android.content.Context
import android.util. Log
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
    private val baseUrl = "http://100.25.170.26"
    private val TAG = "UsuarioApi"

    /** INGRESAR USUARIO SIN INICIAR SESIÓN (para admin/lista) */
    fun ingresarUsuario(
        nombre: String,
        apellido: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/registrar.php"
        val body = JSONObject(). apply {
            put("nombres", nombre)
            put("apellidos", apellido)
            put("correo", email)
            put("password", password)
        }
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                val ok = r.optBoolean("success", false) || r.optString("status") == "success"
                if (ok) onSuccess() else onError(r. optString("message", "Error creando usuario"))
            },
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
        )
        q.add(req)
    }

    /** LOGIN - MEJORADO CON VALIDACIÓN DE ESTADO */
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

        Log.d(TAG, "=== LOGIN REQUEST ===")
        Log. d(TAG, "URL: $url")
        Log.d(TAG, "Body: $body")

        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                Log.d(TAG, "=== LOGIN RESPONSE SUCCESS ===")
                Log. d(TAG, "Response: $r")

                try {
                    if (r.optString("status") == "success" || r.optBoolean("success", false)) {
                        val u = r.optJSONObject("usuario")
                        if (u != null) {
                            Log.d(TAG, "Usuario object: $u")

                            val id = parseId(u)
                            val nombres = u.optString("nombres", "")
                            val apellidos = u.optString("apellidos", "")
                            val email = u.optString("correo", "")
                            val rol = u.optString("rol", "operador")
                            val estado = u.optString("estado", "activo")

                            Log. d(TAG, "Parsed: id=$id, nombres=$nombres, apellidos=$apellidos, rol=$rol, estado=$estado")

                            if (estado == "inactivo") {
                                onError("Usuario inactivo.  Contacte al administrador")
                            } else {
                                onSuccess(id, nombres, apellidos, email, rol, estado)
                            }
                        } else {
                            Log.e(TAG, "Usuario object is null")
                            onError("Datos de usuario incompletos")
                        }
                    } else {
                        val errorMsg = r.optString("message", "Error de login")
                        Log.e(TAG, "Login failed: $errorMsg")
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception parsing success response: ${e.message}", e)
                    onError("Error procesando respuesta: ${e.message}")
                }
            },
            { e ->
                val statusCode = e.networkResponse?.statusCode ?: 0
                val rawBody = e.networkResponse?.data?.let { String(it) } ?: ""

                Log.e(TAG, "=== LOGIN RESPONSE ERROR ===")
                Log.e(TAG, "Status code: $statusCode")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Raw body: $rawBody")
                Log.e(TAG, "Exception: ", e)

                try {
                    val errorJson = JSONObject(rawBody)
                    val errorMsg = errorJson.optString("message", "Error de conexión")
                    onError(errorMsg)
                } catch (ex: Exception) {
                    Log.e(TAG, "Can't parse error JSON: ${ex.message}")

                    if (rawBody.isNotEmpty()) {
                        onError("Error del servidor: ${rawBody. take(200)}")
                    } else {
                        when (statusCode) {
                            401 -> onError("Credenciales incorrectas")
                            403 -> onError("Usuario inactivo")
                            500 -> onError("Error interno del servidor")
                            0 -> onError("No se pudo conectar al servidor.  Verifica tu conexión.")
                            else -> onError("Error de conexión (código: $statusCode)")
                        }
                    }
                }
            }
        ). apply {
            retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f)
        }

        q.add(req)
    }

    /** CREAR USUARIO */
    fun crearUsuario(
        nombre: String,
        apellido: String,
        email: String,
        password: String,
        onSuccess: (createdId: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/registrar.php"
        val body = JSONObject().apply {
            put("nombres", nombre)
            put("apellidos", apellido)
            put("correo", email)
            put("password", password)
        }
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                val ok = r.optBoolean("success", false) || r.optString("status") == "success"
                if (ok) onSuccess(r.optInt("id", 0))
                else onError(r. optString("message", "Error registrando"))
            },
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
        )
        q.add(req)
    }

    /** LISTAR USUARIOS */
    fun listarUsuarios(
        onSuccess: (List<Usuario>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/listar.php"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val arr = r.getJSONArray("usuarios")
                    val out = ArrayList<Usuario>(arr.length())
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        out.add(
                            Usuario(
                                id = parseId(o),
                                nombre = o.optString("nombres", ""),
                                apellido = o.optString("apellidos", ""),
                                email = o.optString("correo", "")
                            )
                        )
                    }
                    onSuccess(out)
                } catch (e: Exception) {
                    onError("Error parseando: ${e.message}")
                }
            },
            { e -> onError("Conexión fallida: ${e. message ?: "desconocido"}") }
        )
        q. add(req)
    }

    /** OBTENER USUARIO POR ID */
    fun getUsuarioPorId(
        id: Int,
        onSuccess: (Usuario) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/getUsuarioPorId.php? id=$id"
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { r ->
                try {
                    val ok = r.optBoolean("success", false)
                    if (ok) {
                        val u = r.getJSONObject("usuario")
                        val usuario = Usuario(
                            id = parseId(u),
                            nombre = u.optString("nombres", ""),
                            apellido = u.optString("apellidos", ""),
                            email = u.optString("correo", "")
                        )
                        onSuccess(usuario)
                    } else {
                        onError(r.optString("message", "Usuario no encontrado"))
                    }
                } catch (e: Exception) {
                    onError("Error parseando: ${e.message}")
                }
            },
            { e -> onError("Conexión fallida: ${e. message ?: "desconocido"}") }
        )
        q.add(req)
    }

    /** MODIFICAR USUARIO */
    fun modificarUsuario(
        id: Int,
        nombre: String,
        apellido: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/actualizar.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    if (resp.isEmpty()) {
                        onError("Servidor no devolvió respuesta")
                        return@Listener
                    }
                    val r = JSONObject(resp)
                    val ok = r.optBoolean("success", false) || r.optString("status") == "success"
                    if (ok) onSuccess() else onError(r.optString("message", "Error modificando"))
                } catch (e: Exception) {
                    onError("Error parseando respuesta: ${e.message}")
                }
            },
            Response. ErrorListener { e ->
                val body = e.networkResponse?.data?. let { String(it) } ?: ""
                onError("Conexión fallida: ${e. message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "id" to id.toString(),
                "nombres" to nombre,
                "apellidos" to apellido,
                "correo" to email
            )
        }.apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    /** ELIMINAR USUARIO */
    fun eliminarUsuario(
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/eliminar.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                try {
                    if (resp.isEmpty()) {
                        onError("Servidor no devolvió respuesta")
                        return@Listener
                    }
                    val r = JSONObject(resp)
                    val ok = r.optBoolean("success", false) || r.optString("status") == "success"
                    if (ok) onSuccess() else onError(r.optString("message", "Error eliminando"))
                } catch (e: Exception) {
                    onError("Error parseando respuesta: ${e. message}")
                }
            },
            Response.ErrorListener { e ->
                val body = e. networkResponse?.data?.let { String(it) } ?: ""
                onError("Conexión fallida: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "id" to id.toString()
            )
        }.apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    /** ENVIAR CÓDIGO DE RECUPERACIÓN */
    fun enviarCodigoRecuperacion(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/enviar_codigo.php"
        val req = object : StringRequest(Method.POST, url,
            Response. Listener { resp ->
                Log. d(TAG, "enviar_codigo resp=${resp.take(500)}")
                val (ok, msg) = parseServerSuccess(resp, listOf("envi", "ok", "success"))
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?. let { String(it) } ?: ""
                Log.e(TAG, "enviar_codigo error=${e.message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("correo" to email)
        }.apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    /** VERIFICAR CÓDIGO */
    fun verificarCodigo(
        email: String,
        codigo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/verificar_codigo.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                Log.d(TAG, "verificar_codigo resp=${resp.take(500)}")
                val (ok, msg) = parseServerSuccess(resp, listOf("verific", "valid", "válid", "correct", "ok", "success"))
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?. data?.let { String(it) } ?: ""
                Log. e(TAG, "verificar_codigo error=${e.message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("correo" to email, "codigo" to codigo)
        }. apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    /** CREAR NUEVA CONTRASEÑA */
    fun crearContrasenia(
        email: String,
        codigo: String,
        nueva: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/crear_contrasenia. php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                Log. d(TAG, "crear_contrasenia resp=${resp.take(500)}")
                val (ok, msg) = parseServerSuccess(resp, listOf("actualiz", "cambi", "restable", "ok", "success"))
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                Log.e(TAG, "crear_contrasenia error=${e. message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("correo" to email, "codigo" to codigo, "password" to nueva)
        }.apply { retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f) }
        q.add(req)
    }

    private fun parseServerSuccess(resp: String, successKeywords: List<String>): Pair<Boolean, String> {
        val trimmed = resp.trim()
        try {
            val r = JSONObject(trimmed)
            val ok = r.optBoolean("success", false) || r.optString("status"). equals("success", true)
            val msg = r.optString("message", if (ok) "OK" else "Operación no exitosa")
            return ok to msg
        } catch (_: Exception) {
            val noHtml = trimmed.replace(Regex("<[^>]*>"), " ")
            val lower = noHtml.lowercase()
            val simplified = lower. replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
            val looksOk = successKeywords.any { kw -> simplified.contains(kw) }
            val msg = if (looksOk) trimmed.take(200) else "Respuesta inválida del servidor: ${trimmed.take(200)}"
            return looksOk to msg
        }
    }

    private fun parseId(o: JSONObject? ): Int {
        if (o == null) return 0
        val keys = arrayOf("id", "usuario_id", "id_usuario", "codigo", "user_id")
        for (k in keys) {
            val v = o.optInt(k, Int.MIN_VALUE)
            if (v != Int.MIN_VALUE && v > 0) return v
        }
        for (k in keys) {
            val s = o.optString(k, "")
            s.toIntOrNull()?.let { if (it > 0) return it }
        }
        return 0
    }
}