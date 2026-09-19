package com.tupastilla.data

/** Lo que pinta la cabecera de B9. */
data class Cumplimiento(
    val confirmadas: Int,
    val omitidas: Int,
    val pendientesHoy: Int
) {
    val total: Int get() = confirmadas + omitidas

    /** Porcentaje entero de tomas confirmadas. Sin tomas resueltas, 0. */
    val porcentaje: Int
        get() = if (total == 0) 0 else Math.round(confirmadas * 100f / total)
}
