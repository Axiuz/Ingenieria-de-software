package com.tupastilla.personas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tupastilla.R
import com.tupastilla.data.local.PersonaConMedicinas
import java.util.Locale

/** Las personas a cargo, con cuantos medicamentos lleva cada una. */
class PersonaAdapter(
    private val alAbrir: (PersonaConMedicinas) -> Unit,
    private val alMantener: (PersonaConMedicinas) -> Unit
) : ListAdapter<PersonaConMedicinas, PersonaAdapter.Fila>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Fila(
        LayoutInflater.from(parent.context).inflate(R.layout.item_persona, parent, false)
    )

    override fun onBindViewHolder(holder: Fila, position: Int) = holder.pintar(getItem(position))

    inner class Fila(vista: View) : RecyclerView.ViewHolder(vista) {
        private val iniciales: TextView = vista.findViewById(R.id.iniciales)
        private val nombre: TextView = vista.findViewById(R.id.nombre)
        private val resumen: TextView = vista.findViewById(R.id.resumen)

        fun pintar(item: PersonaConMedicinas) {
            val contexto = itemView.context
            nombre.text = item.persona.nombre
            iniciales.text = inicialesDe(item.persona.nombre)

            val cuantas = item.medicinas.size
            val cuenta = if (cuantas == 0) {
                contexto.getString(R.string.persona_sin_medicinas)
            } else {
                contexto.resources.getQuantityString(R.plurals.medicamentos_cuenta, cuantas, cuantas)
            }
            resumen.text = listOf(cuenta, item.persona.nota)
                .filter { it.isNotBlank() }
                .joinToString(" · ")

            itemView.contentDescription = "${item.persona.nombre}. ${resumen.text}"
            itemView.setOnClickListener { alAbrir(item) }
            itemView.setOnLongClickListener {
                alMantener(item)
                true
            }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<PersonaConMedicinas>() {
        override fun areItemsTheSame(a: PersonaConMedicinas, b: PersonaConMedicinas) =
            a.persona.id == b.persona.id

        override fun areContentsTheSame(a: PersonaConMedicinas, b: PersonaConMedicinas) = a == b
    }

    companion object {
        /** "Rosa Méndez" a "RM". Lo usan la lista y el encabezado de Hoy. */
        fun inicialesDe(nombre: String): String = nombre
            .split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.take(1).uppercase(Locale.forLanguageTag("es")) }
    }
}
