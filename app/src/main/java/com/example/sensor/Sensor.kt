package com.example.sensor

data class Sensor(
    val id: Int,
    val codigo: String,
    val estado: String, // 'activo', 'inactivo', 'perdido', 'bloqueado'
    val tipo: String,   // 'llavero', 'tarjeta'
    val fechaAlta: String,
    val departamentoNombre: String // Para mostrar "Torre X - Depto Y"
)