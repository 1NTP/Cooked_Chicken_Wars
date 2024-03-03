package me.uwuaden.kotlinplugin.npc

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.setCooldown
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCDeathEvent
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.*

private fun npcDamage(attacker: HumanEntity): Double {
    val npc = CitizensAPI.getNPCRegistry().getByUniqueIdGlobal(attacker.uniqueId)
    if (npc != null) {
        val item = (attacker).inventory.itemInMainHand
        val cd = when (item.type) {
            Material.WOODEN_SWORD -> 12
            Material.IRON_SWORD -> 12
            Material.DIAMOND_SWORD -> 12
            Material.NETHERITE_SWORD -> 12
            else -> 4
        }
        (attacker as LivingEntity).setCooldown("GENERIC_ATTACK", cd)
        return when (item.type) {
            Material.WOODEN_SWORD -> 4.0
            Material.IRON_SWORD -> 6.0
            Material.DIAMOND_SWORD -> 7.0
            Material.NETHERITE_SWORD -> 8.0
            else -> 1.0
        }
    }
    return -1.0
}

class NPCEvent: Listener {
    companion object {
        var aiWorld = HashMap<UUID, World>()
        var targetData = ArrayList<LivingEntity>()
    }
    @EventHandler
    fun onNPCAttack(e: EntityDamageByEntityEvent) {
        if (e.damager is HumanEntity) {
            val damage = npcDamage(e.damager as HumanEntity)
            if (damage != -1.0) {
                e.damage = damage
            }
        }
    }

    @EventHandler
    fun onNPCDeath(e: NPCDeathEvent) {
        val uuid = e.npc.uniqueId
        val world = aiWorld[uuid] ?: return
        val npc = CitizensAPI.getNPCRegistry().getByUniqueId(uuid)
        aiWorld.remove(uuid)
        if (world.name.contains("Field-")) {
            val data = WorldManager.initData(world)
            data.aiData.removeIf { it.uuid == uuid }
        }
        if (npc != null && npc.name == "AI-Bot") {
            scheduler.scheduleSyncDelayedTask(plugin, {
                e.npc.destroy()
            }, 20*5)
        }
    }
}