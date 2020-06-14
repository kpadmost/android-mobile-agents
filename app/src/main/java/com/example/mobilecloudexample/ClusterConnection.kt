package com.example.mobilecloudexample

import android.os.Bundle
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket

class ClusterConnection(
    initLatencies : HashMap<String, Int>,
    val adresses : HashMap<String, Pair<String, Int>>
) {
    val latencies = initLatencies

    var connected : String? = null // denounce current connected mode


    public fun connectToClosest() {
        val minServer = latencies.minBy { it.value }
        // TODO: implement if some not accessible
        val address = adresses[minServer!!.key]

    }

    public fun onLatencyChange(s: String, l: Int) {
        latencies[s] = l
        Log.i("connl", "latency for".plus(s).plus("changed: ").plus(l))
    }
}