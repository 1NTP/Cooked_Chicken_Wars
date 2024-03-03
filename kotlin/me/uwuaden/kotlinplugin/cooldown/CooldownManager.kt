package me.uwuaden.kotlinplugin.cooldown

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.cooldown.Cooldown.Companion.cooldowns
import org.bukkit.entity.Entity

object CooldownManager {
    fun Entity.setCooldown(tag: String, cooldown: Int) {
        val entity = this
        cooldowns[Pair(entity.uniqueId, tag)] = cooldown
    }
    fun Entity.isOnCooldown(tag: String): Boolean {
        val entity = this
        return (cooldowns[Pair(entity.uniqueId, tag)] ?: 0) != 0
    }
    fun Entity.getCooldown(tag: String): Int {
        val entity = this
        return cooldowns[Pair(entity.uniqueId, tag)] ?: 0
    }

    fun Entity.resetCooldown() {
        val entity = this
        cooldowns.keys.forEach {
            if (it.first == entity.uniqueId) {
                cooldowns.remove(it)
            }
        }
    }

    fun sch() {
        scheduler.scheduleAsyncRepeatingTask(plugin, {
            try {
                scheduler.scheduleSyncDelayedTask(plugin, {
                    cooldowns.keys.removeIf { (cooldowns[it] ?: 0) <= 0 }
                    cooldowns.filter { it.value > 0 }.forEach {
                        cooldowns[it.key] = it.value - 1
                    }
                }, 0)
            } catch (e: Exception) {
                println(e)
            }
        }, 0, 1)
    }
}