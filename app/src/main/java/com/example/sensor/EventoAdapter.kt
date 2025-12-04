package com.example.sensor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventoAdapter : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    private var lista = listOf<Evento>()

    fun setItems(items: List<Evento>) {
        this.lista = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val item = lista[position]

        holder.tvUsuario.text = item.usuarioNombre
        holder.tvSensor.text = "Sensor: ${item.sensorCodigo}"
        holder.tvFecha.text = item.fecha
        holder.tvResultado.text = item.resultado
        holder.tvTipo.text = item.tipo.replace("_", " ")

        // Colores según tipo de evento
        if (item.tipo.contains("RECHAZADO") || item.resultado.contains("Fallido")) {
            holder.tvTipo.setTextColor(Color.RED)
        } else {
            holder.tvTipo.setTextColor(Color.parseColor("#2E7D32")) // Verde
        }
    }

    override fun getItemCount() = lista.size

    class EventoViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTipo: TextView = v.findViewById(R.id.tv_tipo_evento)
        val tvFecha: TextView = v.findViewById(R.id.tv_fecha)
        val tvUsuario: TextView = v.findViewById(R.id.tv_usuario)
        val tvSensor: TextView = v.findViewById(R.id.tv_sensor)
        val tvResultado: TextView = v.findViewById(R.id.tv_resultado)
    }
}