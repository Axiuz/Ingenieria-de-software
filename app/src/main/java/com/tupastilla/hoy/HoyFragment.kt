package com.tupastilla.hoy

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tupastilla.data.Calendario
import com.tupastilla.data.local.Estado
import com.tupastilla.data.local.TomaConMedicina
import com.tupastilla.medicinas.AltaEdicionFragment
import com.tupastilla.ui.FilaToma
import com.tupastilla.ui.HojaSeleccion
import com.tupastilla.ui.Preferencias
import com.tupastilla.ui.SeparacionDecoracion
import com.tupastilla.ui.TomaAdapter
import com.tupastilla.ui.colorDeAtributo
import kotlinx.coroutines.launch

/** A1 · Las tomas de hoy: la proxima destacada y el resto agrupado. */
class HoyFragment : Fragment(R.layout.fragment_hoy) {

    private val modelo: HoyViewModel by viewModels()
    private lateinit var adaptador: TomaAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val modo = Preferencias(requireContext()).modoVision
        adaptador = TomaAdapter(
            modoVision = modo,
            onDeshacer = { modelo.deshacer(it.toma.id) },
            onTocar = { accionesDeToma(it) }
        )

        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SeparacionDecoracion(10))
            adapter = adaptador
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                modelo.estado.collect { pintar(view, it) }
            }
        }
    }

    private fun pintar(vista: View, estado: HoyUi) {
        pintarFecha(vista, estado)
        pintarFiltroDePersonas(vista, estado)
        pintarProxima(vista, estado.proxima)
        pintarExistenciasBajas(vista, estado)
        pintarVacio(vista, estado)
        adaptador.submitList(if (estado.esCuidador) filasDelTurno(estado) else filasDe(estado))
    }

    /** Al cuidador le decimos de cuanta gente esta a cargo; al paciente, la fecha. */
    private fun pintarFecha(vista: View, estado: HoyUi) {
        val fecha = Calendario.fechaLarga(System.currentTimeMillis())
        vista.findViewById<TextView>(R.id.fecha).text = if (!estado.esCuidador) {
            fecha
        } else {
            val cuantas = estado.personas.size
            fecha + " · " + resources
                .getQuantityString(R.plurals.personas_a_cargo, cuantas, cuantas)
        }
    }

    /** Chips para ver el turno completo o el de una sola persona. */
    private fun pintarFiltroDePersonas(vista: View, estado: HoyUi) {
        val grupo = vista.findViewById<ChipGroup>(R.id.filtroPersonas)
        val contenedor = vista.findViewById<View>(R.id.filtroPersonasScroll)
        if (!estado.esCuidador || estado.personas.isEmpty()) {
            contenedor.visibility = View.GONE
            return
        }
        contenedor.visibility = View.VISIBLE

        // Se reconstruye solo si cambio el reparto: si no, cada toma confirmada
        // repintaria los chips y perderiamos el que estaba pulsado.
        val firma = estado.personas.joinToString(",") { it.id.toString() } + "|" + estado.filtro
        if (grupo.tag == firma) return
        grupo.tag = firma
        grupo.removeAllViews()

        agregarChipDeFiltro(grupo, getString(R.string.hoy_cuidador_todas), null, estado.filtro)
        estado.personas.forEach { persona ->
            agregarChipDeFiltro(grupo, persona.nombre, persona.id, estado.filtro)
        }
    }

    private fun agregarChipDeFiltro(grupo: ChipGroup, texto: String, id: Long?, filtro: Long?) {
        val chip = layoutInflater.inflate(R.layout.view_chip_dia, grupo, false) as Chip
        chip.text = texto
        chip.isChecked = id == filtro
        chip.setOnClickListener { modelo.filtrarPor(id) }
        grupo.addView(chip)
    }

    /**
     * El turno: primero la hora, debajo cada persona con lo que le toca a esa hora.
     * Es la vista util cuando se atiende a varias personas a la vez.
     */
    private fun filasDelTurno(estado: HoyUi): List<FilaToma> {
        val filas = mutableListOf<FilaToma>()
        estado.delTurno
            .groupBy { Calendario.hhmm(it.toma.programadaPara) }
            .toSortedMap()
            .forEach { (hora, deLaHora) ->
                filas.add(FilaToma.Encabezado(hora, destacado = true))
                deLaHora
                    .groupBy { it.medicina.personaId }
                    .forEach { (personaId, deLaPersona) ->
                        val persona = estado.personasPorId[personaId]
                        if (persona != null && estado.filtro == null) {
                            filas.add(FilaToma.Encabezado(persona.nombre))
                        }
                        deLaPersona.forEach {
                            filas.add(
                                FilaToma.Tarjeta(
                                    it,
                                    conDeshacer = it.toma.estado != Estado.PENDIENTE,
                                    accionable = it.toma.estado == Estado.PENDIENTE
                                )
                            )
                        }
                    }
            }
        return filas
    }

    /** Confirmar u omitir desde el turno, sin tarjeta destacada de por medio. */
    private fun accionesDeToma(item: TomaConMedicina) {
        val opciones = listOf(
            getString(R.string.a1_ya_la_tome), getString(R.string.a1_no_la_voy_a_tomar)
        )
        HojaSeleccion.nueva(item.medicina.nombre, item.medicina.dosis, opciones).apply {
            alElegir = { indice ->
                if (indice == 0) modelo.confirmar(item.toma.id) { } else preguntarMotivo(item.toma.id)
            }
        }.show(childFragmentManager, "acciones")
    }

    private fun pintarProxima(vista: View, proxima: TomaConMedicina?) {
        val tarjeta = vista.findViewById<MaterialCardView>(R.id.tarjetaProxima)
        if (proxima == null) {
            tarjeta.visibility = View.GONE
            return
        }
        tarjeta.visibility = View.VISIBLE
        tarjeta.strokeColor = requireContext().colorDeAtributo(R.attr.tpAccion)

        val hora = Calendario.hhmm(proxima.toma.programadaPara)
        val yaEsHora = proxima.toma.programadaPara <= System.currentTimeMillis()
        vista.findViewById<TextView>(R.id.proximaEtiqueta).text =
            getString(if (yaEsHora) R.string.a1_ahora else R.string.a1_a_las, hora)
        vista.findViewById<TextView>(R.id.proximaNombre).text = proxima.medicina.nombre
        vista.findViewById<TextView>(R.id.proximaDosis).text = proxima.medicina.dosis

        vista.findViewById<MaterialButton>(R.id.confirmarProxima).setOnClickListener {
            modelo.confirmar(proxima.toma.id) { nombre -> irAConfirmacion(nombre) }
        }
        vista.findViewById<MaterialButton>(R.id.omitirProxima).setOnClickListener {
            preguntarMotivo(proxima.toma.id)
        }
    }

    private fun pintarExistenciasBajas(vista: View, estado: HoyUi) {
        val banner = vista.findViewById<MaterialCardView>(R.id.bannerBajas)
        val baja = estado.existenciasBajas
        if (baja == null) {
            banner.visibility = View.GONE
            return
        }
        banner.visibility = View.VISIBLE
        banner.strokeColor = requireContext().colorDeAtributo(R.attr.tpOmitida)
        vista.findViewById<TextView>(R.id.bajasTitulo).text =
            getString(R.string.a1_bajas_titulo, baja.existencias, baja.nombre)
        banner.setOnClickListener { preguntarCaja(baja.id) }
    }

    private fun pintarVacio(vista: View, estado: HoyUi) {
        val vacio = vista.findViewById<View>(R.id.estadoVacio)
        vacio.visibility = if (estado.sinMedicinas) View.VISIBLE else View.GONE
        if (!estado.sinMedicinas) return

        // Al cuidador sin personas no le sirve dar de alta una medicina: primero
        // tiene que decir a quien cuida.
        val sinPersonas = estado.esCuidador && estado.personas.isEmpty()

        vista.findViewById<TextView>(R.id.vacioTitulo)
            .setText(if (sinPersonas) R.string.personas_vacio_titulo else R.string.a1_sin_meds_titulo)
        vista.findViewById<TextView>(R.id.vacioDesc)
            .setText(if (sinPersonas) R.string.personas_vacio_desc else R.string.a1_sin_meds_desc)

        vista.findViewById<MaterialButton>(R.id.vacioBoton).apply {
            visibility = if (estado.puedeEditar) View.VISIBLE else View.GONE
            setText(if (sinPersonas) R.string.personas_agregar else R.string.a4b_agregar_corto)
            setOnClickListener {
                if (sinPersonas) {
                    (activity as MainActivity).irAPestana(1)
                } else {
                    (activity as MainActivity).apilar(AltaEdicionFragment.nueva(), "alta")
                }
            }
        }
    }

    /** Las dos secciones del diseno: lo que falta y lo que ya se resolvio. */
    private fun filasDe(estado: HoyUi): List<FilaToma> {
        val filas = mutableListOf<FilaToma>()
        if (estado.masTarde.isNotEmpty()) {
            filas.add(FilaToma.Encabezado(getString(R.string.a1_mas_tarde)))
            estado.masTarde.forEach { filas.add(FilaToma.Tarjeta(it)) }
        }
        if (estado.resueltas.isNotEmpty()) {
            filas.add(FilaToma.Encabezado(getString(R.string.a1_ya_resueltas)))
            estado.resueltas.forEach { filas.add(FilaToma.Tarjeta(it, conDeshacer = true)) }
        }
        return filas
    }

    /** El motivo es opcional: cerrar la hoja tambien omite, sin motivo. */
    private fun preguntarMotivo(tomaId: Long) {
        val motivos = listOf(
            getString(R.string.motivo_olvido), getString(R.string.motivo_mal),
            getString(R.string.motivo_sin_medicina), getString(R.string.motivo_reservado)
        )
        HojaSeleccion.nueva(
            getString(R.string.motivo_titulo), getString(R.string.motivo_subtitulo), motivos
        ).apply {
            alElegir = { indice -> modelo.omitir(tomaId, motivos[indice].lowercase()) }
            alCancelar = { modelo.omitir(tomaId, null) }
        }.show(childFragmentManager, "motivo")
    }

    private fun preguntarCaja(medicinaId: Long) {
        val cajas = listOf(14, 28, 30)
        HojaSeleccion.nueva(
            getString(R.string.resurtir_titulo), null, cajas.map { it.toString() }
        ).apply {
            alElegir = { indice -> modelo.resurtir(medicinaId, cajas[indice]) }
        }.show(childFragmentManager, "resurtir")
    }

    private fun irAConfirmacion(nombre: String) {
        val texto = getString(R.string.a3_texto, nombre, Calendario.hhmm(System.currentTimeMillis()))
        (activity as MainActivity).mostrarConfirmacion(texto)
    }
}
