package me.uwuaden.kotlinplugin.assets

import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.util.*

object CustomItemData {
    fun getRevelation(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.GOLDEN_SWORD,
            1,
            "${ChatColor.AQUA}${ChatColor.BOLD}Revelation",
            listOf("${ChatColor.GRAY}5초의 쿨타임으로 관통 대미지를 넣습니다!", "${ChatColor.GRAY}킬이 높을수록 대미지가 감소하며, 킬이 3을 넘어가면 능력을 잃습니다.")
        )
        val m = item.itemMeta
        m.isUnbreakable = true
        item.itemMeta = m
        return item
    }
    fun getVallista(): ItemStack {
        return ItemManager.enchantItem(
            ItemManager.createNamedItem(
                Material.CROSSBOW,
                1,
                "${ChatColor.YELLOW}${ChatColor.BOLD}Vallista",
                listOf("${ChatColor.GRAY}관통하는 히트스캔 방식의 화살을 발사합니다!", "${ChatColor.GRAY}관통대미지를 넣습니다.")
            ), Enchantment.QUICK_CHARGE, 1
        )
    }
    fun getEXI(): ItemStack {
        return ItemManager.createNamedItem(
            Material.NETHERITE_HOE,
            1,
            "${ChatColor.AQUA}${ChatColor.BOLD}Prototype E-XI",
            listOf("${ChatColor.DARK_GRAY}Charge Capacity: 400", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[⚙] Tech", "${ChatColor.GRAY}우클릭을 홀드하면 앞에 대상에게 지속적인 대미지를 줍니다!", "${ChatColor.GRAY}대상을 피격 중이면 대미지가 증가하고, 평상시에는 대미지가 감소합니다.", "${ChatColor.GRAY}또한 아이템을 다시 들면 충전량이 초기화 됩니다.", "${ChatColor.GRAY}또한 점프시 쿨타임이 걸립니다.", "${ChatColor.GREEN}초당 대미지: 0.5~5.0", " ", "${ChatColor.DARK_AQUA}Charge: 0", " ", "${ChatColor.GRAY}Gadget")
        )

    }
    fun getPrismShooter(): ItemStack {
        return ItemManager.createNamedItem(Material.IRON_SHOVEL, 1, "${ChatColor.YELLOW}Prism Shooter", listOf("${ChatColor.GRAY}무지개 빛 총 공격을 합니다!", "${ChatColor.GRAY}각 색은 다른 디버프를 부여합니다."))
    }

    fun getExplosiveBow(): ItemStack {
        return ItemManager.createNamedItem(Material.BOW, 1, "${ChatColor.YELLOW}Explosive Bow", listOf("${ChatColor.GRAY}폭발하는 화살을 발사합니다!"))
    }

    fun getFlareGun(): ItemStack {
        return ItemManager.createNamedItem(
            Material.REDSTONE_TORCH,
            1,
            "${ChatColor.RED}Flare Gun",
            listOf(
                "${ChatColor.GRAY}하늘에 발사시",
                "${ChatColor.GRAY}보급품이 떨어집니다!",
                " ",
                "${ChatColor.GRAY}보급품에 깔리지 않게 조심하세요!"
            )
        )
    }
    fun getAntiGravityG(): ItemStack {
        return ItemManager.createNamedItem(
            Material.WARPED_BUTTON,
            1,
            "${ChatColor.YELLOW}반중력 수류탄",
            listOf("${ChatColor.GRAY}폭발시 강력한 반중력장을 형성합니다.", "${ChatColor.GRAY}반중력장은 주변을 강하게 밀쳐냅니다.")
        )
    }

    fun getGravityG(): ItemStack {
        return ItemManager.createNamedItem(Material.CRIMSON_BUTTON, 1, "${ChatColor.YELLOW}중력 수류탄", listOf("${ChatColor.GRAY}폭발시 미니 블랙홀을 생성합니다.", "${ChatColor.GRAY}미니 블랙홀은 주변을 천천히 당깁니다."))
    }

    fun getAGShotGun(): ItemStack {
        return ItemManager.createNamedItem(
            Material.GLOW_INK_SAC,
            1,
            "${ChatColor.YELLOW}반중력 샷건",
            listOf("${ChatColor.GRAY}1회용*", "${ChatColor.GRAY}강한 반동과 함께 적과 자신을 밀쳐냅니다.", "${ChatColor.GRAY}탄환에 맞은 적은 스턴이 적용됩니다.")
        )
    }
    fun getSmokeG(): ItemStack {
        return ItemManager.createNamedItem(
            Material.STONE_BUTTON,
            1,
            "${ChatColor.YELLOW}연막탄",
            listOf("${ChatColor.GRAY}우클릭으로 투척시 주변에 연막을 생성합니다.", "${ChatColor.GRAY}연막은 원형 모양으로, 적의 시야를 차단할 수 있습니다.")
        )
    }
    fun getLiberation(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.STONE_SWORD,
            1,
            "${ChatColor.DARK_PURPLE}${ChatColor.BOLD}Liberation",
            listOf("${ChatColor.DARK_GRAY}Charge Capacity: 800", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[🧨] Chaos", "${ChatColor.GRAY}주변 30블럭에 자신을 포함한 플레이어가 3명 이상일 경우 아래 능력들을 발동시킵니다.", "${ChatColor.GRAY}모든 공격에 2만큼 추가 피해를 줍니다. (2초 쿨타임)", "${ChatColor.GRAY}체력이 최대일때, 아래 능력들을 추가 발동 시킵니다.", "${ChatColor.GRAY}추가피해의 대미지가 2배가 되며, 우클릭 시 앞으로 순간이동하는 능력이 추가됩니다. (2초 쿨타임)", " ", "${ChatColor.GRAY}Gadget")
        )
        val m = item.itemMeta
        m.addEnchant(Enchantment.DAMAGE_ALL, 1, false)
        m.isUnbreakable = true
        item.itemMeta = m
        return item
    }
    fun getTeleportLeggings(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.LEATHER_LEGGINGS,
            1,
            "${ChatColor.AQUA}${ChatColor.BOLD}Teleport Leggings",
            listOf("${ChatColor.DARK_GRAY}Charge Capacity: 500", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[⚙] Tech", "${ChatColor.YELLOW}${ChatColor.BOLD}Shift 키: ${ChatColor.GRAY}보는 방향으로 7~10칸 텔레포트합니다. (0.25초 쿨타임)", "${ChatColor.GRAY}텔레포트 위치에 블럭이 있을 경우: 자신이 40의 피해를 받습니다.", "${ChatColor.GRAY}텔레포트 위치에 엔티티가 있을 경우: 해당 엔티티와 자신이 5만큼 대미지를 받습니다.", "${ChatColor.GRAY}사용하기 어려운 아이템 입니다! 조심하세요!", " ", "${ChatColor.GRAY}Gadget")
        )

        val leatherMeta = item.itemMeta as LeatherArmorMeta
        leatherMeta.isUnbreakable = true
        leatherMeta.setColor(Color.AQUA)
        leatherMeta.removeAttributeModifier(EquipmentSlot.LEGS)
        leatherMeta.addAttributeModifier(Attribute.GENERIC_ARMOR, AttributeModifier(UUID.randomUUID(), "", 5.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS))
        item.itemMeta = leatherMeta
        return item
    }
    fun getStinger(): ItemStack {
        return ItemManager.enchantItem(ItemManager.createNamedItem(Material.BOW, 1, "${ChatColor.AQUA}${ChatColor.BOLD}Stinger", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 200", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[⚙] Tech", "${ChatColor.GRAY}투사체 속도가 더 빠른 활입니다!", " ", "${ChatColor.GRAY}Gadget")), Enchantment.ARROW_DAMAGE, 1)
    }
    fun getDivinityShield(): ItemStack {
        return ItemManager.createNamedItem(
            Material.YELLOW_DYE,
            1,
            "${ChatColor.GOLD}${ChatColor.BOLD}Shield of Divinity",
            listOf("${ChatColor.YELLOW}${ChatColor.BOLD}우클릭: ${ChatColor.GRAY}사용시 10초간 받는 대미지가 100% 감소하고 신속 1이 부여됩니다.", "${ChatColor.GRAY}Gadget")
        )
    }


}