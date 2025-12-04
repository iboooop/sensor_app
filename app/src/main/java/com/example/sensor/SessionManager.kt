package com.example.sensor

import android.app.Activity
import android.content.Context
import android.content.Intent

object SessionManager {
    private const val PREFS = "usuario_prefs"
    private const val KEY_ID = "usuario_id"
    private const val KEY_NOMBRES = "nombres"
    private const val KEY_APELLIDOS = "apellidos"
    private const val KEY_EMAIL = "correo"
    private const val KEY_ROL = "rol"
    private const val KEY_ESTADO = "estado"
    private const val KEY_ID_DEPTO = "id_departamento"

    fun saveUser(
        ctx: Context,
        id: Int,
        nombres: String,
        apellidos: String,
        email: String,
        rol: String = "operador",
        estado: String = "activo",
        idDepartamento: Int
    ) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRES, nombres)
            .putString(KEY_APELLIDOS, apellidos)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROL, rol)
            .putString(KEY_ESTADO, estado)
            .putInt(KEY_ID_DEPTO, idDepartamento)
            .apply()
    }

    fun getUserId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ID, -1)

    // ===== CORRECCIÓN DE NOMBRE AQUÍ =====
    /**
     * Obtiene el ID del departamento del usuario que ha iniciado sesión.
     * Devuelve -1 si no se encuentra.
     */
    fun getDepartamentoId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ID_DEPTO, -1)
    // =====================================

    fun getFullName(ctx: Context): String? {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = sp.getString(KEY_NOMBRES, "") ?: ""
        val a = sp.getString(KEY_APELLIDOS, "") ?: ""
        val full = (n + " " + a).trim()
        return if (full.isBlank()) null else full
    }

    fun getNombres(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOMBRES, "") ?: ""

    fun getApellidos(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_APELLIDOS, "") ?: ""

    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""

    fun getRol(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROL, "operador") ?: "operador"

    // Esta función es redundante, pero la mantenemos por si la usas en otro lado.
    fun getRole(ctx: Context): String = getRol(ctx)

    fun getEstado(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ESTADO, "activo") ?: "activo"

    fun isAdmin(ctx: Context): Boolean = getRol(ctx) == "admin"
    fun isOperador(ctx: Context): Boolean = getRol(ctx) == "operador"
    fun isActivo(ctx: Context): Boolean = getEstado(ctx) == "activo"

    fun hasActiveSession(ctx: Context): Boolean =
        getUserId(ctx) != -1 && getFullName(ctx) != null

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun logoutAndGoToLogin(activity: Activity) {
        clear(activity)
        val i = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(i)
        activity.finish()
    }
}