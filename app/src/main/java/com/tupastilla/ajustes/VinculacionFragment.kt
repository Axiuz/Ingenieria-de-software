package com.tupastilla.ajustes

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.data.repo.PerfilRepository
import kotlinx.coroutines.launch

/**
 * C12a · Codigo de seis digitos que vence en 15 minutos. Se dicta en voz alta, por eso
 * va grande y con letterSpacing: hay que poder leerlo de un vistazo.
 */
class VinculacionFragment : Fragment(R.layout.fragment_vinculacion) {

    private var cuentaAtras: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialButton>(R.id.volver).apply {
            setText(R.string.c12a_volver)
            setOnClickListener { (activity as MainActivity).volver() }
        }
        view.findViewById<MaterialButton>(R.id.regenerar).setOnClickListener { generar(view) }

        viewLifecycleOwner.lifecycleScope.launch {
            val perfil = Grafo.perfiles(requireContext()).actual()
            val restante = perfil?.codigoGeneradoEn?.let {
                PerfilRepository.VIGENCIA_CODIGO_MS - (System.currentTimeMillis() - it)
            } ?: 0L
            // Solo se reaprovecha un codigo que siga vivo; si no, se genera otro.
            if (perfil?.codigoVinculo != null && restante > 0) {
                mostrar(view, perfil.codigoVinculo, restante)
            } else {
                generar(view)
            }
        }
    }

    private fun generar(vista: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val codigo = Grafo.perfiles(requireContext()).generarCodigo()
            mostrar(vista, codigo, PerfilRepository.VIGENCIA_CODIGO_MS)
        }
    }

    private fun mostrar(vista: View, codigo: String, restanteMs: Long) {
        val texto = vista.findViewById<TextView>(R.id.codigo)
        texto.text = codigo.toCharArray().joinToString(" ")
        // TalkBack lee digito a digito, no "cuatrocientos setenta y dos mil...".
        texto.contentDescription = codigo.toCharArray().joinToString(", ")

        val vence = vista.findViewById<TextView>(R.id.vence)
        cuentaAtras?.cancel()
        cuentaAtras = object : CountDownTimer(restanteMs, 1_000L) {
            override fun onTick(faltan: Long) {
                val segundos = faltan / 1000
                vence.text = getString(R.string.c12a_vence, segundos / 60, segundos % 60)
            }

            override fun onFinish() {
                vence.setText(R.string.c12a_vencido)
            }
        }.also { it.start() }
    }

    override fun onDestroyView() {
        cuentaAtras?.cancel()
        cuentaAtras = null
        super.onDestroyView()
    }
}
