package com.teamwizardry.librarianlib.features.facade.components

import com.teamwizardry.librarianlib.features.facade.HandlerList
import com.teamwizardry.librarianlib.features.facade.value.IMValue
import com.teamwizardry.librarianlib.features.facade.value.IMValueBoolean
import com.teamwizardry.librarianlib.features.facade.component.GuiComponent
import com.teamwizardry.librarianlib.features.facade.provided.pastry.components.ItemStackTooltip
import com.teamwizardry.librarianlib.features.facade.provided.pastry.components.VanillaTooltip
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import com.teamwizardry.librarianlib.features.kotlin.plus
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting

open class ComponentItemStack(posX: Int, posY: Int) : GuiComponent(posX, posY, 16, 16) {

    val stack_im: IMValue<ItemStack> = IMValue(ItemStack.EMPTY)
    val enableTooltip_im: IMValueBoolean = IMValueBoolean(true)

    var stack: ItemStack by stack_im
    var enableTooltip: Boolean by enableTooltip_im
    val quantityText = HandlerList<(ComponentItemStack, String?) -> String?>()
    val itemInfo = HandlerList<(ComponentItemStack, MutableList<String>) -> Unit>()
    private val vanillaTooltip = ItemStackTooltip()

    init {
        tooltip = vanillaTooltip
        vanillaTooltip.stack_im {
            stack
        }
        vanillaTooltip.isVisible_im {
            enableTooltip
        }
    }

    override fun draw(partialTicks: Float) {
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.pushMatrix()

        val stack = this.stack
        if (stack.isNotEmpty) {
            var str: String? = stack.count.toString()
            if (str == "1") str = null
            str = quantityText.fireModifier(str) { h, v -> h(this, v) }

            val itemRender = Minecraft.getMinecraft().renderItem
            itemRender.zLevel = -130f

            GlStateManager.scale(size.xf / 16, size.yf / 16, 1f)

            val fr = (stack.item.getFontRenderer(stack) ?: Minecraft.getMinecraft().fontRenderer)
            itemRender.renderItemAndEffectIntoGUI(stack, 0, 0)
            itemRender.renderItemOverlayIntoGUI(fr, stack, 0, 0, str)

            itemRender.zLevel = 0.0f

            if (mouseOver && enableTooltip) {
                val font = stack.item.getFontRenderer(stack)
//                tooltip = getTooltip(stack) { itemInfo.fireAll { h -> h(this, it) } }
//                tooltipFont = font ?: Minecraft.getMinecraft().fontRenderer
            }
        }

        GlStateManager.popMatrix()
        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
    }
}
