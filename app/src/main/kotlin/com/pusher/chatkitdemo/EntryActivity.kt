package com.pusher.chatkitdemo

import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkitdemo.ChatKitDemoApp.Companion.app
import com.pusher.chatkitdemo.EntryActivity.State.*
import com.pusher.chatkitdemo.arch.viewModel
import com.pusher.chatkitdemo.navigation.NavigationEvent
import com.pusher.chatkitdemo.navigation.navigationEvent
import com.pusher.chatkitdemo.navigation.openInBrowser
import com.pusher.chatkitdemo.navigation.openMain
import elements.Error
import elements.NetworkError
import kotlinx.android.synthetic.main.activity_entry.*
import kotlinx.android.synthetic.main.activity_entry_loaded.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.include_loading.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import okhttp3.*
import kotlin.properties.Delegates

class EntryActivity : AppCompatActivity() {

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }
    @ExperimentalCoroutinesApi
    private val viewModel by lazy { viewModel<EntryViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override fun onStart() {
        super.onStart()
        // TODO: Investigate potential leak into viewModel
        GlobalScope.launch { viewModel.states.consumeEach { state = it } }
    }

    @ExperimentalCoroutinesApi
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val navigationEvent = intent?.navigationEvent
        when (navigationEvent) {
            is NavigationEvent.Entry.WithGitHubCode -> viewModel.authorize(navigationEvent.code)// Log.d("TOKEN", "token: ${navigationEvent.code}")
        }
    }

    @ExperimentalCoroutinesApi
    private var state by Delegates.observable<State>(Idle) { _, _, state ->
        state.render()
    }

    sealed class State {
        object Idle : State()
        data class RequiresAuth(val authUrl: String) : State()
        data class UserIdReady(val userId: String) : State()
        data class UserReady(val currentUser: CurrentUser) : State()
        data class Failure(val error: Error) : State()
    }

    @ExperimentalCoroutinesApi
    private fun State.render() = when (this) {
        is Idle -> GlobalScope.launch(Dispatchers.Main) { renderIdle() }
        is UserIdReady -> GlobalScope.launch(Dispatchers.Main) {
            renderIdle()
            viewModel.loadUser(userId)
        }
        is UserReady -> GlobalScope.launch(Dispatchers.Main) { renderUser(currentUser) }
        is Failure -> GlobalScope.launch(Dispatchers.Main) { renderFailure(error) }
        is RequiresAuth -> GlobalScope.launch(Dispatchers.Main) { openInBrowser(authUrl) }
    }

    private fun renderIdle() {
        views.showOnly(idleLayout)
        loadingTextView.setText(R.string.logging_you_in)
    }

    private fun renderUser(user: CurrentUser) {
        views.showOnly(loadedLayout)
        userNameView.text = "Welcome ${user.name}"
        continueButton.setOnClickListener {
            openMain(user.id)
        }
    }

    @ExperimentalCoroutinesApi
    private fun renderFailure(error: Error) {
        views.showOnly(errorLayout)
        errorMessageView.text = "$error"
        retryButton.setOnClickListener {
            state = Idle
            viewModel.load()
        }
    }

}

@ExperimentalCoroutinesApi
class EntryViewModel : ViewModel() {

    private object Github {
        private const val gitHubClientId = "20cdd317000f92af12fe"
        private const val callbackUri = "https://chatkit-demoauth-server.herokuapp.com/success?url=chatkit://auth"

        const val gitHubAuthUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=$gitHubClientId" +
                "&scope=user:email" +
                "&redirect_uri=$callbackUri"
    }

    @ExperimentalCoroutinesApi
    private val stateBroadcast = BroadcastChannel<EntryActivity.State>(Channel.CONFLATED)
    @ExperimentalCoroutinesApi
    val states get() = stateBroadcast.openSubscription()

    private val client by lazy { OkHttpClient() }
    private val gson by lazy { GsonBuilder().create() }

    init {
        stateBroadcast.offer(Idle)
        load()
    }

    fun load() = GlobalScope.launch {
        app.userId.let { id ->
            when (id) {
                null -> RequiresAuth(Github.gitHubAuthUrl)
                else -> UserIdReady(id)
            }.let { stateBroadcast.send(it) }
        }
    }

    suspend fun loadUser(userId: String) {
        app.userId = userId
        val state = UserReady(app.currentUser())
        stateBroadcast.send(state)
    }

    override fun onCleared() {
        stateBroadcast.close()
    }

    data class AuthResponseBody(val id: String, val token: String)
    data class AuthRequestBody(val code: String)

    fun authorize(code: String) {
        GlobalScope.launch {
            if (Looper.myLooper() == null) Looper.prepare()
            val requestBody = RequestBody.create(MediaType.parse("text/plain"), AuthRequestBody(code).toJson())
            val request = Request.Builder().apply {
                url("https://chatkit-demoauth-server.herokuapp.com/auth")
                post(requestBody)
            }.build()
            val response = client.newCall(request).execute()
            val responseBody = response.body()?.fromJson<AuthResponseBody>()
            when (responseBody) {
                null -> stateBroadcast.offer(Failure(NetworkError("Oops! response: $response")))
                else -> responseBody.let { (id, token) ->
                    app.userId = id
                    app.token = token
                    stateBroadcast.send(UserIdReady(id))
                }
            }
        }
    }

    private fun <A> A.toJson(): String =
            gson.toJson(this)

    private inline fun <reified A> ResponseBody.fromJson(): A =
            gson.fromJson(this.charStream(), A::class.java)

}
