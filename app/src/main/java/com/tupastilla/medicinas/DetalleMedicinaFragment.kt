package com.tupastilla.medicinas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.ExpansorHorarios
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.MedicinaConHorarios
import com.tupastilla.ui.HojaSeleccion
import com.tupastilla.ui.Roles
import com.tupastilla.ui.colorDeAtributo
import kotlinx.coroutines.launch

/** B10 · Ficha completa, existencias y las dos acciones destructivas. */
class DetalleMedicinaFragment : Fragment(R.layout.fragment_detalle_medicina) {

    private val medicinaId: Long get() = requireArguments().getLong(MEDICINA_ID)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialButton>(R.id.volver).apply {
            setText(R.string.b10_volver)
            setOnClickListener { (activity as MainActivity).volver() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Grafo.medicamentos(requireContext()).medicina(medicinaId).collect { item ->
                    // Se borro desde otro sitio: no hay nada que mostrar.
                    if (item == null) (activity as MainActivity).volver() else pintar(view, item)
                }
            }
        }
    }

    private fun pintar(vista: View, item: MedicinaConHorarios) {
        vista.findViewById<TextView>(R.id.nombre).text = item.medicina.nombre
        pintarFilas(vista, item)
        pintarCaja(vista, item)

        // En solo lectura la ficha se consulta, pero no se edita ni se borra.
        val editar = vista.findViewById<MaterialButton>(R.id.editar)
        val borrar = vista.findViewById<MaterialButton>(R.id.borrar)
        editar.setOnClickListener {
            (activity as MainActivity).apilar(AltaEdicionFragment.nueva(item.medicina.id), "alta")
        }
        borrar.setOnClickListener { confirmarBorrado(item) }

        viewLifecycleOwner.lifecycleScope.launch {
            val puedeEditar = Roles.puedeEditar(Grafo.perfiles(requireContext()).actual()?.rol)
            if (!isAdded) return@launch
            val visible = if (puedeEditar) View.VISIBLE else View.GONE
            editar.visibility = visible
            borrar.visibility = visible
            vista.findViewById<MaterialButton>(R.id.resurtir).visibility = visible
        }
    }

    private fun pintarFilas(vista: View, item: MedicinaConHorarios) {
        val contenedor = vista.findViewById<LinearLayout>(R.id.filas)
        contenedor.removeAllViews()

        val horarios = if (item.horarios.isEmpty()) {
            getString(R.string.a4b_sin_horario)
        } else {
            item.horarios.sortedBy { it.hora }.joinToString("   ") {
                it.hora + " · " + ExpansorHorarios.descripcion(it.diasSemana).lowercase()
            }
        }

        listOf(
            getString(R.string.b10_dosis) to item.medicina.dosis.ifBlank { getString(R.string.b10_sin_especificar) },
            getString(R.string.b10_forma) to item.medicina.forma.ifBlank { getString(R.string.b10_sin_especificar) },
            getString(R.string.b10_instrucciones) to item.medicina.instrucciones.ifBlank { getString(R.string.b10_sin_indicaciones) },
            getString(R.string.b10_horarios) to horarios
        ).forEach { (etiqueta, valor) ->
            val fila = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_fila_dato, contenedor, false)
            fila.findViewById<TextView>(R.id.etiqueta).text = etiqueta
            fila.findViewById<TextView>(R.id.valor).text = valor
            contenedor.addView(fila)
        }
    }

    /** Bajo el umbral el borde pasa a ?attr/tpOmitida, no solo el texto. */
    private fun pintarCaja(vista: View, item: MedicinaConHorarios) {
        val bajas = item.medicina.existencias <= item.medicina.umbralAviso
        val tomasPorDia = item.horarios.size.coerceAtLeast(1)

        vista.findViewById<TextView>(R.id.existencias).text = item.medicina.existencias.toString()
        vista.findViewById<TextView>(R.id.existenciasTexto).text =
            if (bajas) getString(R.string.b10_pocas)
            else getString(R.string.b10_alcanzan, item.medicina.existencias / tomasPorDia)

        vista.findViewById<MaterialCardView>(R.id.tarjetaCaja).strokeColor =
            if (bajas) requireContext().colorDeAtributo(R.attr.tpOmitida)
            else requireContext().getColor(R.color.tp_borde)

        vista.findViewById<MaterialButton>(R.id.resurtir).setOnClickListener {
            val cajas = listOf(14, 28, 30)
            HojaSeleccion.nueva(getString(R.string.resurtir_titulo), null, cajas.map { it.toString() })
                .apply {
                    alElegir = { indice ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            Grafo.medicamentos(requireContext())
                                .resurtir(item.medicina.id, cajas[indice])
                        }
                    }
                }.show(childFragmentManager, "resurtir")
        }
    }

    private fun confirmarBorrado(item: MedicinaConHorarios) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.b10_quitar_confirmar, item.medicina.nombre))
            .setMessage(R.string.b10_quitar_desc)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.b10_quitar_si) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Grafo.medicamentos(requireContext()).borrar(item.medicina.id)
                    (activity as MainActivity).irAPestana(1)
                }
            }
            .show()
    }

    companion object {
        private const val MEDICINA_ID = "medicina_id"

        fun nueva(medicinaId: Long) = DetalleMedicinaFragment().apply {
            arguments = Bundle().apply { putLong(MEDICINA_ID, medicinaId) }
        }
    }
}
