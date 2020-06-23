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
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    var conL: ConstraintLayout? = null
    var toggle: Boolean = true

    val imPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val imPaintW: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var imB: Bitmap? = null
    var imCanvas: Canvas? = null
    private val s = State(-1.0f, -1.0f)
    var clientId : String? = null

    private var clusterConnection: ExecutionManager? = null


    class State(var x: Float, var y: Float)



    class MySeekerListener(private val clName : String, private val callback: (String, Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

            callback(clName, scaledProgress(seekBar?.progress!!))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

//        text_send.
        text_send.text = getString(R.string.not_connected)

        conL = findViewById(R.id.content_main)


        imPaint.color = Color.BLACK
        imPaint.strokeWidth = 10.0f
        imPaint.isDither = false



        imPaintW.color = Color.BLACK
        imPaintW.strokeWidth = 15f
        imPaintW.textSize = 38f
        imPaintW.isDither = true



        val d = imageView.drawable
        imB = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        imCanvas = Canvas(imB!!)
        Log.i("conns", "w".plus(d.intrinsicWidth).plus("s").plus(d.intrinsicHeight))



        val messageListener : (String?) -> Unit = {m  -> publishMessage(m!!)}
        val clientIdListener : (String?) -> Unit = {m  ->
            this.clientId = m
        }
        val connListener : (String) -> Unit = {m -> text_send.text = m }
        clusterConnection = ExecutionManager(hashMapOf(
            Pair("seed", scaledProgress(seedBar.progress)),
            Pair("c1", scaledProgress(c1Bar.progress)),
            Pair("c2", scaledProgress(c2Bar.progress))
        ), messageListener, clientIdListener, connListener)

        val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        clientId = sp.getString("client_id", "xxx")
        val b : HashMap<String, Pair<String, Int>> = HashMap()
        arrayOf("seed", "c1", "c2").forEach { s ->
            val p = parseHostPortFromString(sp.getString(s.plus("_address"), "localhost:80")!!)
            Log.i("conns", s.plus(p.toString()))
            b[s] = p
        }


        val scheduler = Executors.newSingleThreadScheduledExecutor()

        scheduler.scheduleAtFixedRate({

            try {
                Log.i("messr", "Reconnecting? " + clientId)
                clusterConnection?.reconnectToClosest(clientId!!, b)
            } catch (e : Exception) {
                Log.e("messr", "exception while reconnecting")
                e.printStackTrace()
            }
        }, 8, 15, TimeUnit.SECONDS)

        seedBar.setOnSeekBarChangeListener(MySeekerListener("seed", clusterConnection!!::onLatencyChange))
        c1Bar.setOnSeekBarChangeListener(MySeekerListener("c1", clusterConnection!!::onLatencyChange))
        c2Bar.setOnSeekBarChangeListener(MySeekerListener("c2",  clusterConnection!!::onLatencyChange))

        fab.setOnClickListener(this::onMailClick)


    }

    override fun onDestroy() {
        super.onDestroy()
        clusterConnection?.destroyAll()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickMenu(view: MenuItem) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onClicksaveid(view : MenuItem) {
        val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sp.edit().putString("client_id", this.clientId).apply()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickConnect(view: MenuItem) {
        val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val lataddr = parseHostPortFromString(sp.getString("latency_address", "192.168.0.101:21003")!!)
        clusterConnection?.connectForLatency(lataddr)


        val b : HashMap<String, Pair<String, Int>> = hashMapOf()
        arrayOf("seed", "c1", "c2").forEach { s ->
            val p = parseHostPortFromString(sp.getString(s.plus("_address"), "localhost:80")!!)
            Log.i("conns", s.plus(p.toString()))
            b[s] = p
        }
        val client_id = sp.getString("client_id", "xxx")
        if(client_id.startsWith("xxx"))
            clusterConnection?.connectToClosest(b)
        else
            clusterConnection?.connectToClosest(b, client_id!!)
    }


    private fun publishMessage(message: String) {
        val sp = message.split(':')
        val x = sp[0].toFloat()
        val y = sp[1].toFloat()
        val c = "counter " + sp[2].toInt()
        val d = imageView.drawable ?: return
        imageView.post {
            imB = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
            imCanvas = Canvas(imB!!)
            imCanvas?.drawRect(x, y, x + 50f, y + 50f, imPaint)
            imCanvas?.drawText(c, 50f, 50f, imPaintW)
            imageView.setImageBitmap(imB)
//            if(s.x < 0f) {
//                s.x = x
//                s.y = y
//                imCanvas?.drawRect(x, y, x + 50f, y + 50f, imPaint)
//            } else {
//                imCanvas?.drawRect(s.x, s.y, s.x + 50f, s.y + 50f, imPaintW)
//                imCanvas?.drawRect(x, y, x + 50f, y + 50f, imPaint)
//                s.x = x
//                s.y = y
//                imageView.setImageBitmap(imB)
//            }

        }
    }

    @Suppress("UNUSED_PARAMETER")
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

    private fun animation(t : Boolean) {
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
