package com.pusher.chatkitdemo.parallel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event.*
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi

@ExperimentalCoroutinesApi
fun <A> LifecycleOwner.onLifecycle(block: () -> ReceiveChannel<A>): ReceiveChannel<A> =
        LifecycleReceiverChannel(this.lifecycle, block)

private class LifecycleReceiverChannel<out A> @ExperimentalCoroutinesApi constructor(
        lifecycle: Lifecycle,
        private val block: () -> ReceiveChannel<A>,
        private val broadcastChannel: BroadcastChannel<A> = BroadcastChannel(Channel.CONFLATED),
        private val subscription: ReceiveChannel<A> = broadcastChannel.openSubscription()
) : ReceiveChannel<A> by subscription, LifecycleObserver {

    private var channel: ReceiveChannel<A>? = null

    init {
        lifecycle.addObserver(this)
    }

    @ObsoleteCoroutinesApi
    @OnLifecycleEvent(ON_START)
    fun onStart() {
        channel?.cancel()
        channel = block()
        GlobalScope.launch { channel?.toChannel(broadcastChannel) }
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        channel?.cancel()
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() {
        subscription.cancel()
    }

}
