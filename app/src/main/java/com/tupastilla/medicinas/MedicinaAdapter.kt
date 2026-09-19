package com.tupastilla.medicinas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tupastilla.R
import com.tupastilla.data.ExpansorHorarios
import com.tupastilla.data.local.MedicinaConHorarios

/** A4b · Nombre y resumen de horarios. El detalle se ve en B10. */
class MedicinaAdapter(
    private val alAbrir: (MedicinaConHorarios) -> Unit
) : ListAdapter<MedicinaConHorarios, MedicinaAdapter.Fila>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Fila(
        LayoutInflater.from(parent.context).inflate(R.layout.item_medicina, parent, false)
    )

    override fun onBindViewHolder(holder: Fila, position: Int) = holder.pintar(getItem(position))

    inner class Fila(vista: View) : RecyclerView.ViewHolder(vista) {
        private val nombre: TextView = vista.findViewById(R.id.nombre)
        private val resumen: TextView = vista.findViewById(R.id.resumen)

        fun pintar(item: MedicinaConHorarios) {
            val contexto = itemView.context
            nombre.text = item.medicina.nombre
            resumen.text = resumenDe(item, contexto.getString(R.string.a4b_sin_horario))
            itemView.contentDescription = "${item.medicina.nombre}. ${resumen.text}"
            itemView.setOnClickListener { alAbrir(item) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<MedicinaConHorarios>() {
        override fun areItemsTheSame(a: MedicinaConHorarios, b: MedicinaConHorarios) =
            a.medicina.id == b.medicina.id

        override fun areContentsTheSame(a: MedicinaConHorarios, b: MedicinaConHorarios) = a == b
    }

    companion object {
        /** "08:00 y 20:00 · todos los días" */
        fun resumenDe(item: MedicinaConHorarios, sinHorario: String): String {
            if (item.horarios.isEmpty()) return sinHorario
            val horas = item.horarios.sortedBy { it.hora }.joinToString(" y ") { it.hora }
            val dias = ExpansorHorarios.descripcion(item.horarios.first().diasSemana)
            return "$horas · ${dias.lowercase()}"
        }
    }
}
