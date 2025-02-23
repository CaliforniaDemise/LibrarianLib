package com.teamwizardry.librarianlib.features.facade.component.supporting

import com.teamwizardry.librarianlib.features.facade.EnumMouseButton
import com.teamwizardry.librarianlib.features.facade.component.GuiComponent
import com.teamwizardry.librarianlib.features.facade.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.facade.components.RootComponent
import com.teamwizardry.librarianlib.features.facade.value.IMValue
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.math.Vec2d
import com.teamwizardry.librarianlib.features.utilities.client.LibCursor
import java.util.Collections

interface IComponentMouse {
    /**
     * The position of the mouse in this component's coordinate space
     */
    val mousePos: Vec2d

    /**
     * The [mousePos] value from the previous frame
     */
    val lastMousePos: Vec2d

    val mouseHit: MouseHit?

    val mouseOver: Boolean

    val mouseInside: Boolean

    /**
     * The set of mouse buttons currently being pressed
     */
    val pressedButtons: Set<EnumMouseButton>

    /**
     * Whether this component will block the mouse being over those behind it.
     *
     * If this is true, as it is by default, this component will "shade" those behind it, preventing the mouse
     * from counting as [over them][mouseOver]. The default value is true.
     */
    var isOpaqueToMouse: Boolean

    /**
     * This flag controls the effect the mouse being [inside][mouseInside] or [over][mouseOver] this component has on
     * its parent. The default value is [NONE][MousePropagationType.NONE]
     */
    var propagateMouse: Boolean

    /**
     * This flag controls whether the mouse being within this component's bounding rectangle should count as it being
     * [inside][mouseInside] or [over][mouseOver] this component. The default value is true.
     */
    var disableMouseCollision: Boolean

    val cursor_im: IMValue<LibCursor?>

    /**
     * If nonnull, the cursor will randomizedSwitch to this when hovering.
     */
    var cursor: LibCursor?

    /**
     * Update the mousePos of this component and its children based on the given mouse position in its parent.
     */
    fun updateMouse(parentMousePos: Vec2d)

    /**
     * Update the mouseInside of this component and its children based on the current mousePos.
     */
    fun updateHits(root: RootComponent, parentZ: Double)

    /**
     * Update the mouseOver of this component and its children based on the current mousePos
     */
    fun propagateHits()

    fun mouseDown(button: EnumMouseButton)

    fun mouseUp(button: EnumMouseButton)

    fun mouseWheel(amount: Double)
}

enum class MousePropagationType {
    /**
     * The mouse being inside or over this component will not count as the mouse being inside/over its parent
     */
    NONE,
    /**
     * The mouse being [inside][IComponentMouse.mouseInside] this component will count as the mouse being inside its
     * parent, however the mouse being [over][IComponentMouse.mouseOver] this component will not count as the mouse
     * being over its parent.
     */
    INSIDE,
    /**
     * The mouse being [inside][IComponentMouse.mouseInside] or [over][IComponentMouse.mouseOver] this component will
     * count as the mouse being inside or over its parent.
     */
    OVER
}

class ComponentMouseHandler: IComponentMouse {
    lateinit var component: GuiComponent

    override var mousePos: Vec2d = Vec2d.ZERO
        private set
    override var lastMousePos: Vec2d = Vec2d.ZERO
        private set
    override var mouseOver: Boolean = false
        private set
    override val mouseInside: Boolean
        get() = mouseHit != null
    override var mouseHit: MouseHit? = null
        private set

    private val buttonsDownOver = mutableMapOf<EnumMouseButton, Boolean>()
    override val pressedButtons: Set<EnumMouseButton> = Collections.unmodifiableSet(buttonsDownOver.keys)

    override var isOpaqueToMouse: Boolean = true
    override var propagateMouse: Boolean = true
    override var disableMouseCollision: Boolean = false
    override val cursor_im: IMValue<LibCursor?> = IMValue()
    override var cursor: LibCursor? by cursor_im

    private var hadMouseHit = false
    private var lastMouseDragPos = vec(0, 0)

