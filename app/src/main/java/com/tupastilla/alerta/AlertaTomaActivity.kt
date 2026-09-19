package com.tupastilla.alerta

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tupastilla.R
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.alarma.Notificaciones
import com.tupastilla.data.Calendario
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.TomaConMedicina
import com.tupastilla.ui.Tema
import kotlinx.coroutines.launch

/**
 * A2 + A3 · Activity propia y no un fragment de MainActivity: se lanza desde un
 * full-screen intent con el telefono bloqueado, asi que necesita showWhenLocked y
 * turnScreenOn, que son propiedades de Activity.
 */
class AlertaTomaActivity : AppCompatActivity() {

    private var tomaId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        Tema.aplicar(this, R.style.Theme_TuPastilla_Alerta)
        super.onCreate(savedInstanceState)
        mostrarSobreLaPantallaBloqueada()
        setContentView(R.layout.activity_alerta_toma)

        tomaId = intent.getLongExtra(AlarmaScheduler.EXTRA_TOMA_ID, -1L)
        if (tomaId < 0) {
            finish()
            return
        }

        lifecycleScope.launch {
            val item = Grafo.medicamentos(this@AlertaTomaActivity).toma(tomaId)
            if (item == null) finish() else mostrarAlerta(item)
        }
    }

    private fun mostrarAlerta(item: TomaConMedicina) {
        val fragment = AlertaTomaFragment().apply {
            this.item = item
            alConfirmar = { confirmar(item) }
            alPosponer = { posponer(item) }
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    private fun confirmar(item: TomaConMedicina) {
        lifecycleScope.launch {
            val ahora = System.currentTimeMillis()
            Grafo.medicamentos(this@AlertaTomaActivity).confirmar(item.toma.id, ahora)
            AlarmaScheduler(this@AlertaTomaActivity).cancelar(item.toma.id)
            Notificaciones.quitar(this@AlertaTomaActivity, item.toma.id)

            val perfil = Grafo.perfiles(this@AlertaTomaActivity).actual()
            val texto = getString(
                if (perfil?.vinculado == true && perfil.avisoFamiliar) R.string.a3_texto_familiar
                else R.string.a3_texto,
                item.medicina.nombre, Calendario.hhmm(ahora)
            )
            val confirmacion = ConfirmacionFragment.nueva(texto).apply { alVolver = { finish() } }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, confirmacion).commit()
        }
    }

    /** "Ahora no" no omite la toma: la deja pendiente y vuelve a avisar en 10 minutos. */
    private fun posponer(item: TomaConMedicina) {
        Notificaciones.quitar(this, item.toma.id)
        AlarmaScheduler(this).programarRepeticion(item.toma)
        finish()
    }

    @Suppress("DEPRECATION")
    private fun mostrarSobreLaPantallaBloqueada() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
