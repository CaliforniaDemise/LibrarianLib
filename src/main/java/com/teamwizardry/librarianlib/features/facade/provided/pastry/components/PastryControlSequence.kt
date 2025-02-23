package com.teamwizardry.librarianlib.features.facade.provided.pastry.components

class PastryControlSequence {
    private var head: PastryControl? = null

    fun <T: PastryControl> add(control: T): T {
        head?.next = control
        control.previous = head
        head = control
        return control
    }
}