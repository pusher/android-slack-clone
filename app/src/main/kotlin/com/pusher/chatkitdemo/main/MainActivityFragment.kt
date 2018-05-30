package com.pusher.chatkitdemo.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.pusher.chatkit.Room
import com.pusher.chatkit.RoomsListener
import com.pusher.chatkitdemo.MainActivity
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.navigation.open
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.RoomFragment
import com.pusher.chatkitdemo.room.coolName
import elements.Error
import kotlinx.android.synthetic.main.fragment_main_activity.*
import kotlinx.android.synthetic.main.item_room.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates


class MainActivityFragment : Fragment() {

    lateinit var progressBar:ProgressBar

    private val adapter = dataAdapterFor(R.layout.item_room) { room: Room ->
        @SuppressLint("SetTextI18n")
        roomNameView.text = room.coolName
        roomItemLayout.setOnClickListener {
            if (MainActivity.twoPane){
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.chatRoomTablet,RoomFragment())
                        .commit()
            } else {
                activity!!.open(room)
            }
        }
    }

    private var state by Delegates.observable<State>(State.Idle) { _, _, state ->
        state.render()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main_activity, container, false)
        progressBar = view.findViewById(R.id.progress)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomListView.adapter = adapter
        roomListView.layoutManager = LinearLayoutManager(activity)

        state = State.Idle
        launch {
            app.currentUser().getJoinableRooms(RoomsListener { rooms ->
                state = State.Loaded(rooms) })
        }
    }

    private fun State.render() = when (this) {
        is State.Idle -> launch(UI) { renderIdle() }
        is State.Loaded -> launch(UI) { renderLoaded(rooms) }
        is State.Failed -> launch(UI) { renderFailed(error) }
    }

    private fun renderIdle() {
        progressBar.visibility = View.VISIBLE
        roomListView.visibility = View.GONE
        errorView.visibility = View.GONE
    }

    private fun renderLoaded(rooms: MutableList<Room>) {
        progress.visibility = View.GONE
        roomListView.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        adapter.data = rooms.filter { it.memberUserIds.size < 100 }
    }

    private fun renderFailed(error: Error) {
        progress.visibility = View.GONE
        roomListView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        errorView.text = "$error"
    }

    sealed class State {
        object Idle : State()
        data class Loaded(val rooms: MutableList<Room>) : State()
        data class Failed(val error: Error) : State()
    }


}
