package com.pusher.chatkitdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val navController: NavController by lazy {
        findNavController(R.id.navHostFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        ChatKitDemoApp.twoPane = findViewById<LinearLayout>(R.id.tabletLinearLayout) != null
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp()
            = navController.navigateUp()


}

