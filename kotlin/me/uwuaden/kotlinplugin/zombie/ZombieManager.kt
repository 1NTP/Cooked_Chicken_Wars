package me.uwuaden.kotlinplugin.zombie

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.isOnCooldown
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.setCooldown
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.roundToInt

private fun getColoredItem(type: Material, color: Color): ItemStack {
    val targetItem = ItemStack(type)
    val meta = targetItem.itemMeta as LeatherArmorMeta
    meta.setColor(color)
    targetItem.itemMeta = meta
    return targetItem
}
private fun setHelmet(entity: LivingEntity, item: ItemStack) {
    entity.equipment?.setHelmet(item, true)
}
private fun setChestplate(entity: LivingEntity, item: ItemStack) {
    entity.equipment?.setChestplate(item, true)
}
private fun setLeggings(entity: LivingEntity, item: ItemStack) {
    entity.equipment?.setLeggings(item, true)
}
private fun setBoots(entity: LivingEntity, item: ItemStack) {
    entity.equipment?.setBoots(item, true)
}
object ZombieManager {
    //몹 리스트: normal, speed, heavy, boss1, shadow, lava, void, boss2
    fun getMobList(wave: Int): MutableList<String> {
        return when (wave) {
            1 -> mutableListOf("normal:10")
            2 -> mutableListOf("normal:15")
            3 -> mutableListOf("normal:20")
            4 -> mutableListOf("normal:15", "speed: 10")
            5 -> mutableListOf("normal:15", "speed: 10")
            6 -> mutableListOf("normal:20", "speed: 20")
            7 -> mutableListOf("speed:40")
            8 -> mutableListOf("normal:10", "speed:20", "heavy:5")
            9 -> mutableListOf("speed:10", "heavy:10")
            10 -> mutableListOf("speed:20", "heavy:20")
            11 -> mutableListOf("heavy:40")
            12 -> mutableListOf("speed:10", "heavy:10", "boss1:1")
            13 -> mutableListOf("heavy:10", "shadow:5")
            14 -> mutableListOf("speed:10", "shadow:10")
            15 -> mutableListOf("lava:5", "shadow:15")
            16 -> mutableListOf("lava:20", "shadow:10", "boss1:2")
            17 -> mutableListOf("lava:15", "shadow:10", "void:5")
            18 -> mutableListOf("lava:10", "shadow:15", "void:10")
            19 -> mutableListOf("lava:20", "shadow:20", "void:10")
            20 -> mutableListOf("lava:10", "shadow:10", "void:5", "boss2:1")
            21 -> mutableListOf("lava:10", "shadow:5", "void:5", "explosion:5", "boss2:2")
            22 -> mutableListOf("lava:5", "shadow:5", "void:5", "explosion:10", "boss2:1")
            23 -> mutableListOf("lava:10", "shadow:10", "void:10", "explosion:10", "boss2:2")
            24 -> mutableListOf("lava:5", "shadow:5", "void:10", "explosion:5", "boss2:4")
            25 -> mutableListOf("lava:5", "shadow:5", "void:5", "explosion:20", "boss2:1")
            26 -> mutableListOf("lava:5", "flash:5", "void:5", "explosion:10", "boss2:2")
            27 -> mutableListOf("lava:10", "flash:10", "void:5", "explosion:10", "boss2:1")
            28 -> mutableListOf("flash:20", "void:5", "explosion:5", "boss2:2")
            29 -> mutableListOf("lava:5", "flash:15", "void:10", "explosion:10", "boss2:1")
            30 -> mutableListOf("flash:30", "void:5", "explosion:5", "boss2:1")
            31 -> mutableListOf("flash:10", "explosion:10", "boss2:2", "elect:5")
            32 -> mutableListOf("flash:5", "explosion:5", "boss2:2", "elect:10")
            33 -> mutableListOf("flash:5", "explosion:10", "boss2:1", "elect:15")
            34 -> mutableListOf("flash:10", "explosion:10", "boss2:1", "elect:15")
            35 -> mutableListOf("flash:10", "explosion:10", "boss2:2", "elect:20")
            else -> mutableListOf()
        }
    }
    fun convertMobList(pls: Int, list: MutableList<String>): MutableList<String> {
        var countM = pls/4
        if (countM < 1) countM = 1
        if (countM > 4) countM = 4
        val convertedList = mutableListOf<String>()
        val convertedList2 = mutableListOf<String>()
        list.forEach { str ->
            if(str.contains(":")) {
                val mobName = str.split(":")[0].trim()
                val n = str.split(":")[1].trim().toInt()
                for (i in 0 until n) {
                    convertedList.add(mobName)
                }
            } else {
                convertedList.add(str.trim())
            }
        }
        convertedList.forEach {
            for (i in 0 until countM) {
                convertedList2.add(it)
            }
        }
        return convertedList2
    }
    fun zombieSkillSch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { world ->
                world.livingEntities.filter { it.scoreboardTags.contains("Spawned-Zombie") }.forEach { entity ->
                    entity as Zombie
                    if (null == entity.target) {
                        val players = entity.location.world.players.filter { it.location.distance(entity.location) <= 320.0 }.filter { p -> p.gameMode != GameMode.SPECTATOR }.sortedBy { p -> entity.location.distance(p.location) }
                        if (players.isNotEmpty()) entity.target = players[0]
                    }
                    if(WorldManager.isOutsideBorder(entity.location)) {
                        entity.damage(0.5)
                    }

                    if (entity.customName == "§0Shadow" && entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        val players = entity.location.getNearbyPlayers(10.0).filter { it.gameMode == GameMode.SURVIVAL }.toList()
                        if (players.isNotEmpty()) {
                            val target = players[0]
                            entity.teleport(target.location)
                            EffectManager.playSurroundSound(entity.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                            entity.world.spawnParticle(Particle.REVERSE_PORTAL, entity.location.clone().add(0.0, 1.0, 0.0), 20, 1.0, 1.0, 1.0, 0.0)
                            entity.world.spawnParticle(Particle.SMOKE_NORMAL, entity.location.clone().add(0.0, 1.0, 0.0), 5, 1.0, 1.0, 1.0, 0.0)
                            entity.removePotionEffect(PotionEffectType.INVISIBILITY)
                            entity.equipment?.setItemInMainHand(ItemStack(Material.IRON_SWORD))
                            entity.isSilent = false
                            target.damage(1.0)
                        }
                    } else if (entity.customName == "§c§lLava" && entity.health <= entity.maxHealth*0.2 && entity.hasAI()) {
                        entity.setAI(false)
                        entity.isSilent = true
                        entity.customName = "§8§lLava"
                        entity.location.world.spawnParticle(Particle.SMOKE_NORMAL, entity.eyeLocation, 200, 0.25, 1.0, 0.25, 0.0)
                        EffectManager.playSurroundSound(entity.location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0f)
                        setHelmet(entity, getColoredItem(Material.LEATHER_HELMET, Color.fromRGB(85, 85, 85)))
                        setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(68, 68, 68)))
                        setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(54, 54, 54)))
                        setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(43, 43, 43)))
                    } else if (entity.customName == "§0Void" && entity.isSilent) {
                        val players = entity.location.getNearbyPlayers(20.0).filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.isNotEmpty()) {
                            val target = players[0]
                            entity.isSilent = false
                            target.location.world.spawnParticle(Particle.REDSTONE, target.location, 400, 0.0, 100.0, 0.0, DustOptions(Color.BLACK, 1.0f))
                            target.teleport(entity.location)
                            EffectManager.playSurroundSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0f)
                            EffectManager.playSurroundSound(target.location, Sound.ENTITY_ALLAY_DEATH, 1.0F, 0.5f)
                            target.location.world.spawnParticle(Particle.REDSTONE, target.location, 400, 0.0, 100.0, 0.0, DustOptions(Color.BLACK, 1.0f))
                            target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20*3, 0, false, false))
                            target.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, 20*3, 0, false, false))
                        }
                    } else if (entity.customName == "§2Boss Zombie 2") {
                        val score = entity.scoreboardTags.filter { it.contains("Zombie-SkillCD:") }
                        if (score.isNotEmpty()) {
                            val milli = score[0]!!.split(":")[1].trim().toLong()
                            if (System.currentTimeMillis() >= milli) {
                                entity.scoreboardTags.removeIf { it.contains("Zombie-SkillCD:") }
                                entity.scoreboardTags.add("Zombie-SkillCD:${System.currentTimeMillis()+30*1000}")
                                val targetPlayers = entity.location.getNearbyPlayers(8.0).filter { it.gameMode == GameMode.SURVIVAL }
                                targetPlayers.forEach {
                                    val locClone = entity.location.clone()
                                    locClone.y = it.y
                                    val direction = it.location.toVector().subtract(locClone.toVector()).normalize()
                                    it.velocity = direction.multiply(-1.8)
                                    it.damage(5.0)
                                }

                                if (targetPlayers.isNotEmpty()) {
                                    val loc = entity.location.clone()
                                    EffectManager.playSurroundSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
                                    for (x in -5..5) {
                                        for (y in -5..5) {
                                            for (z in -5..5) {
                                                val cloneLoc = loc.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                                                if (cloneLoc.y.roundToInt() > Main.groundY) {
                                                    cloneLoc.world.spawnParticle(Particle.BLOCK_CRACK, cloneLoc, 5, 0.5, 0.5, 0.5, 0.0, cloneLoc.block.blockData)
                                                    cloneLoc.block.type = Material.AIR
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (entity.customName == "§bElectro Zombie") {
                        EffectManager.drawParticleCircle(entity.location.clone().add(0.0, 0.2, 0.0), 4.0, Color.AQUA)
                        entity.location.getNearbyPlayers(4.0).forEach {
                            it.damage(2.0)
                            CustomItemManager.drawLine(entity.eyeLocation, it.eyeLocation, 0.5, 0, 0, 255)
                        }
                    } else if (entity.customName == "§5Summoner") {
                        if (!entity.isOnCooldown("SUMMONER_SPAWN")) {
                            entity.setCooldown("SUMMONER_SPAWN", 20*30)
                            EffectManager.playSurroundSound(entity.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f)
                            EffectManager.playSurroundSound(entity.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
                            entity.world.spawnParticle(Particle.SPELL_WITCH, entity.location, 50, 2.0, 2.0, 2.0)

                            val spawned = entity.world.spawnEntity(entity.location, EntityType.SKELETON) as LivingEntity
                            entity.customName = "§fSkeleton"
                            entity.maxHealth = 40.0
                            entity.health = entity.maxHealth

                            entity.canPickupItems = false
                            entity.isPersistent = true
                            entity.removeWhenFarAway = false

                            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*2.0
                            entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                            entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                            entity.equipment.setItemInMainHand(ItemStack(Material.IRON_SWORD))

                        }
                    } else if (entity.customName == "§bBoss Zombie 3") {
                        if (!entity.isOnCooldown("BOSS3_ATTACK")) {
                            entity.setCooldown("BOSS3_ATTACK", 15 * 20)
                            val loc = entity.location.getNearbyPlayers(10.0).filter { it.gameMode == GameMode.SURVIVAL }.randomOrNull()?.location
                            if (loc != null) {
                                loc.add(0.0, 300.0, 0.0)
                                val armorStand = loc.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                                armorStand.isInvisible = true
                                armorStand.isInvulnerable = true
                                armorStand.isSmall = true

                                val display = loc.world.spawnEntity(loc, EntityType.ITEM_DISPLAY) as ItemDisplay
                                display.itemStack = ItemStack(Material.DIAMOND_SWORD)
                                val transform = display.transformation
                                transform.scale.set(10.0)
                                transform.rightRotation.set(0.0f, 0.0f, 0.7f, 0.0f)
                                display.transformation = transform

                                armorStand.addPassenger(display)
                            }
                        }
                    }
                }
            }
        }, 0, 10)
    }
    fun spawnZombie(name: String, loc: Location): Entity {
        val world = loc.world
        val playerCount = world.players.filter { it.gameMode == GameMode.SURVIVAL }.size
        when (name) {
            "normal" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§aNormal Zombie"
                entity.maxHealth = 10.0
                entity.health = entity.maxHealth

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "speed" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§bSpeed Zombie"
                entity.maxHealth = 8.0
                entity.health = entity.maxHealth

                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(85, 154, 185)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(72, 122, 144)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(63, 95, 109)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.4
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "heavy" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§8Heavy Zombie"
                entity.maxHealth = 20.0
                entity.health = entity.maxHealth

                setHelmet(entity, getColoredItem(Material.LEATHER_HELMET, Color.fromRGB(85, 85, 85)))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(170, 170, 170)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(170, 170, 170)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(85, 85, 85)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*0.8
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "boss1" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§2Boss Zombie 1"
                entity.maxHealth = 40.0
                entity.health = entity.maxHealth

                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(0, 170, 0)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(14, 122, 14)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(20, 89, 20)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.0
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "shadow" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§0Shadow"
                entity.maxHealth = 15.0
                entity.health = entity.maxHealth

                entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20*1000000, 0, false, false))

                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(0, 0, 0)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.6
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isSilent = true
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "lava"-> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§c§lLava"
                entity.maxHealth = 40.0
                entity.health = entity.maxHealth

                setHelmet(entity, getColoredItem(Material.LEATHER_HELMET, Color.fromRGB(207, 16, 32)))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(150, 28, 38)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(110, 32, 39)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(82, 32, 36)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.4
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "void"-> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§0Void"
                entity.maxHealth = 40.0
                entity.health = entity.maxHealth
                setHelmet(entity, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTNlMGJmODA3YmM2ZGQ2N2JkMzNmMmViNTFmOWIyNzBlYjMxYThhYjJlOGJmMjUzODU0MjM2YzIzOGM0MGJhNyJ9fX0="))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(0, 0, 0)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(0, 0, 0)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(0, 0, 0)))

                entity.isSilent = true
                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.2
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "boss2"-> {
                val entity = world.spawnEntity(loc, EntityType.HUSK, false) as Husk
                entity.customName = "§2Boss Zombie 2"
                entity.maxHealth = 100.0
                entity.health = entity.maxHealth

                setHelmet(entity, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzNjMGQzYTJlMzA4OGFmZjZmYWNiMTA3ZDU1ZDg0Y2EyN2YyZTcwMzU0NDJiMjMzN2QxMjFmNGJhMzNhOGM5MyJ9fX0="))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(60, 46, 22)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(45, 36, 21)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(34, 28, 19)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.0
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")
                entity.scoreboardTags.add("Zombie-SkillCD:${System.currentTimeMillis()+1000*30}")

                return entity
            }
            "explosion" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§cExplosion"
                entity.maxHealth = 60.0
                entity.health = entity.maxHealth

                setHelmet(entity, ItemStack(Material.TNT))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(255, 0, 0)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(184, 20, 20)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(134, 29, 29)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.4
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "flash" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§eFlash"
                entity.maxHealth = 20.0
                entity.health = entity.maxHealth


                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*3.0
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20*1000000, 0, false, false))
                entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20*1000000, 0, false, false))

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "elect" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§bElectro Zombie"
                entity.maxHealth = 80.0
                entity.health = entity.maxHealth

                setHelmet(entity, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTI4ZjFlZDFiOWNhNzZhYzcxNGYxODEzODNhNzVlNTQ0ZGRkYmVkNmZkOGUwMTJiOTU0OTI0NTI0ODBmNyJ9fX0="))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(85, 154, 185)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(72, 122, 144)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(0, 0, 0)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.5
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "summoner" -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§5Summoner"
                entity.maxHealth = 200.0
                entity.health = entity.maxHealth

                setHelmet(entity, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzViMzc2YTdlYTcwM2Q0ODNiZTI3ODI5ZjU4OWU0MjVlMzUxYzlmYmFmMDViZTE2MGRjYmMwZWQ0ZTczYzI0NiJ9fX0="))
                setChestplate(entity, getColoredItem(Material.LEATHER_CHESTPLATE, Color.fromRGB(68, 32, 99)))
                setLeggings(entity, getColoredItem(Material.LEATHER_LEGGINGS, Color.fromRGB(54, 31, 74)))
                setBoots(entity, getColoredItem(Material.LEATHER_BOOTS, Color.fromRGB(43, 28, 56)))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.0
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.setCooldown("SUMMONER_SPAWN", 20*10)

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            "boss3" -> {
                val entity = world.spawnEntity(loc, EntityType.HUSK, false) as Husk
                entity.customName = "§bBoss Zombie 3"
                entity.maxHealth = 300.0
                entity.health = entity.maxHealth

                setChestplate(entity, ItemStack(Material.DIAMOND_CHESTPLATE))
                entity.equipment.setItemInMainHand(ItemStack(Material.DIAMOND_SWORD))

                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.23*1.0
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1000.0
                entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = 0.0
                entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.baseValue = 0.0


                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.setCooldown("BOSS3_ATTACK", 15*20)

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
            else -> {
                val entity = world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                entity.customName = "§cERROR"
                entity.maxHealth = 1000.0
                entity.health = entity.maxHealth

                entity.canPickupItems = false
                entity.isPersistent = true
                entity.removeWhenFarAway = false

                entity.scoreboardTags.add("Spawned-Zombie")

                return entity
            }
        }
    }
}