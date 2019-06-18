package com.pusher.chatkitdemo.parallel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope


@ExperimentalCoroutinesApi
fun <A> lazyBroadcast(block: suspend BroadcastChannel<A>.() -> Unit = {}) =
        lazy { broadcast(block) }

@ExperimentalCoroutinesApi
fun <A> broadcast(block: suspend BroadcastChannel<A>.() -> Unit = {}) =
        BroadcastChannel<A>(Channel.CONFLATED).also {
            GlobalScope.launch { block(it) }
        }
