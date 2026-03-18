package com.ethiostat.app.domain.model

data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component1(): A = first
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component2(): B = second
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component3(): C = third
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component4(): D = fourth
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component5(): E = fifth
