package com.teamwizardry.librarianlib.features.gui.component.supporting

import com.teamwizardry.librarianlib.features.gui.EnumMouseButton
import com.teamwizardry.librarianlib.features.gui.Key
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.math.Vec2d
import org.lwjgl.input.Keyboard

interface IComponentGuiEvent {
    fun tick()

    /**
     * Called when a key is pressed in the parent component.
     *
     * @param key The actual character that was pressed
     * @param keyCode The key code, codes listed in [Keyboard]
     */
    fun keyPressed(key: Char, keyCode: Int)

    /**
     * Called when a key is released in the parent component.
     *
     * @param key The actual key that was pressed
     * @param keyCode The key code, codes listed in [Keyboard]
     */
    fun keyReleased(key: Char, keyCode: Int)
}

class ComponentGuiEventHandler: IComponentGuiEvent {
    lateinit var component: GuiComponent

    internal var mouseButtonsDownInside = arrayOfNulls<Vec2d?>(EnumMouseButton.values().size)
    internal var mouseButtonsDownOutside = arrayOfNulls<Vec2d?>(EnumMouseButton.values().size)
    internal var keysDown: MutableMap<Key, Boolean> = HashMap<Key, Boolean>().withDefault { false }

    override fun tick() {
        component.BUS.fire(GuiComponentEvents.ComponentTickEvent())
        component.subComponents.forEach { it.tick() }
    }

    /**
     * Called when a key is pressed in the parent component.
     *
     * @param key The actual character that was pressed
     * @param keyCode The key code, codes listed in [Keyboard]
     */
    override fun keyPressed(key: Char, keyCode: Int) {
        if (!component.isVisible) return
        if (component.BUS.fire(GuiComponentEvents.KeyDownEvent(key, keyCode)).isCanceled())
            return

        keysDown.put(Key[key, keyCode], true)

        component.subComponents.forEach { child ->
            child.keyPressed(key, keyCode)
        }
    }

    /**
     * Called when a key is released in the parent component.
     *
     * @param key The actual key that was pressed
     * @param keyCode The key code, codes listed in [Keyboard]
     */
    override fun keyReleased(key: Char, keyCode: Int) {
        if (!component.isVisible) return
        keysDown.put(Key[key, keyCode], false) // do this before so we don't have lingering keyDown entries

        if (component.BUS.fire(GuiComponentEvents.KeyUpEvent(key, keyCode)).isCanceled())
            return

        component.subComponents.forEach { child ->
            child.keyReleased(key, keyCode)
        }
    }
}
