package com.example.mobilecloudexample

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.*
import kotlin.concurrent.thread

class SingleConnection(mainaddr : InetSocketAddress, listener: (mess: String?) -> Unit) {
    private val maddr = mainaddr
    private var socket : Socket? = null
    private var clId : String? = null
    private val listener = listener




    public fun initConnection() {
        try {
            Log.i("messc", maddr.hostName)
            Log.i("messc", maddr.port.toString())
            socket = Socket(maddr.hostName, maddr.port)
            Log.i("messc", socket?.isConnected.toString())

        } catch (e: Exception) {
            Log.e("messs", e.message)
            Log.e("messs", e.stackTrace.toString())
        }
    }

    public fun getLine() : String? {
        val istr: BufferedReader? = BufferedReader(InputStreamReader(socket?.getInputStream()))
        return istr?.readLine()
    }


    public fun waitForConnectionInfoAndStartListening(latency : Int) {
        val istr: BufferedReader? = BufferedReader(InputStreamReader(socket?.getInputStream()))

       val res = istr?.readLine()
        Log.i("mappr", res)
        try {
            val js = JSONObject(res)
            val clientId = js.getString("clid")
            clId = clientId
            Log.i("messc", clientId)
            val jsm = JSONObject()
            jsm.put("latency", latency)
            jsm.put("command", "init")
            writeInfo(jsm.toString())
            startListening()
        } catch (e: JSONException) {
            Log.e("mappr", e.message)
        }
        Log.i("mappr", res)

    }

    public fun connected() : Boolean {
        return socket != null && socket!!.isConnected
    }


    public fun startListening() {
        val istr = socket?.getInputStream()
        istr.use {
            try{
                val bistr: BufferedReader? = BufferedReader(InputStreamReader(istr))
                while (socket != null && socket!!.isConnected) {
                    val res = bistr?.readLine()
                    listener.invoke(res)
                }
            } catch (e: Exception) {
                Log.e("mapps", e.message)
            }
        }
    }


    public fun destroy() {
        socket?.close()
    }


    public fun writeInfo(mess: String) {
        try {
            val ostr = socket?.getOutputStream()?.writer()
            ostr?.write(mess)
            ostr?.flush()
        } catch (e: IOException) {
            Log.i("blah", e.message)
        }
        Log.i("messr", socket?.getInputStream()?.available().toString())
    }
}
