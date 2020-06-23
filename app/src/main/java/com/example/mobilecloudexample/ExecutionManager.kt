package com.example.mobilecloudexample

import android.util.Log
import org.json.JSONObject
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class ExecutionManager (
    initLatencies : HashMap<String, Int>,
    val receiveMessageListener : (String?) -> Unit,
    val passIdListener : (String?) -> Unit,
    val connL : (String) -> Unit
) {
    val latencies = initLatencies

    var localConnection : SingleConnection? = null // connection for cluster
    var latencyConnection : SingleConnection? = null // connection for latency

    var connected : String? = null // denounce current connected mode

    fun connectForLatency(adress: Pair<String, Int>) {
        thread {
            val l : (String?)->Unit = { _: String? ->  Log.i("lass", "sada")}
            Log.i("lacc", "connected to")
            latencyConnection = SingleConnection(InetSocketAddress(adress.first, adress.second), l)
            latencyConnection?.initConnection()
            Log.i("lacc", "connected to")
        }
    }

    fun connectToClosest(adresses : HashMap<String, Pair<String, Int>>, client_id : String) {
        thread {
            try {
                if (localConnection?.connected() != null && localConnection?.connected()!!) {
                    Log.i("messc", "stub reconnect")
                    destroy()
                }
                val candidate: String = getCandidate()!!

                connectToAddress(candidate, adresses[candidate]!!)
                val unsd1 = localConnection?.getLine()
                val jsn : JSONObject = JSONObject()
                jsn.put("command", "renew_connection")
                jsn.put("latency", latencies[candidate])
                jsn.put("clid", client_id)
                localConnection?.writeInfo(jsn.toString())
                localConnection?.startListening()
            } catch (e:Exception) {
                Log.e("connc", "Error while connecting!")
                Log.e("connc", e.toString())

            }
        }
    }

    fun connectToClosest(adresses : HashMap<String, Pair<String, Int>>) {
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
                connL("Connected to: ".plus(connected!!).plus(" latency: ").plus(latencies[candidate]))
            } catch (e: Exception) {
                Log.e("connc", "Error while connecting to closest")
                Log.e("connc", e.message)
            }
    }

    fun onLatencyChange(s: String, l: Int) {
        latencies[s] = l
        Log.i("connl", "latency for".plus(s).plus("changed: ").plus(l))
        if(latencyConnection != null && latencyConnection?.connected()!!) {
            thread {
                try {
                    val js = JSONObject()
                    js.put("node", s)
                    js.put("latency", l)
                    latencyConnection?.writeInfo(js.toString())
                    connL("Connected to: ".plus(connected!!).plus(" latency: ").plus(latencies[connected!!]))
                } catch (e: Exception) {
                    Log.e("sendlch", e.toString())
                }
            }
        }
    }

    fun checkConnection() {
        if(localConnection?.connected() == null || !localConnection?.connected()!!) {
            destroy()
        }
    }

    fun reconnectToClosest(client_id: String, adresses : HashMap<String, Pair<String, Int>>) {
        thread {
            try {
                // TODO: if latency is higher here, reconnect, delay
                if (localConnection?.connected() == null || !localConnection?.connected()!!) {
                    Log.i("connc", "can't reconnect! No connection established!")
                    adresses.remove(connected)
                    latencies.remove(connected)
                    destroy()

                    connectToClosest(adresses, client_id)
                    return@thread
                }



                Log.i("connc", "reconn to")
                val candidate = getCandidate()
                checkConnection()
                Log.i("connc", "" + (candidate == connected))
                Log.i("connc", "" + (localConnection?.connected()))
                if (candidate == connected) return@thread
                // reconnecting


                val js = JSONObject()
                js.put("clid", client_id)
                js.put("latency", latencies[candidate])
                js.put("where", candidate)
                js.put("command", "reconnect")
                localConnection?.writeInfo(js.toString())

                destroy()
                connectToAddress(candidate!!, adresses[candidate]!!)

                val unsd1 = localConnection?.getLine()
                val jsn : JSONObject = JSONObject()
                jsn.put("command", "renew_connection")
                jsn.put("latency", latencies[candidate])
                jsn.put("clid", client_id)
                localConnection?.writeInfo(jsn.toString())
                localConnection?.startListening()

            } catch (e:Exception) {
                Log.e("connr", "error wh reconnecting")
                e.printStackTrace()
                Log.e("connr", e.message)

            }
        }
    }

    private fun getCandidate() : String? {
        return latencies.minBy { it.value }?.key
    }

    fun destroyAll() {
        localConnection?.destroy()
        localConnection = null
        connected = null
        latencyConnection?.destroy()
        latencyConnection = null
    }


    private fun destroy() {
        Log.e("mdestr", "destroying connection")
        localConnection?.destroy()
        localConnection = null
        connected = null

    }

}