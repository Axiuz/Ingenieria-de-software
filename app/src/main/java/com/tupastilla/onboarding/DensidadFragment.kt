package com.tupastilla.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Densidad
import kotlinx.coroutines.launch

/** C11b · El mismo contenido a dos escalas. Se elige el que se lee mejor. */
class DensidadFragment : Fragment(R.layout.fragment_densidad) {

    private var elegida = Densidad.ACCESIBLE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)

        view.findViewById<View>(R.id.opcionGrande).setOnClickListener {
            elegida = Densidad.ACCESIBLE
            marcar(view)
        }
        view.findViewById<View>(R.id.opcionCompacta).setOnClickListener {
            elegida = Densidad.COMPACTA
            marcar(view)
        }
        view.findViewById<MaterialButton>(R.id.continuar).setOnClickListener { continuar() }
        marcar(view)
    }

    /** La opcion elegida se marca con transparencia, no con color: el modo 4 lo exige. */
    private fun marcar(vista: View) {
        val grande = vista.findViewById<View>(R.id.opcionGrande)
        val compacta = vista.findViewById<View>(R.id.opcionCompacta)
        grande.alpha = if (elegida == Densidad.ACCESIBLE) 1f else 0.55f
        compacta.alpha = if (elegida == Densidad.COMPACTA) 1f else 0.55f
        grande.isSelected = elegida == Densidad.ACCESIBLE
        compacta.isSelected = elegida == Densidad.COMPACTA
    }

    private fun continuar() {
        viewLifecycleOwner.lifecycleScope.launch {
            Grafo.perfiles(requireContext()).fijarDensidad(elegida)
            // recreate() aplica el ThemeOverlay de densidad y vuelve al onboarding.
            (activity as MainActivity).avanzar(PermisosFragment())
        }
    }
}
