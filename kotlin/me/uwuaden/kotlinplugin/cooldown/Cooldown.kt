package me.uwuaden.kotlinplugin.cooldown

import java.util.*

class Cooldown() {
    companion object {
        val cooldowns = HashMap<Pair<UUID, String>, Int>()
    }
}