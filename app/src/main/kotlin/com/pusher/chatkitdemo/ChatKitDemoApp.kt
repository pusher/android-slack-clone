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
    val userPreferences by lazy { UserPreferences(this) }

    val chat: ChatManager by lazy {
        ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = userPreferences.userId ?: USER_ID,
            context = applicationContext,
            tokenProvider = tokenProvider
        )
    }

    init {
        maybeApp = this
        chat.connect(object : UserSubscriptionListener {
            override fun removedFromRoom(roomId: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userLeft(user: User?, room: Room?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun usersUpdated() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userCameOnline(user: User?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun roomUpdated(room: Room?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addedToRoom(room: Room?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun roomDeleted(roomId: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userWentOffline(user: User?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userStoppedTyping(user: User?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userJoined(user: User?, room: Room?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun userStartedTyping(user: User?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onError(error: Error?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

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
