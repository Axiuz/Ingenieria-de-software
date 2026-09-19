package com.tupastilla.ui

import com.tupastilla.R
import com.tupastilla.data.local.Rol

/**
 * Los tres roles de C10, con su texto. El rol decide que puede editar cada quien:
 * el autonomo y el cuidador administran la lista, el supervisado solo la consulta.
 */
data class TipoDePerfil(
    val valor: String,
    val nombreCorto: Int,
    val descripcion: Int,
    /** Si puede dar de alta, editar y borrar medicinas. */
    val puedeEditar: Boolean
)

object Roles {

    val todos = listOf(
        TipoDePerfil(Rol.AUTONOMO, R.string.rol_autonomo_corto, R.string.c16_rol_autonomo, true),
        TipoDePerfil(Rol.SUPERVISADO, R.string.rol_supervisado_corto, R.string.c16_rol_supervisado, false),
        TipoDePerfil(Rol.CUIDADOR, R.string.rol_cuidador_corto, R.string.c16_rol_cuidador, true)
    )

    fun de(valor: String?): TipoDePerfil = todos.firstOrNull { it.valor == valor } ?: todos.first()

    fun puedeEditar(valor: String?): Boolean = de(valor).puedeEditar
}
