package com.tupastilla.data

import android.content.Context
import com.tupastilla.BuildConfig
import com.tupastilla.auth.AlmacenSesionCifrado
import com.tupastilla.auth.AuthApi
import com.tupastilla.auth.SesionRepository
import com.tupastilla.data.local.SeedDataSource
import com.tupastilla.data.local.TuPastillaDatabase
import com.tupastilla.data.repo.MedicamentoRepository
import com.tupastilla.data.repo.PerfilRepository
import com.tupastilla.data.repo.RoomMedicamentoRepository
import com.tupastilla.ui.Preferencias
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Un localizador minimo. La app no usa inyeccion de dependencias, asi que este objeto
 * es el unico sitio donde se decide que implementacion recibe la UI.
 */
object Grafo {

    @Volatile private var medicamentos: MedicamentoRepository? = null
    @Volatile private var perfiles: PerfilRepository? = null
    @Volatile private var sesion: SesionRepository? = null

    fun medicamentos(context: Context): MedicamentoRepository =
        medicamentos ?: synchronized(this) {
            medicamentos ?: TuPastillaDatabase.get(context).let {
                RoomMedicamentoRepository(
                    it.personaDao(), it.medicinaDao(), it.horarioDao(), it.tomaDao()
                )
            }.also { medicamentos = it }
        }

    fun perfiles(context: Context): PerfilRepository =
        perfiles ?: synchronized(this) {
            perfiles ?: Preferencias(context).let { prefs ->
                PerfilRepository(TuPastillaDatabase.get(context).perfilDao()) { prefs.guardar(it) }
            }.also { perfiles = it }
        }

    fun semilla(context: Context): SeedDataSource = TuPastillaDatabase.get(context).let {
        SeedDataSource(it.personaDao(), it.medicinaDao(), it.horarioDao(), it.tomaDao())
    }

    /** Timeouts de 10 s de conexion y 15 s de lectura para no dejar el login colgado. */
    fun sesion(context: Context): SesionRepository =
        sesion ?: synchronized(this) {
            sesion ?: SesionRepository(
                AuthApi(
                    BuildConfig.API_URL.toHttpUrl(),
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                ),
                AlmacenSesionCifrado(context)
            ).also { sesion = it }
        }
}
