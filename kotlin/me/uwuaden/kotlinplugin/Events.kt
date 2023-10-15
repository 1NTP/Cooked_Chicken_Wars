package me.uwuaden.kotlinplugin

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import me.uwuaden.kotlinplugin.Main.Companion.lastDamager
import me.uwuaden.kotlinplugin.Main.Companion.lastWeapon
import me.uwuaden.kotlinplugin.Main.Companion.lobbyLoc
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.gameSystem.LastWeaponData
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerCoin
import me.uwuaden.kotlinplugin.skillSystem.SkillManager
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import kotlin.math.roundToInt

private fun deathPlayer(p: Player) {
    if (p.world.name.contains("Field-")) {
        p.gameMode = GameMode.SPECTATOR

        var killer = p.killer
        if (killer == null) {
            killer = lastDamager[p]
        }
        val dataClass = WorldManager.initData(p.world)
        if (dataClass.deadPlayer.contains(p)) return
        if (killer != null) {
            dataClass.playerKill[killer.uniqueId] = (dataClass.playerKill[killer.uniqueId] ?: 0) + 1
            SkillManager.addCapacityPoint(killer, 100)
        }
        if (lastDamager[p] == null) {
            WorldManager.broadcastWorld(
                p.world,
                "${ChatColor.RED}☠   ${ChatColor.BOLD}➔ ${ChatColor.RED}${p.name}"
            )
        }
        else {
            var weaponName: String? = null

            if (lastWeapon[p] != null) {
                val data = lastWeapon[p]!!
                if (data.effTimeMilli > System.currentTimeMillis() && data.item.itemMeta.displayName != "") {
                    weaponName = data.item.itemMeta.displayName

                }
            }

            var msg = "${ChatColor.RED}☠ ${lastDamager[p]!!.name} ${ChatColor.BOLD}➔ ${ChatColor.RED}${p.name}"
            if (weaponName != null) msg += "${ChatColor.RED} with ${ChatColor.BOLD}[${weaponName}${ChatColor.RED}${ChatColor.BOLD}]"

            WorldManager.broadcastWorld(
                p.world,
                msg
            )
        }

        val drop = ItemManager.createDroppedItem(p.location, true, 6)

        val items = ArrayList<ItemStack>()

        p.inventory.forEach {
            if (it != null) {
                items.add(it)
            }
        }
        p.inventory.armorContents?.forEach {
            if (it != null) {
                items.add(it)
            }
        }
        if (p.inventory.itemInOffHand.type != Material.AIR) {
            items.add(p.inventory.itemInOffHand)
        }

        items.removeIf { it.itemMeta.lore?.contains("${ChatColor.GRAY}Gadget") == true }

        items.shuffle()
        var i = 0
        items.forEach {
            drop.items[i] = it
            i++
        }
        ItemManager.createDisplay(drop)

        p.inventory.clear()

        lastDamager.remove(p)

        if (!dataClass.deadPlayer.contains(p) && dataClass.worldMode[p.world] == "Solo") {
            dataClass.deadPlayer.add(p)

            RankSystem.updateMMR(p, dataClass.playerKill[p.uniqueId]?: 0, dataClass.totalPlayer, p.world.players.filter { it.gameMode == GameMode.SURVIVAL }.size + 1, dataClass.avgMMR)
            RankSystem.updateRank(p, dataClass.playerKill[p.uniqueId]?: 0, dataClass.totalPlayer, p.world.players.filter { it.gameMode == GameMode.SURVIVAL }.size + 1, dataClass.avgMMR)

        }
    }
}

