package com.tupastilla.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.tupastilla.R
import com.tupastilla.data.Calendario
import com.tupastilla.data.local.Estado
import com.tupastilla.data.local.TomaConMedicina

/**
 * Traduce un estado de toma a lo que se ve. Los 12 casos del diseno (3 estados x 4
 * modos de vision) salen de aqui y comparten un solo layout.
 *
 * La regla que sostiene los cuatro modos: el estado decide el glifo, el relleno del
 * icono, el grosor del borde y el texto de la etiqueta. El modo solo decide de que
 * atributo sale el color. Nunca se distingue un estado por color a secas.
 */
data class EstadoVisual(
    val titulo: String,
    val meta: String,
    val glifo: String,
    val color: Int,
    val colorGlifo: Int,
    val iconoRelleno: Boolean,
    val bordeTarjetaDp: Float,
    val bordeDiscontinuo: Boolean,
    val descripcion: String
) {

    /**
     * El circulo del icono: relleno o calado, siempre del color del estado.
     *
     * [colorEstado] es una copia local a proposito: dentro de apply sobre un
     * GradientDrawable, `color` resolveria a la propiedad del drawable (un
     * ColorStateList nulo) y no a la de EstadoVisual, y no se pintaria nada.
     */
    fun fondoDelIcono(context: Context): GradientDrawable {
        val colorEstado = color
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (iconoRelleno) {
                setColor(colorEstado)
            } else {
                setColor(TRANSPARENTE)
                setStroke(context.dp(2f), colorEstado)
            }
        }
    }

    /** El borde de la tarjeta: 2 dp normal, 4 dp si es omitida sin color, discontinuo si es pendiente sin color. */
    fun fondoDeLaTarjeta(context: Context): GradientDrawable {
        val colorEstado = color
        val grosor = context.dp(bordeTarjetaDp)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.getDimension(R.dimen.tp_radio_tarjeta)
            setColor(TRANSPARENTE)
            if (bordeDiscontinuo) {
                setStroke(grosor, colorEstado, context.dp(6f).toFloat(), context.dp(5f).toFloat())
            } else {
                setStroke(grosor, colorEstado)
            }
        }
    }

    companion object {

        /** Para la lista de Hoy: la meta habla del dia de hoy. */
        fun deHoy(context: Context, item: TomaConMedicina, modo: Int): EstadoVisual =
            construir(context, item, modo, momento = null)

        /** Para el historial: la meta lleva delante el dia al que pertenece la toma. */
        fun deHistorial(context: Context, item: TomaConMedicina, modo: Int, dia: String): EstadoVisual =
            construir(context, item, modo, momento = dia)

        private fun construir(
            context: Context,
            item: TomaConMedicina,
            modo: Int,
            momento: String?
        ): EstadoVisual {
            val nombre = item.medicina.nombre
            val hora = Calendario.hhmm(item.toma.programadaPara)
            val sobreAccion = ContextCompat.getColor(context, R.color.tp_sobre_accion)
            val sinColor = modo == MODO_SIN_COLOR

            return when (item.toma.estado) {
                Estado.CONFIRMADA -> {
                    val color = context.colorDeAtributo(R.attr.tpConfirmada)
                    val confirmada = Calendario.hhmm(item.toma.confirmadaEn ?: item.toma.programadaPara)
                    EstadoVisual(
                        titulo = context.getString(R.string.estado_confirmada_titulo, nombre),
                        meta = momento?.let { "$it $hora" }
                            ?: context.getString(R.string.estado_confirmada_meta, confirmada),
                        glifo = context.getString(R.string.glifo_confirmada),
                        color = color, colorGlifo = sobreAccion, iconoRelleno = true,
                        bordeTarjetaDp = 2f, bordeDiscontinuo = false,
                        descripcion = "$nombre, ${item.medicina.dosis}, $hora, tomada"
                    )
                }

                Estado.OMITIDA -> {
                    val color = context.colorDeAtributo(R.attr.tpOmitida)
                    val motivo = item.toma.motivo ?: context.getString(R.string.estado_omitida_sin_motivo)
                    EstadoVisual(
                        titulo = context.getString(R.string.estado_omitida_titulo, nombre),
                        meta = context.getString(
                            R.string.estado_omitida_meta,
                            momento?.let { "$it $hora" } ?: hora, motivo
                        ),
                        glifo = context.getString(R.string.glifo_omitida),
                        color = color,
                        // Sin color el icono va calado, no relleno: es la unica senal que queda.
                        colorGlifo = if (sinColor) color else sobreAccion,
                        iconoRelleno = !sinColor,
                        bordeTarjetaDp = if (sinColor) 4f else 2f, bordeDiscontinuo = false,
                        descripcion = "$nombre, ${item.medicina.dosis}, $hora, no tomada"
                    )
                }

                else -> {
                    val color = context.colorDeAtributo(R.attr.tpPendiente)
                    EstadoVisual(
                        titulo = context.getString(R.string.estado_pendiente_titulo, nombre),
                        meta = context.getString(
                            R.string.estado_pendiente_meta,
                            momento?.let { "$it $hora" } ?: hora
                        ),
                        glifo = context.getString(R.string.glifo_pendiente),
                        color = color, colorGlifo = color, iconoRelleno = false,
                        bordeTarjetaDp = 2f, bordeDiscontinuo = sinColor,
                        descripcion = "$nombre, ${item.medicina.dosis}, $hora, pendiente"
                    )
                }
            }
        }

        const val MODO_SIN_COLOR = 4
        private const val TRANSPARENTE = 0x00000000
    }
}
