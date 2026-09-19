package com.tupastilla.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AlmacenSesion {
    fun leer(): Sesion?
    fun guardar(sesion: Sesion)
    fun borrar()
}

/**
 * AES-256-GCM con una clave no exportable en Android Keystore. Si descifrar falla
 * (la clave no viaja en respaldos ni sobrevive a reinstalar) se borra la sesion y se pide login.
 */
class AlmacenSesionCifrado(context: Context) : AlmacenSesion {

    private val prefs = context.applicationContext
        .getSharedPreferences("tupastilla_sesion", Context.MODE_PRIVATE)

    override fun leer(): Sesion? {
        val iv = prefs.getString(IV, null) ?: return null
        val datos = prefs.getString(DATOS, null) ?: return null
        return try {
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            Sesion.deJson(String(cifrador.doFinal(Base64.decode(datos, Base64.NO_WRAP)), Charsets.UTF_8))
        } catch (e: GeneralSecurityException) {
            borrar()
            null
        } catch (e: org.json.JSONException) {
            borrar()
            null
        }
    }

    override fun guardar(sesion: Sesion) {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val datos = cifrador.doFinal(sesion.aJson().toByteArray(Charsets.UTF_8))
        prefs.edit {
            putString(IV, Base64.encodeToString(cifrador.iv, Base64.NO_WRAP))
            putString(DATOS, Base64.encodeToString(datos, Base64.NO_WRAP))
        }
    }

    override fun borrar() {
        prefs.edit { clear() }
    }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "tupastilla_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV = "iv"
        const val DATOS = "datos"
    }
}
