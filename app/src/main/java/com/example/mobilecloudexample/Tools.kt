package com.example.mobilecloudexample


data class Tuple<out A, out B>(val _1: A, val _2: B)

public fun parseHostPortFromString(str : String) : Pair<String, Int> {
    val sp = str.split(':')
    return Pair(sp[0], sp[1].toInt())
}