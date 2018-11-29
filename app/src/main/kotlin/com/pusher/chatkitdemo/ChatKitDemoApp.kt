package com.pusher.chatkitdemo

import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import com.pusher.chatkit.AndroidChatkitDependencies
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatkitTokenProvider
import com.pusher.chatkit.CurrentUser
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.util.Result
import elements.Error
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlin.properties.Delegates

private const val INSTANCE_LOCATOR = "v1:us1:05f46048-3763-4482-9cfe-51ff327c3f29"
private const val TOKEN_PROVIDER_ENDPOINT = "https://chatkit-demo-server.herokuapp.com/token"

val Context.app: ChatKitDemoApp
    get() = when (applicationContext) {
        null -> error("Application context is null")
        is ChatKitDemoApp -> applicationContext as ChatKitDemoApp
        else -> error("Application context ($applicationContext) is not ${nameOf<ChatKitDemoApp>()}")
    }

val Fragment.app: ChatKitDemoApp
    get() = context!!.app

class ChatKitDemoApp : Application() {

    companion object {
        private var maybeApp: ChatKitDemoApp? = null
        val app get() = checkNotNull(maybeApp)
    }

    val logger: Logger by lazy { AndroidLogger(LogLevel.VERBOSE) }
    private val userPreferences by lazy { UserPreferences(this) }

    var userId: String?
        get() = userPreferences.userId
        set(value) {
            userPreferences.userId = value
            tryConnect(value, token)
        }

    var token: String?
        get() = userPreferences.token
        set(value) {
            userPreferences.token = value
            tryConnect(userId, value)
        }

    private fun tryConnect(userId: String?, token: String?) = when {
        userId != null && token != null -> connect(userId, token)
        else -> Unit // ignore
    }

    private lateinit var chat: ChatManager

    override fun onCreate() = super.onCreate().also {
        maybeApp = this
    }

    private fun connect(userId: String, token: String) {

        val dependencies = AndroidChatkitDependencies(
                tokenProvider = ChatkitTokenProvider(
                        endpoint = "$TOKEN_PROVIDER_ENDPOINT?token=$token",
                        userId = userId
                ),
                context = applicationContext
        )

        chat = ChatManager(
                instanceLocator = INSTANCE_LOCATOR,
                userId = userId,
                dependencies = dependencies
        )

        chat.connect { result ->
            when (result) {
                is Result.Success -> {
                    currentUser = result.value
                }

                is Error -> logger.error("Failed to connect and  get the current user")
            }
        }
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
