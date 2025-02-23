package com.teamwizardry.librarianlib.features.facade.mixin

import com.teamwizardry.librarianlib.features.eventbus.Event
import com.teamwizardry.librarianlib.features.eventbus.EventCancelable
import com.teamwizardry.librarianlib.features.facade.EnumMouseButton
import com.teamwizardry.librarianlib.features.facade.component.GuiComponent
import com.teamwizardry.librarianlib.features.facade.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.math.Vec2d

class DragMixin(protected var component: GuiComponent, protected var correctionFunction: (Vec2d) -> Vec2d) {

    /**
     * Called when the component is picked up
     *
     * @property component The component this mixin is for
     * @property button The button clicked
     * @property mousePos The position of the mouse in the component
     */
    class DragPickupEvent(@JvmField val component: GuiComponent, val button: EnumMouseButton, val mousePos: Vec2d) : EventCancelable()

    /**
     * Called when the component is dropped up
     *
     * @property component The component this mixin is for
     * @property button The button released
     * @property previousPos The position of the component when it was picked up
     */
    class DragDropEvent(@JvmField val component: GuiComponent, val button: EnumMouseButton, val previousPos: Vec2d) : EventCancelable()

    /**
     * Called when the component is about to move
     *
     * @property component The component this mixin is for
     * @property button The button released
     * @property pos The current component position
     * @property newPos What the component's position will be set to after this event
     */
    class DragMoveEvent(@JvmField val component: GuiComponent, val button: EnumMouseButton, val pos: Vec2d, var newPos: Vec2d) : Event()

    var mouseDown: EnumMouseButton? = null
    var clickedPoint = Vec2d.ZERO
    var previousPos = Vec2d.ZERO

    init {
        init()
    }

    private fun init() {
        component.BUS.hook(GuiComponentEvents.MouseDownEvent::class.java) { event ->
            if (mouseDown == null && component.mouseOver && !component.BUS.fire(DragPickupEvent(component, event.button, component.mousePos)).isCanceled()) {
                mouseDown = event.button
                clickedPoint = component.mousePos
                previousPos = component.pos
            }
        }
        component.BUS.hook(GuiComponentEvents.MouseUpEvent::class.java) { event ->
            if (mouseDown == event.button && !component.BUS.fire(DragDropEvent(component, event.button, previousPos)).isCanceled()) {
                mouseDown = null
            }
        }

        component.BUS.hook(GuiComponentEvents.CalculateMousePositionEvent::class.java) { event ->
            val mouseButton = mouseDown
            if (mouseButton != null) {
                val pinnedPoint = component.convertPointToParent(clickedPoint)
                val offset = component.convertPointToParent(event.mousePos) - pinnedPoint
                val newPos = correctionFunction(component.pos + offset)

                if (newPos != component.pos) {
                    component.pos = component.BUS.fire(
                            DragMoveEvent(component, mouseButton, component.pos, newPos)
                    ).newPos
                    event.mousePos = component.parentComponent?.let { parent ->
                        component.convertPointFromParent(parent.mousePos)
                    } ?: component.mousePos
                }
            }
        }
    }
}
