package com.example.sensor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsuarioRvAdapter(
    private val onItemClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioRvAdapter.VH>() {

    private val data = mutableListOf<Usuario>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvNombre: TextView = v.findViewById(R.id.tv_nombre)
        private val tvEmail: TextView = v.findViewById(R.id.tv_email)
        fun bind(u: Usuario) {
            tvNombre.text = "${u.nombre} ${u.apellido}".trim()
            tvEmail.text = u.email
            itemView.setOnClickListener { onItemClick(u) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    override fun getItemCount(): Int = data.size

    fun setItems(items: List<Usuario>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }
}