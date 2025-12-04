package com.example.sensor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

// --- PASO 1: MODIFICAR EL CONSTRUCTOR ---
// Ahora el adaptador necesita saber si el usuario es admin y necesita un "listener"
// para comunicar las acciones a la Activity.
class EventoAdapter(
    private val esAdmin: Boolean,
    private val listener: OnEventoAdminActionsListener
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    private var eventos = listOf<Evento>()

    fun setItems(lista: List<Evento>) {
        this.eventos = lista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventos[position]
        // Le pasamos toda la información necesaria al ViewHolder para que se configure.
        holder.bind(evento, esAdmin, listener)
    }

    override fun getItemCount(): Int = eventos.size

    // --- PASO 2: AÑADIR LAS VISTAS FALTANTES AL VIEWHOLDER ---
    class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Vistas originales
        private val tvTipo: TextView = itemView.findViewById(R.id.tv_tipo_evento)
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)
        private val tvUsuario: TextView = itemView.findViewById(R.id.tv_usuario)
        private val tvSensor: TextView = itemView.findViewById(R.id.tv_sensor)
        private val tvResultado: TextView = itemView.findViewById(R.id.tv_resultado)
        private val iconResultado: ImageView = itemView.findViewById(R.id.icon_resultado)

        // Nuevas vistas para las acciones del admin
        private val layoutAcciones: LinearLayout = itemView.findViewById(R.id.layout_acciones_admin)
        private val btnAceptar: MaterialButton = itemView.findViewById(R.id.btn_aceptar)
        private val btnRechazar: MaterialButton = itemView.findViewById(R.id.btn_rechazar)

        // --- PASO 3: CREAR LA LÓGICA DE VISUALIZACIÓN ---
        fun bind(evento: Evento, esAdmin: Boolean, listener: OnEventoAdminActionsListener) {
            // Rellenar datos básicos
            tvTipo.text = evento.tipo.replace("_", " ")
            tvUsuario.text = evento.usuarioNombre
            tvFecha.text = evento.fecha
            tvSensor.text = "Sensor: ${evento.sensorCodigo}"
            tvResultado.text = evento.resultado

            // Configurar icono y color según el resultado
            if (evento.resultado == "EXITOSO" || evento.resultado == "PERMITIDO") {
                iconResultado.setImageResource(R.drawable.ic_success) // Asegúrate de tener este drawable
                tvTipo.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSuccess)) // Verde
            } else {
                iconResultado.setImageResource(R.drawable.ic_failure) // Asegúrate de tener este drawable
                tvTipo.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorError)) // Rojo
            }

            // --- LÓGICA PRINCIPAL DE VISIBILIDAD DE BOTONES ---
            if (esAdmin && evento.tipo == "ACCESO_RECHAZADO") {
                layoutAcciones.visibility = View.VISIBLE

                // Asignar los clics a los botones, notificando al listener (la Activity)
                btnAceptar.setOnClickListener {
                    listener.onAceptarAcceso(evento)
                    // Ocultamos los botones después de un clic para evitar acciones duplicadas
                    layoutAcciones.visibility = View.GONE
                }
                btnRechazar.setOnClickListener {
                    listener.onRechazarAcceso(evento)
                    layoutAcciones.visibility = View.GONE
                }
            } else {
                // Para cualquier otro caso (no es admin o no es un evento rechazado), se ocultan.
                layoutAcciones.visibility = View.GONE
            }
        }
    }

    // --- PASO 4: DEFINIR LA INTERFAZ DE COMUNICACIÓN ---
    // Este es el "contrato" que la Activity debe firmar para que el Adapter le pueda hablar.
    interface OnEventoAdminActionsListener {
        fun onAceptarAcceso(evento: Evento)
        fun onRechazarAcceso(evento: Evento)
    }
}