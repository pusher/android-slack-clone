package com.pusher.chatkitdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import com.pusher.chatkitdemo.room.RoomFragment
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        var twoPane = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        if (findViewById<LinearLayout>(R.id.tabletLinearLayout) != null) {
            twoPane = true

            supportFragmentManager.beginTransaction()
                    .replace(R.id.chatRoomTablet,
                            RoomFragment())
                    .commit()

        } else {
            twoPane = false
        }

    }


}

