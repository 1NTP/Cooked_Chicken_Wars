package me.uwuaden.kotlinplugin

import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addEnchant
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addLores
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addName
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addUnsafeEnchant
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setName
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

object GuideBookGUI {
    fun openFileDropInvNormal(player: Player) {
        val inventory = Bukkit.createInventory(null, 54, "§e§lItem Guide Book")
        for (i in 0 until 9) inventory.setItem(
            i,
            ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null)
        )
        for (i in 9 until 18) inventory.setItem(
            i,
            ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
        )
        for (i in 0..2) inventory.setItem(
            i * 9 + 26,
            ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
        )
        for (z in 0..3) for (i in 18..19) inventory.setItem(
            i + z * 9,
            ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
        )
        for (i in 45 until 53) inventory.setItem(
            i,
            ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null)
        )

        inventory.setItem(
            0, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§b일반 아이템", null).addUnsafeEnchant(Enchantment.DURABILITY, 1))
        inventory.setItem(1, ItemManager.createNamedItem(Material.DIAMOND_PICKAXE, 1, "§e유틸 아이템", null))
        inventory.setItem(2, ItemManager.createNamedItem(Material.CHEST, 1, "§e보급 아이템", null))


        inventory.setItem(
            53,
            ItemManager.createNamedItem(
                Material.OAK_SIGN,
                1,
                "§a기본 아이템",
                listOf("§7필드에 등장하는 기본적인 아이템들입니다!")
            )
        )
        for (i in 18..19) inventory.setItem(
            i,
            ItemManager.createNamedItem(
                Material.GREEN_STAINED_GLASS_PANE,
                1,
                "§e기본 드롭",
                listOf("§7다음 아이템 여러개가 필드에 등장합니다.")
            )
        )
        //기본드랍 - 확률
        for (i in listOf(27, 28, 36, 37)) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.YELLOW_STAINED_GLASS_PANE,
                    1,
                    "§e확정 드롭",
                    listOf("§7다음 아이템 중 하나가 필드에 등장합니다.", "§7이름이 노락색이거나 노란색 별이 표기된 경우, 더욱 낮은 확률로 등장합니다.")
                )
            )
        }
        inventory.setItem(20, ItemManager.createNamedItem(Material.ARROW, 1, "§f화살", null))

        inventory.setItem(21, ItemManager.createNamedItem(Material.COOKED_BEEF, 1, "§f스테이크", null))


        inventory.setItem(
            29,
            ItemManager.createNamedItem(
                Material.IRON_AXE,
                1,
                "§f도끼",
                listOf("${ChatColor.WHITE}도끼는 유리나 문을 좌클릭으로 부술 수 있습니다", " ", "§7다음 중 하나 획득 가능:", "  §f돌 도끼", "  §e철 도끼")
            ).addEnchant(Enchantment.DIG_SPEED, 3)
        )

        inventory.setItem(
            30,
            ItemManager.createNamedItem(
                Material.IRON_PICKAXE,
                1,
                "§f곡괭이",
                listOf("§7다음 중 하나 획득 가능:", "  §f돌 곡괭이", "  §e철 곡괭이")
            ).addEnchant(Enchantment.DIG_SPEED, 3)
        )

        inventory.setItem(31, CustomItemData.getShield().setName("§f방패"))

        inventory.setItem(32, CustomItemData.getEnchantedShield().setName("§e인첸트된 방패"))

        inventory.setItem(
            33,
            ItemManager.createNamedItem(
                Material.IRON_CHESTPLATE,
                1,
                "§f철 갑옷",
                listOf("§7다음 중 하나 획득 가능:", "  §f철 모자", "  §f철 흉갑", "  §f철 바지", "  §f철 신발")
            )
        )

        inventory.setItem(
            34,
            ItemManager.createNamedItem(
                Material.DIAMOND_CHESTPLATE,
                1,
                "§e다이아몬드 갑옷",
                listOf("§7다음 중 하나 획득 가능:", "  §e다이아몬드 모자", "  §e다이아몬드 흉갑", "  §e다이아몬드 바지", "  §e다이아몬드 신발")
            )
        )

        inventory.setItem(
            38, ItemManager.createNamedItem(
                Material.IRON_SWORD,
                1,
                "§f철 검",
                null
            )
        )

        inventory.setItem(
            39,
            ItemManager.createNamedItem(
                Material.DIAMOND_SWORD,
                1,
                "§e다이아몬드 검",
                null
            )
        )

        inventory.setItem(40, ItemManager.createNamedItem(Material.BOW, 1, "§f활", listOf("§7활은 방패를 해제시킬 수 있습니다!")))

        inventory.setItem(
            41,
            ItemManager.createNamedItem(Material.BOW, 1, "§e활 (힘)", listOf("§7활은 방패를 해제시킬 수 있습니다!"))
                .addEnchant(Enchantment.ARROW_DAMAGE, 1)
        )

        inventory.setItem(42, ItemManager.createNamedItem(Material.GOLDEN_APPLE, 1, "§b황금사과", null))
        inventory.setItem(43, CustomItemData.getGoldenCarrot().addName("§e*"))

        player.openInventory(inventory)
    }

    fun openFileDropInvUtill(player: Player) {

        val inventory = Bukkit.createInventory(null, 54, "§e§lItem Guide Book")

        for (i in 3..8) {
            inventory.setItem(i, ItemManager.createNamedItem(Material.ORANGE_STAINED_GLASS_PANE, 1, " ", null))
        }
        for (i in listOf(26, 35, 44)) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
            )
        }
        for (i in 9..17) {
            inventory.setItem(
                i, ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
            )
        }
        for (i in 18..19) {
            inventory.setItem(
                i, ItemManager.createNamedItem(
                    Material.GREEN_STAINED_GLASS_PANE,
                    1,
                    "§e다음 아이템들은 서로 3초의 쿨타임을 공유합니다.",
                    listOf("§7또한 앉기를 홀드해 던지는 방식을 바꿀 수 있습니다.")
                )
            )
        }
        for (i in listOf(27, 28, 36, 37)) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.YELLOW_STAINED_GLASS_PANE,
                    1,
                    "§e확정 드롭",
                    listOf("§7다음 아이템들중 하나가 필드에 등장합니다.")
                )
            )
        }
        inventory.setItem(
            53,
            ItemManager.createNamedItem(
                Material.OAK_SIGN,
                1,
                "§a유틸 아이템",
                listOf("§7필드에 드랍되는 강력한 아이템들입니다!",
                    "§7다음 아이템들은 필드에 등장하는 기본 아이템들보다 확률이 더 낮게 나옵니다."
                )
            )
        )
        inventory.setItem(0, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§e일반 아이템", null))
        inventory.setItem(1, ItemManager.createNamedItem(Material.DIAMOND_PICKAXE, 1, "§b유틸 아이템", null).addUnsafeEnchant(Enchantment.DURABILITY, 1))
        inventory.setItem(2, ItemManager.createNamedItem(Material.CHEST, 1, "§e보급 아이템", null))

        inventory.setItem(
            20,
                CustomItemData.getEarthGr(
            )
        )

        inventory.setItem(
            21,
            CustomItemData.getSmokeG(
            ).addLores(
                listOf(" ",
                        "§2${ChatColor.DARK_GREEN}지속시간: ?초",
                        " ",
                        "연막 안에서는 주변 시아가 차단됩니다"
                )
            )
        )
        inventory.setItem(
            22,
            CustomItemData.getMolt(
            ).addLores(listOf(" ",
                "§2불 장판 지속시간: 8초.",
                "§2화염대미지 지속시간: 1.0 x 4틱 (팀에게는 2틱).")
            )
        )
        inventory.setItem(
            23,
            CustomItemData.getGravityG()
        )
        inventory.setItem(
            24, CustomItemData.getFlashBang()
        )
        inventory.setItem(
            29,
            CustomItemData.getVallista().addLores(
                listOf(
                    " ",
                    "§2대미지: 4.0",
                    "§2재장전 속도: 1초 + 빠른장전"
                )
            )
        )
        inventory.setItem(
            30,
            CustomItemData.getRevelation()
                .addLores(listOf
                    (" ",
                    "§e§l킬 비례 대미지 : 0/1/2/3 킬 -> 4.0/3.0/2.0/0"
                )
            )
        )
        inventory.setItem(
            31,
            CustomItemData.getAGShotGun()
                .addLores(listOf("§7§l스턴은은 3초동안 점프를 할 수 없으며 구속 3에 걸립니다")
                )

        )

        inventory.setItem(
            32,
            CustomItemData.getRocketLauncher(
            )
        )
        inventory.setItem(
            33,
            CustomItemData.getEnergyDrink()
        )
        inventory.setItem(
            38,
            ItemManager.createNamedItem(
                Material.BLACK_DYE,
                1,
                "§b컨버터",
                listOf(
                    "§7다이아몬드 장비 5개를 '§e§l보호의 조각§7' 아이템으로 변환시킵니다.",
                    "§7우클릭하여 컨버터 메뉴를 열 수 있습니다",
                )
            )
        )
        inventory.setItem(
            39,
            ItemManager.createNamedItem(
                Material.ECHO_SHARD,
                1,
                "${ChatColor.BLUE}${ChatColor.BOLD}보호의 조각",
                listOf(
                    "${ChatColor.YELLOW}이 아이템은 컨버터 혹은 플레이어 시체에서만 획득할 수 있습니다",
                    " ",
                    "${ChatColor.GRAY}물리 피해 대미지를 조각 하나당 5% 감소시킵니다.",
                    "${ChatColor.GRAY}(최대 5개까지 적용)"
                )
            )
        )
        inventory.setItem(
            40,
            CustomItemData.getFlareGun().addLores(listOf(" ",
                "§7§l보급품안에는 강력한 아이템들이 들어있습니다!",
                "§e자세한건 보급 아이템 참조",
                " ",
                "§7보급상자 드랍 시간 : 15초 / 보급상자 대미지: 15.0",
                "§7}보급탄 대미지 : 6.0 x 화염대미지 1.0 6틱"))
            )
        for (i in 45..52)
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.ORANGE_STAINED_GLASS_PANE, 1, " ", null
                )
            )

        player.openInventory(inventory)
    }

    fun openFileDropInvFlare(player: Player) {
        val inventory = Bukkit.createInventory(null, 54, "§e§lItem Guide Book")

        for (i in 3..8) {
            inventory.setItem(i, ItemManager.createNamedItem(Material.RED_STAINED_GLASS_PANE, 1, " ", null))
        }
        for (i in 45..52) {
            inventory.setItem(i, ItemManager.createNamedItem(Material.RED_STAINED_GLASS_PANE, 1, " ", null))
        }
        for (i in listOf(26, 35, 44)) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null)
            )
        }
        for (i in 9..17) {
            inventory.setItem(i,
                ItemManager.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null))
        }
        inventory.setItem(
            53,
            ItemManager.createNamedItem(
                Material.OAK_SIGN,
                1,
                "§a보급 아이템",
                listOf("§cFlare Gun §7으로부터 나온 상자에 담겨있는 강력한 아이템들입니다!")
            )
        )
        inventory.setItem(0, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§e일반 아이템", null))
        inventory.setItem(1, ItemManager.createNamedItem(Material.DIAMOND_PICKAXE, 1, "§e유틸 아이템", null))
        inventory.setItem(2, ItemManager.createNamedItem(Material.CHEST, 1, "§b보급 아이템", null).addUnsafeEnchant(Enchantment.DURABILITY, 1))

        for (i in 18..19) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.GREEN_STAINED_GLASS_PANE,
                    1,
                    "§e기본 드롭",
                    listOf("§7다음 아이템이 보급상자에 여러개 나옵니다!")
                )
            )
        }
        for (i in 27..28) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.YELLOW_STAINED_GLASS_PANE,
                    1,
                    "§e확정 드롭",
                    listOf("§7다음 강력한 아이템들중 두 개가 상자에 담겨있습니다!")
                )
            )
        }
        for (i in 36..37) {
            inventory.setItem(
                i,
                ItemManager.createNamedItem(
                    Material.BLUE_STAINED_GLASS_PANE,
                    1,
                    "§e인첸트된 책",
                    listOf("§7확정 드롭에 포함되는 아이템들이며,",
                        "§7모루 + 책 중 하나가 묶음으로 나옵니다!"
                    )
                )
            )
        }
        inventory.setItem(
            20,
            ItemManager.createNamedItem(
                Material.COMPASS,
                1,
                "${ChatColor.RED}Player Tracker ",
                listOf(
                    "${ChatColor.GRAY}160블럭 내에 있는 가장 가까운 플레이어를 추적합니다!",
                    " ",
                    "${ChatColor.GRAY}160칸 안에 플레이어가 없을시 작동하지 않고 10?초의 재사용 대기시간이 걸립니다",
                    "${ChatColor.GRAY}또한 다음 아이템은 상자당 1개씩 나옵니다"
                )
            )
        )
        inventory.setItem(
            21,
            ItemManager.createNamedItem(
                Material.COOKED_BEEF,
                1,
                "§f스테이크",
                null
            )
        )
        inventory.setItem(
            22,
            ItemManager.createNamedItem(
                Material.GOLDEN_APPLE,
                1,
                "§b황금사과",
                null
            )
        )
        inventory.setItem(
            23,
            CustomItemData.getGoldenCarrot()
        )
        inventory.setItem(
            29,
                CustomItemData.getPrototypeV3(
                ).addLores
                    (listOf(
                    " ",
                    "§2거리비례 대미지 : 3.0 ~ 10.0",
                        "§2재사용 대기시간 : 8초",
                        "§7§l해당 아이템을 들 때 구속 3 및 점프불가에 걸립니다"
                        )
                    )
        )
        inventory.setItem(
            30,
            CustomItemData.getExplosiveBow().addLores(listOf(
                "§2폭발 대미지 : 2.0")
            )
        )
        inventory.setItem(
            31,
            CustomItemData.getPurify().addLores(
                listOf(
                    "§2충격파 반경 : 20칸 (중앙 기준 지름)",
                    "",
                    "§7§l발사 후에 반동으로 느린낙하를 2초간 받습니다",
                    "§7§l또한 기절은 다음 효과들을 4초간 받습니다",
                    "§2실명 1 / 어둠 1 / 나약함 5"
                )
            )
        )
        inventory.setItem(
            32,
            CustomItemData.getExosist().addLores(
                listOf("§7§l탄퍼짐이 심한 아이템입니다!",
                    "§7§l다만 발광 화살이 준비되었을 땐 탄퍼짐이 사라지며,"
                    ,"§7§l플레이어 주위에 파티클이 뜹니다.")
            )
        )
        inventory.setItem(
            33,
            CustomItemData.getHolyShield()
        )
        player.openInventory(inventory
        )
        inventory.setItem(
            38,
            ItemManager.createNamedItem(
                Material.ANVIL,
                1,
                "§f모루",
                listOf("§7닭갈비에는 아이템에 인첸트를 붙이고 수리하는데 필요한","§7경험치가 없습니다! 언제든 모루를 사용해보세요!")
            )
        )
        inventory.setItem(
            39,
            ItemManager.createNamedItem(
                Material.ENCHANTED_BOOK,
                1,
                "§e인첸트된 책",
                listOf("§7다음 중 하나 획득 가능:",
                    "§b날카로움 1",
                    "§b밀어내기 1",
                    "§b힘 2",
                    "§b보호 2"
                )
            )
        )
        inventory.setItem(
            40,
            ItemManager.createNamedItem(
                Material.IRON_PICKAXE,
                1,
                "§e인첸트 레벨 제한 설명서",
                listOf("§7게임의 밸런스를 위해 일부 인첸트들의 최대 레벨이 상향평준화되었습니다.",
                    "§f날카로움 5",
                    "§f밀어내기 2",
                    "§f보호 4",
                    "§e힘 3*",
                    "§e빠른장전 2*",
                )
            )
        )
        player.openInventory(inventory)
    }
}





