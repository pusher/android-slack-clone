package com.pusher.chatkitdemo

import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import com.pusher.chatkit.*
import com.pusher.chatkitdemo.BuildConfig.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlin.properties.Delegates

val Context.app: ChatKitDemoApp
    get() = when (applicationContext) {
        null -> throw IllegalStateException("Application context is null")
        is ChatKitDemoApp -> applicationContext as ChatKitDemoApp
        else -> throw IllegalStateException("Application context ($applicationContext) is not ${nameOf<ChatKitDemoApp>()}")
    }

val Fragment.app: ChatKitDemoApp
    get() = context!!.app

class ChatKitDemoApp : Application() {

    companion object {
        private var maybeApp: ChatKitDemoApp? = null
        val app get() = checkNotNull(maybeApp)
    }

    private val tokenProvider: TokenProvider
        get() {
            val userId = userPreferences.userId
            val token = userPreferences.token
            checkNotNull(userId) { "No user id available" }
            checkNotNull(token) { "No token available" }
            val endpoint = "$TOKEN_PROVIDER_ENDPOINT?user=$userId&token=$token"
            return ChatkitTokenProvider(endpoint)
        }

    val logger: Logger by lazy { AndroidLogger(LogLevel.VERBOSE) }
    private val userPreferences by lazy { UserPreferences(this) }

    var userId: String?
        get() = userPreferences.userId
        set(value) {
            userPreferences.userId = value
            value?.let { id ->
                userPreferences.userId = id
                connect()
            }
        }

    var token : String?
        get() = userPreferences.token
        set(value) { userPreferences.token = value }

    private val chat: ChatManager by lazy {
        ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = userPreferences.userId ?: USER_ID,
            context = applicationContext,
            tokenProvider = tokenProvider
        )
    }

    override fun onCreate() = super.onCreate().also {
        maybeApp = this
    }

    private fun connect() {
        chat.connect(object : UserSubscriptionListener {
            override fun removedFromRoom(roomId: Int) = Unit
            override fun userLeft(user: User?, room: Room?) = Unit
            override fun usersUpdated() = Unit
            override fun userCameOnline(user: User?) = Unit
            override fun roomUpdated(room: Room?) = Unit
            override fun addedToRoom(room: Room?) = Unit
            override fun roomDeleted(roomId: Int) = Unit
            override fun userWentOffline(user: User?) = Unit
            override fun userStoppedTyping(user: User?) = Unit
            override fun userJoined(user: User?, room: Room?) = Unit
            override fun userStartedTyping(user: User?) = Unit
            override fun onError(error: Error?) = Unit
            override fun currentUserReceived(currentUser: CurrentUser?) {
                app.currentUser = currentUser
            }
        })
    }

    private var currentUser by Delegates.observable<CurrentUser?>(null) { _, _, new ->
        new?.let { userBroadcast.offer(it) }
    }

    private val userBroadcast = BroadcastChannel<CurrentUser>(capacity = Channel.CONFLATED)

    suspend fun currentUser(): CurrentUser = when (currentUser) {
        null -> userBroadcast.openSubscription().receive()
        else -> currentUser!!
    }

}
