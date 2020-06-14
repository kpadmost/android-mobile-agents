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

class Connection(mainaddr : InetSocketAddress, listenAddr : InetSocketAddress, listener: (mess: String?) -> Unit) {
    private val maddr = mainaddr
    private val listenaddr = listenAddr
    private var socket : Socket? = null
    private var clId : String? = null
    private val listener = listener




    public fun initConnection() {
        try {
            socket = Socket(maddr.address, maddr.port)
            Log.i("messc", socket?.isConnected.toString())
            waitForInfo()
        } catch (e: Exception) {
            Log.e("messs", e.message)
            Log.e("messs", e.stackTrace.toString())
        }
    }


    private fun waitForInfo() {
        val istr: BufferedReader? = BufferedReader(InputStreamReader(socket?.getInputStream()))

       val res = istr?.readLine()
        Log.i("mappr", res)
        try {
            val js = JSONObject(res)
            val clientId = js.getString("clid")
            clId = clientId
            Log.i("messc", clientId)
            val jsm = JSONObject()
            jsm.put("address", maddr.hostName)
            jsm.put("port", listenaddr.port)
            sendInfo(jsm.toString(), socket)
//            socket?.close()
            startListening()
        } catch (e: JSONException) {
            Log.e("mappr", e.message)
        }
        Log.i("mappr", res)

    }

    private fun startListening() {
        thread {
            try {
//                listenSocket = ServerSocket(listenaddr.port)
//                socket = listenSocket?.accept()
                Log.i("messl", "Accepted!")
                readFromSocket()
            } catch (e: Exception) {
                Log.e("messl", e.message)
            }
        }
    }

    private fun readFromSocket() {
        try {
            val istr: BufferedReader? = BufferedReader(InputStreamReader(socket?.getInputStream()))
            while (true) {
                val res = istr?.readLine()
                listener.invoke(res)
            }
        } catch (e: Exception) {
            Log.e("mapps", e.message)
        }
    }



    private fun sendInfo(mess: String, socket: Socket?) {
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
