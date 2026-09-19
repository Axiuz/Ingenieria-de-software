package com.tupastilla.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tupastilla.R
import com.tupastilla.data.local.TomaConMedicina

/** Una fila de la lista de tomas: o un encabezado de seccion, o una tarjeta. */
sealed interface FilaToma {
    /** [destacado] lo usa el turno del cuidador para la hora; sin el, es una persona. */
    data class Encabezado(val texto: String, val destacado: Boolean = false) : FilaToma

    data class Tarjeta(
        val item: TomaConMedicina,
        /** "Ayer", "24 ago"… solo en el historial. En Hoy va null. */
        val dia: String? = null,
        val conDeshacer: Boolean = false,
        /** Tocarla abre las acciones de la toma. Lo usa el turno del cuidador. */
        val accionable: Boolean = false
    ) : FilaToma
}

/**
 * La misma tarjeta en Hoy y en Historial. Es el componente que mas se reutiliza, por
 * eso vive en ui/ y no dentro de una pantalla.
 */
class TomaAdapter(
    private val modoVision: Int,
    private val onDeshacer: (TomaConMedicina) -> Unit = {},
    private val onTocar: (TomaConMedicina) -> Unit = {}
) : ListAdapter<FilaToma, RecyclerView.ViewHolder>(Diferencias) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is FilaToma.Encabezado) TIPO_ENCABEZADO else TIPO_TARJETA

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflador = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_ENCABEZADO) {
            EncabezadoViewHolder(inflador.inflate(R.layout.item_encabezado, parent, false))
        } else {
            TarjetaViewHolder(inflador.inflate(R.layout.item_tarjeta_medicamento, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = getItem(position)) {
            is FilaToma.Encabezado -> (holder as EncabezadoViewHolder).pintar(fila)
            is FilaToma.Tarjeta -> (holder as TarjetaViewHolder).pintar(fila)
        }
    }

    private class EncabezadoViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        fun pintar(fila: FilaToma.Encabezado) {
            val texto = itemView as TextView
            texto.text = fila.texto
            // La hora manda sobre el nombre de la persona, que va sangrado debajo.
            texto.setTextColor(
                if (fila.destacado) texto.context.colorDeAtributo(R.attr.tpAccion)
                else ContextCompat.getColor(texto.context, R.color.tp_texto_terciario)
            )
            texto.setPadding(if (fila.destacado) 0 else texto.context.dp(8f), 0, 0, 0)

            // La hora abre seccion y respira; el nombre de la persona va pegado a ella.
            (texto.layoutParams as? ViewGroup.MarginLayoutParams)?.let { parametros ->
                parametros.topMargin = texto.context.dp(if (fila.destacado) 22f else 12f)
                parametros.bottomMargin = texto.context.dp(6f)
            }
        }
    }

    private inner class TarjetaViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        private val glifo: TextView = vista.findViewById(R.id.glifo)
        private val titulo: TextView = vista.findViewById(R.id.titulo)
        private val meta: TextView = vista.findViewById(R.id.meta)
        private val deshacer: MaterialButton = vista.findViewById(R.id.deshacer)

        /** setBackground borra el padding del layout, asi que lo guardamos antes. */
        private val relleno = vista.paddingLeft

        fun pintar(fila: FilaToma.Tarjeta) {
            val contexto = itemView.context
            val visual = if (fila.dia == null) {
                EstadoVisual.deHoy(contexto, fila.item, modoVision)
            } else {
                EstadoVisual.deHistorial(contexto, fila.item, modoVision, fila.dia)
            }

            itemView.background = visual.fondoDeLaTarjeta(contexto)
            itemView.setPadding(relleno, relleno, relleno, relleno)
            itemView.contentDescription = visual.descripcion

            glifo.text = visual.glifo
            glifo.setTextColor(visual.colorGlifo)
            glifo.background = visual.fondoDelIcono(contexto)

            titulo.text = visual.titulo
            meta.text = visual.meta

            deshacer.visibility = if (fila.conDeshacer) View.VISIBLE else View.GONE
            deshacer.setOnClickListener { onDeshacer(fila.item) }

            itemView.isClickable = fila.accionable
            if (fila.accionable) itemView.setOnClickListener { onTocar(fila.item) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<FilaToma>() {
        override fun areItemsTheSame(a: FilaToma, b: FilaToma): Boolean = when {
            a is FilaToma.Encabezado && b is FilaToma.Encabezado -> a.texto == b.texto
            a is FilaToma.Tarjeta && b is FilaToma.Tarjeta -> a.item.toma.id == b.item.toma.id
            else -> false
        }

        override fun areContentsTheSame(a: FilaToma, b: FilaToma): Boolean = a == b
    }

    private companion object {
        const val TIPO_ENCABEZADO = 0
        const val TIPO_TARJETA = 1
    }
}

/** Separacion uniforme entre filas, para no repetir margenes en cada layout. */
class SeparacionDecoracion(private val separacionDp: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.getChildAdapterPosition(view) > 0) {
            outRect.top = view.context.dp(separacionDp.toFloat())
        }
    }
}
