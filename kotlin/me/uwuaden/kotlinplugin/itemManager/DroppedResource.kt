package me.uwuaden.kotlinplugin.itemManager

import org.bukkit.Location
import org.bukkit.Material
import java.util.*

class DroppedResource(var uuid: UUID, var loc: Location, var type: Material = Material.STONE, var maxHealth: Int = 10, var health: Int = 10) {
    fun initHealth(amount: Int) {
        this.maxHealth = amount
        this.health = amount

    }
}