package com.tupastilla.data.local

import com.tupastilla.data.Calendario
import com.tupastilla.data.ExpansorHorarios

/**
 * Datos de prueba para recorrer la app sin capturar nada. Lo dispara el boton de
 * Ajustes, nunca el arranque.
 *
 * Siembra distinto segun el rol: el autonomo y el supervisado son una sola persona
 * con sus medicinas; el cuidador tiene varias a su cargo, que es el caso de un
 * familiar con dos padres o de alguien en un geriatrico.
 */
class SeedDataSource(
    private val personaDao: PersonaDao,
    private val medicinaDao: MedicinaDao,
    private val horarioDao: HorarioDao,
    private val tomaDao: TomaDao
) {

    /** Una medicina de ejemplo con sus horas, antes de tocar la base. */
    private data class Receta(
        val nombre: String,
        val dosis: String,
        val forma: String,
        val instrucciones: String,
        val existencias: Int,
        val horas: List<Pair<String, String>>
    )

    suspend fun cargar(rol: String, ahora: Long = System.currentTimeMillis()) {
        tomaDao.borrarTodo()
        medicinaDao.borrarTodo()
        personaDao.borrarTodo()

        if (rol == Rol.CUIDADOR) sembrarVariasPersonas() else sembrarUnaPersona()

        val inicioDeHoy = Calendario.inicioDelDia(ahora)
        generarVentana(inicioDeHoy, inicioDeHoy + 2 * Calendario.DIA_MS)
        marcarAlgunasDeHoy(ahora)
        generarHistorial(ahora)
    }

    /** Autonomo y supervisado: las tres medicinas del prototipo, todas suyas. */
    private suspend fun sembrarUnaPersona() {
        val yo = personaDao.insertar(Persona(nombre = "Yo"))
        alta(yo, MEDICINAS_BASE)
    }

    /**
     * Seis residentes con horarios repartidos entre las 07:00 y las 22:00. Son
     * suficientes para que el turno se vea como el de un geriatrico de verdad, con
     * varias personas coincidiendo a la misma hora.
     */
    private suspend fun sembrarVariasPersonas() {
        RESIDENTES.forEach { (persona, recetas) ->
            alta(personaDao.insertar(persona), recetas)
        }
    }

    private suspend fun alta(personaId: Long, recetas: List<Receta>) {
        recetas.forEach { receta ->
            val id = medicinaDao.insertar(
                Medicina(
                    personaId = personaId, nombre = receta.nombre, dosis = receta.dosis,
                    forma = receta.forma, instrucciones = receta.instrucciones,
                    existencias = receta.existencias
                )
            )
            receta.horas.forEach { (hora, dias) ->
                horarioDao.insertar(Horario(medicinaId = id, hora = hora, diasSemana = dias))
            }
        }
    }

    private suspend fun generarVentana(desde: Long, hasta: Long) {
        val tomas = horarioDao.todos().flatMap { ExpansorHorarios.tomas(it, desde, hasta) }
        if (tomas.isNotEmpty()) tomaDao.insertar(tomas)
    }

    /**
     * Deja unas cuantas tomas de hoy ya resueltas: dos confirmadas y una omitida,
     * para que se vean los tres estados nada mas cargar los datos.
     */
    private suspend fun marcarAlgunasDeHoy(ahora: Long) {
        val inicio = Calendario.inicioDelDia(ahora)
        val deHoy = tomaDao.pendientesEntre(inicio, inicio + Calendario.DIA_MS)

        deHoy.take(CONFIRMADAS_DE_HOY).forEach {
            tomaDao.marcar(it.id, Estado.CONFIRMADA, it.programadaPara + 7 * 60_000L, null)
        }
        deHoy.getOrNull(CONFIRMADAS_DE_HOY)
            ?.let { tomaDao.marcar(it.id, Estado.OMITIDA, null, "se le olvidó") }
    }

    /**
     * Seis dias hacia atras. El patron se reparte entre todos los horarios para que
     * el cumplimiento no salga del 100% y el historial tenga algo que contar.
     */
    private suspend fun generarHistorial(ahora: Long) {
        val horarios = horarioDao.todos()
        if (horarios.isEmpty()) return

        var contador = 0
        (1..DIAS_DE_HISTORIAL).forEach { diasAtras ->
            val fecha = Calendario.masDias(Calendario.inicioDelDia(ahora), -diasAtras)
            horarios.forEach { horario ->
                if (ExpansorHorarios.instantes(horario, fecha, fecha + Calendario.DIA_MS).isEmpty()) {
                    return@forEach
                }
                val programada = Calendario.enElDia(fecha, horario.hora)
                val estado = if (contador++ % OMITIDA_CADA == 0) Estado.OMITIDA else Estado.CONFIRMADA
                tomaDao.insertar(
                    listOf(
                        Toma(
                            medicinaId = horario.medicinaId, horarioId = horario.id,
                            programadaPara = programada, estado = estado,
                            confirmadaEn = if (estado == Estado.CONFIRMADA) programada else null
                        )
                    )
                )
            }
        }
    }

    private companion object {
        const val DIAS_DE_HISTORIAL = 6
        const val CONFIRMADAS_DE_HOY = 2

        /** Una de cada siete sale omitida: deja el cumplimiento en torno al 85%. */
        const val OMITIDA_CADA = 7

        /** El reparto del geriatrico: quien vive donde y que le toca. */
        val RESIDENTES: List<Pair<Persona, List<Receta>>> = listOf(
            Persona(nombre = "Rosa Méndez", nota = "Habitación 4") to listOf(
                Receta(
                    "Losartán", "50 mg · 1 pastilla", "Blanca redonda",
                    "Con agua, después del desayuno", 4,
                    listOf("08:00" to Horario.TODOS_LOS_DIAS)
                ),
                Receta(
                    "Metformina 850 mg", "1 pastilla", "Ovalada blanca",
                    "Con la comida", 28,
                    listOf("08:00" to Horario.TODOS_LOS_DIAS, "20:00" to Horario.TODOS_LOS_DIAS)
                )
            ),
            Persona(nombre = "Luis Ortega", nota = "Habitación 7") to listOf(
                Receta(
                    "Enalapril 10 mg", "1 pastilla", "Redonda naranja",
                    "Antes del desayuno", 30,
                    listOf("08:00" to Horario.TODOS_LOS_DIAS, "20:00" to Horario.TODOS_LOS_DIAS)
                )
            ),
            Persona(nombre = "Carmen Díaz", nota = "Habitación 9") to listOf(
                Receta(
                    "Vitamina D", "1 cápsula", "Cápsula amarilla",
                    "Sin indicaciones especiales", 46,
                    // lunes (2) y jueves (5)
                    listOf("14:00" to "2,5")
                ),
                Receta(
                    "Paracetamol 500 mg", "1 pastilla", "Ovalada blanca",
                    "Solo si tiene dolor", 12,
                    listOf("08:00" to Horario.TODOS_LOS_DIAS)
                )
            ),
            Persona(nombre = "Alberto Ruiz", nota = "Habitación 12") to listOf(
                Receta(
                    "Levotiroxina 75 mcg", "1 pastilla", "Redonda blanca",
                    "En ayunas, media hora antes del desayuno", 60,
                    listOf("07:00" to Horario.TODOS_LOS_DIAS)
                ),
                Receta(
                    "Omeprazol 20 mg", "1 cápsula", "Cápsula rosa y blanca",
                    "En ayunas", 25,
                    listOf("07:00" to Horario.TODOS_LOS_DIAS)
                )
            ),
            Persona(nombre = "Esperanza Vidal", nota = "Habitación 15") to listOf(
                Receta(
                    "Furosemida 40 mg", "1 pastilla", "Redonda blanca ranurada",
                    "No darla por la noche", 18,
                    listOf("08:00" to Horario.TODOS_LOS_DIAS, "14:00" to Horario.TODOS_LOS_DIAS)
                ),
                Receta(
                    "Calcio con vitamina D3", "1 sobre", "Sobre efervescente",
                    "Disuelto en medio vaso de agua", 40,
                    listOf("22:00" to Horario.TODOS_LOS_DIAS)
                )
            ),
            Persona(nombre = "Tomás Nieto", nota = "Habitación 18") to listOf(
                Receta(
                    "Insulina lenta", "12 unidades", "Pluma azul",
                    "Antes de cada comida principal", 20,
                    listOf(
                        "07:00" to Horario.TODOS_LOS_DIAS,
                        "14:00" to Horario.TODOS_LOS_DIAS,
                        "20:00" to Horario.TODOS_LOS_DIAS
                    )
                ),
                Receta(
                    "Amlodipino 5 mg", "1 pastilla", "Redonda blanca",
                    "Con la cena", 30,
                    listOf("20:00" to Horario.TODOS_LOS_DIAS)
                )
            )
        )

        val MEDICINAS_BASE = listOf(
            Receta(
                "Losartán", "50 mg · 1 pastilla", "Blanca redonda",
                "Con agua, después del desayuno", 4,
                listOf("08:00" to Horario.TODOS_LOS_DIAS)
            ),
            Receta(
                "Metformina 850 mg", "1 pastilla", "Ovalada blanca",
                "Con la comida", 28,
                listOf("08:00" to Horario.TODOS_LOS_DIAS, "20:00" to Horario.TODOS_LOS_DIAS)
            ),
            Receta(
                "Vitamina D", "1 cápsula", "Cápsula amarilla",
                "Sin indicaciones especiales", 46,
                // lunes (2) y jueves (5)
                listOf("14:00" to "2,5")
            )
        )
    }
}
