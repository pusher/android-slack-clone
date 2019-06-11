package com.pusher.chatkitdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View.GONE
import android.view.View.VISIBLE
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkitdemo.MainActivity.State.Loaded
import com.pusher.chatkitdemo.navigation.open
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.coolName
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_room.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.android.synthetic.main.activity_main.room_list as roomListView

import android.util.Log

class MainActivity : AppCompatActivity() {

    private val adapter = dataAdapterFor(R.layout.item_room) { room: Room ->
        @SuppressLint("SetTextI18n")
        roomNameView.text = room.coolName
        roomItemLayout.setOnClickListener {
            open(room)
        }
    }

    private var state by Delegates.observable<State>(State.Idle) { _, _, state ->
        state.render()
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        roomListView.adapter = adapter
        roomListView.layoutManager = LinearLayoutManager(this)

        state = State.Idle



        GlobalScope.launch {
            state = Loaded(app.currentUser().rooms.toSet())

        }


    }

    private fun State.render() = when (this) {
        is State.Idle -> GlobalScope.launch(Dispatchers.Main) { renderIdle() }
        is State.Loaded -> GlobalScope.launch(Dispatchers.Main) { renderLoaded(rooms) }
        is State.Failed -> GlobalScope.launch(Dispatchers.Main) { renderFailed(error) }
    }

    private fun renderIdle() {
        progress.visibility = VISIBLE
        roomListView.visibility = GONE
        errorView.visibility = GONE
    }

    private fun renderLoaded(rooms: Set<Room>) {
        progress.visibility = GONE
        roomListView.visibility = VISIBLE
        errorView.visibility = GONE
        adapter.data = rooms.filter { it.memberUserIds.size < 100 }
    }

    private fun renderFailed(error: Error) {
        progress.visibility = GONE
        roomListView.visibility = GONE
        errorView.visibility = VISIBLE
        errorView.text = "$error"
    }

    sealed class State {
        object Idle : State()
        data class Loaded(val rooms: Set<Room>) : State()
        data class Failed(val error: Error) : State()
    }
}
