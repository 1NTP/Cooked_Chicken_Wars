package me.uwuaden.kotlinplugin.npc

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.isOnCooldown
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.setCooldown
import me.uwuaden.kotlinplugin.npc.NPCEvent.Companion.aiWorld
import me.uwuaden.kotlinplugin.npc.NPCEvent.Companion.targetData
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.ai.goals.MoveToGoal
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.GameModeTrait
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.projectiles.ProjectileSource
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

private fun swapItemSlot(inv: Inventory, slot1: Int, slot2: Int) {
    val item1 = inv.getItem(slot1)?.clone() ?: return
    val item2 = inv.getItem(slot2)?.clone() ?: return
    inv.setItem(slot2, item1)
    inv.setItem(slot1, item2)
}

private fun heal(entity: LivingEntity, amount: Double) {
    if (entity.health + amount > entity.maxHealth) {
        entity.health = entity.maxHealth
    } else {
        entity.health += amount
    }
}
private fun isTargetable(entity: LivingEntity): Boolean {
    if (entity is Player) {
        if (entity.gameMode == GameMode.SPECTATOR) return false
        if (entity.gameMode == GameMode.CREATIVE) return false
    } else {
        if (entity is ArmorStand) return false
    }
    return true
}

private fun makeLookAt(entity: LivingEntity, lookat: Location): Pair<Float, Float> {
    //Clone the loc to prevent applied changes to the input loc
    val loc = entity.eyeLocation.clone()

    // Values of change in distance (make it relative)
    val dx = lookat.x - loc.x
    val dy = lookat.y - loc.y
    val dz = lookat.z - loc.z

    // Set yaw
    if (dx != 0.0) {
        // Set yaw start value based on dx
        if (dx < 0) {
            loc.yaw = (1.5 * Math.PI).toFloat()
        } else {
            loc.yaw = (0.5 * Math.PI).toFloat()
        }
        loc.yaw = loc.yaw - atan(dz / dx).toFloat()
    } else if (dz < 0) {
        loc.yaw = Math.PI.toFloat()
    }

    // Get the distance from dx/dz
    val dxz = sqrt(dx.pow(2.0) + dz.pow(2.0))

    // Set pitch
    loc.pitch = (-atan(dy / dxz)).toFloat()

    // Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
    loc.yaw = -loc.yaw * 180f / Math.PI.toFloat()
    loc.pitch = loc.pitch * 180f / Math.PI.toFloat()
    return Pair(loc.yaw, loc.pitch)
}
object NPCBehavior {
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            targetData.clear()
            aiWorld.values.toSet().forEach { world ->
                world.livingEntities.forEach {
                    targetData.add(it)
                }
            }
        }, 0, 4)
    }
    fun createAI(location: Location): NPC {
        val npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "AI-Bot")
        npc.spawn(location)
        npc.getOrAddTrait(GameModeTrait::class.java).gameMode = GameMode.SURVIVAL
        npc.isProtected = false
        npc.name = "AI-Bot"
        npc.data().set(NPC.Metadata.PICKUP_ITEMS, true)
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false)
        npc.data().set(NPC.Metadata.SPAWN_NODAMAGE_TICKS, 0)
        aiWorld[npc.uniqueId] = location.world
        return npc
    }

    fun NPC.meleeAttack(target: LivingEntity): Boolean {
        val npc = this
        val entity = npc.entity as HumanEntity

        var itemList = listOf(Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.WOODEN_SWORD)
        if (!itemList.contains(entity.inventory.itemInMainHand.type)) {
            itemList.forEach {
                if (npc.swapItem(it)) {
                    entity.setCooldown("GENERIC_ATTACK", 4)
                    return false
                }
            }
        }

        if (entity.eyeLocation.distance(target.eyeLocation) <= 2.7 && !entity.isOnCooldown("GENERIC_ATTACK")) {
            if (target is Player && target.isBlocking) {
                for (item in listOf(Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE)) {
                    if (npc.swapItem(item)) break
                }
            } else {
                entity.setCooldown("GENERIC_ATTACK", 4)
            }
            npc.navigator.defaultParameters.defaultAttackStrategy().handle(entity, target)
            return true
        }
        return false
    }

    fun NPC.walkToLoc(location: Location) {
        val npc = this
        val entity = npc.entity as HumanEntity
        if (!entity.isOnCooldown("GENERIC_BOW_SHOOT")) entity.setRotation(entity.yaw, 0.0f)
        npc.navigator.cancelNavigation()
        npc.navigator.localParameters.speedModifier(1.18f)
        npc.navigator.setTarget(location)
    }

    fun NPC.runToLoc(location: Location) {
        val npc = this
        val entity = npc.entity as HumanEntity
        if (!entity.isOnCooldown("GENERIC_BOW_SHOOT")) entity.setRotation(entity.yaw, 0.0f)
        npc.navigator.cancelNavigation()
        npc.navigator.localParameters.speedModifier(1.4f)
        npc.navigator.setTarget(location)
    }

    fun NPC.stopMove() {
        val npc = this
        if (npc.navigator.isNavigating) {
            val vel = npc.entity.velocity.clone()
            npc.navigator.cancelNavigation()
            npc.entity.velocity = vel
        }
    }

    fun NPC.eat(safe: Boolean = false) {
        val npc = this
        val npcEntity = this.entity as HumanEntity
        if (npcEntity.isOnCooldown("GENERIC_EATING")) return
        val foodList = mutableListOf(Material.COOKED_BEEF)
        if (!safe) foodList.add(0, Material.GOLDEN_APPLE)
        var itemInUse = ItemStack(Material.AIR)

        npcEntity.setCooldown("GENERIC_EATING", 30)

        if (npcEntity.inventory.contents.clone().filterNotNull().any { foodList.contains(it.type) }) {
            for (item in foodList) {
                if (npc.swapItem(item)) {
                    itemInUse = npcEntity.inventory.filterNotNull().filter { it.type == item }.first()
                    break
                }
            }
            npc.navigator.localParameters.speedModifier(0.6f)
            Main.scheduler.runTaskAsynchronously(plugin, Runnable {
                for (i in 0 until 7) {
                    Main.scheduler.scheduleSyncDelayedTask(plugin, {
                        npcEntity.world.playSound(npcEntity.location, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f)
                        npcEntity.world.spawnParticle(Particle.ITEM_CRACK, npcEntity.eyeLocation.clone().add(0.0, -1.0, 0.0), 10, 0.2, 0.2, 0.2, 0.0, itemInUse, )
                        if (i == 6) {
                            npcEntity.world.playSound(npcEntity.location, Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f)
                            itemInUse.amount -= 1
                            when (itemInUse.type) {
                                Material.COOKED_BEEF -> heal(npcEntity, 3.0)
                                Material.GOLDEN_APPLE -> {
                                    heal(npcEntity, 2.0)
                                    npcEntity.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20*120, 0))
                                    npcEntity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20*5, 1))
                                }
                                else -> {}
                            }
                            npc.navigator.localParameters.speedModifier(1.18f)
                            npcEntity.setCooldown("PVP_EATING", 20*4)
                        }
                    }, 0)
                    Thread.sleep(1000 / 5)
                }
            })
        }
    }
    fun NPC.equipBetterArmor() {
        val helmetPoint = listOf(Material.DIAMOND_HELMET, Material.IRON_HELMET)
        val chestPoint = listOf(Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE)
        val leggingsPoint = listOf(Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS)
        val bootsPoint = listOf(Material.DIAMOND_BOOTS, Material.IRON_BOOTS)

        for (item in helmetPoint) if (equip(item, EquipmentSlot.HEAD)) break
        for (item in chestPoint) if (equip(item, EquipmentSlot.CHEST)) break
        for (item in leggingsPoint) if (equip(item, EquipmentSlot.LEGS)) break
        for (item in bootsPoint) if (equip(item, EquipmentSlot.FEET)) break

    }

    fun NPC.attackWithBreaking(target: LivingEntity) {
        val npc = this
        if (meleeAttack(target)) {
            if (npc.entity.isOnGround) {
                scheduler.scheduleSyncDelayedTask(plugin, {
                    npc.stopMove()
                }, 2)
            }

            npc.entity.setCooldown("ATTACK_BREAKING", 3)
        }
        if (!npc.navigator.isNavigating && npc.entity.isOnGround && !npc.entity.isOnCooldown("ATTACK_BREAKING")) {
            npc.runToLoc(target.location)
        }
    }
    fun NPC.idle() {
        val npc = this
        if (npc.entity != null) {
            val npcEntity = npc.entity as HumanEntity
            npc.entity.setCooldown("GENERIC_IDLE", Int.MAX_VALUE)

            scheduler.runTaskAsynchronously(plugin, Runnable {
                while (npc.entity != null && npc.entity.isOnCooldown("GENERIC_IDLE")) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        if (npc.entity != null && npc.entity.location.isChunkLoaded) {
                            val entity = npc.entity as LivingEntity

                            npc.equipBetterArmor()

                            val tps = plugin.server.tps.first()
                            val range = when (tps) {
                                in 18.0..20.0 -> 50.0
                                in 16.0..18.0 -> 40.0
                                in 14.0..16.0 -> 30.0
                                in 10.0..14.0 -> 15.0
                                in 6.0..10.0 -> 10.0
                                in 0.0..6.0 -> 5.0
                                else -> 50.0
                            }
                            val target = targetData.asSequence().filter { it.world == npcEntity.world }.filter { it.location.distance(npcEntity.location) <= range }
                                .filter { isTargetable(it) }
                                .filter { it != npcEntity }
                                .filter {
                                    !it.isDead && npcEntity.hasLineOfSight(it.eyeLocation) || it.isOnCooldown(
                                        "GENERIC_NPC_FOUND"
                                    )
                                }
                                .sortedBy { it.location.distance(npc.entity.location) }.firstOrNull()
                            if (target == null) {
                                if (entity.health <= entity.maxHealth * 0.8) {
                                    npc.eat(true)
                                }
                            } else {
                                target.setCooldown("GENERIC_NPC_FOUND", 20*30)
                                if (!npcEntity.isOnCooldown("GENERIC_EATING")) {
                                    val dist = target.location.distance(npc.entity.location)
                                    if (!npc.entity.isOnCooldown("GENERIC_CHASE") && npc.entity.isOnGround) {
                                        npc.entity.setCooldown("GENERIC_CHASE", 20 * 2)
                                        npc.runToLoc(target.location)
                                    }
                                    if (entity.health <= entity.maxHealth * 0.5 && !entity.isOnCooldown("PVP_EATING")) {
                                        npc.eat()
                                    } else if (dist > 30.0 && npcEntity.inventory.contains(Material.CROSSBOW) && npcEntity.inventory.contains(
                                            Material.ARROW
                                        ) && npcEntity.hasLineOfSight(target.eyeLocation)
                                    ) {
                                        if (npcEntity.isOnGround && npc.entity.isOnCooldown("GENERIC_BOW_SHOOT")) {
                                            npc.runToLoc(target.location)
                                        }
                                        npc.shootCrossbow(target)
                                    } else if (dist in 5.0..30.0 && npcEntity.inventory.contains(Material.BOW) && npcEntity.inventory.contains(
                                            Material.ARROW
                                        ) && npcEntity.hasLineOfSight(target.eyeLocation)
                                    ) {
                                        if (npcEntity.isOnGround && npc.entity.isOnCooldown("GENERIC_BOW_SHOOT")) {
                                            npc.runToLoc(target.location)
                                        }
                                        npc.shootBow(target)
                                    } else {
                                        npc.attackWithBreaking(target)
                                    }
                                }
                            }
                        }
                    }, 0)
                    Thread.sleep(1000 / 10)
                }
            })
        }
    }

    fun NPC.swapItem(type: Material, slot: Int = 0): Boolean {
        val npc = this.entity as HumanEntity
        npc.inventory.heldItemSlot = slot
        if (!npc.inventory.contains(type)) return false

        npc.inventory.contents.withIndex().filter { it.value?.type == type }.forEach {
            swapItemSlot(npc.inventory, slot, it.index)
            return true
        }
        return false
    }

    fun NPC.equip(type: Material, slot: EquipmentSlot): Boolean {
        val npc = this
        val npcEntity = npc.entity as HumanEntity

        val item = npcEntity.inventory.storageContents.filterNotNull().filter { it.type == type }.maxByOrNull { it.enchantments.size } ?: return false
        val dropItem = npcEntity.equipment.getItem(slot).clone()

        val helmetPoint = hashMapOf(Material.DIAMOND_HELMET to 2, Material.IRON_HELMET to 1)
        val chestPoint = hashMapOf(Material.DIAMOND_CHESTPLATE to 2, Material.IRON_CHESTPLATE to 1)
        val leggingsPoint = hashMapOf(Material.DIAMOND_LEGGINGS to 2, Material.IRON_LEGGINGS to 1)
        val bootsPoint = hashMapOf(Material.DIAMOND_BOOTS to 2, Material.IRON_BOOTS to 1)

        when (slot) {
            EquipmentSlot.HEAD -> { if (item.enchantments.size + (helmetPoint[item.type] ?: 0) <= dropItem.enchantments.size + (helmetPoint[dropItem.type] ?: 0)) return false }
            EquipmentSlot.CHEST -> { if (item.enchantments.size + (chestPoint[item.type] ?: 0) <= dropItem.enchantments.size + (chestPoint[dropItem.type] ?: 0)) return false }
            EquipmentSlot.LEGS -> { if (item.enchantments.size + (leggingsPoint[item.type] ?: 0) <= dropItem.enchantments.size + (leggingsPoint[dropItem.type] ?: 0)) return false }
            EquipmentSlot.FEET -> { if (item.enchantments.size + (bootsPoint[item.type] ?: 0) <= dropItem.enchantments.size + (bootsPoint[dropItem.type] ?: 0)) return false }
            else -> return false
        }

        npcEntity.inventory.remove(item)

        npcEntity.equipment.setItem(slot, ItemStack(Material.AIR))
        npcEntity.equipment.setItem(slot, item)

        npcEntity.world.dropItem(npcEntity.eyeLocation, dropItem)
        return true
    }
    fun NPC.test(location: Location) {
        val npc = this
        npc.defaultGoalController.addGoal(MoveToGoal(npc, location), 1)
    }
    fun NPC.shootBow(target: LivingEntity): Boolean {
        val npc = this
        val npcEntity = this.entity as HumanEntity
        if (npc.entity.isOnCooldown("GENERIC_BOW_SHOOT")) return false

        if (!npcEntity.inventory.contains(Material.ARROW)) return false

        if (npcEntity.inventory.itemInMainHand.type == Material.BOW) {
            npc.entity.setCooldown("GENERIC_BOW_SHOOT", 30)
            npc.navigator.localParameters.speedModifier(0.6f)
            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (i in 0 until 10) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        val dir = makeLookAt(npcEntity, target.eyeLocation)
                        npcEntity.setRotation(dir.first, dir.second)
                    }, 0)
                    Thread.sleep(1000/10)
                }
                scheduler.scheduleSyncDelayedTask(plugin, {
                    val dir = makeLookAt(npcEntity, target.eyeLocation)
                    npcEntity.setRotation(dir.first, dir.second)

                    val arrow = npcEntity.inventory.contents.filterNotNull().filter { it.type == Material.ARROW }.firstOrNull()
                    if (arrow != null) {
                        arrow.amount -= 1
                    }

                    val projectile = npcEntity.launchProjectile(Arrow::class.java)
                    npcEntity.world.playSound(npcEntity.location, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f)

                    entity?.setCooldown("GENERIC_ATTACK", 12)
                    val item = npcEntity.inventory.itemInMainHand
                    if (item.itemMeta.hasEnchant(Enchantment.ARROW_DAMAGE)) {
                        projectile.damage*=(1.25 + item.itemMeta.getEnchantLevel(Enchantment.ARROW_DAMAGE)*0.25)
                    }
                    projectile.shooter = entity as ProjectileSource
                    projectile.isCritical = true
                    npc.navigator.localParameters.speedModifier(1.4f)
                }, 0)
            })
            return true
        } else {
            npc.swapItem(Material.BOW)
            npc.entity.setCooldown("GENERIC_BOW_SHOOT", 20)
            return false
        }
    }
    fun NPC.shootCrossbow(target: LivingEntity): Boolean {
        val npc = this
        val npcEntity = this.entity as HumanEntity
        if (npc.entity.isOnCooldown("GENERIC_BOW_SHOOT")) return false

        if (!npcEntity.inventory.contains(Material.ARROW)) return false

        if (npcEntity.inventory.itemInMainHand.type == Material.CROSSBOW) {
            npc.entity.setCooldown("GENERIC_BOW_SHOOT", 80)
            npc.navigator.localParameters.speedModifier(0.6f)
            val soundList = listOf(Sound.ITEM_CROSSBOW_LOADING_START, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, Sound.ITEM_CROSSBOW_LOADING_END)
            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (i in 0 until 4) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        val dir = makeLookAt(npcEntity, target.eyeLocation)
                        npcEntity.setRotation(dir.first, dir.second)
                        if (i > 0) {
                            npcEntity.world.playSound(npcEntity, soundList[i-1], 1.0f, 1.0f)
                        }
                    }, 0)
                    Thread.sleep(1000/2)
                }
                scheduler.scheduleSyncDelayedTask(plugin, {
                    val dir = makeLookAt(npcEntity, target.eyeLocation)
                    npcEntity.setRotation(dir.first, dir.second)

                    npcEntity.inventory.contents.filterNotNull().filter { it.type == Material.ARROW }.first().amount -= 1

                    val projectile = npcEntity.launchProjectile(Arrow::class.java)
                    npcEntity.world.playSound(npcEntity.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f)

                    entity.setCooldown("GENERIC_ATTACK", 12)
                    projectile.isCritical = true
                    projectile.shooter = entity as ProjectileSource
                    npc.navigator.localParameters.speedModifier(1.4f)
                }, 0)
            })
            return true
        } else {
            npc.swapItem(Material.CROSSBOW)
            npc.entity.setCooldown("GENERIC_BOW_SHOOT", 20)
            return false
        }
    }
    fun NPC.stopIdle() {
        val npc = this
        npc.entity.setCooldown("GENERIC_IDLE", 0)
    }
    fun NPC.eliminateEntity(target: LivingEntity) {
        val npc = this
        val npcEntity = npc.entity as HumanEntity
        if (!npc.entity.isOnCooldown("GENERIC_ELIMINATE")) {
            npc.entity.setCooldown("GENERIC_ELIMINATE", Int.MAX_VALUE)
            scheduler.runTaskAsynchronously(plugin, Runnable {
                while (!target.isDead && npc.isSpawned) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        val dist = target.location.distance(npc.entity.location)
                        if (npc.entity != null) {
                            val entity = npc.entity as LivingEntity
                            if (entity.health <= entity.maxHealth * 0.3 && entity is HumanEntity) {
                                npc.eat()
                            } else {
                                if (dist in 10.0..30.0 && npcEntity.inventory.contains(Material.BOW) && npcEntity.inventory.contains(Material.ARROW) && npcEntity.hasLineOfSight(target.eyeLocation)) {
                                    npc.shootBow(target)
                                } else {
                                    npc.attackWithBreaking(target)
                                }
                            }
                        }
                    }, 0)
                    Thread.sleep(1000 / 10)
                }
            })
            this.meleeAttack(target)
        }
    }
}