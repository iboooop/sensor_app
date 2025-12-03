package com.example.sensor

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val telefono: String = "",
    val rut: String = "",
    val rol: String = "",
    val estado: String = "",
    val idDepartamento: Int = 0,
    // Nuevo campo para mostrar en la lista
    val departamentoNombre: String = ""
)