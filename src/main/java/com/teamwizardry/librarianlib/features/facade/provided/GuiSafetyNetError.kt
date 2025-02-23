package com.teamwizardry.librarianlib.features.facade.provided

import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.roundBy
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color
import kotlin.math.min

class GuiSafetyNetError(e: Exception): GuiScreen() {

    private val guiWidth: Int
    private val guiHeight: Int

    private val gap = 2
    private val title = "§4§nSafety net caught an exception:"

    private val errorClass: String = e.javaClass.simpleName
    private val messageLines: List<String>

    init {
        val fontRenderer = Minecraft.getMinecraft().fontRenderer

        val maxWidth = 300
        val strings = mutableListOf(
            errorClass,
            title
        )

        val message = e.message
        if(message != null) {
            messageLines = fontRenderer.listFormattedStringToWidth(message, maxWidth)
            strings += messageLines
        } else {
            messageLines = emptyList()
        }

        guiWidth = min(maxWidth, strings.map { fontRenderer.getStringWidth(it) }.max()!!).roundBy(2)
        guiHeight = 0 +
            2 * fontRenderer.FONT_HEIGHT + gap + // title + gap + class name
            if(messageLines.isEmpty()) {
                0
            } else { 0 +
                2 * gap + 1 + // gap + separator + gap
                messageLines.size * fontRenderer.FONT_HEIGHT + // line height
                (messageLines.size-1) // 1px between message lines
            }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.drawDefaultBackground()

        val fontRenderer = Minecraft.getMinecraft().fontRenderer
        val topLeft = vec(width-guiWidth, height-guiHeight) / 2
        val border = 8

        Gui.drawRect(
            topLeft.xi - border, topLeft.yi - border,
            topLeft.xi + guiWidth + border, topLeft.yi + guiHeight + border,
            Color.lightGray.rgb
        )

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width)/2).toDouble(), ((height-guiHeight)/2).toDouble(), 0.0)

        var y = 0
        fun drawCenteredStringNoShadow(fontRenderer: FontRenderer, text: String , x: Int, y: Int, color: Int) {
            fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, color)
        }

        drawCenteredStringNoShadow(fontRenderer, title, 0, y, 0)
        y += fontRenderer.FONT_HEIGHT + gap
        drawCenteredStringNoShadow(fontRenderer, errorClass, 0, y, 0)
        y += fontRenderer.FONT_HEIGHT + gap

        if(messageLines.isNotEmpty()) {
            Gui.drawRect(-guiWidth/2, y-1, guiWidth/2, y, Color.darkGray.rgb)
            y += 1 + gap
            if(messageLines.size == 1) {
                drawCenteredStringNoShadow(fontRenderer, messageLines[0], 0, y, 0)
                y += fontRenderer.FONT_HEIGHT + 1
            } else {
                messageLines.forEach { line ->
                    fontRenderer.drawString(line, -guiWidth/2, y, 0)
                    y += fontRenderer.FONT_HEIGHT + 1
                }
            }
        }

        GlStateManager.popMatrix()
    }
}