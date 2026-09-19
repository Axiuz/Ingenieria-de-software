package com.tupastilla.medicinas

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
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.ui.Roles
import com.tupastilla.ui.SeparacionDecoracion
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * A4b · La lista de medicinas.
 *
 * Sin argumentos es la lista propia del paciente. Con una persona es la de esa
 * persona, que es como entra el cuidador desde su lista de personas a cargo.
 */
class MisMedicinasFragment : Fragment(R.layout.fragment_mis_medicinas) {

    private val personaId: Long?
        get() = arguments?.getLong(PERSONA_ID, -1L)?.takeIf { it >= 0 }

    private val nombrePersona: String?
        get() = arguments?.getString(NOMBRE_PERSONA)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pintarCabecera(view)

        val adaptador = MedicinaAdapter { item ->
            (activity as MainActivity).apilar(DetalleMedicinaFragment.nueva(item.medicina.id), "detalle")
        }
        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SeparacionDecoracion(10))
            adapter = adaptador
        }
        val agregar = view.findViewById<MaterialButton>(R.id.agregar)
        agregar.setOnClickListener {
            (activity as MainActivity)
                .apilar(AltaEdicionFragment.nueva(personaId = personaId), "alta")
        }

        view.findViewById<TextView>(R.id.vacioTitulo).setText(R.string.a4b_vacia_titulo)
        view.findViewById<TextView>(R.id.vacioDesc).setText(R.string.a4b_vacia_desc)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val repositorio = Grafo.medicamentos(requireContext())
                val lista = personaId?.let { repositorio.medicinasDe(it) } ?: repositorio.medicinas()
                combine(
                    lista,
                    Grafo.perfiles(requireContext()).observar()
                ) { medicinas, perfil -> medicinas to Roles.puedeEditar(perfil?.rol) }
                    .collect { (medicinas, puedeEditar) ->
                        adaptador.submitList(medicinas)
                        view.findViewById<View>(R.id.estadoVacio).visibility =
                            if (medicinas.isEmpty()) View.VISIBLE else View.GONE

                        // A4a: el mismo layout, sin boton de alta y con el aviso.
                        agregar.visibility = if (puedeEditar) View.VISIBLE else View.GONE
                        view.findViewById<View>(R.id.avisoSoloLectura).visibility =
                            if (puedeEditar) View.GONE else View.VISIBLE
                    }
            }
        }
    }

    /** Con persona: titulo con su nombre y boton de volver. Sin persona, la propia. */
    private fun pintarCabecera(vista: View) {
        val volver = vista.findViewById<MaterialButton>(R.id.volver)
        val nombre = nombrePersona
        if (nombre == null) {
            volver.visibility = View.GONE
            return
        }
        volver.visibility = View.VISIBLE
        volver.setText(R.string.personas_titulo)
        volver.setOnClickListener { (activity as MainActivity).volver() }
        vista.findViewById<TextView>(R.id.titulo).text =
            getString(R.string.medicamentos_de, nombre)
    }

    companion object {
        private const val PERSONA_ID = "persona_id"
        private const val NOMBRE_PERSONA = "nombre_persona"

        fun nueva(personaId: Long, nombre: String) = MisMedicinasFragment().apply {
            arguments = Bundle().apply {
                putLong(PERSONA_ID, personaId)
                putString(NOMBRE_PERSONA, nombre)
            }
        }
    }
}