    override fun updateMouse(parentMousePos: Vec2d) {
        this.lastMousePos = mousePos
        this.mousePos = component.BUS.fire(GuiComponentEvents.CalculateMousePositionEvent(
            component.convertPointFromParent(parentMousePos)
        )).mousePos
        if(lastMouseDragPos.squareDist(mousePos) > 0.1 * 0.1) {
            if(pressedButtons.isNotEmpty())
                component.BUS.fire(GuiComponentEvents.MouseDragEvent())
            component.BUS.fire(GuiComponentEvents.MouseMoveEvent())
            lastMouseDragPos = mousePos
        }
        component.subComponents.forEach {
            it.updateMouse(this.mousePos)
        }
        this.hadMouseHit = component.mouseHit != null
        this.mouseHit = null
    }

    override fun updateHits(root: RootComponent, parentZ: Double) {
        val zIndex = parentZ + component.zIndex
        if(!component.disableMouseCollision && component.isPointInBounds(component.mousePos)) {
            val mouseHit = MouseHit(this.component, zIndex, this.cursor)
            this.mouseHit = mouseHit
            if(component.isOpaqueToMouse && mouseHit > root.topMouseHit) {
                root.topMouseHit = mouseHit
            }
        }

        if(!component.isPointClipped(component.mousePos)) {
            for (child in component.subComponents) {
                if (child.isVisible) {
                    child.updateHits(root, zIndex)
                }
            }
        }
    }

    override fun propagateHits() {
        for(child in component.subComponents) {
            child.propagateHits()
            if(child.isVisible && child.propagateMouse && child.mouseHit > component.mouseHit) {
                this.mouseHit = child.mouseHit
            }
        }

        val wasMouseOver = component.mouseOver

        val mouseHit = this.component.mouseHit
        val topHit = this.component.gui?.topMouseHit

        mouseOver = mouseHit != null && mouseHit >= topHit
        if(mouseOver)
            this.component.gui?.mouseOverComponents?.add(this.component)

        if(wasMouseOver && !component.mouseOver) {
            if(pressedButtons.isNotEmpty())
                component.BUS.fire(GuiComponentEvents.MouseDragLeaveEvent())
            component.BUS.fire(GuiComponentEvents.MouseLeaveEvent())
        } else if(!wasMouseOver && component.mouseOver) {
            if(pressedButtons.isNotEmpty())
                component.BUS.fire(GuiComponentEvents.MouseDragEnterEvent())
            component.BUS.fire(GuiComponentEvents.MouseEnterEvent())
        }

        if(hadMouseHit && component.mouseHit == null) {
            if(pressedButtons.isNotEmpty())
                component.BUS.fire(GuiComponentEvents.MouseDragOutEvent())
            component.BUS.fire(GuiComponentEvents.MouseMoveOutEvent())
        } else if(!hadMouseHit && component.mouseHit != null) {
            if(pressedButtons.isNotEmpty())
                component.BUS.fire(GuiComponentEvents.MouseDragInEvent())
            component.BUS.fire(GuiComponentEvents.MouseMoveInEvent())
        }
    }

    override fun mouseDown(button: EnumMouseButton) {
        if (!component.isVisible) return
        component.BUS.fire(GuiComponentEvents.MouseDownEvent(button))

        buttonsDownOver[button] = mouseOver

        component.subComponents.forEach { child ->
            child.mouseDown(button)
        }
    }

    override fun mouseUp(button: EnumMouseButton) {
        if (!component.isVisible) return

        val wasOver = buttonsDownOver[button] ?: false
        buttonsDownOver.remove(button)

        component.BUS.fire(GuiComponentEvents.MouseUpEvent(button))

        if (component.mouseOver) {
            if(wasOver) {
                component.BUS.fire(GuiComponentEvents.MouseClickEvent(button))
            } else {
                component.BUS.fire(GuiComponentEvents.MouseClickDragInEvent(button))
            }
        } else {
            if(wasOver) {
                component.BUS.fire(GuiComponentEvents.MouseClickDragOutEvent(button))
            } else {
                component.BUS.fire(GuiComponentEvents.MouseClickOutsideEvent(button))
            }
        }

        component.subComponents.forEach { child ->
            child.mouseUp(button)
        }
    }

    override fun mouseWheel(amount: Double) {
        if (!component.isVisible) return

        component.BUS.fire(GuiComponentEvents.MouseWheelEvent(amount))

        component.subComponents.forEach { child ->
            child.mouseWheel(amount)
        }
    }


}