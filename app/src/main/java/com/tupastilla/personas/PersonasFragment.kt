package com.tupastilla.personas

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Persona
import com.tupastilla.data.local.PersonaConMedicinas
import com.tupastilla.medicinas.MisMedicinasFragment
import com.tupastilla.ui.SeparacionDecoracion
import kotlinx.coroutines.launch

/**
 * La pestana 2 en el rol cuidador: a cuantas personas cuida y que lleva cada una.
 * Tocar una persona abre su lista de medicamentos; mantener pulsado, sus opciones.
 */
class PersonasFragment : Fragment(R.layout.fragment_personas) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adaptador = PersonaAdapter(
            alAbrir = { item ->
                (activity as MainActivity).apilar(
                    MisMedicinasFragment.nueva(item.persona.id, item.persona.nombre), "medicinas"
                )
            },
            alMantener = { item -> menuDePersona(item) }
        )

        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SeparacionDecoracion(10))
            adapter = adaptador
        }
        view.findViewById<MaterialButton>(R.id.agregar).setOnClickListener { editarPersona(null) }
        view.findViewById<TextView>(R.id.vacioTitulo).setText(R.string.personas_vacio_titulo)
        view.findViewById<TextView>(R.id.vacioDesc).setText(R.string.personas_vacio_desc)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Grafo.medicamentos(requireContext()).personasConMedicinas().collect { personas ->
                    adaptador.submitList(personas)
                    view.findViewById<View>(R.id.estadoVacio).visibility =
                        if (personas.isEmpty()) View.VISIBLE else View.GONE
                    view.findViewById<TextView>(R.id.cuenta).text = resources
                        .getQuantityString(R.plurals.personas_a_cargo, personas.size, personas.size)
                }
            }
        }
    }

    private fun menuDePersona(item: PersonaConMedicinas) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.persona.nombre)
            .setItems(
                arrayOf(getString(R.string.persona_editar), getString(R.string.persona_quitar))
            ) { _, indice ->
                if (indice == 0) editarPersona(item.persona) else confirmarBorrado(item.persona)
            }
            .show()
    }

    private fun editarPersona(persona: Persona?) {
        val vista = layoutInflater.inflate(R.layout.dialogo_persona, null)
        val nombre = vista.findViewById<android.widget.EditText>(R.id.entradaNombre)
        val nota = vista.findViewById<android.widget.EditText>(R.id.entradaNota)
        nombre.setText(persona?.nombre.orEmpty())
        nota.setText(persona?.nota.orEmpty())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (persona == null) R.string.persona_nueva else R.string.persona_editar)
            .setView(vista)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.b6_guardar) { _, _ ->
                val texto = nombre.text.toString().trim()
                if (texto.isBlank()) {
                    avisar(getString(R.string.persona_error_nombre))
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    Grafo.medicamentos(requireContext()).guardarPersona(
                        (persona ?: Persona(nombre = texto)).copy(
                            nombre = texto, nota = nota.text.toString().trim()
                        )
                    )
                }
            }
            .show()
    }

    private fun confirmarBorrado(persona: Persona) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.persona_quitar_confirmar, persona.nombre))
            .setMessage(R.string.persona_quitar_desc)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.b10_quitar_si) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Grafo.medicamentos(requireContext()).borrarPersona(persona.id)
                }
            }
            .show()
    }

    private fun avisar(mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(mensaje)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
