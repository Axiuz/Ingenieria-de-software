package com.tupastilla.onboarding

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.ui.ModosDeVision
import com.tupastilla.ui.colorDeAtributo
import com.tupastilla.ui.dp
import kotlinx.coroutines.launch

/**
 * C11 · Se elige por comparacion, nunca por diagnostico: cada opcion muestra las tres
 * senales (relleno, borde y linea discontinua) con los colores de ese modo, y el
 * usuario dice en cual las distingue mejor.
 */
class ModoVisionFragment : Fragment(R.layout.fragment_modo_vision) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)
        val contenedor = view.findViewById<LinearLayout>(R.id.muestras)

        ModosDeVision.todos.forEach { modo ->
            val opcion = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_muestra_modo, contenedor, false)

            // El truco: resolvemos los colores contra el overlay de ese modo, sin
            // cambiar el tema de la pantalla.
            val contextoDelModo = ContextThemeWrapper(requireContext(), ModosDeVision.overlayDe(modo.numero))
            pintarMuestra(opcion.findViewById(R.id.muestra1), contextoDelModo.colorDeAtributo(R.attr.tpConfirmada), relleno = true, discontinuo = false)
            pintarMuestra(opcion.findViewById(R.id.muestra2), contextoDelModo.colorDeAtributo(R.attr.tpOmitida), relleno = modo.numero != 4, discontinuo = false)
            pintarMuestra(opcion.findViewById(R.id.muestra3), contextoDelModo.colorDeAtributo(R.attr.tpPendiente), relleno = false, discontinuo = true)

            val etiqueta = getString(R.string.c11_opcion, modo.numero, getString(modo.pista))
            opcion.findViewById<TextView>(R.id.etiqueta).text = etiqueta
            opcion.contentDescription = etiqueta
            opcion.setOnClickListener { elegir(modo.numero) }
            (opcion.layoutParams as LinearLayout.LayoutParams).topMargin =
                if (contenedor.childCount > 0) requireContext().dp(12f) else 0
            contenedor.addView(opcion)
        }

        // Quien no ve diferencia en ninguna se queda en el modo que no depende del color.
        view.findViewById<MaterialButton>(R.id.inseguro).setOnClickListener { elegir(4) }
    }

    private fun pintarMuestra(vista: View, color: Int, relleno: Boolean, discontinuo: Boolean) {
        vista.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = requireContext().dp(10f).toFloat()
            if (relleno) {
                setColor(color)
            } else {
                setColor(0x00000000)
                val grosor = requireContext().dp(3f)
                if (discontinuo) {
                    setStroke(grosor, color, requireContext().dp(6f).toFloat(), requireContext().dp(5f).toFloat())
                } else {
                    setStroke(grosor, color)
                }
            }
        }
    }

    private fun elegir(modo: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            Grafo.perfiles(requireContext()).fijarModoVision(modo)
            (activity as MainActivity).avanzar(DensidadFragment())
        }
    }
}
