package com.tupastilla.alerta

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.R
import com.tupastilla.ui.colorDeAtributo

/**
 * A3 · La pantalla que cierra el circulo despues de confirmar una toma. Se usa desde
 * la alerta de pantalla completa y desde la lista de Hoy.
 */
class ConfirmacionFragment : Fragment(R.layout.fragment_confirmacion) {

    /** Lo pone quien la abre: volver al inicio o cerrar la Activity de la alerta. */
    var alVolver: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val detalle = requireArguments().getString(DETALLE).orEmpty()
        val color = requireContext().colorDeAtributo(R.attr.tpConfirmada)

        view.findViewById<TextView>(R.id.circulo).apply {
            text = getString(R.string.glifo_confirmada)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.tp_sobre_accion))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }
        view.findViewById<TextView>(R.id.detalle).text = detalle
        view.findViewById<MaterialButton>(R.id.volver).setOnClickListener { alVolver?.invoke() }

        // TalkBack lee el resultado sin que haya que ir a buscarlo. La API esta
        // marcada como obsoleta pero sigue siendo la unica que funciona en minSdk 24.
        @Suppress("DEPRECATION")
        view.announceForAccessibility(getString(R.string.a3_listo) + ". " + detalle)
    }

    companion object {
        private const val DETALLE = "detalle"

        fun nueva(detalle: String) = ConfirmacionFragment().apply {
            arguments = Bundle().apply { putString(DETALLE, detalle) }
        }
    }
}
