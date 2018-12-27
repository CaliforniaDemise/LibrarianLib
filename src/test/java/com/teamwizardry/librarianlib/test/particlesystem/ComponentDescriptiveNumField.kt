package com.teamwizardry.librarianlib.test.particlesystem

import com.teamwizardry.librarianlib.features.gui.components.ComponentTextField
import com.teamwizardry.librarianlib.features.gui.layers.TextLayer
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.Minecraft

class ComponentDescriptiveNumField constructor(description: String? = null, defaultValue: Double, x: Int, y: Int, width: Int, height: Int, onEdit: (Double) -> Unit) : ComponentTextField(x, y, width, height) {

    var textLayer: TextLayer? = null
    val field = ComponentTextField(0, 0, width, height)

    init {
        field.writeText("$defaultValue")
        field.BUS.hook<ComponentTextField.TextEditEvent> {
            try {
                onEdit(it.whole.toDouble())
            } catch (ignored: Exception) {
                it.cancel()
            }
        }
        add(field)

        if (description != null) {
            val stringWidth = Minecraft().fontRenderer.getStringWidth(description)
            textLayer = TextLayer(-stringWidth - 5, 0, 0, 0)
            textLayer!!.fitToText = true
            textLayer!!.text = description

            add(textLayer)
        }
    }

    fun updateText(description: String) {
        val stringWidth = Minecraft().fontRenderer.getStringWidth(description)
        if (textLayer == null) {
            textLayer = TextLayer(-stringWidth - 5, 0, 0, 0)
            add(textLayer)
        } else {
            textLayer!!.pos = vec(-stringWidth - 5, 0)
        }
        textLayer!!.fitToText = true
        textLayer!!.text = description
    }
}