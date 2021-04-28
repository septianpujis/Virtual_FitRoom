package com.skripsisepti.virtualfitroom

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import kotlin.system.exitProcess


@SuppressLint("Registered")
class TabelList : AppCompatActivity() {

    private var gambarTabel: ImageView? = null
    private var keKiri: View? = null
    private var keKanan: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabel_list)
        setSupportActionBar(findViewById(R.id.toolbar))

        gambarTabel = findViewById(R.id.gam1)
        keKiri = findViewById(R.id.back)
        keKanan = findViewById(R.id.next)


        keKiri!!.setOnClickListener{
            if(mCounter>=1){
                mCounter-=1
                counterCheck(mCounter)
            }else
                Toast.makeText(this, "Akhir", Toast.LENGTH_SHORT).show()
        }
        keKanan!!.setOnClickListener{
            if(mCounter<=4){
                mCounter+=1
                counterCheck(mCounter)
            }else
                Toast.makeText(this, "Akhir", Toast.LENGTH_SHORT).show()
        }

    }
    private var mCounter = 1
    private fun counterCheck(counter: Int){
        when (counter) {
            1 -> {
                gambarTabel!!.setImageResource(R.drawable.polo_male)
            }
            2 -> {
                gambarTabel!!.setImageResource(R.drawable.kemeja_male)
            }
            3 -> {
                gambarTabel!!.setImageResource(R.drawable.polo_female)
            }
            4 -> {
                gambarTabel!!.setImageResource(R.drawable.kejema_female)
            }
        }
    }

    //===========================================================    OPTION MENU
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_option_2, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        return when (item.itemId) {
            R.id.home -> {
                i = Intent(this, CameraActivity::class.java)
                finish()
                startActivity(i)
                true
            }
            R.id.tabel_baju -> {
                i = Intent(this, TabelList::class.java)
                finish()
                startActivity(i)
                true
            }
            R.id.about -> {
                true
            }
            R.id.quit -> {
                finish()
                exitProcess(0)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}