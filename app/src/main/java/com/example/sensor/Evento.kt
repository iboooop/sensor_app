package com.example.sensor

data class Evento(
    val id: Int,
    val tipo: String,
    val resultado: String,
    val fecha: String,
    val usuarioNombre: String,
    val sensorCodigo: String
)