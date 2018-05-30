package com.pusher.chatkitdemo.room

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pusher.chatkitdemo.R
import kotlinx.android.synthetic.main.activity_room.*
import android.net.Uri.parse as parseUri

class RoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
                .add(R.id.frameLayoutActivityRoom, RoomFragment())
                .commit()

    }

}
