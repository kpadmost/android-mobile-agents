package com.example.mobilecloudexample

import android.util.Log
import org.json.JSONObject
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class ClusterConnection(
    initLatencies : HashMap<String, Int>,
    val receiveMessageListener : (String?) -> Unit,
    val passIdListener : (String?) -> Unit,
    val connL : (String) -> Unit
) {
    val latencies = initLatencies

    var localConnection : SingleConnection? = null

    var connected : String? = null // denounce current connected mode


    public fun connectToClosest(adresses : HashMap<String, Pair<String, Int>>) {
        thread {
            try {
                if (localConnection?.connected() != null && localConnection?.connected()!!) {
                    Log.i("messc", "stub reconnect")
                    destroy()
                }
                val candidate: String = getCandidate()!!

                connectToAddress(candidate, adresses[candidate]!!)
                establishConnectionInfo(latencies[candidate]!!)
                localConnection?.startListening()
            } catch (e:Exception) {
                Log.e("connc", "Error while connecting!")
                Log.e("connc", e.toString())

            }
        }
    }


    private fun establishConnectionInfo(latency: Int) {
        val line = localConnection?.getLine()
        val jso = JSONObject(line)
        val clid = jso.getString("clid")
        passIdListener(clid)
        val jsm = JSONObject()
        jsm.put("latency", latency)
        jsm.put("command", "init")
        localConnection?.writeInfo(jsm.toString())
    }

    private fun connectToAddress(candidate : String, inetaddress : Pair<String, Int>) {
            try {
                Log.i("clconn", "connecting to".plus(candidate))

                localConnection =
                    SingleConnection(InetSocketAddress(inetaddress.first, inetaddress.second), receiveMessageListener)
                localConnection?.initConnection()

                connected = candidate
                connL(connected!!)
            } catch (e: Exception) {
                Log.e("connc", "Error while connecting to closest")
                Log.e("connc", e.message)
            }
    }

    public fun onLatencyChange(s: String, l: Int) {
        latencies[s] = l
        Log.i("connl", "latency for".plus(s).plus("changed: ").plus(l))
        if(localConnection != null && localConnection?.connected()!! && connected?.equals(s)!!) {
            thread {
                try {
                    val js = JSONObject()
                    js.put("command", "change_latency")
                    js.put("latency", l)
                    localConnection?.writeInfo(js.toString())
                } catch (e: Exception) {
                    Log.e("sendlch", e.toString())
                }
            }
        }
    }

    public fun reconnectToClosest(client_id: String, adresses : HashMap<String, Pair<String, Int>>) {
        thread {
            try {
                // TODO: if latency is higher here, reconnect, delay
                if (localConnection?.connected() == null || !localConnection?.connected()!!) {
                    Log.i("connc", "can't reconnect! No connection established!")
                    return@thread
                }
                Log.i("connc", "reconn to")
                val candidate = getCandidate()
                if (candidate == connected) return@thread
                // reconnecting


                val js : JSONObject = JSONObject()
                js.put("clid", client_id)
                js.put("latency", latencies[candidate])
                js.put("where", candidate)
                js.put("command", "reconnect")
                localConnection?.writeInfo(js.toString())

                destroy()
                connectToAddress(candidate!!, adresses[candidate]!!)

                val unsd1 = localConnection?.getLine()
                val jsn : JSONObject = JSONObject()
                js.put("command", "renew_connection")
                js.put("latency", latencies[candidate])
                js.put("clid", client_id)
                localConnection?.writeInfo(js.toString())
                localConnection?.startListening()

            } catch (e:Exception) {
                Log.e("connr", "error wh reconnecting")
                Log.e("connr", e.toString())

            }
        }
    }

    private fun getCandidate() : String? {
        return latencies.minBy { it.value }?.key
    }


    public fun destroy() {
        localConnection?.destroy()
        localConnection = null
    }

}