package com.pusher.chatkitdemo.room

import com.pusher.chatkit.rooms.Room

val Room.coolName
    get() = "#$name"