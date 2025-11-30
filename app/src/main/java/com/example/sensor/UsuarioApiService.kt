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
        val body = JSONObject().apply {
            put("nombres", nombre)
            put("apellidos", apellido)
            put("correo", email)
            put("password", password)
        }
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                val ok = r.optBoolean("success", false) || r.optString("status") == "success"
                if (ok) onSuccess() else onError(r.optString("message", "Error creando usuario"))
            },
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
        )
        q.add(req)
    }

    /** LOGIN */
    fun login(
        correo: String,
        password: String,
        onSuccess: (id: Int, nombres: String, apellidos: String, email: String, rol: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/login.php"
        val body = JSONObject().apply {
            put("correo", correo)
            put("password", password)
        }
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { r ->
                if (r.optString("status") == "success" || r.optBoolean("success", false)) {
                    val u = r.optJSONObject("usuario")
                    val id = parseId(u)
                    val nombres = u?.optString("nombres").orEmpty()
                    val apellidos = u?.optString("apellidos").orEmpty()
                    val email = u?.optString("correo").orEmpty()
                    // --- NUEVO ---
                    val rol = u?.optString("rol", "operador").orEmpty() // "operador" como default
                    onSuccess(id, nombres, apellidos, email, rol)
                } else {
                    onError(r.optString("message", "Error de login"))
                }
            },
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
        )
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
                else onError(r.optString("message", "Error registrando"))
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
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
        )
        q.add(req)
    }

    /** OBTENER USUARIO POR ID */
    fun getUsuarioPorId(
        id: Int,
        onSuccess: (Usuario) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/getUsuarioPorId.php?id=$id"
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
            { e -> onError("Conexión fallida: ${e.message ?: "desconocido"}") }
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
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                onError("Conexión fallida: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "id" to id.toString(),
                "nombres" to nombre,
                "apellidos" to apellido,
                "correo" to email
            )
        }.apply { retryPolicy = DefaultRetryPolicy(15_000, 1, 1.0f) }
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
                    onError("Error parseando respuesta: ${e.message}")
                }
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                onError("Conexión fallida: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "id" to id.toString()
            )
        }.apply { retryPolicy = DefaultRetryPolicy(15_000, 1, 1.0f) }
        q.add(req)
    }

    // =======================
    // RECUPERAR CONTRASEÑA
    // =======================

    /** 1) Enviar código de recuperación a correo (5 dígitos, 1 min) */
    fun enviarCodigoRecuperacion(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/enviar_codigo.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                Log.d(TAG, "enviar_codigo resp=${resp.take(500)}")
                val (ok, msg) = parseServerSuccess(
                    resp,
                    // stems: envi.. (enviado/enviamos), ok, success
                    listOf("envi", "ok", "success")
                )
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                Log.e(TAG, "enviar_codigo error=${e.message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("correo" to email)
        }.apply { retryPolicy = DefaultRetryPolicy(15_000, 1, 1.0f) }
        q.add(req)
    }

    /** 2) Verificar código de 5 dígitos */
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
                val (ok, msg) = parseServerSuccess(
                    resp,
                    // stems: verific.. (verificado/verifica), valid.., correct..
                    listOf("verific", "valid", "válid", "correct", "ok", "success")
                )
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                Log.e(TAG, "verificar_codigo error=${e.message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf("correo" to email, "codigo" to codigo)
        }.apply { retryPolicy = DefaultRetryPolicy(15_000, 1, 1.0f) }
        q.add(req)
    }

    /** 3) Crear nueva contraseña usando email + código */
    fun crearContrasenia(
        email: String,
        codigo: String,
        nueva: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/crear_contrasenia.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { resp ->
                Log.d(TAG, "crear_contrasenia resp=${resp.take(500)}")
                val (ok, msg) = parseServerSuccess(
                    resp,
                    // stems: actualiz.., cambi.., restable..
                    listOf("actualiz", "cambi", "restable", "ok", "success")
                )
                if (ok) onSuccess() else onError(msg)
            },
            Response.ErrorListener { e ->
                val body = e.networkResponse?.data?.let { String(it) } ?: ""
                Log.e(TAG, "crear_contrasenia error=${e.message} body=${body.take(500)}")
                onError("Error de red: ${e.message ?: "desconocido"} ${body.take(200)}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf("correo" to email, "codigo" to codigo, "password" to nueva)
        }.apply { retryPolicy = DefaultRetryPolicy(15_000, 1, 1.0f) }
        q.add(req)
    }

    /** Parser tolerante a texto/HTML/emojis: intenta JSON; si falla, busca stems en texto plano */
    private fun parseServerSuccess(resp: String, successKeywords: List<String>): Pair<Boolean, String> {
        val trimmed = resp.trim()
        // 1) Intentar JSON
        try {
            val r = JSONObject(trimmed)
            val ok = r.optBoolean("success", false) || r.optString("status").equals("success", true)
            val msg = r.optString("message", if (ok) "OK" else "Operación no exitosa")
            return ok to msg
        } catch (_: Exception) {
            // 2) Texto/HTML: limpiar etiquetas y emojis y normalizar a minúsculas
            val noHtml = trimmed.replace(Regex("<[^>]*>"), " ")
            val lower = noHtml.lowercase()
            // quitar caracteres no alfanuméricos (emojis, etc.) para facilitar búsqueda
            val simplified = lower.replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
            val looksOk = successKeywords.any { kw -> simplified.contains(kw) }
            val msg = if (looksOk) {
                // intenta sacar un mensaje legible
                trimmed.take(200)
            } else {
                "Respuesta inválida del servidor: ${trimmed.take(200)}"
            }
            return looksOk to msg
        }
    }

    /** PARSEAR ID (compatible con varias claves) */
    private fun parseId(o: JSONObject?): Int {
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