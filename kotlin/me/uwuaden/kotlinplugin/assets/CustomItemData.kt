package me.uwuaden.kotlinplugin.assets

import me.uwuaden.kotlinplugin.assets.ItemManipulator.addCustomModelData
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addEnchant
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

object CustomItemData {
    fun getRevelation(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.GOLDEN_SWORD,
            1,
            "§b§lRevelation",
            listOf("§7적을 공격시 5초의 쿨타임으로 적에게 관통 대미지를 넣습니다!", "§7킬이 높을수록 대미지가 감소하며, 킬이 3을 넘어가면 능력을 잃습니다.")
        ).addCustomModelData(10002)
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
                "§e§lVallista",
                listOf("§7관통하는 히트스캔 방식의 화살을 발사합니다!", "§7관통대미지를 넣습니다.")
            ), Enchantment.QUICK_CHARGE, 1
        )
    }
    fun getEXI(): ItemStack {
        return ItemManager.createNamedItem(
            Material.NETHERITE_HOE,
            1,
            "§b§lPrototype E-XI",
            listOf("§8Charge Capacity: 400", "§8Max Use: 1", "§8[⚙] Tech", "§7우클릭을 홀드하면 앞에 대상에게 지속적인 대미지를 줍니다!", "§7대상을 피격 중이면 대미지가 증가하고, 평상시에는 대미지가 감소합니다.", "§7또한 아이템을 다시 들면 충전량이 초기화 됩니다.", "§7또한 점프시 쿨타임이 걸립니다.", "§2초당 대미지: 0.5~5.0", " ", "§3Charge: 0", " ", "§7Gadget")
        )

    }
    fun getPrismShooter(): ItemStack {
        return ItemManager.createNamedItem(Material.IRON_SHOVEL, 1, "§ePrism Shooter", listOf("§7무지개 빛 총 공격을 합니다!", "§7각 색은 다른 디버프를 부여합니다."))
    }

    fun getExplosiveBow(): ItemStack {
        return ItemManager.createNamedItem(Material.BOW, 1, "§eExplosive Bow", listOf("§7폭발하는 화살을 발사합니다!")).addCustomModelData(1)
    }

    fun getFlareGun(): ItemStack {
        return ItemManager.createNamedItem(
            Material.REDSTONE_TORCH,
            1,
            "§cFlare Gun",
            listOf(
                "§7하늘에 발사시",
                "§7보급품이 떨어집니다!",
                " ",
                "§7보급품에 깔리지 않게 조심하세요!"
            )
        ).addCustomModelData(10000)
    }
    fun getAntiGravityG(): ItemStack {
        return ItemManager.createNamedItem(
            Material.WARPED_BUTTON,
            1,
            "§e반중력 수류탄",
            listOf("§7폭발시 강력한 반중력장을 형성합니다.", "§7반중력장은 주변을 강하게 밀쳐냅니다.")
        )
    }

    fun getGravityG(): ItemStack {
        return ItemManager.createNamedItem(Material.CRIMSON_BUTTON, 1, "§e중력 수류탄", listOf("§71회용*", "§7우클릭으로 투척할 수 있습니다.", "§7폭발시 미니 블랙홀을 생성합니다.", "§7미니 블랙홀은 주변 플레이어를 천천히 당깁니다.", "§7블랙홀이 사라질때 약한 대미지를 줍니다."," ","§2대미지: 2.0")).addCustomModelData(10008)
    }

    fun getAGShotGun(): ItemStack {
        return ItemManager.createNamedItem(
            Material.GLOW_INK_SAC,
            1,
            "§e반중력 샷건",
            listOf("§71회용*", "§7우클릭시 탄환을 발사하며, 강한 반동과 함께 적과 자신을 밀쳐냅니다.", "§7탄환에 맞은 적은 스턴이 적용됩니다."," ", "§2대미지: 0.25 x 12")
        ).addCustomModelData(10011)
    }
    fun getSmokeG(): ItemStack {
        return ItemManager.createNamedItem(
            Material.STONE_BUTTON,
            1,
            "§e연막탄",
            listOf("§71회용*", "§7우클릭으로 투척시 주변에 연막을 생성합니다.", "§7연막은 원형 모양으로, 적의 시야를 차단할 수 있습니다.")
        ).addCustomModelData(10009)
    }
    fun getLiberationOrigin(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.STONE_SWORD,
            1,
            "§5§lLiberation",
            listOf("§8Charge Capacity: 800", "§8Max Use: 1", "§8[🧨] Chaos", "§7주변 30블럭에 자신을 포함한 플레이어가 3명 이상일 경우 아래 능력들을 발동시킵니다.", "§7모든 공격에 2만큼 추가 피해를 줍니다. (2초 쿨타임)", "§7체력이 최대일때, 아래 능력들을 추가 발동 시킵니다.", "§7추가피해의 대미지가 2배가 되며, 우클릭 시 앞으로 순간이동하는 능력이 추가됩니다. (2초 쿨타임)", " ", "§7Gadget")
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
            "§b§lTeleport Leggings",
            listOf("§8Charge Capacity: 500", "§8Max Use: 1", "§8[⚙] Tech", "§l§eShift 키: §7보는 방향으로 7~10칸 텔레포트합니다. (0.25초 쿨타임)", "§7텔레포트 위치에 블럭이 있을 경우: 자신이 40의 피해를 받습니다.", "§7텔레포트 위치에 엔티티가 있을 경우: 해당 엔티티와 자신이 5만큼 대미지를 받습니다.", "§7사용하기 어려운 아이템 입니다! 조심하세요!", " ", "§7Gadget")
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
        return ItemManager.enchantItem(ItemManager.createNamedItem(Material.BOW, 1, "§b§lStinger", listOf("§8Charge Capacity: 200", "§8Max Use: 1", "§8[⚙] Tech", "§7투사체 속도가 더 빠른 활입니다!", " ", "§7§oA Technical Mechanized Bow", " ", "§7Gadget")), Enchantment.ARROW_DAMAGE, 1).addCustomModelData(3)
    }
    fun getDivinityShield(): ItemStack {
        return ItemManager.createNamedItem(
            Material.YELLOW_DYE,
            1,
            "§6§lShield of Divinity",
            listOf("§e§l우클릭: §7사용시 10초간 받는 대미지가 100% 감소하고 구속 1이 부여됩니다.", "§7Gadget")
        )
    }
    fun getEnergyDrink(): ItemStack {
        val item = ItemManager.createNamedItem(Material.POTION, 1, "§b§lPOWER INIZER", listOf("§7마실 수 있는 에너지 드링크입니다!", "§7마시면 30초간 신속, 대미지 증가, 성급함 등의 효과를 얻습니다!", "§8THE NEW ENERGY DRINK", "§8 ", "§8  *NO SUGAR", "§8  *NO ADDICTION", "§8  *NO BALANCE")).addCustomModelData(10004)
        val meta = item.itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(PotionEffectType.SPEED, 20*30, 0, false, true), true)
        meta.addCustomEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 20*30, 0, false, true), true)
        meta.addCustomEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20*30, 0, false, true), true)
        meta.color = Color.fromRGB(149, 255, 240)
        item.itemMeta = meta
        return item
    }
    fun getBookOfMastery(): ItemStack {
        val item = ItemManager.createNamedItem(Material.ENCHANTED_BOOK, 1, "§6§lBook of Mastery", listOf("§7고성능 인챈트북입니다.", "§7인챈트 제한을 무시합니다.", "§7Gadget"))
        val meta = item.itemMeta as EnchantmentStorageMeta
        meta.addStoredEnchant(Enchantment.DAMAGE_ALL, 2, false)
        meta.addStoredEnchant(Enchantment.FIRE_ASPECT, 1, false)
        meta.addStoredEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, false)
        meta.addStoredEnchant(Enchantment.ARROW_DAMAGE, 4, false)
        meta.addStoredEnchant(Enchantment.QUICK_CHARGE, 3, false)
        item.itemMeta = meta

        return item
    }
    fun getGoldenCarrot(): ItemStack {
        return ItemManager.createNamedItem(Material.GOLDEN_CARROT, 1, "§6Golden Carrot", listOf("§71회용*", "§7우클릭시 다음 효과를 획득합니다:", "  §7즉시회복 I", "  §7재생 I 10초", "  §7흡수 I 2분"))
    }
    fun getRocketLauncher(): ItemStack {
        val item = ItemManager.createNamedItem(Material.WOODEN_SHOVEL, 1, "§e§lRocket Launcher", listOf("§7우클릭시 높은 대미지의 로켓을 발사합니다.", "§7로켓은 건물을 파괴시킬 수 있습니다.", " ", "§2대미지: 6.0  §2쿨타임: 30초")).addCustomModelData(10003)
        val meta = item.itemMeta
        meta.isUnbreakable = true
        item.itemMeta = meta
        return item
    }
    fun getBookOfSalvation(): ItemStack {
        val item = ItemManager.createNamedItem(Material.BOOK, 1, "§e§lBook of Salvation", listOf("§7클릭한 팀원의 위치로 순간이동합니다.", "§7순간이동시 4칸의 체력을 회복하며, 3초간 신속과 면역상태가 됩니다.", "§2쿨타임: 60초", " ", "§7Gadget"))
        val meta = item.itemMeta
        meta.isUnbreakable = true
        item.itemMeta = meta
        return item
    }
    fun getMolt(): ItemStack {
        return ItemManager.createNamedItem(
            Material.OAK_BUTTON,
            1,
            "§e화염병",
            listOf("§71회용*", "§7우클릭으로 투척 후 화염병이 깨지면, 주변에 지속적인 화염 대미지를 줍니다.", "§7불에 탈 때 회복불가 상태가 됩니다.")
        ).addCustomModelData(10006)
    }
    fun getSwordOfHealing(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.IRON_SWORD,
            1,
            "§e§lClaire",
            listOf("§7적을 공격시 8초의 쿨타임으로 주변 8블럭 안에", "§7자신을 포함한 팀원에게 재생 3를 1.7초간 부여합니다. (체력 한칸 회복)", "§7또한 들고 있을시 자신에게 재생 1을 부여합니다.", " ", "§7Gadget")
        )
        val m = item.itemMeta
        m.isUnbreakable = true
        m.addEnchant(Enchantment.DAMAGE_ALL, 1, false)
        item.itemMeta = m
        return item
    }
    fun getShotGun(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.IRON_HOE,
            1,
            "§7§lShotgun",
            listOf("§7우클릭으로 발사할 수 있습니다.", "§7근접에서 강한 위력을 보여주는 클래식한 산탄총입니다.", "§7거리가 가까울수록 대미지가 높아집니다.", "§e§l뉴비 추천 무기!", " ", "§2대미지: 0.3*12", " ", "§7Gadget")
        )
        val m = item.itemMeta
        m.isUnbreakable = true
        item.itemMeta = m
        return item
    }
    fun getQuickRocketLauncher(): ItemStack {
        val item = ItemManager.createNamedItem(Material.STONE_SHOVEL, 1, "§e§lRocket Launcher*", listOf("§e§l개조: 경형화", "§7우클릭시 낮은 대미지의 로켓을 발사합니다.", "§7로켓은 건물을 파괴시킬 수 있습니다.", "§7기존 로켓런처보다 파괴력을 낮추고, 연사력을 늘린 로켓런처입니다!", "§2대미지: 4.0  §2쿨타임: 4초", " ", "§7Gadget"))
        val meta = item.itemMeta
        meta.isUnbreakable = true
        item.itemMeta = meta
        return item
    }
    fun getFlashBang(): ItemStack {
        return ItemManager.createNamedItem(Material.WARPED_BUTTON, 1, "§e섬광탄", listOf("§71회용*", "§7우클릭으로 투척할 수 있습니다.", "§7터질 경우 시아 내에 섬광탄이 있으면, 시아가 차단됩니다.")).addCustomModelData(10007)
    }
    fun getEarthGr(): ItemStack {
        return ItemManager.createNamedItem(
            Material.DARK_OAK_BUTTON,
            1,
            "§e영역 수류탄",
            listOf(
                "§71회용*",
                "§7폭발시 건물을 붕괴시키는 지진을 일으키고 약한 대미지를 줍니다.",
                "§7폭발 위치보다 높은 곳의 건물만 파괴할 수 있습니다.",
                " ",
                "§2대미지: 1.0 x 3"
            )
        ).addCustomModelData(10010)
    }
    fun getEnchantedShield(): ItemStack {
        return ItemManager.addItemData(
            ItemManager.enchantItem(ItemStack(Material.SHIELD), Enchantment.DURABILITY, 3),
            1,
            "§b§lEnchanted Shield",
            listOf("§7왼손에 들고 있을 시, 받는 근접 대미지를 20% 감소 시킵니다.")
        )
    }
    fun getExosist(): ItemStack {
        return ItemManager.enchantItem(
            ItemManager.createNamedItem(
                Material.CROSSBOW,
                1,
                "§d§lExosist",
                listOf("§7벽을 관통하는 히트스캔 방식의 화살을 빠른 속도로 발사합니다.", "§715초마다 5초간 적에게 발광효과를 적용시키는 화살을 발사합니다!", "§7벽을 관통 후 최대 5블럭까지 날아가며, 대미지가 절반으로 감소됩니다."," ", "§2대미지: 2.0")
            ), Enchantment.QUICK_CHARGE, 3
        )
    }
    fun getPurify(): ItemStack {
        return ItemManager.enchantItem(ItemManager.createNamedItem(Material.BOW, 1, "§e§lPurify", listOf("§7킬이 0이고 공중에 있을 때 능력이 발동됩니다.", "§7폭발하는 히트스캔 화살을 발사합니다.", "§7풀차징일때 넉백과 기절을 부여합니다."," ", "§2폭발 대미지 (최대): 8.0  / 충격파 대미지 (최대): 2.0")), Enchantment.ARROW_DAMAGE, 1).addCustomModelData(2)
    }
    fun getHolyShield(): ItemStack {
        return ItemManager.createNamedItem(Material.NETHER_STAR, 1, "§bHoly Shield", listOf("§7인벤토리에 소지시,", "§7받는 피해량이 5를 넘으면 대미지를 무효화 시키고, 1초간 무적효과를 부여합니다", "§7또한 주변에 약한 대미지를 주며, 적을 밀쳐냅니다."," ", "§2쿨타임: 60초")).addCustomModelData(10005)
    }
    fun getCompass(): ItemStack {
        return ItemManager.createNamedItem(Material.COMPASS, 1, "§cPlayer Tracker", listOf("§7160블럭 내에 있는 가장 가까운 플레이어를 추적합니다!"))
    }
    fun getDevineSword(): ItemStack {
        return ItemManager.createNamedItem(Material.IRON_SWORD, 1, "§b§lDivine Sword", listOf("§7들고 있는 동안 속도가 20% 증가합니다.", " ", "§7Gadget"))
    }
    fun getPrototypeV3(): ItemStack {
        return ItemManager.createNamedItem(Material.NETHERITE_SHOVEL, 1, "§b§lPrototype V3", listOf("§7매우 강력한 스나이퍼 라이플입니다.", "§7거리가 멀수록 대미지가 증가합니다!","§7§oMasterPiece No.3")).addCustomModelData(10000)
    }
    fun getGravitization(): ItemStack {
        return ItemManager.createNamedItem(Material.RED_DYE, 1, "§c§lGRAVITIZATION", listOf("§7클릭한 위치에 강력한 중력장을 소환합니다.", "§7중력장은 아래로 적을 당기며 속도를 감소시킵니다.", "§7또한 느린 투사체를 막아냅니다.", "§2지속시간: 10초, 쿨타임: 30초, 초당 대미지: 1.0", " ", "§7Gadget"))
    }

    fun getOverFlow(): ItemStack {
        return ItemManager.createNamedItem(Material.DIAMOND_HELMET, 1, "§c§lOverFlow", listOf("§7체력이 낮을 때 근접 대미지가 증가합니다.","§2대미지 배수: x1.0 ~ x2.0, 체력 비례: 10칸 ~ 2칸"," ","§7§o그가 만들어낸 저주는 모두를 혼돈으로 빠트렸다"," ","§7Gadget"))
    }
    fun getBowOfEternity(): ItemStack {
        return ItemManager.createNamedItem(Material.BOW, 1, "§e§lBow of Eternity", listOf("§2일반 공격: §7히트스캔 방식의 화살을 발사합니다.", "§7같은 플레이어를 계속 맞추면 스택이 쌓입니다.", "§2좌클릭: §7스택을 제거하고, 스택이 쌓인 플레이어에게 스택에 비례한 고정피해를 입힙니다.", " ", "§2대미지: 2.0, 최대스택: 10, 스택 대미지: (스택)*1.0", "§7(화살 대미지는 인첸트의 영향을 받습니다.)", "§7Gadget", "§2Player: ", "§2Stack: ")).addEnchant(Enchantment.ARROW_DAMAGE, 1).addEnchant(Enchantment.DURABILITY, 3)
    }
    fun getShield(): ItemStack {
        return ItemManager.createNamedItem(
            Material.SHIELD,
            1,
            "§eShield",
            listOf("§7왼손에 들고 있을 시, 받는 근접 대미지를 10% 감소 시킵니다.")
        )
    }
    fun getGoldenApple(): ItemStack {
        return ItemManager.createNamedItem(
            Material.GOLDEN_APPLE,
            1,
            "§6황금사과",
            listOf(" ",
            "§7Gadget"
            )
        )
    }
    fun getSolarCannon(): ItemStack {
        val item = ItemManager.createNamedItem(
            Material.GOLDEN_SHOVEL,
            1,
            "§e§lSolar Cannon",
            listOf("§7거리가 멀수록 대미지가 증가하는 광선을 3번 발사합니다.", "§7원거리 공격일 경우 적에게 불을 붙히며", "§7불이 붙은 적은 잠시후 폭발합니다.", " ", "§2광선 대미지: ~2.0  폭발 대미지: ~2.5*3")
        )
        val m = item.itemMeta
        m.isUnbreakable = true
        item.itemMeta = m
        return item
    }
    fun getLiberation(): ItemStack {
        return ItemManager.createNamedItem(Material.RED_DYE, 1, "§c§lLiberation", listOf("§7주변 플레이어 수에 비례하여, 능력 지속시간이 증가합니다.", "§7능력이 발동되면 아래 효과를 발동시킵니다.", "§f - 주변 플레이어 지속피해 및 이동속도 감소", "§f - 받는 대미지 40% 감소", "§f - 힘 버프", " ", "§2쿨타임: 60초  능력 지속시간: 플레이어당 5초 (최대 20초)  범위: 10칸  초당대미지: 1.0", " ", "§7Gadget"))
    }
    fun getAltar(): ItemStack {
        return ItemManager.createNamedItem(Material.REINFORCED_DEEPSLATE, 1, "§c§lAltar of Sacrifice", listOf("§71회용*", "§7자신의 누적킬을 2 제거하고, 랜덤한 아이템을 생성합니다.", " ", "§7해당 아이템은 플레이어가 죽었을때 사라지지 않습니다."))
    }
    fun getSwordOfEternal(): ItemStack {
        val item = ItemManager.createNamedItem(Material.DIAMOND_SWORD, 1, "§f§lSword of Eternal", listOf("§7해당 칼의 모든 공격에 1.0 대미지가 추가됩니다."))
        val meta = item.itemMeta
        meta.isUnbreakable = true
        item.itemMeta = meta
        return item
    }
}