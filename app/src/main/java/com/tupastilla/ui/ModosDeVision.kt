package com.tupastilla.ui

import com.tupastilla.R

/** Los cuatro modos de C11, con su texto. El color lo pone el ThemeOverlay. */
data class ModoDeVision(
    val numero: Int,
    val nombreCorto: Int,
    val pista: Int,
    val descripcion: Int
)

object ModosDeVision {

    val todos = listOf(
        ModoDeVision(1, R.string.c11_m1_corto, R.string.c11_m1, R.string.c11_m1_desc),
        ModoDeVision(2, R.string.c11_m2_corto, R.string.c11_m2, R.string.c11_m2_desc),
        ModoDeVision(3, R.string.c11_m3_corto, R.string.c11_m3, R.string.c11_m3_desc),
        ModoDeVision(4, R.string.c11_m4_corto, R.string.c11_m4, R.string.c11_m4_desc)
    )

    fun de(numero: Int): ModoDeVision = todos.firstOrNull { it.numero == numero } ?: todos.first()

    /** El overlay con el que se dibuja una muestra de ese modo sin cambiar el tema. */
    fun overlayDe(numero: Int): Int = when (numero) {
        2 -> R.style.ThemeOverlay_TuPastilla_Modo2
        3 -> R.style.ThemeOverlay_TuPastilla_Modo3
        4 -> R.style.ThemeOverlay_TuPastilla_Modo4
        else -> R.style.Theme_TuPastilla
    }
}