class Events: Listener {
    @EventHandler
    fun slotChange(e: PlayerItemHeldEvent) {
        val item = e.player.inventory.getItem(e.newSlot) ?: return
        if (ItemManager.isRangedWeapon(item.type)) {
            ItemManager.rangedWeaponList().forEach {
                if (e.player.getCooldown(it) < 20) {
                    e.player.setCooldown(it, 20)
                }
            }
        }
    }
    @EventHandler
    fun onDamageByEntity(e: EntityDamageByEntityEvent) {
        if (e.damager is Player && e.entity is Player) {
            val damager = e.damager as Player
            val victim = e.entity as Player

            lastDamager[e.entity as Player] = e.damager as Player
            LastWeaponData(damager.inventory.itemInMainHand, System.currentTimeMillis()+1000*10).set(e.entity as Player)
            if ((victim.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)?: return).amplifier >= 4) {
                damager.sendActionBar("${ChatColor.GRAY}${victim.name}님은 면역상태입니다.")
                e.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.entity is Player) {
            val player = e.entity as Player
            if ((player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)?: return).amplifier >= 4) {
                e.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onPlayerJump(e: PlayerJumpEvent) {
        val player = e.player
        if ((player.getPotionEffect(PotionEffectType.SLOW)?: return).amplifier >= 4) {
            e.isCancelled = true
            player.sendActionBar("${ChatColor.GRAY}구속 효과로 인해 점프가 불가능합니다.")
        }
    }
    @EventHandler
    fun onFirework(e: EntityDamageByEntityEvent) {
        if (e.damager is Firework && e.damager.scoreboardTags.contains("display_firework")) {
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        if (!e.player.world.name.contains("Field-")) return
        e.isCancelled = true
        e.keepInventory = true
        e.drops.clear()
        deathPlayer(e.player)
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        e.player.teleport(lobbyLoc)
        e.player.gameMode = GameMode.SURVIVAL
        e.player.inventory.clear()
        e.player.level = 0
        e.player.exp = 0.0F
        if (playerCoin[e.player.uniqueId] == null) playerCoin[e.player.uniqueId] = 10000

    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        deathPlayer(e.player)
    }

    @EventHandler
    fun onChat(e: PlayerChatEvent) {
        val player = e.player
        val world = player.world
        val msg = e.message
        val team = TeamManager.getTeam(world, player) ?: return
        e.isCancelled = true
        if (msg == "집결") {
            player.performCommand("teamcmd assemble")
        } else if (msg == "아이템") {
            team.players.filter { it.location.distance(player.location) <= 50 }.forEach {
                it.sendMessage(Component.text("§a${player.name}님이 주변에 여유 아이템이 있다고 합니다!"))
            }
        } else {
            team.players.forEach {
                it.sendMessage(Component.text("§a[Team] §e${player.name}: $msg §7(${player.location.distance(it.location).roundToInt()}m)"))
            }
        }
    }

    @EventHandler
    fun useAxe(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isLeftClick) return
        if (e.clickedBlock == null) return
        if (!e.player.inventory.itemInMainHand.type.name.contains("_AXE")) return
        val clickedBlock = e.clickedBlock!!

        val blockLocations = mutableSetOf<Location>()
        blockLocations.add(clickedBlock.location)


        for (i in 0 until 3) {
            val locations = blockLocations.toList()
            locations.forEach {
                for (x in -1..1) {
                    for (y in -1..1) {
                        for (z in -1..1) {
                            if (it.block.type.name.contains("GLASS") || it.block.type.name.contains("IRON_DOOR") || it.block.type.name.contains("_DOOR")) {
                                val newLoc = it.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                                blockLocations.add(newLoc)
                            }
                        }
                    }
                }
            }
        }

        blockLocations.forEach {
            if (it.block.type.name.contains("GLASS")) {
                it.world.spawnParticle(Particle.BLOCK_CRACK, it, 5, 0.5, 0.5, 0.5, 0.0, it.block.blockData)
                it.block.type = Material.AIR
                EffectManager.playSurroundSound(it, Sound.BLOCK_GLASS_BREAK, 1.0F, 1.5F)
            }
            if (it.block.type.name.contains("IRON_DOOR")) {
                it.world.spawnParticle(Particle.BLOCK_CRACK, it, 5, 0.5, 0.5, 0.5, 0.0, it.block.blockData)
                EffectManager.playSurroundSound(it, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 1.0F)
            } else if (it.block.type.name.contains("_DOOR")) {
                it.world.spawnParticle(Particle.BLOCK_CRACK, it, 5, 0.5, 0.5, 0.5, 0.0, it.block.blockData)
                it.block.type = Material.AIR
                EffectManager.playSurroundSound(it, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.5F)
            }
        }
    }

    @EventHandler
    fun onProjectileHit(e: ProjectileHitEvent) {
        val projectile = e.entity
        val shooter = e.entity.shooter ?: return
        val entity = e.hitEntity ?: return
        if (shooter is Player && entity is Player) {
            if (!CustomItemManager.isHittable(shooter, entity)) {
                e.isCancelled = true
                projectile.remove()
                return
            }

            if (entity.isBlocking) {
                CustomItemManager.disablePlayerShield(entity)
                entity.setCooldown(Material.SHIELD, 20 * 8)
                projectile.remove()

            }
            if (entity != shooter) lastDamager[entity] = shooter
        }
    }
    @EventHandler
    fun interactWorldItems(e: PlayerInteractEvent) {
        val clickedBlock = e.clickedBlock ?: return
        if (e.action.isLeftClick) return
        if (!e.player.world.name.contains("Field-")) return
        if (clickedBlock.type == Material.CHEST) {
            val chest = clickedBlock.state as Chest
            if (chest.customName != "${ChatColor.YELLOW}Supplies") {
                e.isCancelled = true
            }
        } else if (clickedBlock.type == Material.TRAPPED_CHEST) {
            e.isCancelled = true
        } else if (clickedBlock.type == Material.ENDER_CHEST) {
            e.isCancelled = true
        } else if (clickedBlock.type.toString().contains("_SHULKER_BOX")) {
            e.isCancelled = true
        } else if (clickedBlock.type == Material.BARREL) {
            e.isCancelled = true
        }
    }

}