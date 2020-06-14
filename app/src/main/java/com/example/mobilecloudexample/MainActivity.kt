package com.example.mobilecloudexample

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.transition.TransitionManager
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Exception

import java.net.InetSocketAddress

import kotlin.text.toInt
import java.net.Socket
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    var conL : ConstraintLayout? = null
    var toggle : Boolean = true
    var socket : Socket? = null
    var connection : Connection? = null

    val imPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val imPaintW : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var imB : Bitmap? = null
    var imCanvas : Canvas? = null
    val s = State(-1.0f, -1.0f)


    var clusterConnection : ClusterConnection? = null


    class State(var x: Float, var y: Float)


    class MySeekerListener(val clName : String, val callback: (String, Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            callback.invoke(clName, seekBar?.progress!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        conL = findViewById(R.id.content_main)


        imPaint.color = Color.BLACK
        imPaint.strokeWidth = 10.0f
        imPaint.isDither = false



        imPaintW.color = Color.WHITE
        imPaint.strokeWidth = 10.0f
        imPaint.isDither = false



        val d = imageView.drawable
        imB = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        imCanvas = Canvas(imB)


        val b : HashMap<String, Pair<String, Int>> = hashMapOf()
        arrayOf("seed", "c1", "c2").forEach { s ->
            val p = parseHostPortFromString(sp.getString(s.plus("_address"), "localhost:80")!!)
            b[s] = p
        }


        clusterConnection = ClusterConnection(socket, hashMapOf(
            Pair("seed", seedBar.progress),
            Pair("c1", c1Bar.progress),
            Pair("c2", c2Bar.progress)
        ), b)



        seedBar.setOnSeekBarChangeListener(MySeekerListener("seed", clusterConnection!!::onLatencyChange))
        c1Bar.setOnSeekBarChangeListener(MySeekerListener("c1", clusterConnection!!::onLatencyChange))
        c2Bar.setOnSeekBarChangeListener(MySeekerListener("c2",  clusterConnection!!::onLatencyChange))

//         c1bar.setOnSeekBarChangeListener(MySeekerListener("seed"){ s: String, v : Int ->  latencyState!!.onLatencyChanged(s, v)}
//        c2bar.setOnSeekBarChangeListener(MySeekerListener("seed"){ s: String, v : Int ->  latencyState!!.onLatencyChanged(s, v)}
//            .setOnSeekBarChangeListener(MySeekerListener("c1", latencyState!!::onLatencyChanged)
//        seedBar.setOnSeekBarChangeListener(MySeekerListener("c2", latencyState!!::onLatencyChanged)




//
        fab.setOnClickListener(this::onMailClick)


    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
    }

    fun onClickMenu(view: MenuItem) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onClickConnect(view: MenuItem) {
        if(socket != null && socket!!.isConnected) return
        val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val address : List<String> = sp.getString("address", "localhost:1234").split(':')

        val ipaddress : String = address.get(0)
        val port : Int = address.get(1).toInt()
        val lport = sp.getString("listenport", "55555").toInt()
        val saddr = InetSocketAddress(ipaddress, port)
        thread {
            try {
                connection = Connection(saddr, InetSocketAddress(lport)){ m -> publishMessage(m!!)}

                connection?.initConnection()
            } catch (e : IOException) {
                Log.e("mapp", "can't connect!")
                Log.e("mapp", e.message)
            } catch (e: Exception) {
                Log.e("mapp", e.message)
            }
        }
    }


    fun publishMessage(message: String) {
        val sp = message.split(':')
        val x = sp[0].toFloat()
        val y = sp[1].toFloat()
        imageView.post {

            if(s.x < 0f) {
                s.x = x
                s.y = y
                imCanvas?.drawRect(x, y, x + 50f, y + 50f, imPaint)
            } else {
                imCanvas?.drawRect(s.x, s.y, s.x + 50f, s.y + 50f, imPaintW)
                imCanvas?.drawRect(x, y, x + 50f, y + 50f, imPaint)
                s.x = x
                s.y = y
                imageView.setImageBitmap(imB)
            }

        }
    }


    fun receiveLoop() {
        val ist : InputStream? = socket?.getInputStream()
        Log.i("mappr", "here2 + " + (ist == null))


        Log.i("mappr", "here3")
        val isr : InputStreamReader? = InputStreamReader(ist)
        val istr : BufferedReader? = BufferedReader(isr)
        Log.i("mappr", "here4 " + (istr == null))


        while(true) {
            val res = istr?.readLine()
            try {
                val js = JSONObject(res)
//                val uname = js.getString("username")
                publishMessage(res!!)
            } catch (e: JSONException ) {
                Log.e("mappr", e.message)
            }
            Log.i("mappr", res)
        }


    }



    fun onClickSend(view: MenuItem) {
        val mes : String? = findViewById<EditText>(R.id.text_send).text.toString()
        Log.i("mapp", "$mes")

        thread {
            try {
                val ostr = socket?.getOutputStream()?.writer()
                ostr?.write(mes)
                ostr?.flush()
//                ostr?.flush()
            } catch (e: IOException) {
                Log.i("blah", e.message)
            }
            Log.i("messr", socket?.getInputStream()?.available().toString())
//            receiveLoop()
        }
    }

    fun onMailClick(view : View) {
        // connecting to the server

        animation(toggle)
        toggle = !toggle

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        //menuInflater.inflate(R.menu.)
        return true
    }

    fun animation(t : Boolean) {
        val v : View = findViewById(R.id.seekers)
        val vis : Int = v.visibility
        Log.i("mapp", "$vis")
        val constraintSet = ConstraintSet()
        if(t)
            constraintSet.load(this, R.layout.content_details)
        else
            constraintSet.load(this, R.layout.content_main)
        TransitionManager.beginDelayedTransition(findViewById(R.id.mainv))
        constraintSet.applyTo(conL)

    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
