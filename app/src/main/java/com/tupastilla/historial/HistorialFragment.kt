package com.tupastilla.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.tupastilla.R
import com.tupastilla.data.Calendario
import com.tupastilla.data.Cumplimiento
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.TomaConMedicina
import com.tupastilla.ui.FilaToma
import com.tupastilla.ui.Preferencias
import com.tupastilla.ui.SeparacionDecoracion
import com.tupastilla.ui.TomaAdapter
import com.tupastilla.ui.colorDeAtributo
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * B8 + B9 · El cumplimiento arriba y el historial agrupado por dia debajo, con la
 * misma tarjeta de toma que usa Hoy.
 */
class HistorialFragment : Fragment(R.layout.fragment_historial) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val modo = Preferencias(requireContext()).modoVision
        val adaptador = TomaAdapter(modo)

        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SeparacionDecoracion(10))
            adapter = adaptador
        }
        view.findViewById<TextView>(R.id.vacioTitulo).setText(R.string.b8_vacio_titulo)
        view.findViewById<TextView>(R.id.vacioDesc).setText(R.string.b8_vacio_desc)

        val repositorio = Grafo.medicamentos(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    repositorio.historial(),
                    repositorio.tomasDeHoy(),
                    repositorio.cumplimiento()
                ) { historial, hoy, cumplimiento ->
                    Triple(historial, hoy, cumplimiento)
                }.collect { (historial, hoy, cumplimiento) ->
                    val resueltasDeHoy = hoy.filter {
                        it.toma.estado != com.tupastilla.data.local.Estado.PENDIENTE
                    }
                    val filas = filasDe(resueltasDeHoy + historial)
                    adaptador.submitList(filas)
                    pintarCumplimiento(view, cumplimiento, filas.isNotEmpty())
                    view.findViewById<View>(R.id.estadoVacio).visibility =
                        if (filas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    /** Un encabezado por dia y debajo sus tomas, de lo mas reciente a lo mas antiguo. */
    private fun filasDe(tomas: List<TomaConMedicina>): List<FilaToma> {
        val ayer = Calendario.inicioDelDia(System.currentTimeMillis()) - Calendario.DIA_MS
        return tomas
            .sortedByDescending { it.toma.programadaPara }
            .groupBy { Calendario.inicioDelDia(it.toma.programadaPara) }
            .toSortedMap(compareByDescending { it })
            .flatMap { (dia, delDia) ->
                val titulo = when (dia) {
                    Calendario.inicioDelDia(System.currentTimeMillis()) -> getString(R.string.a1_titulo)
                    ayer -> getString(R.string.b8_ayer)
                    else -> Calendario.fechaLarga(dia)
                }
                val corto = if (dia == ayer) getString(R.string.b8_ayer) else Calendario.fechaCorta(dia)
                listOf(FilaToma.Encabezado(titulo)) + delDia.map { FilaToma.Tarjeta(it, dia = corto) }
            }
    }

    private fun pintarCumplimiento(vista: View, cumplimiento: Cumplimiento, hayDatos: Boolean) {
        val tarjeta = vista.findViewById<MaterialCardView>(R.id.tarjetaCumplimiento)
        tarjeta.visibility = if (hayDatos) View.VISIBLE else View.GONE
        if (!hayDatos) return

        vista.findViewById<TextView>(R.id.cumplimientoMes).text =
            getString(R.string.b9_cumplimiento, Calendario.mesYAno(System.currentTimeMillis()))
        vista.findViewById<TextView>(R.id.porcentaje).text = "${cumplimiento.porcentaje}%"

        vista.findViewById<LinearProgressIndicator>(R.id.barra).apply {
            max = 100
            setProgressCompat(cumplimiento.porcentaje, true)
            setIndicatorColor(requireContext().colorDeAtributo(R.attr.tpConfirmada))
        }

        val contenedor = vista.findViewById<LinearLayout>(R.id.resumen)
        contenedor.removeAllViews()
        listOf(
            getString(R.string.b9_confirmadas) to
                getString(R.string.b9_confirmadas_v, cumplimiento.confirmadas, cumplimiento.total),
            getString(R.string.b9_omitidas) to cumplimiento.omitidas.toString(),
            getString(R.string.b9_pendientes) to cumplimiento.pendientesHoy.toString()
        ).forEach { (etiqueta, valor) ->
            val fila = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_fila_resumen, contenedor, false)
            fila.findViewById<TextView>(R.id.etiqueta).text = etiqueta
            fila.findViewById<TextView>(R.id.valor).text = valor
            contenedor.addView(fila)
        }
    }
}
