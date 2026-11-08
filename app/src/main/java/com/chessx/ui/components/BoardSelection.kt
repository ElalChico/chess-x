/*
 * CHESS-X · © Elal Chico
 * Clase de datos para la selección de casillas en el tablero.
 */

package com.chessx.ui.components

/**
 * Selección de casilla actual.
 * @param from casilla origen seleccionada (0..63) o null
 * @param legalTargets destinos legales desde [from]
 * @param lastMove último movimiento jugado (from, to)
 * @param checkKingSquare casilla del rey que está en jaque (resaltado rojo)
 * @param errorSquare casilla donde se intentó un movimiento ilegal
 */
data class BoardSelection(
    val from: Int? = null,
    val legalTargets: List<Int> = emptyList(),
    val lastMove: Pair<Int, Int>? = null,
    val checkKingSquare: Int? = null,
    val errorSquare: Int? = null
)