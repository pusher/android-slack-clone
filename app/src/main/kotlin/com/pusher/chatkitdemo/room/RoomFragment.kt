package com.pusher.chatkitdemo.room

import android.arch.lifecycle.Lifecycle.State.STARTED
import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.os.Looper
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.messages.multipart.Payload
import com.pusher.chatkit.messages.multipart.Payload.Inline
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkitdemo.ChatKitDemoApp.Companion.app
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.RoomState.*
import com.pusher.chatkitdemo.showOnly
import com.pusher.util.Result
import elements.Subscription
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room_loaded.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.*
import kotlin.properties.Delegates

typealias PusherError = elements.Error

class RoomFragment : Fragment() {

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }

    private var state by Delegates.observable<RoomState>(RoomState.Initial) { _, _, new ->
        new.render()
    }

    private val adapter = dataAdapterFor<Item> {
        on<Item.Loaded>(R.layout.item_message) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Pending>(R.layout.item_message_pending) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Failed>(R.layout.item_message_pending) { (details, _) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_room, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageList.adapter = adapter
        messageList.layoutManager = LinearLayoutManager(activity).apply {
            reverseLayout = false
            stackFromEnd = false
            isSmoothScrollbarEnabled = true
        }
        sendButton.setOnClickListener {
            messageInput.text.takeIf { it.isNotBlank() }?.let { text ->
                state.let { it as? Ready }?.let { it.room.id }?.let { roomId ->
                    sendMessage(roomId, text.toString())
                    messageInput.text.clear()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        state.let { it as? Ready }?.sub?.unsubscribe()
    }

    @ExperimentalCoroutinesApi
    private fun sendMessage(roomId: String, text: String) = GlobalScope.launch {
        app.currentUser().apply {
            val item = Item.Pending(Item.Details(name ?: id, text))
            addItem(item)

            sendMultipartMessage(
                    roomId = roomId,
                    parts = listOf(NewPart.Inline(text, "text/plain")),
                    callback = { result ->
                        when (result) {
                            is Result.Success -> {
                                removeItem(item)
                            }
                            is Result.Failure -> {
                                removeItem(item)
                            }
                        }
                    }
            )
        }
    }

    @ExperimentalCoroutinesApi
    fun bind(roomId: String) = GlobalScope.launch {
        // if (Looper.myLooper() == null) Looper.prepare() // Old version of the SDK uses a handle and breaks

        with(app.currentUser()) {
            val room = rooms.find { it.id == roomId }
            var messagecontent: String = ""
            when (room) {
                null -> renderFailed(Error("Room not found"))
                else -> subscribeToRoomMultipart(
                        room = room,
                        messageLimit = 20,
                        listeners = RoomListeners(
                                onErrorOccurred = { error ->
                                    state = Failed(error)
                                },
                                onMultipartMessage = { message ->
                                    message.parts.forEach { part ->
                                        val payload = part.payload
                                        when (payload) {
                                            is Inline -> {
                                                messagecontent = payload.content
                                            }
                                            is Payload.Url -> {
                                            }
                                            is Payload.Attachment -> {
                                            }
                                            else -> {
                                            }
                                        }
                                    }
                                    Log.d("Slackclone", "Message added" + message.sender?.name)

                                    addItem(
                                            Item.Loaded(
                                                    Item.Details(
                                                            userName = message.sender?.name
                                                                    ?: "???",
                                                            message = messagecontent ?: "----"
                                                    )
                                            )
                                    )


                                }
                        ),


                        callback = { subscription ->
                            Log.d("Slackclone", "Room Ready")
                            state = Ready(room, subscription, adapter.data)
                        }
                )
            }
        }
    }

    private fun addItem(item: Item) = launchOnUi {
        adapter.data = adapter.data + item
        messageList.scrollToPosition(adapter.itemCount - 1)
    }


    private fun removeItem(item: Item) = launchOnUi {
        adapter.data = adapter.data - item
    }


    private fun RoomState.render(): Job = when (this) {
        is Initial -> renderIdle()
        is Ready -> renderLoadedCompletely(room, items)
        is Failed -> renderFailed(Error("$error"))
    }

    @UiThread
    private fun renderIdle() = launchOnUi {
        views.showOnly(idleLayout)
        adapter.data = emptyList()
    }

    private fun renderLoadedCompletely(room: Room, messages: List<RoomState.Item>) = launchOnUi {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
        adapter.data = messages
    }

    private fun renderFailed(error: Error) = launchOnUi {
        views.showOnly(errorLayout)
        errorMessageView.text = error.message
        retryButton.visibility = View.GONE // TODO: Retry button
    }
}

private fun LifecycleOwner.launchOnUi(block: suspend CoroutineScope.() -> Unit) = when {
    lifecycle.currentState > STARTED -> GlobalScope.launch(context = Dispatchers.Main, block = block)
    else -> GlobalScope.launch { Log.d("Slackclone", "Unexpected lifecycle state: ${lifecycle.currentState}") }
}

sealed class RoomState {

    object Initial : RoomState()
    data class Ready(val room: Room, val sub: Subscription, val items: List<Item>) : RoomState()
    data class Failed(val error: PusherError) : RoomState()

    sealed class Item {
        abstract val details: Details

        data class Loaded(override val details: Details) : Item()
        data class Pending(override val details: Details) : Item()
        data class Failed(override val details: Details, val error: PusherError) : Item()

        data class Details(val userName: String, val message: String)
    }
}