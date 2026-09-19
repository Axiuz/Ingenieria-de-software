package com.tupastilla.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {

    @Query("SELECT * FROM perfil WHERE id = 1")
    fun observar(): Flow<Perfil?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun obtener(): Perfil?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: Perfil)

    @Query("DELETE FROM perfil")
    suspend fun borrar()
}

@Dao
interface PersonaDao {

    @Query("SELECT * FROM persona WHERE activa = 1 ORDER BY nombre COLLATE NOCASE")
    fun observar(): Flow<List<Persona>>

    @Transaction
    @Query("SELECT * FROM persona WHERE activa = 1 ORDER BY nombre COLLATE NOCASE")
    fun observarConMedicinas(): Flow<List<PersonaConMedicinas>>

    @Query("SELECT * FROM persona ORDER BY id LIMIT 1")
    suspend fun primera(): Persona?

    @Query("SELECT * FROM persona WHERE id = :id")
    suspend fun porId(id: Long): Persona?

    @Insert
    suspend fun insertar(persona: Persona): Long

    @Update
    suspend fun actualizar(persona: Persona)

    @Delete
    suspend fun borrar(persona: Persona)

    @Query("DELETE FROM persona")
    suspend fun borrarTodo()
}

@Dao
interface MedicinaDao {

    @Transaction
    @Query("SELECT * FROM medicina WHERE activa = 1 ORDER BY nombre COLLATE NOCASE")
    fun observarConHorarios(): Flow<List<MedicinaConHorarios>>

    @Transaction
    @Query(
        "SELECT * FROM medicina WHERE activa = 1 AND personaId = :personaId " +
            "ORDER BY nombre COLLATE NOCASE"
    )
    fun observarDePersona(personaId: Long): Flow<List<MedicinaConHorarios>>

    @Transaction
    @Query("SELECT * FROM medicina WHERE id = :id")
    fun observarUna(id: Long): Flow<MedicinaConHorarios?>

    @Transaction
    @Query("SELECT * FROM medicina WHERE id = :id")
    suspend fun conHorarios(id: Long): MedicinaConHorarios?

    @Query("SELECT * FROM medicina WHERE personaId = :personaId")
    suspend fun conPersona(personaId: Long): List<Medicina>

    @Query("SELECT * FROM medicina WHERE activa = 1 AND existencias <= umbralAviso ORDER BY existencias")
    fun observarBajas(): Flow<List<Medicina>>

    @Insert
    suspend fun insertar(medicina: Medicina): Long

    @Update
    suspend fun actualizar(medicina: Medicina)

    @Delete
    suspend fun borrar(medicina: Medicina)

    @Query("UPDATE medicina SET existencias = :cantidad WHERE id = :id")
    suspend fun fijarExistencias(id: Long, cantidad: Int)

    @Query("UPDATE medicina SET existencias = existencias - 1 WHERE id = :id AND existencias > 0")
    suspend fun descontarUna(id: Long)

    @Query("DELETE FROM medicina")
    suspend fun borrarTodo()
}

@Dao
interface HorarioDao {

    @Query("SELECT * FROM horario WHERE medicinaId = :medicinaId ORDER BY hora")
    suspend fun deMedicina(medicinaId: Long): List<Horario>

    @Query("SELECT * FROM horario")
    suspend fun todos(): List<Horario>

    @Insert
    suspend fun insertar(horario: Horario): Long

    @Query("DELETE FROM horario WHERE medicinaId = :medicinaId")
    suspend fun borrarDeMedicina(medicinaId: Long)
}

@Dao
interface TomaDao {

    @Transaction
    @Query(
        "SELECT * FROM toma WHERE programadaPara >= :desde AND programadaPara < :hasta " +
            "ORDER BY programadaPara"
    )
    fun observarEntre(desde: Long, hasta: Long): Flow<List<TomaConMedicina>>

    @Transaction
    @Query(
        "SELECT * FROM toma WHERE estado != 'PENDIENTE' AND programadaPara < :hasta " +
            "ORDER BY programadaPara DESC"
    )
    fun observarResueltas(hasta: Long): Flow<List<TomaConMedicina>>

    @Transaction
    @Query("SELECT * FROM toma WHERE id = :id")
    suspend fun conMedicina(id: Long): TomaConMedicina?

    @Query("SELECT * FROM toma WHERE id = :id")
    suspend fun porId(id: Long): Toma?

    @Query(
        "SELECT * FROM toma WHERE estado = 'PENDIENTE' AND programadaPara >= :desde " +
            "AND programadaPara < :hasta ORDER BY programadaPara"
    )
    suspend fun pendientesEntre(desde: Long, hasta: Long): List<Toma>

    /** IGNORE: al rellenar la ventana de 48 h las tomas que ya existian se saltan. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(tomas: List<Toma>)

    @Query("UPDATE toma SET estado = :estado, confirmadaEn = :cuando, motivo = :motivo WHERE id = :id")
    suspend fun marcar(id: Long, estado: String, cuando: Long?, motivo: String?)

    @Query("DELETE FROM toma WHERE medicinaId = :medicinaId AND estado = 'PENDIENTE'")
    suspend fun borrarPendientesDe(medicinaId: Long)

    @Query("DELETE FROM toma WHERE horarioId NOT IN (SELECT id FROM horario) AND estado = 'PENDIENTE'")
    suspend fun limpiarHuerfanas()

    @Query("DELETE FROM toma")
    suspend fun borrarTodo()
}
