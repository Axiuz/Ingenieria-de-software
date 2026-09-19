package com.tupastilla.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Una toma con la medicina a la que pertenece. Es lo unico que la UI necesita para
 * pintar una tarjeta: nombre, dosis, hora y estado.
 */
data class TomaConMedicina(
    @Embedded val toma: Toma,
    @Relation(parentColumn = "medicinaId", entityColumn = "id")
    val medicina: Medicina
)

/** Una persona con sus medicinas, para la lista de personas a cargo del cuidador. */
data class PersonaConMedicinas(
    @Embedded val persona: Persona,
    @Relation(parentColumn = "id", entityColumn = "personaId")
    val medicinas: List<Medicina>
)

/** Una medicina con todos sus horarios, para la lista y el detalle. */
data class MedicinaConHorarios(
    @Embedded val medicina: Medicina,
    @Relation(parentColumn = "id", entityColumn = "medicinaId")
    val horarios: List<Horario>
)
