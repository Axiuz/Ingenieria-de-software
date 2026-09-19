package com.tupastilla.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.alarma.Notificaciones

/**
 * C13 · Los dos permisos que hacen falta para avisar a tiempo, explicados por lo que
 * consiguen y no por su nombre de sistema. Los dos se pueden saltar.
 */
class PermisosFragment : Fragment(R.layout.fragment_permisos) {

    private val pedirNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> marcarConcedido(R.id.permitirAvisos, concedido) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)
        Notificaciones.crearCanal(requireContext())

        view.findViewById<MaterialButton>(R.id.permitirExacta).setOnClickListener {
            pedirAlarmaExacta()
        }
        view.findViewById<MaterialButton>(R.id.permitirAvisos).setOnClickListener {
            pedirNotificacionesDelSistema()
        }
        view.findViewById<MaterialButton>(R.id.continuar).setOnClickListener { avanzar() }
        view.findViewById<MaterialButton>(R.id.masTarde).setOnClickListener { avanzar() }
    }

    override fun onResume() {
        super.onResume()
        // Volver de los ajustes del sistema: hay que releer el estado real.
        marcarConcedido(R.id.permitirExacta, AlarmaScheduler(requireContext()).puedeProgramarExactas())
        marcarConcedido(R.id.permitirAvisos, tieneNotificaciones())
    }

    /** En API 24-30 la alarma exacta no se pide: se tiene. */
    private fun pedirAlarmaExacta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            marcarConcedido(R.id.permitirExacta, true)
            return
        }
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }

    /** POST_NOTIFICATIONS solo existe desde Android 13. */
    private fun pedirNotificacionesDelSistema() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            marcarConcedido(R.id.permitirAvisos, true)
            return
        }
        pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun tieneNotificaciones(): Boolean =
        androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

    private fun marcarConcedido(botonId: Int, concedido: Boolean) {
        val boton = view?.findViewById<MaterialButton>(botonId) ?: return
        boton.setText(if (concedido) R.string.c13_permitido else R.string.c13_permitir)
        boton.isEnabled = !concedido
    }

    private fun avanzar() {
        (activity as MainActivity).avanzar(PrimeraMedicinaFragment())
    }
}
