package com.example.sensor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

// 1. CAMBIO: El constructor ahora recibe dos acciones en lugar de una.
class UsuarioRvAdapter(
    private val onEditClick: (Usuario) -> Unit,
    private val onEventsClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioRvAdapter.Vh>() {

    private var lista = listOf<Usuario>()

    fun setItems(newItems: List<Usuario>) {
        lista = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return Vh(v)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size

    inner class Vh(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- Tu código existente se mantiene ---
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_completo)
        private val tvRol: TextView = itemView.findViewById(R.id.tv_rol)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        private val tvEmail: TextView = itemView.findViewById(R.id.tv_email)
        private val tvDepto: TextView = itemView.findViewById(R.id.tv_departamento)
        // ------------------------------------

        // 2. AÑADIDO: Referencias a los dos botones.
        private val btnEditar: Button = itemView.findViewById(R.id.btn_editar)
        private val btnVerEventos: Button = itemView.findViewById(R.id.btn_ver_eventos)


        fun bind(u: Usuario) {
            // --- Tu lógica para poner los datos se mantiene intacta ---
            tvNombre.text = "${u.nombre} ${u.apellido}"
            tvEmail.text = u.email

            // Config Rol
            tvRol.text = u.rol.uppercase(Locale.getDefault())

            // Config Estado
            tvEstado.text = u.estado.uppercase(Locale.getDefault())
            if (u.estado.equals("activo", true)) {
                tvEstado.setTextColor(Color.parseColor("#4CAF50")) // Verde
            } else {
                tvEstado.setTextColor(Color.parseColor("#F44336")) // Rojo
            }

            // Config Depto
            if (u.departamentoNombre.isNotEmpty()) {
                tvDepto.text = u.departamentoNombre
                tvDepto.visibility = View.VISIBLE
            } else {
                tvDepto.visibility = View.GONE
            }
            // --------------------------------------------------------

            // 3. CAMBIO: Se elimina el listener del item completo y se añaden los de los botones.
            // itemView.setOnClickListener { onClick(u) } // Se elimina esta línea
            btnEditar.setOnClickListener { onEditClick(u) }
            btnVerEventos.setOnClickListener { onEventsClick(u) }
        }
    }
}