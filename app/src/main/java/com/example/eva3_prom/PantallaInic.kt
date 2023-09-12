package com.example.eva3_prom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class PantallaInic : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_inic)

        val btnVolver = findViewById<Button>(R.id.button)
        btnVolver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
       val btnVolver2 = findViewById<Button>(R.id.button2)
        btnVolver2.setOnClickListener {
            val intent = Intent(this, Mapa::class.java)
            startActivity(intent)
        }

    }
}