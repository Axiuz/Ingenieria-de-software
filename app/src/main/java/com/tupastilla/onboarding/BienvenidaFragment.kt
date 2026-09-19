package com.tupastilla.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R

/** C0 · Paso 1 de 5. El rol ya viene de la cuenta, asi que se pasa directo al modo de vision. */
class BienvenidaFragment : Fragment(R.layout.fragment_bienvenida) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)
        view.findViewById<MaterialButton>(R.id.empezar).setOnClickListener {
            (activity as MainActivity).avanzar(ModoVisionFragment())
        }
    }
}
