package com.tupastilla.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Los tres roles de C10. Solo el autonomo esta implementado por ahora. */
object Rol {
    const val AUTONOMO = "AUTONOMO"
    const val SUPERVISADO = "SUPERVISADO"
    const val CUIDADOR = "CUIDADOR"
}

/** Las dos densidades de C11b. */
object Densidad {
    const val ACCESIBLE = "ACCESIBLE"
    const val COMPACTA = "COMPACTA"
}

/** Los tres estados de una toma. Nunca se distinguen solo por color. */
object Estado {
    const val PENDIENTE = "PENDIENTE"
    const val CONFIRMADA = "CONFIRMADA"
    const val OMITIDA = "OMITIDA"
}

/**
 * Alguien a quien se le llevan las medicinas. En los roles autonomo y supervisado hay
 * una sola, que es el propio usuario; el cuidador puede tener varias a su cargo.
 */
@Entity(tableName = "persona")
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** Texto libre para ubicarla: "Mi mama", "Habitacion 4". */
    val nota: String = "",
    val activa: Boolean = true
)

/**
 * Una sola fila, siempre con id 1: quien usa el telefono y como quiere verlo.
 * Que exista o no es lo que decide si la app arranca en el onboarding o en Hoy.
 */
@Entity(tableName = "perfil")
data class Perfil(
    @PrimaryKey val id: Int = 1,
    val nombre: String,
    val rol: String = Rol.AUTONOMO,
    /** 1..4, los cuatro modos de vision de C11. */
    val modoVision: Int = 1,
    val densidad: String = Densidad.ACCESIBLE,
    val codigoVinculo: String? = null,
    /** Momento en que se genero el codigo; vence a los 15 minutos. */
    val codigoGeneradoEn: Long? = null,
    val vinculado: Boolean = false,
    val avisoExacto: Boolean = true,
    val avisoRepetir: Boolean = true,
    val avisoFamiliar: Boolean = false,
    /** Filtro del cuidador en la pantalla de Hoy. Null: ve a todas sus personas. */
    val personaActivaId: Long? = null
)

@Entity(
    tableName = "medicina",
    foreignKeys = [ForeignKey(
        entity = Persona::class, parentColumns = ["id"],
        childColumns = ["personaId"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("personaId")]
)
data class Medicina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** A quien le toca. Borrar a la persona se lleva sus medicinas en cascada. */
    val personaId: Long,
    val nombre: String,
    val dosis: String = "",
    /** Como se ve la pastilla: "Blanca redonda". Ayuda a no confundirla. */
    val forma: String = "",
    val instrucciones: String = "",
    val existencias: Int = 0,
    val umbralAviso: Int = 5,
    val activa: Boolean = true
)

@Entity(
    tableName = "horario",
    foreignKeys = [ForeignKey(
        entity = Medicina::class, parentColumns = ["id"],
        childColumns = ["medicinaId"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicinaId")]
)
data class Horario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicinaId: Long,
    /** "08:00", en 24 horas. */
    val hora: String,
    /** Dias de Calendar.DAY_OF_WEEK separados por coma: "1,2,3,4,5,6,7" (1 = domingo). */
    val diasSemana: String = TODOS_LOS_DIAS,
    val desde: Long = 0,
    val hasta: Long? = null
) {
    companion object {
        const val TODOS_LOS_DIAS = "1,2,3,4,5,6,7"
    }
}

/**
 * Una toma concreta: esta medicina, este horario, este dia a esta hora.
 * El indice unico sobre (horarioId, programadaPara) es lo que permite volver a
 * generar la ventana de 48 horas sin duplicar nada.
 */
@Entity(
    tableName = "toma",
    indices = [
        Index("medicinaId"),
        Index(value = ["horarioId", "programadaPara"], unique = true)
    ]
)
data class Toma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicinaId: Long,
    val horarioId: Long,
    /** Epoch millis del momento en que deberia tomarse. */
    val programadaPara: Long,
    val estado: String = Estado.PENDIENTE,
    val confirmadaEn: Long? = null,
    val motivo: String? = null
)
