package com.tupastilla.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.medicinas.AltaEdicionFragment

/** C15 · Ultimo paso. Agregar reutiliza B6; saltar cierra el onboarding. */
class PrimeraMedicinaFragment : Fragment(R.layout.fragment_primera_medicina) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)

        view.findViewById<MaterialButton>(R.id.agregar).setOnClickListener {
            (activity as MainActivity).avanzar(AltaEdicionFragment.nueva(primeraVez = true))
        }
        view.findViewById<MaterialButton>(R.id.saltar).setOnClickListener {
            (activity as MainActivity).terminarOnboarding()
        }
    }
}
