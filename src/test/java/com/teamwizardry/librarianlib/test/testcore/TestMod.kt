package com.teamwizardry.librarianlib.test.testcore

import com.teamwizardry.librarianlib.common.core.LoggerBase
import com.teamwizardry.librarianlib.common.util.autoregister.AutoRegisterHandler
import com.teamwizardry.librarianlib.test.saving.SavingEntryPoint
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

/**
 * Created by TheCodeWarrior
 */
@Mod(modid = TestMod.MODID, version = TestMod.VERSION, name = TestMod.MODNAME, dependencies = TestMod.DEPENDENCIES, useMetadata = true)
class TestMod {

    @Mod.EventHandler
    fun preInit(e: FMLPreInitializationEvent) {
        AutoRegisterHandler.registerPrefix("com.teamwizardry.librarianlib.test.", TestMod.MODID)
        entrypoints = arrayOf(
                SavingEntryPoint
        )
        PROXY.pre(e)
        entrypoints.forEach {
            it.preInit(e)
        }
    }

    @Mod.EventHandler
    fun init(e: FMLInitializationEvent) {
        PROXY.init(e)
        entrypoints.forEach {
            it.init(e)
        }
    }

    @Mod.EventHandler
    fun postInit(e: FMLPostInitializationEvent) {
        PROXY.post(e)
        entrypoints.forEach {
            it.postInit(e)
        }
    }

    companion object {

        const val MODID = "librarianlibtest"
        const val MODNAME = "LibrarianLib Test"
        const val VERSION = "0.0"
        const val CLIENT = "com.teamwizardry.librarianlib.test.testcore.LibTestClientProxy"
        const val SERVER = "com.teamwizardry.librarianlib.test.testcore.LibTestCommonProxy"
        const val DEPENDENCIES = "required-before:librarianlib"

        @JvmStatic
        @SidedProxy(clientSide = CLIENT, serverSide = SERVER)
        lateinit var PROXY: LibTestCommonProxy

        lateinit var entrypoints: Array<TestEntryPoint>

        val tab = object : CreativeTabs("LibLibTesting") {
            override fun getTabIconItem() = Item.getItemFromBlock(Blocks.BOOKSHELF)
        }
    }

}

object TestLog : LoggerBase("LibrarianLibTest")
