package com.tupastilla.medicinas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.data.ExpansorHorarios
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Horario
import com.tupastilla.data.local.Medicina
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Un horario mientras se edita, antes de tocar la base. */
data class HorarioBorrador(val hora: String, val dias: Set<Int>)

/** El formulario de B6 y B7 mientras se llena. */
data class Borrador(
    val id: Long = 0,
    /** A quien le toca esta medicina. */
    val personaId: Long = 0,
    val nombre: String = "",
    val dosis: String = "",
    val forma: String = "",
    val instrucciones: String = "",
    val existencias: Int = 30,
    val horarios: List<HorarioBorrador> = emptyList(),
    val desde: Long = 0,
    val hasta: Long? = null
)

/**
 * Compartido por AltaEdicionFragment y HorariosFragment con activityViewModels: son
 * dos pantallas del mismo formulario, no dos formularios.
 */
class EdicionViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio = Grafo.medicamentos(aplicacion)
    private val alarmas = AlarmaScheduler(aplicacion)

    var borrador = Borrador()
        private set

    /**
     * Carga la medicina a editar, o deja el formulario en blanco para un alta.
     * En un alta sin persona indicada se usa la de por defecto, que es lo que pasa
     * en los roles autonomo y supervisado: solo hay una.
     */
    fun preparar(medicinaId: Long?, personaId: Long?, nombrePorDefecto: String, alCargar: () -> Unit) {
        if (medicinaId == null) {
            viewModelScope.launch {
                val destino = personaId ?: repositorio.personaPorDefecto(nombrePorDefecto)
                borrador = Borrador(personaId = destino, desde = System.currentTimeMillis())
                alCargar()
            }
            return
        }
        viewModelScope.launch {
            // Un unico valor: el formulario no se recarga solo mientras se edita.
            val conHorarios = repositorio.medicina(medicinaId).first() ?: return@launch
            borrador = Borrador(
                id = conHorarios.medicina.id,
                personaId = conHorarios.medicina.personaId,
                nombre = conHorarios.medicina.nombre,
                dosis = conHorarios.medicina.dosis,
                forma = conHorarios.medicina.forma,
                instrucciones = conHorarios.medicina.instrucciones,
                existencias = conHorarios.medicina.existencias,
                horarios = conHorarios.horarios.map {
                    HorarioBorrador(it.hora, ExpansorHorarios.diasDe(it.diasSemana))
                },
                desde = conHorarios.horarios.firstOrNull()?.desde ?: System.currentTimeMillis(),
                hasta = conHorarios.horarios.firstOrNull()?.hasta
            )
            alCargar()
        }
    }

    fun actualizar(cambio: Borrador.() -> Borrador) {
        borrador = borrador.cambio()
    }

    fun agregarHora(hora: String) {
        if (borrador.horarios.any { it.hora == hora }) return
        val dias = ExpansorHorarios.diasDe(Horario.TODOS_LOS_DIAS)
        borrador = borrador.copy(
            horarios = (borrador.horarios + HorarioBorrador(hora, dias)).sortedBy { it.hora }
        )
    }

    fun quitarHora(indice: Int) {
        borrador = borrador.copy(horarios = borrador.horarios.filterIndexed { i, _ -> i != indice })
    }

    /** Los dias se aplican a todos los horarios de la medicina, como en el diseno. */
    fun fijarDias(dias: Set<Int>) {
        if (dias.isEmpty()) return
        borrador = borrador.copy(horarios = borrador.horarios.map { it.copy(dias = dias) })
    }

    fun diasActuales(): Set<Int> =
        borrador.horarios.firstOrNull()?.dias ?: ExpansorHorarios.diasDe(Horario.TODOS_LOS_DIAS)

    /** Guarda y reprograma las alarmas de la medicina. Devuelve false si falta el nombre. */
    fun guardar(alGuardar: (Long) -> Unit): Boolean {
        val actual = borrador
        if (actual.nombre.isBlank()) return false

        viewModelScope.launch {
            val medicina = Medicina(
                id = actual.id, personaId = actual.personaId,
                nombre = actual.nombre.trim(), dosis = actual.dosis.trim(),
                forma = actual.forma.trim(), instrucciones = actual.instrucciones.trim(),
                existencias = actual.existencias
            )
            val horarios = actual.horarios.map {
                Horario(
                    medicinaId = actual.id, hora = it.hora,
                    diasSemana = ExpansorHorarios.textoDe(it.dias),
                    desde = actual.desde, hasta = actual.hasta
                )
            }
            val id = repositorio.guardar(medicina, horarios)
            launch(Dispatchers.IO) { alarmas.reprogramarTodo() }
            alGuardar(id)
        }
        return true
    }
}
