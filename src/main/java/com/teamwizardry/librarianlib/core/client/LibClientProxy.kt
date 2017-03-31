package com.teamwizardry.librarianlib.core.client

import com.teamwizardry.librarianlib.core.LibrarianLib
import com.teamwizardry.librarianlib.features.forgeevents.CustomWorldRenderEvent
import com.teamwizardry.librarianlib.features.forgeevents.ResourceReloadEvent
import com.teamwizardry.librarianlib.features.shader.LibShaders
import com.teamwizardry.librarianlib.features.shader.ShaderHelper
import com.teamwizardry.librarianlib.core.client.ModelsInit
import com.teamwizardry.librarianlib.features.sprite.SpritesMetadataSection
import com.teamwizardry.librarianlib.features.sprite.SpritesMetadataSectionSerializer
import com.teamwizardry.librarianlib.features.sprite.Texture
import com.teamwizardry.librarianlib.features.utilities.client.F3Handler
import com.teamwizardry.librarianlib.features.utilities.client.ScissorUtil
import com.teamwizardry.librarianlib.features.utilities.ClientRunnable
import com.teamwizardry.librarianlib.core.common.LibCommonProxy
import com.teamwizardry.librarianlib.features.methodhandles.MethodHandleHelper
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.times
import com.teamwizardry.librarianlib.features.kotlin.unaryMinus
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraft.client.resources.IResourceManager
import net.minecraft.client.resources.IResourceManagerReloadListener
import net.minecraft.client.resources.data.MetadataSerializer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.model.animation.Animation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Prefixed with Lib so code suggestion in dependent projects doesn't suggest it
 */
@SideOnly(Side.CLIENT)
class LibClientProxy : LibCommonProxy(), IResourceManagerReloadListener {

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    override fun pre(e: FMLPreInitializationEvent) {
        super.pre(e)

        F3Handler
        ModelsInit
        ScissorUtil
        LibShaders
        ShaderHelper.init()

        val s = MethodHandleHelper.wrapperForGetter(Minecraft::class.java, "metadataSerializer_", "field_110452_an")(Minecraft.getMinecraft()) as MetadataSerializer
        s.registerMetadataSectionType(SpritesMetadataSectionSerializer(), SpritesMetadataSection::class.java)
        SpritesMetadataSection.registered = true

        Texture.register()

        (Minecraft.getMinecraft().resourceManager as IReloadableResourceManager).registerReloadListener(this)
        onResourceManagerReload(Minecraft.getMinecraft().resourceManager)

        if (LibrarianLib.DEV_ENVIRONMENT)
            TextureMapExporter
    }

    override fun latePre(e: FMLPreInitializationEvent) {
        super.latePre(e)
        ModelHandler.preInit()
    }

    override fun lateInit(e: FMLInitializationEvent) {
        super.lateInit(e)
        ModelHandler.init()
    }

    override fun translate(s: String, vararg format: Any?): String {
        return I18n.format(s, *format)
    }

    override fun canTranslate(s: String): Boolean {
        return I18n.hasKey(s)
    }

    override fun getResource(modId: String, path: String): InputStream? {
        val resourceManager = Minecraft.getMinecraft().resourceManager
        try {
            return resourceManager.getResource(ResourceLocation(modId, path)).inputStream
        } catch (e: IOException) {
            return null
        }
    }

    override fun runIfClient(clientRunnable: ClientRunnable) = clientRunnable.runIfClient()

    override fun getClientPlayer(): EntityPlayer = Minecraft.getMinecraft().player

    override fun getDataFolder(): File = Minecraft.getMinecraft().mcDataDir

    // Custom events

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        MinecraftForge.EVENT_BUS.post(ResourceReloadEvent(resourceManager))
    }

    @SubscribeEvent
    fun renderWorldEvent(e: RenderWorldLastEvent) {
        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()
        val player = Minecraft.getMinecraft().player

        val lastPos = vec(player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ)
        val partialOffset = (player.positionVector - lastPos) * (1 - Animation.getPartialTickTime())

        val globalize = -(player.positionVector - partialOffset)
        GlStateManager.translate(globalize.xCoord, globalize.yCoord, globalize.zCoord)


        GlStateManager.disableTexture2D()
        GlStateManager.color(1f, 1f, 1f, 1f)

        MinecraftForge.EVENT_BUS.post(CustomWorldRenderEvent(Minecraft.getMinecraft().world, e.context, Animation.getPartialTickTime()))

        GlStateManager.enableTexture2D()
        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }
}