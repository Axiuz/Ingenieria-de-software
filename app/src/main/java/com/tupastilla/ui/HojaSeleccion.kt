package com.tupastilla.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.R

/**
 * Hoja inferior con una pregunta y unas cuantas respuestas grandes. La usan los
 * motivos de omision y el tamano de la caja al resurtir.
 *
 * Cada opcion es un boton completo de ?attr/tpTouchMin, no una fila de lista: el
 * area tactil minima no es negociable.
 */
class HojaSeleccion : BottomSheetDialogFragment() {

    /** Lo rellena quien la abre; se pierde al girar la pantalla, y ahi se cierra sola. */
    var alElegir: ((Int) -> Unit)? = null

    /** Cancelar la hoja tambien es una respuesta: omitir sin motivo, por ejemplo. */
    var alCancelar: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.hoja_seleccion, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val argumentos = requireArguments()
        view.findViewById<TextView>(R.id.hojaTitulo).text = argumentos.getString(TITULO)
        view.findViewById<TextView>(R.id.hojaSubtitulo).apply {
            val subtitulo = argumentos.getString(SUBTITULO)
            text = subtitulo
            visibility = if (subtitulo.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        val contenedor = view.findViewById<LinearLayout>(R.id.hojaOpciones)
        argumentos.getStringArrayList(OPCIONES)?.forEachIndexed { indice, texto ->
            contenedor.addView(botonDeOpcion(contenedor, texto, indice), parametros(indice))
        }

        view.findViewById<MaterialButton>(R.id.hojaCancelar).setOnClickListener {
            alCancelar?.invoke()
            dismiss()
        }
    }

    private fun botonDeOpcion(contenedor: LinearLayout, texto: String, indice: Int): MaterialButton {
        val boton = layoutInflater
            .inflate(R.layout.view_boton_opcion, contenedor, false) as MaterialButton
        boton.text = texto
        boton.setOnClickListener {
            alElegir?.invoke(indice)
            dismiss()
        }
        return boton
    }

    private fun parametros(indice: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        requireContext().dimensionDeAtributo(R.attr.tpTouchMin)
    ).apply { if (indice > 0) topMargin = requireContext().dp(10f) }

    companion object {
        private const val TITULO = "titulo"
        private const val SUBTITULO = "subtitulo"
        private const val OPCIONES = "opciones"

        fun nueva(titulo: String, subtitulo: String?, opciones: List<String>) = HojaSeleccion().apply {
            arguments = Bundle().apply {
                putString(TITULO, titulo)
                putString(SUBTITULO, subtitulo)
                putStringArrayList(OPCIONES, ArrayList(opciones))
            }
        }
    }
}
