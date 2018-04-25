package com.pusher.chatkitdemo.room

import android.arch.lifecycle.Lifecycle.State.STARTED
import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pusher.chatkit.Message
import com.pusher.chatkit.Room
import com.pusher.chatkit.RoomSubscriptionListeners
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.showOnly
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room_loaded.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import com.pusher.chatkitdemo.room.RoomState.*
import kotlinx.coroutines.experimental.Job

typealias PusherError = elements.Error

class RoomFragment : Fragment() {

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }

    private val adapter = dataAdapterFor<Item> {
        on<Item.Loaded>(R.layout.item_message) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Pending>(R.layout.item_message_pending) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Failed>(R.layout.item_message_pending) { (details, error) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_room, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            messageList.adapter = adapter
            messageList.layoutManager = LinearLayoutManager(activity).apply {
                reverseLayout = true
            }
            sendButton.setOnClickListener {
                val messageText = messageInput.text
                if (messageText.isNotBlank()) {
                    TODO()
                }
            }
        }
    }

    fun bind(roomId: Int) = launch {
        with(app.currentUser()) {
            val room = getRoom(roomId)
            when (room) {
                null -> renderFailed(Error("Room not found"))
                else -> subscribeToRoom(room, listeners = object : RoomSubscriptionListeners {
                    override fun onError(error: PusherError?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onNewMessage(message: Message?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                })
            }

        }
    }

    private fun RoomState.render(): Job = when (this) {
        is Initial -> renderIdle()
        is Idle -> renderIdle()
        is NoMembership -> renderNoMembership()
        is RoomLoaded -> renderLoadedRoom(room)
        is Ready -> renderLoadedCompletely(room, items)
        is Failed -> renderFailed(Error("$error"))
    }

    @UiThread
    private fun renderIdle() = launchOnUi {
        views.showOnly(idleLayout)
        adapter.data = emptyList()
    }

    private fun renderLoadedRoom(room: Room) = launchOnUi {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
    }

    private fun renderLoadedCompletely(room: Room, messages: List<RoomState.Item>) = launchOnUi {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
        adapter.data = messages
    }

    private fun renderFailed(error: Error) = launchOnUi {
        views.showOnly(errorLayout)
        errorMessageView.text = error.message
        retryButton.visibility = View.GONE // TODO: Retry policy
    }

    private fun renderNoMembership() =
        renderIdle()

}

private fun LifecycleOwner.launchOnUi(block: suspend CoroutineScope.() -> Unit) = when {
    lifecycle.currentState > STARTED -> launch(context = UI, block = block)
    else -> launch { Log.d("Boo", "Unexpected lifecycle state: ${lifecycle.currentState}") }
}

sealed class RoomState {

    object Initial : RoomState()
    data class Idle(val roomId: Int) : RoomState()
    data class NoMembership(val room: Room) : RoomState()
    data class RoomLoaded(val room: Room) : RoomState()

    data class Ready(val room: Room, val items: List<Item>) : RoomState()

    data class Failed (val error: Error) : RoomState()

    sealed class Item {
        abstract val details: Details

        data class Loaded(override val details: Details) : Item()
        data class Pending(override val details: Details) : Item()
        data class Failed(override val details: Details, val error: Error) : Item()

        data class Details(val userName: CharSequence, val message: CharSequence)

    }

}

