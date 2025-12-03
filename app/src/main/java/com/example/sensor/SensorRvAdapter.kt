package com.example.sensor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SensorRvAdapter(
    private val onClick: (Sensor) -> Unit
) : RecyclerView.Adapter<SensorRvAdapter.Vh>() {

    private var lista = listOf<Sensor>()

    fun setItems(newItems: List<Sensor>) {
        lista = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return Vh(v)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size

    inner class Vh(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCodigo: TextView = itemView.findViewById(R.id.tv_codigo_sensor)
        private val tvTipo: TextView = itemView.findViewById(R.id.tv_tipo)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        private val tvDepto: TextView = itemView.findViewById(R.id.tv_departamento)
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)

        fun bind(s: Sensor) {
            tvCodigo.text = s.codigo
            tvTipo.text = s.tipo.uppercase(Locale.getDefault())
            tvFecha.text = "Alta: ${s.fechaAlta}"
            tvDepto.text = s.departamentoNombre

            // Config Estado y Color
            val estadoUpper = s.estado.uppercase(Locale.getDefault())
            tvEstado.text = estadoUpper

            val color = when (s.estado.lowercase()) {
                "activo" -> "#4CAF50"   // Verde
                "inactivo" -> "#FF9800" // Naranja
                "perdido" -> "#F44336"  // Rojo
                "bloqueado" -> "#9E9E9E"// Gris
                else -> "#000000"
            }
            tvEstado.setTextColor(Color.parseColor(color))

            // Clic para ir a modificar (cuando crees esa pantalla)
            itemView.setOnClickListener { onClick(s) }
        }
    }
}