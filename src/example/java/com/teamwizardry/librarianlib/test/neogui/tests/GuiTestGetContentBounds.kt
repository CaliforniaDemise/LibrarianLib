package com.teamwizardry.librarianlib.test.neogui.tests

import com.teamwizardry.librarianlib.features.neogui.GuiBase
import com.teamwizardry.librarianlib.features.neogui.component.GuiLayer
import com.teamwizardry.librarianlib.features.neogui.layers.ColorLayer
import com.teamwizardry.librarianlib.features.neogui.value.GuiAnimator
import com.teamwizardry.librarianlib.features.helpers.vec
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Created by TheCodeWarrior
 */
class GuiTestGetContentBounds : GuiBase() {
    init {
        main.size = vec(100, 100)

        val background = ColorLayer(Color.WHITE, 0, 0, 100, 100)

        val containerContents = ColorLayer(Color.GREEN.darker(), 20, 5, 40, 10)
        val containerContentsContents = ColorLayer(Color.GREEN.darker().darker(), 15, 5, 10, 10)
        val containerIgnoredContents = ColorLayer(Color.RED, 15, 5, 10, 20)

        val anim = GuiAnimator.animate(20f) {
            containerContentsContents.isVisible = false
        }
        anim.shouldReverse = true
        anim.repeatCount = -1
        containerContentsContents.add(anim)

        val container = object: GuiLayer(0, 0, 40, 10) {
            init {
                this.add(ColorLayer(Color.GREEN, 0, 0, 40, 10))
            }
            override fun draw(partialTicks: Float) {
                var contentsBounds = getContentsBounds({ it != containerIgnoredContents && it.isVisible }, { it.isVisible })!!
                val ignoreInvisible = contentsBounds.max
                contentsBounds = getContentsBounds({ it != containerContents }, { true })!!
                val ignoreContents = contentsBounds.max

                val tessellator = Tessellator.getInstance()
                val vb = tessellator.buffer

                GlStateManager.disableTexture2D()

                GlStateManager.enableBlend()
                var c = Color.BLUE
                GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f)

                vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
                vb.pos(ignoreInvisible.x, 0.0, 1.0).endVertex()
                vb.pos(ignoreInvisible.x, 100.0, 1.0).endVertex()
                vb.pos(0.0, ignoreInvisible.y, 1.0).endVertex()
                vb.pos(100.0, ignoreInvisible.y, 1.0).endVertex()
                tessellator.draw()

                c = Color.RED
                GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f)

                vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
                vb.pos(ignoreContents.x+1, 0.0, 1.0).endVertex()
                vb.pos(ignoreContents.x+1, 100.0, 1.0).endVertex()
                vb.pos(0.0, ignoreContents.y+1, 1.0).endVertex()
                vb.pos(100.0, ignoreContents.y+1, 1.0).endVertex()
                tessellator.draw()

                GlStateManager.enableTexture2D()
            }
        }

        container.add(containerContents, containerIgnoredContents)
        containerContents.add(containerContentsContents)

        main.add(background, container)
    }
}
