package com.tupastilla.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tupastilla.data.local.Estado
import com.tupastilla.data.local.Horario
import com.tupastilla.data.local.Medicina
import com.tupastilla.data.local.Persona
import com.tupastilla.data.local.TuPastillaDatabase
import com.tupastilla.data.repo.RoomMedicamentoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room de verdad, en memoria: lo que se prueba es el SQL y las cascadas. */
@RunWith(AndroidJUnit4::class)
class MedicamentoRepositoryTest {

    private lateinit var base: TuPastillaDatabase
    private lateinit var repositorio: RoomMedicamentoRepository

    /**
     * Reloj fijo a las 00:30 de hoy. Sin el, la ventana de 48 h empieza a la hora
     * real de la prueba y la toma de las 08:00 puede caer manana, con lo que ya no
     * cuenta como toma de hoy y el resultado depende de cuando se corra la suite.
     */
    private val relojFijo = Calendario.inicioDelDia(System.currentTimeMillis()) + 30 * 60_000L

    @Before
    fun crearBase() {
        base = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TuPastillaDatabase::class.java
        ).build()
        repositorio = RoomMedicamentoRepository(
            base.personaDao(), base.medicinaDao(), base.horarioDao(), base.tomaDao(),
            reloj = { relojFijo }
        )
    }

    @After
    fun cerrarBase() = base.close()

    private suspend fun altaLosartan(existencias: Int = 30): Long {
        val persona = repositorio.personaPorDefecto("Rosa")
        return repositorio.guardar(
            Medicina(
                personaId = persona, nombre = "Losartán", dosis = "50 mg",
                existencias = existencias
            ),
            listOf(Horario(medicinaId = 0, hora = "08:00"))
        )
    }

    @Test
    fun guardarUnaMedicinaGeneraSusTomasPendientes() = runTest {
        altaLosartan()

        val medicinas = repositorio.medicinas().first()
        assertEquals(1, medicinas.size)
        assertEquals(1, medicinas[0].horarios.size)
        // La ventana de 48 h deja al menos una toma programada por delante.
        assertTrue(base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).isNotEmpty())
    }

    @Test
    fun confirmarDescuentaUnaPastillaDeLaCaja() = runTest {
        val id = altaLosartan(existencias = 10)
        val toma = base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).first()

        repositorio.confirmar(toma.id, cuando = 1_700_000_000_000L)

        val guardada = base.tomaDao().porId(toma.id)!!
        assertEquals(Estado.CONFIRMADA, guardada.estado)
        assertEquals(1_700_000_000_000L, guardada.confirmadaEn)
        assertEquals(9, base.medicinaDao().conHorarios(id)!!.medicina.existencias)
    }

    @Test
    fun confirmarDosVecesLaMismaTomaNoDescuentaDosPastillas() = runTest {
        val id = altaLosartan(existencias = 10)
        val toma = base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).first()

        repositorio.confirmar(toma.id)
        repositorio.confirmar(toma.id)

        assertEquals(9, base.medicinaDao().conHorarios(id)!!.medicina.existencias)
    }

    @Test
    fun omitirGuardaElMotivoYNoTocaLaCaja() = runTest {
        val id = altaLosartan(existencias = 10)
        val toma = base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).first()

        repositorio.omitir(toma.id, "se me olvidó")

        val guardada = base.tomaDao().porId(toma.id)!!
        assertEquals(Estado.OMITIDA, guardada.estado)
        assertEquals("se me olvidó", guardada.motivo)
        assertNull(guardada.confirmadaEn)
        assertEquals(10, base.medicinaDao().conHorarios(id)!!.medicina.existencias)
    }

    @Test
    fun rellenarLaVentanaDosVecesNoDuplicaTomas() = runTest {
        altaLosartan()
        val antes = base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).size

        repositorio.rellenarVentana()
        repositorio.rellenarVentana()

        assertEquals(antes, base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).size)
    }

    @Test
    fun borrarUnaMedicinaSeLlevaSusHorariosEnCascada() = runTest {
        val id = altaLosartan()

        repositorio.borrar(id)

        assertTrue(repositorio.medicinas().first().isEmpty())
        assertTrue(base.horarioDao().deMedicina(id).isEmpty())
    }

    @Test
    fun elCumplimientoCuentaConfirmadasYOmitidasPeroNoPendientes() = runTest {
        altaLosartan()
        val inicioDeHoy = Calendario.inicioDelDia(relojFijo)
        val deHoy = base.tomaDao().pendientesEntre(inicioDeHoy, inicioDeHoy + Calendario.DIA_MS)
        assertEquals(1, deHoy.size)

        repositorio.confirmar(deHoy[0].id)

        val cumplimiento = repositorio.cumplimiento().first()
        assertEquals(1, cumplimiento.confirmadas)
        assertEquals(0, cumplimiento.omitidas)
        // La toma de manana sigue pendiente pero no es de hoy: no debe contarse.
        assertEquals(0, cumplimiento.pendientesHoy)
    }

    @Test
    fun cadaPersonaVeSoloSusMedicinas() = runTest {
        val rosa = repositorio.guardarPersona(Persona(nombre = "Rosa"))
        val luis = repositorio.guardarPersona(Persona(nombre = "Luis"))
        repositorio.guardar(
            Medicina(personaId = rosa, nombre = "Losartán"),
            listOf(Horario(medicinaId = 0, hora = "08:00"))
        )
        repositorio.guardar(
            Medicina(personaId = luis, nombre = "Enalapril"),
            listOf(Horario(medicinaId = 0, hora = "20:00"))
        )

        assertEquals(2, repositorio.medicinas().first().size)
        assertEquals("Losartán", repositorio.medicinasDe(rosa).first().single().medicina.nombre)
        assertEquals("Enalapril", repositorio.medicinasDe(luis).first().single().medicina.nombre)
    }

    @Test
    fun borrarUnaPersonaSeLlevaSusMedicinasYNoLasDeLasDemas() = runTest {
        val rosa = repositorio.guardarPersona(Persona(nombre = "Rosa"))
        val luis = repositorio.guardarPersona(Persona(nombre = "Luis"))
        repositorio.guardar(
            Medicina(personaId = rosa, nombre = "Losartán"),
            listOf(Horario(medicinaId = 0, hora = "08:00"))
        )
        repositorio.guardar(
            Medicina(personaId = luis, nombre = "Enalapril"),
            listOf(Horario(medicinaId = 0, hora = "20:00"))
        )

        repositorio.borrarPersona(rosa)

        assertEquals(1, repositorio.personas().first().size)
        assertEquals("Enalapril", repositorio.medicinas().first().single().medicina.nombre)
        // Las tomas pendientes de Rosa tampoco deben quedar sueltas.
        assertTrue(base.tomaDao().pendientesEntre(0, Long.MAX_VALUE).none { it.medicinaId == rosa })
    }

    @Test
    fun personaPorDefectoNoDuplicaLaQueYaExiste() = runTest {
        val primera = repositorio.personaPorDefecto("Yo")
        val segunda = repositorio.personaPorDefecto("Yo")

        assertEquals(primera, segunda)
        assertEquals(1, repositorio.personas().first().size)
    }
}
