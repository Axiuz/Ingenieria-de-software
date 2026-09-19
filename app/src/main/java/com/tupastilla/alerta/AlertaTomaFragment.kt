package com.tupastilla.alerta

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.R
import com.tupastilla.data.Calendario
import com.tupastilla.data.local.TomaConMedicina

/** A2 · El contenido de la alerta. Solo pinta; la Activity decide que hacer. */
class AlertaTomaFragment : Fragment(R.layout.fragment_alerta_toma) {

    var item: TomaConMedicina? = null
    var alConfirmar: (() -> Unit)? = null
    var alPosponer: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toma = item ?: return
        val hora = Calendario.hhmm(toma.toma.programadaPara)

        view.findViewById<TextView>(R.id.etiquetaHora).text = getString(R.string.a2_es_hora, hora)
        view.findViewById<TextView>(R.id.nombre).text = toma.medicina.nombre
        view.findViewById<TextView>(R.id.dosis).text = toma.medicina.dosis

        view.findViewById<MaterialButton>(R.id.confirmar).apply {
            setOnClickListener { alConfirmar?.invoke() }
            // El foco inicial es confirmar, que es lo que se espera a esta hora.
            requestFocus()
        }
        view.findViewById<MaterialButton>(R.id.posponer)
            .setOnClickListener { alPosponer?.invoke() }
    }
}
