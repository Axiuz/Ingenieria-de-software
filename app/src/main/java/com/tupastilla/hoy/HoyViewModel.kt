package com.tupastilla.hoy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.alarma.Notificaciones
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Estado
import com.tupastilla.data.local.Medicina
import com.tupastilla.data.local.Persona
import com.tupastilla.data.local.Rol
import com.tupastilla.data.local.TomaConMedicina
import com.tupastilla.ui.Roles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lo que necesita pintar A1, ya resuelto.
 *
 * El cuidador ve el turno completo: las tomas de todas las personas a su cargo
 * ordenadas por hora. El autonomo y el supervisado ven solo las suyas, con la
 * proxima destacada.
 */
data class HoyUi(
    val proxima: TomaConMedicina? = null,
    val masTarde: List<TomaConMedicina> = emptyList(),
    val resueltas: List<TomaConMedicina> = emptyList(),
    /** Turno del cuidador: pendientes y resueltas juntas, en orden de hora. */
    val delTurno: List<TomaConMedicina> = emptyList(),
    val personas: List<Persona> = emptyList(),
    val personasPorId: Map<Long, Persona> = emptyMap(),
    val filtro: Long? = null,
    val existenciasBajas: Medicina? = null,
    val sinMedicinas: Boolean = false,
    val puedeEditar: Boolean = true,
    val esCuidador: Boolean = false
)

class HoyViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio = Grafo.medicamentos(aplicacion)
    private val perfiles = Grafo.perfiles(aplicacion)
    private val alarmas = AlarmaScheduler(aplicacion)

    val estado: StateFlow<HoyUi> = combine(
        repositorio.tomasDeHoy(),
        repositorio.existenciasBajas(),
        repositorio.medicinas(),
        repositorio.personas(),
        perfiles.observar()
    ) { tomas, bajas, medicinas, personas, perfil ->
        val esCuidador = perfil?.rol == Rol.CUIDADOR
        val filtro = perfil?.personaActivaId?.takeIf { id -> personas.any { it.id == id } }
        val porId = personas.associateBy { it.id }

        // El filtro solo existe para el cuidador; los demas roles tienen una persona.
        val visibles = if (esCuidador && filtro != null) {
            tomas.filter { it.medicina.personaId == filtro }
        } else {
            tomas
        }
        val pendientes = visibles.filter { it.toma.estado == Estado.PENDIENTE }

        // El aviso de existencias tambien respeta el filtro: estando en Luis no
        // tiene sentido avisar de una caja de Rosa.
        val bajasVisibles = if (filtro == null) bajas else bajas.filter { it.personaId == filtro }

        HoyUi(
            proxima = if (esCuidador) null else pendientes.firstOrNull(),
            masTarde = if (esCuidador) emptyList() else pendientes.drop(1),
            resueltas = if (esCuidador) emptyList() else visibles.filter { it.toma.estado != Estado.PENDIENTE },
            delTurno = if (esCuidador) visibles else emptyList(),
            personas = personas,
            personasPorId = porId,
            filtro = filtro,
            existenciasBajas = bajasVisibles.firstOrNull(),
            sinMedicinas = medicinas.isEmpty(),
            puedeEditar = Roles.puedeEditar(perfil?.rol),
            esCuidador = esCuidador
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoyUi())

    /** Al arrancar rellenamos la ventana: puede que la app llevara dias cerrada. */
    init {
        viewModelScope.launch(Dispatchers.IO) { alarmas.reprogramarTodo() }
    }

    fun filtrarPor(personaId: Long?) {
        viewModelScope.launch { perfiles.fijarPersonaActiva(personaId) }
    }

    fun confirmar(tomaId: Long, alTerminar: (String) -> Unit) {
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val item = repositorio.toma(tomaId)
            repositorio.confirmar(tomaId, ahora)
            alarmas.cancelar(tomaId)
            Notificaciones.quitar(getApplication(), tomaId)
            item?.let { alTerminar(it.medicina.nombre) }
        }
    }

    fun omitir(tomaId: Long, motivo: String?) {
        viewModelScope.launch {
            repositorio.omitir(tomaId, motivo)
            alarmas.cancelar(tomaId)
            Notificaciones.quitar(getApplication(), tomaId)
        }
    }

    /** Deshacer devuelve la toma a pendiente y vuelve a programar su aviso. */
    fun deshacer(tomaId: Long) {
        viewModelScope.launch {
            repositorio.deshacer(tomaId)
            repositorio.toma(tomaId)?.let { alarmas.programar(it.toma) }
        }
    }

    fun resurtir(medicinaId: Long, cantidad: Int) {
        viewModelScope.launch { repositorio.resurtir(medicinaId, cantidad) }
    }
}
