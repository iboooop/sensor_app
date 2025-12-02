package com.example.sensor

import android.app.Activity
import android.content. Context
import android.content.Intent

object SessionManager {
    private const val PREFS = "usuario_prefs"
    private const val KEY_ID = "usuario_id"
    private const val KEY_NOMBRES = "nombres"
    private const val KEY_APELLIDOS = "apellidos"
    private const val KEY_EMAIL = "correo"
    private const val KEY_ROL = "rol"
    private const val KEY_ESTADO = "estado"

    /**
     * Guardar usuario completo en sesión
     */
    fun saveUser(
        ctx: Context,
        id: Int,
        nombres: String,
        apellidos: String,
        email: String,
        rol: String = "operador",
        estado: String = "activo"
    ) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRES, nombres)
            . putString(KEY_APELLIDOS, apellidos)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROL, rol)
            . putString(KEY_ESTADO, estado)
            .apply()
    }

    /**
     * Obtener ID del usuario
     * @return ID del usuario o -1 si no hay sesión
     */
    fun getUserId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context. MODE_PRIVATE).getInt(KEY_ID, -1)

    /**
     * Obtener nombre completo del usuario
     * @return "Nombres Apellidos" o null si no hay sesión
     */
    fun getFullName(ctx: Context): String? {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = sp.getString(KEY_NOMBRES, "") ?: ""
        val a = sp.getString(KEY_APELLIDOS, "") ?: ""
        val full = (n + " " + a).trim()
        return if (full.isBlank()) null else full
    }

    /**
     * Obtener nombres del usuario
     */
    fun getNombres(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context. MODE_PRIVATE).getString(KEY_NOMBRES, "") ?: ""

    /**
     * Obtener apellidos del usuario
     */
    fun getApellidos(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_APELLIDOS, "") ?: ""

    /**
     * Obtener email del usuario
     */
    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context. MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""

    /**
     * Obtener rol del usuario
     * @return "admin" o "operador"
     */
    fun getRol(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROL, "operador") ?: "operador"

    /**
     * Obtener estado del usuario
     * @return "activo" o "inactivo"
     */
    fun getEstado(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context. MODE_PRIVATE).getString(KEY_ESTADO, "activo") ?: "activo"

    /**
     * Verificar si el usuario es administrador
     */
    fun isAdmin(ctx: Context): Boolean =
        getRol(ctx) == "admin"

    /**
     * Verificar si el usuario es operador
     */
    fun isOperador(ctx: Context): Boolean =
        getRol(ctx) == "operador"

    /**
     * Verificar si el usuario está activo
     */
    fun isActivo(ctx: Context): Boolean =
        getEstado(ctx) == "activo"

    /**
     * Verificar si hay una sesión activa
     */
    fun hasActiveSession(ctx: Context): Boolean =
        getUserId(ctx) != -1 && getFullName(ctx) != null

    /**
     * Limpiar toda la sesión
     */
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Cerrar sesión y volver al login
     */
    fun logoutAndGoToLogin(activity: Activity) {
        clear(activity)
        val i = Intent(activity, MainActivity::class.java). apply {
            flags = Intent. FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(i)
        activity.finish()
    }

    /**
     * Obtener toda la información del usuario como un objeto
     */
    fun getUserInfo(ctx: Context): UserInfo?  {
        val id = getUserId(ctx)
        if (id == -1) return null

        return UserInfo(
            id = id,
            nombres = getNombres(ctx),
            apellidos = getApellidos(ctx),
            email = getEmail(ctx),
            rol = getRol(ctx),
            estado = getEstado(ctx)
        )
    }

    /**
     * Data class para información del usuario
     */
    data class UserInfo(
        val id: Int,
        val nombres: String,
        val apellidos: String,
        val email: String,
        val rol: String,
        val estado: String
    ) {
        val nombreCompleto: String
            get() = "$nombres $apellidos". trim()

        val isAdmin: Boolean
            get() = rol == "admin"

        val isActivo: Boolean
            get() = estado == "activo"
    }
}