package com.tupastilla.data.repo

import com.tupastilla.data.Calendario
import com.tupastilla.data.Cumplimiento
import com.tupastilla.data.ExpansorHorarios
import com.tupastilla.data.local.Estado
import com.tupastilla.data.local.Horario
import com.tupastilla.data.local.HorarioDao
import com.tupastilla.data.local.Medicina
import com.tupastilla.data.local.MedicinaConHorarios
import com.tupastilla.data.local.MedicinaDao
import com.tupastilla.data.local.Persona
import com.tupastilla.data.local.PersonaConMedicinas
import com.tupastilla.data.local.PersonaDao
import com.tupastilla.data.local.TomaConMedicina
import com.tupastilla.data.local.TomaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomMedicamentoRepository(
    private val personaDao: PersonaDao,
    private val medicinaDao: MedicinaDao,
    private val horarioDao: HorarioDao,
    private val tomaDao: TomaDao,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) : MedicamentoRepository {

    override fun tomasDeHoy(): Flow<List<TomaConMedicina>> {
        val inicio = Calendario.inicioDelDia(reloj())
        return tomaDao.observarEntre(inicio, inicio + Calendario.DIA_MS)
    }

    override fun medicinas(): Flow<List<MedicinaConHorarios>> = medicinaDao.observarConHorarios()

    override fun personas(): Flow<List<Persona>> = personaDao.observar()

    override fun personasConMedicinas(): Flow<List<PersonaConMedicinas>> =
        personaDao.observarConMedicinas()

    override fun medicinasDe(personaId: Long): Flow<List<MedicinaConHorarios>> =
        medicinaDao.observarDePersona(personaId)

    override fun medicina(id: Long): Flow<MedicinaConHorarios?> = medicinaDao.observarUna(id)

    override fun existenciasBajas(): Flow<List<Medicina>> = medicinaDao.observarBajas()

    override fun historial(): Flow<List<TomaConMedicina>> =
        tomaDao.observarResueltas(Calendario.inicioDelDia(reloj()))

    override fun cumplimiento(): Flow<Cumplimiento> =
        combine(historial(), tomasDeHoy()) { resueltas, hoy ->
            val todas = resueltas + hoy.filter { it.toma.estado != Estado.PENDIENTE }
            Cumplimiento(
                confirmadas = todas.count { it.toma.estado == Estado.CONFIRMADA },
                omitidas = todas.count { it.toma.estado == Estado.OMITIDA },
                pendientesHoy = hoy.count { it.toma.estado == Estado.PENDIENTE }
            )
        }

    /** Confirmar descuenta una pastilla de la caja: es lo que dispara el aviso de existencias. */
    override suspend fun confirmar(tomaId: Long, cuando: Long) {
        val toma = tomaDao.porId(tomaId) ?: return
        if (toma.estado == Estado.CONFIRMADA) return
        tomaDao.marcar(tomaId, Estado.CONFIRMADA, cuando, null)
        medicinaDao.descontarUna(toma.medicinaId)
    }

    override suspend fun omitir(tomaId: Long, motivo: String?) {
        tomaDao.marcar(tomaId, Estado.OMITIDA, null, motivo)
    }

    override suspend fun deshacer(tomaId: Long) {
        tomaDao.marcar(tomaId, Estado.PENDIENTE, null, null)
    }

    override suspend fun toma(tomaId: Long): TomaConMedicina? = tomaDao.conMedicina(tomaId)

    override suspend fun guardar(medicina: Medicina, horarios: List<Horario>): Long {
        val id = if (medicina.id == 0L) {
            medicinaDao.insertar(medicina)
        } else {
            medicinaDao.actualizar(medicina)
            medicina.id
        }
        // Los horarios se reemplazan enteros: es mas simple que diferenciarlos y la
        // cascada se lleva por delante las tomas pendientes que ya no aplican.
        horarioDao.borrarDeMedicina(id)
        tomaDao.borrarPendientesDe(id)
        horarios.forEach { horarioDao.insertar(it.copy(id = 0, medicinaId = id)) }
        rellenarVentana()
        return id
    }

    override suspend fun borrar(medicinaId: Long) {
        val conHorarios = medicinaDao.conHorarios(medicinaId) ?: return
        tomaDao.borrarPendientesDe(medicinaId)
        medicinaDao.borrar(conHorarios.medicina)
    }

    override suspend fun guardarPersona(persona: Persona): Long =
        if (persona.id == 0L) {
            personaDao.insertar(persona)
        } else {
            personaDao.actualizar(persona)
            persona.id
        }

    override suspend fun borrarPersona(personaId: Long) {
        val persona = personaDao.porId(personaId) ?: return
        // Las medicinas caen en cascada y con ellas sus horarios; las tomas
        // pendientes hay que quitarlas a mano porque no cuelgan de la persona.
        medicinaDao.conPersona(personaId).forEach { tomaDao.borrarPendientesDe(it.id) }
        personaDao.borrar(persona)
    }

    override suspend fun personaPorDefecto(nombre: String): Long =
        personaDao.primera()?.id ?: personaDao.insertar(Persona(nombre = nombre))

    override suspend fun resurtir(medicinaId: Long, cantidad: Int) {
        medicinaDao.fijarExistencias(medicinaId, cantidad)
    }

    override suspend fun rellenarVentana(horas: Int) {
        val ahora = reloj()
        val hasta = ahora + horas * 60 * 60 * 1000L
        val tomas = horarioDao.todos().flatMap { ExpansorHorarios.tomas(it, ahora, hasta) }
        if (tomas.isNotEmpty()) tomaDao.insertar(tomas)
        tomaDao.limpiarHuerfanas()
    }

    override suspend fun borrarTodo() {
        tomaDao.borrarTodo()
        medicinaDao.borrarTodo()
        personaDao.borrarTodo()
    }
}
