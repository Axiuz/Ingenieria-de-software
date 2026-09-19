package com.tupastilla.medicinas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Calendario
import com.tupastilla.data.ExpansorHorarios
import com.tupastilla.ui.SeparacionDecoracion
import java.util.Locale

/** B7 · Horas, dias de la semana y vigencia de una medicina. */
class HorariosFragment : Fragment(R.layout.fragment_horarios) {

    private val modelo: EdicionViewModel by activityViewModels()
    private lateinit var adaptador: HorarioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nombre = modelo.borrador.nombre
        view.findViewById<TextView>(R.id.titulo).text =
            if (nombre.isBlank()) getString(R.string.b7_titulo_solo)
            else getString(R.string.b7_titulo, nombre)
        view.findViewById<MaterialButton>(R.id.volver).setOnClickListener {
            (activity as MainActivity).volver()
        }
        view.findViewById<MaterialButton>(R.id.listo).setOnClickListener {
            (activity as MainActivity).volver()
        }
        view.findViewById<MaterialButton>(R.id.otraHora).setOnClickListener { elegirHora() }

        adaptador = HorarioAdapter { indice ->
            modelo.quitarHora(indice)
            refrescar(view)
        }
        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SeparacionDecoracion(8))
            adapter = adaptador
        }

        pintarSugeridas(view)
        pintarDias(view)
        view.findViewById<View>(R.id.filaDesde).setOnClickListener { elegirFecha(view, esDesde = true) }
        view.findViewById<View>(R.id.filaHasta).setOnClickListener { elegirFecha(view, esDesde = false) }
        refrescar(view)
    }

    private fun refrescar(vista: View) {
        adaptador.submitList(modelo.borrador.horarios.toList())
        vista.findViewById<TextView>(R.id.valorDesde).text =
            Calendario.fechaLarga(modelo.borrador.desde.takeIf { it > 0 } ?: System.currentTimeMillis())
        vista.findViewById<TextView>(R.id.valorHasta).text =
            modelo.borrador.hasta?.let { Calendario.fechaLarga(it) }
                ?: getString(R.string.b7_sin_termino)
    }

    /** Cuatro horas frecuentes a un toque; el reloj queda para el resto. */
    private fun pintarSugeridas(vista: View) {
        val grupo = vista.findViewById<ChipGroup>(R.id.sugeridas)
        listOf("08:00", "14:00", "20:00", "22:00").forEach { hora ->
            val chip = layoutInflater.inflate(R.layout.view_chip_hora, grupo, false) as Chip
            chip.text = getString(R.string.b7_agregar_hora, hora)
            chip.setOnClickListener {
                modelo.agregarHora(hora)
                refrescar(vista)
            }
            grupo.addView(chip)
        }
    }

    private fun pintarDias(vista: View) {
        val grupo = vista.findViewById<ChipGroup>(R.id.dias)
        val seleccionados = modelo.diasActuales().toMutableSet()
        (1..7).forEach { dia ->
            val chip = layoutInflater.inflate(R.layout.view_chip_dia, grupo, false) as Chip
            chip.text = Calendario.DIAS[dia - 1].take(3)
                .replaceFirstChar { it.uppercase(Locale.forLanguageTag("es")) }
            chip.isChecked = dia in seleccionados
            chip.contentDescription = Calendario.DIAS[dia - 1]
            chip.setOnCheckedChangeListener { _, marcado ->
                if (marcado) seleccionados.add(dia) else seleccionados.remove(dia)
                modelo.fijarDias(seleccionados)
                refrescar(vista)
            }
            grupo.addView(chip)
        }
    }

    private fun elegirHora() {
        val reloj = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(8).setMinute(0)
            .build()
        reloj.addOnPositiveButtonClickListener {
            modelo.agregarHora(String.format(Locale.US, "%02d:%02d", reloj.hour, reloj.minute))
            view?.let { refrescar(it) }
        }
        reloj.show(childFragmentManager, "hora")
    }

    private fun elegirFecha(vista: View, esDesde: Boolean) {
        val calendario = MaterialDatePicker.Builder.datePicker().build()
        calendario.addOnPositiveButtonClickListener { millis ->
            modelo.actualizar { if (esDesde) copy(desde = millis) else copy(hasta = millis) }
            refrescar(vista)
        }
        calendario.show(childFragmentManager, if (esDesde) "desde" else "hasta")
    }
}

/** Las horas ya elegidas, con su boton de quitar. */
class HorarioAdapter(
    private val alQuitar: (Int) -> Unit
) : RecyclerView.Adapter<HorarioAdapter.Fila>() {

    private var horarios: List<HorarioBorrador> = emptyList()

    fun submitList(nuevos: List<HorarioBorrador>) {
        horarios = nuevos
        notifyDataSetChanged()
    }

    override fun getItemCount() = horarios.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Fila(
        LayoutInflater.from(parent.context).inflate(R.layout.item_horario, parent, false)
    )

    override fun onBindViewHolder(holder: Fila, position: Int) =
        holder.pintar(horarios[position], position)

    inner class Fila(vista: View) : RecyclerView.ViewHolder(vista) {
        private val hora: TextView = vista.findViewById(R.id.hora)
        private val dias: TextView = vista.findViewById(R.id.dias)
        private val quitar: MaterialButton = vista.findViewById(R.id.quitar)

        fun pintar(horario: HorarioBorrador, posicion: Int) {
            hora.text = horario.hora
            dias.text = ExpansorHorarios.descripcion(ExpansorHorarios.textoDe(horario.dias))
            itemView.contentDescription = "${horario.hora}, ${dias.text}"
            quitar.contentDescription =
                itemView.context.getString(R.string.b7_quitar) + " " + horario.hora
            quitar.setOnClickListener { alQuitar(posicion) }
        }
    }
}
