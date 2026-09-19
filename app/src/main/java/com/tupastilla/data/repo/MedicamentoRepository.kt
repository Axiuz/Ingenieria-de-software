package com.tupastilla.data.repo

import com.tupastilla.data.Cumplimiento
import com.tupastilla.data.local.Horario
import com.tupastilla.data.local.Medicina
import com.tupastilla.data.local.MedicinaConHorarios
import com.tupastilla.data.local.Persona
import com.tupastilla.data.local.PersonaConMedicinas
import com.tupastilla.data.local.TomaConMedicina
import kotlinx.coroutines.flow.Flow

/**
 * Lo unico que la UI conoce. Hoy lo implementa RoomMedicamentoRepository; manana un
 * RemotoMedicamentoRepository sin tocar fragments ni ViewModels.
 */
interface MedicamentoRepository {

    fun tomasDeHoy(): Flow<List<TomaConMedicina>>

    fun medicinas(): Flow<List<MedicinaConHorarios>>

    /** Las personas a cargo. En autonomo y supervisado siempre es una sola. */
    fun personas(): Flow<List<Persona>>

    fun personasConMedicinas(): Flow<List<PersonaConMedicinas>>

    fun medicinasDe(personaId: Long): Flow<List<MedicinaConHorarios>>

    fun medicina(id: Long): Flow<MedicinaConHorarios?>

    fun existenciasBajas(): Flow<List<Medicina>>

    /** Tomas ya resueltas, de la mas reciente a la mas antigua. */
    fun historial(): Flow<List<TomaConMedicina>>

    fun cumplimiento(): Flow<Cumplimiento>

    suspend fun confirmar(tomaId: Long, cuando: Long = System.currentTimeMillis())

    suspend fun omitir(tomaId: Long, motivo: String? = null)

    /** Devuelve la toma a pendiente: es el "Deshacer" de la lista de Hoy. */
    suspend fun deshacer(tomaId: Long)

    suspend fun toma(tomaId: Long): TomaConMedicina?

    /**
     * Alta o edicion. Devuelve el id de la medicina para poder programar sus alarmas.
     * Los horarios se reemplazan enteros y las tomas pendientes se regeneran.
     */
    suspend fun guardar(medicina: Medicina, horarios: List<Horario>): Long

    suspend fun borrar(medicinaId: Long)

    /** Alta o edicion de una persona a cargo. Devuelve su id. */
    suspend fun guardarPersona(persona: Persona): Long

    /** Borrarla se lleva sus medicinas, horarios y tomas pendientes. */
    suspend fun borrarPersona(personaId: Long)

    /**
     * La persona a la que pertenecen las medicinas cuando no hay que elegir: la
     * primera que exista o una recien creada. Es lo que usan autonomo y supervisado.
     */
    suspend fun personaPorDefecto(nombre: String): Long

    suspend fun resurtir(medicinaId: Long, cantidad: Int)

    /** Rellena las tomas pendientes de las proximas [horas]. Idempotente. */
    suspend fun rellenarVentana(horas: Int = 48)

    suspend fun borrarTodo()
}
