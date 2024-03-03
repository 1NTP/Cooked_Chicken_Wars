package me.uwuaden.kotlinplugin.teamSystem

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.Main.Companion.worldDatas
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern
import java.util.*

private fun setHelmet(player: Player, target: Player, color: Color) {
    if (target.equipment.helmet != null && target.equipment.helmet.type.toString().contains("HELMET")) {
        val item = target.equipment.helmet.clone()
        val meta = item.itemMeta as ArmorMeta
        meta.trim = ArmorTrim(TrimMaterial.EMERALD, TrimPattern.WARD)
        item.itemMeta = meta
        if (item.type != Material.AIR) {
            player.sendEquipmentChange(target, EquipmentSlot.HEAD, item)
        }
    } else {
        val leatherHelm = ItemStack(Material.LEATHER_HELMET)
        val leatherMeta = leatherHelm.itemMeta as LeatherArmorMeta
        leatherMeta.setColor(color)
        leatherHelm.itemMeta = leatherMeta
        player.sendEquipmentChange(target, EquipmentSlot.HEAD, leatherHelm)
    }
}
object TeamManager {
    fun isSameTeam(world: World, player1: Player, player2: Player): Boolean {
        val teams = worldDatas[world]?.teams ?: return false
        if (teams.filter { it.players.contains(player1) }.isEmpty()) return false
        if (teams.filter { it.players.contains(player2) }.isEmpty()) return false
        return teams.filter { it.players.contains(player1) } == teams.filter { it.players.contains(player2) }
    }

    fun isSameTeam(world: World, attacker: Player, victim: LivingEntity): Boolean {
        if (victim !is Player) return false
        val teams = worldDatas[world]?.teams ?: return false
        if (teams.filter { it.players.contains(attacker) }.isEmpty()) return false
        if (teams.filter { it.players.contains(victim) }.isEmpty()) return false
        return teams.filter { it.players.contains(attacker) } == teams.filter { it.players.contains(victim) }
    }

    fun getTeam(world: World, player: Player): TeamClass? {
        val teams = worldDatas[world]?.teams ?: return null
        val t = teams.filter { it.players.contains(player) }
        if (t.isNotEmpty()) {
            return t[0]
        }
        return null
    }
    fun getTeam(world: World, uuid: UUID): TeamClass? {
        val teams = worldDatas[world]?.teams ?: return null
        val t = teams.filter { it.uuid == uuid }
        if (t.isNotEmpty()) {
            return t[0]
        }
        return null
    }
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.filter { it.name.contains("Field-")}.forEach { world ->
                world.players.forEach { p ->
                    if (p.gameMode == GameMode.SPECTATOR) {
                        p.location.getNearbyPlayers(50.0).forEach {
                            val team = getTeam(world, it)
                            if (it != p) {
                                if (isSameTeam(it.world, p, it)) {
                                    setHelmet(p, it, Color.LIME)
                                } else {
                                    if (team != null) {
                                        val leatherHelm = ItemStack(Material.LEATHER_HELMET)
                                        val leatherMeta = leatherHelm.itemMeta as LeatherArmorMeta
                                        leatherMeta.setColor(team.color)

                                        leatherHelm.itemMeta = leatherMeta
                                        p.sendEquipmentChange(it, EquipmentSlot.HEAD, leatherHelm)
                                    }
                                }
                            }
                        }
                    } else {
                        p.location.getNearbyPlayers(50.0).forEach {
                            if (isSameTeam(it.world, p, it)) {
                                if (it != p) {
                                    setHelmet(p, it, Color.LIME)
                                }
                            }
                        }
                    }
                }
            }

        }, 0, 5)
    }
}