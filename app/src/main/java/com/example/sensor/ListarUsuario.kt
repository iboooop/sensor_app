package com.example.sensor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import java.util.Locale

class ListarUsuario : AppCompatActivity() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var api: UsuarioApiService
    private lateinit var adapter: UsuarioRvAdapter

    private var usuarios = mutableListOf<Usuario>()
    private var visibles = mutableListOf<Usuario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listarusuario)

        api = UsuarioApiService(this)
        rvUsuarios = findViewById(R.id.lista_usuarios)
        etSearch = findViewById(R.id.search_usuarios)

        rvUsuarios.layoutManager = LinearLayoutManager(this)

        // ===== CORRECCIÓN AQUÍ =====
        // El parámetro ahora se llama "onEventsClick", igual que en el constructor del Adapter.
        adapter = UsuarioRvAdapter(
            onEditClick = { usuario -> abrirModificar(usuario) },
            onEventsClick = { usuario -> abrirListarEventos(usuario) }
        )
        // ============================

        rvUsuarios.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filtrar(s?.toString().orEmpty()) }
        })
    }

    // El resto de tu código es perfecto y no necesita cambios.
    override fun onStart() {
        super.onStart()
        cargar()
    }

    private fun cargar() {
        api.listarUsuarios(
            onSuccess = { data ->
                usuarios = data.toMutableList()
                visibles = data.toMutableList()
                adapter.setItems(visibles)
            },
            onError = { msg ->
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("No se pudo cargar")
                    .setContentText(msg)
                    .setConfirmText("OK").show()
            }
        )
    }

    private fun filtrar(q: String) {
        val needle = q.trim().lowercase(Locale.getDefault())
        visibles = if (needle.isEmpty()) usuarios.toMutableList() else usuarios.filter { u ->
            u.nombre.lowercase(Locale.getDefault()).contains(needle) ||
                    u.apellido.lowercase(Locale.getDefault()).contains(needle) ||
                    u.email.lowercase(Locale.getDefault()).contains(needle)
        }.toMutableList()
        adapter.setItems(visibles)
    }

    private fun abrirModificar(u: Usuario) {
        val i = Intent(this, ModificarUsuario::class.java).apply {
            putExtra("usuario_id", u.id)
            putExtra("usuario_nombre", u.nombre)
            putExtra("usuario_apellido", u.apellido)
            putExtra("usuario_email", u.email)
        }
        startActivity(i)
    }

    private fun abrirListarEventos(u: Usuario) {
        val i = Intent(this, ListarEventosActivity::class.java).apply {
            putExtra("usuario_id", u.id)
        }
        startActivity(i)
    }
}