package com.teamwizardry.librarianlib.features.facade.components

import com.teamwizardry.librarianlib.core.client.ClientTickHandler
import com.teamwizardry.librarianlib.features.facade.component.GuiComponent
import com.teamwizardry.librarianlib.features.facade.value.RMValue
import com.teamwizardry.librarianlib.features.facade.value.RMValueDouble
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.math.Vec2d

class ComponentSliderTray(posX: Int, posY: Int, offsetX: Int, offsetY: Int) : GuiComponent(posX, posY) {

    internal val offsetX_rm = RMValue(offsetX)
    internal var offsetX: Int by offsetX_rm
    internal val offsetY_rm = RMValue(offsetY)
    internal var offsetY: Int by offsetY_rm
    internal var animatingIn = true
    internal var animatingOut = false
    internal var tickStart: Int = 0

    var lifetime = 5
    internal val currentOffsetX_rm = RMValueDouble(0.0)
    internal var currentOffsetX: Double by currentOffsetX_rm
    internal val rootPos_rm = RMValue(Vec2d.ZERO)
    internal var rootPos: Vec2d by rootPos_rm

    init {
        disableMouseCollision = true
        tickStart = ClientTickHandler.ticks
        this.currentOffsetX = pos.x
        rootPos = pos
    }

    fun close() {
        tickStart = ClientTickHandler.ticks
        animatingIn = false
        animatingOut = true
    }

    override fun draw(partialTicks: Float) {
        // TODO: Respect partialTicks
        val t = (ClientTickHandler.ticks - tickStart).toFloat() / lifetime.toFloat()
        if (t > 1) {
            if (animatingIn)
                animatingIn = false
        }

        if (Math.signum(offsetX.toFloat()) < 0) {
            if (animatingIn)
                if (currentOffsetX >= offsetX) currentOffsetX -= (-offsetX - Math.abs(currentOffsetX)) / 3
            if (animatingOut) {
                if (currentOffsetX < rootPos.x && currentOffsetX + (-offsetX - Math.abs(currentOffsetX)) / 3 < rootPos.x)
                    currentOffsetX += (-offsetX - Math.abs(currentOffsetX)) / 3
                else
                    removeFromParent()
            }

            // TODO: untested math.signum(x) < 0
        } else if (Math.signum(offsetX.toFloat()) < 0) {
            if (animatingIn)
                if (currentOffsetX > rootPos.x && currentOffsetX - (offsetX - Math.abs(currentOffsetX)) / 3 > rootPos.x)
                    currentOffsetX -= (offsetX - Math.abs(currentOffsetX)) / 3
                else
                    removeFromParent()
            if (animatingOut) {
                if (currentOffsetX <= offsetX) currentOffsetX += (offsetX - Math.abs(currentOffsetX)) / 3
            }

        } else
            removeFromParent()

        pos = vec(rootPos.x + currentOffsetX, rootPos.y)
    }
}
