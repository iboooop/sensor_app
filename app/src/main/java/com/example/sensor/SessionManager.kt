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

    fun saveUser(ctx: Context, id: Int, nombres: String, apellidos: String, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRES, nombres)
            .putString(KEY_APELLIDOS, apellidos)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getUserId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ID, -1)

    fun getFullName(ctx: Context): String? {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = sp.getString(KEY_NOMBRES, "") ?: ""
        val a = sp.getString(KEY_APELLIDOS, "") ?: ""
        val full = (n + " " + a).trim()
        return if (full.isBlank()) null else full
    }

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