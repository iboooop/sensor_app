package com.example.sensor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SensorRvAdapter(
    private val onClick: (Sensor) -> Unit
) : RecyclerView.Adapter<SensorRvAdapter.SensorViewHolder>() {

    private var items = listOf<Sensor>()

    fun setItems(newItems: List<Sensor>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount() = items.size

    class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCodigo: TextView = itemView.findViewById(R.id.tv_codigo_sensor)
        private val tvTipo: TextView = itemView.findViewById(R.id.tv_tipo)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        private val tvDepto: TextView = itemView.findViewById(R.id.tv_departamento)
        private val tvUsuario: TextView = itemView.findViewById(R.id.tv_usuario_asignado) // <--- NUEVO
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)

        fun bind(sensor: Sensor, onClick: (Sensor) -> Unit) {
            tvCodigo.text = sensor.codigo
            tvTipo.text = sensor.tipo.uppercase()
            tvDepto.text = sensor.departamentoNombre
            tvFecha.text = "Alta: ${sensor.fechaAlta}"

            // Mostrar usuario
            tvUsuario.text = if (sensor.usuarioNombre == "Sin asignar" || sensor.usuarioNombre.isEmpty()) {
                "Sin Usuario Asignado"
            } else {
                "Usuario: ${sensor.usuarioNombre}"
            }

            // Lógica de colores para estado
            tvEstado.text = sensor.estado.uppercase()
            when (sensor.estado.lowercase()) {
                "activo" -> tvEstado.setTextColor(Color.parseColor("#4CAF50")) // Verde
                "inactivo" -> tvEstado.setTextColor(Color.parseColor("#F44336")) // Rojo
                "perdido" -> tvEstado.setTextColor(Color.parseColor("#FF9800")) // Naranja
                else -> tvEstado.setTextColor(Color.GRAY)
            }

            itemView.setOnClickListener { onClick(sensor) }
        }
    }
}