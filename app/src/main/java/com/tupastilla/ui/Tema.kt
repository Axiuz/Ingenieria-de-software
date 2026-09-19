package com.tupastilla.ui

import android.app.Activity
import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.tupastilla.R
import com.tupastilla.data.local.Densidad

/**
 * Modo de vision y densidad se aplican como ThemeOverlay antes de super.onCreate.
 * Cambiarlos en Ajustes guarda la preferencia y llama recreate().
 */
object Tema {

    fun aplicar(activity: Activity, base: Int = R.style.Theme_TuPastilla) {
        val prefs = Preferencias(activity)
        activity.setTheme(base)
        overlayDeModo(prefs.modoVision)?.let { activity.theme.applyStyle(it, true) }
        if (prefs.densidad == Densidad.COMPACTA) {
            activity.theme.applyStyle(R.style.ThemeOverlay_TuPastilla_Compacta, true)
        }
    }

    /** El modo 1 es el tema base, asi que no necesita overlay. */
    private fun overlayDeModo(modo: Int): Int? = when (modo) {
        2 -> R.style.ThemeOverlay_TuPastilla_Modo2
        3 -> R.style.ThemeOverlay_TuPastilla_Modo3
        4 -> R.style.ThemeOverlay_TuPastilla_Modo4
        else -> null
    }
}

/**
 * Resuelve un ?attr/... de color contra el tema de este contexto.
 *
 * Con obtainStyledAttributes y no con resolveAttribute: el segundo devuelve 0 cuando
 * el atributo apunta a un recurso de color en lugar de a un literal, y entonces los
 * bordes y los iconos de estado salen transparentes.
 */
@ColorInt
fun Context.colorDeAtributo(@AttrRes atributo: Int): Int {
    val atributos = theme.obtainStyledAttributes(intArrayOf(atributo))
    try {
        return atributos.getColor(0, 0)
    } finally {
        atributos.recycle()
    }
}

/** Resuelve un ?attr/... de dimension, en pixeles. */
fun Context.dimensionDeAtributo(@AttrRes atributo: Int): Int {
    val atributos = theme.obtainStyledAttributes(intArrayOf(atributo))
    try {
        return atributos.getDimensionPixelSize(0, 0)
    } finally {
        atributos.recycle()
    }
}

/** dp a pixeles, para lo poco que se calcula en codigo. */
fun Context.dp(valor: Float): Int = (valor * resources.displayMetrics.density).toInt()
