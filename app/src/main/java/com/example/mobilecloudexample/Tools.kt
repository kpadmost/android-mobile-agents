package com.example.mobilecloudexample


data class Tuple<out A, out B>(val _1: A, val _2: B)

public fun parseHostPortFromString(str : String) : Pair<String, Int> {
    val sp = str.split(':')
    return Pair(sp[0], sp[1].toInt())
}

public fun scaledProgress(progress: Int, upped : Int = 2000, lower : Int = 10) : Int {
    return ((progress / 100.0) * (upped - lower) + lower).toInt()
}