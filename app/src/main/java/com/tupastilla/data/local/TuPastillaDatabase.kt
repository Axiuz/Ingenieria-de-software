package com.tupastilla.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Perfil::class, Persona::class, Medicina::class, Horario::class, Toma::class],
    version = 2,
    exportSchema = false
)
abstract class TuPastillaDatabase : RoomDatabase() {

    abstract fun perfilDao(): PerfilDao
    abstract fun personaDao(): PersonaDao
    abstract fun medicinaDao(): MedicinaDao
    abstract fun horarioDao(): HorarioDao
    abstract fun tomaDao(): TomaDao

    companion object {
        @Volatile private var instancia: TuPastillaDatabase? = null

        fun get(context: Context): TuPastillaDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                TuPastillaDatabase::class.java,
                "tupastilla"
            )
                // Es una demo y los datos se regeneran con el boton de Ajustes:
                // no vale la pena mantener migraciones entre versiones del esquema.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instancia = it }
        }
    }
}
