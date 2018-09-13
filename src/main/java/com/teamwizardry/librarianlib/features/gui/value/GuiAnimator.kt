package com.teamwizardry.librarianlib.features.gui.value

import com.teamwizardry.librarianlib.features.animator.AnimatableProperty
import com.teamwizardry.librarianlib.features.animator.Animation
import com.teamwizardry.librarianlib.features.animator.Easing
import com.teamwizardry.librarianlib.features.animator.Lerper
import com.teamwizardry.librarianlib.features.animator.LerperHandler
import java.util.function.Supplier

/**
 * Creates implicit animations for [IMValues][IMValue] and [RMValues][RMValue].
 *
 * For most situations, manually creating [BasicAnimations][BasicAnimation] is overkill, and certainly far too verbose.
 * GuiAnimator provides a method that receives a duration, easing, and callback. Every [IMValue] or [RMValue] that
 * changes inside of that callback will be added to the resulting animation, which can then be further modified
 * (e.g. to loop or reverse) before being added to an animator as you would any other animation.
 *
 *
 * ```java
 * BasicAnimation<YourComponent> animSize = new BasicAnimation<>(component, "size");
 * animSize.setDuration(5);
 * animSize.setEasing(Easing.easeOutCubic);
 * animSize.setRepeatCount(2);
 * animSize.setTo(targetSize);
 * component.add(animSize);
 *
 * BasicAnimation<YourComponent> animPos = new BasicAnimation<>(component, "pos");
 * animPos.setDuration(5);
 * animPos.setEasing(Easing.easeOutCubic);
 * animPos.setRepeatCount(2);
 * animPos.setTo(targetPos);
 * component.add(animPos);
 *
 * BasicAnimation<YourComponent> animRadius = new BasicAnimation<>(component, "radius");
 * animRadius.setDuration(5);
 * animRadius.setEasing(Easing.easeOutCubic);
 * animRadius.setRepeatCount(2);
 * animRadius.setTo(targetColor);
 * component.add(animRadius);
 * ```
 * ### vs.
 * ```java
 * GuiAnimation anim = GuiAnimator.animate(5, Easing.easeOutCubic, () -> {
 *     component.setSize(targetSize);
 *     component.setPos(targetPos);
 *     // the set____ and get____ are generated by kotlin and forward to the I/RMValue. Java code either has to
 *     // manually specify these methods or use the [IMValue.setValue] or [RMValue.set] methods
 *     component.radius.set(targetRadius);
 * });
 * anim.setRepeatCount(2);
 * component.add(anim);
 * ```
 */
class GuiAnimator {
    companion object {
        private val threadLocal = ThreadLocal<GuiAnimator>()

        @JvmStatic
        val current: GuiAnimator
            get() {
                return threadLocal.get() ?: GuiAnimator().also { threadLocal.set(it) }
            }

        /**
         * This method will create an animation that interpolates between the pre- and post-block values of any
         * [IMValues][IMValue] or [RMValues][RMValue] that are modified within the passed block.
         *
         * If you aren't sure whether you can animate a particular property with this, jump to the LibrarianLib
         * source code and check if the property delegates to an [IMValue] or [RMValue].
         * (because LibrarianLib is kotlin `SomeType getSomeProperty` in java will map to
         * `var/val someProperty: SomeType` in kotlin. Delegation is defined using the `by` keyword, so if
         * `by RMValue...` or `by someIMValueProperty` appear after the property declaration that means you can
         * animate it using this method.
         *
         * The internal process by which animations are created is as follows:
         * - The first time any given [IMValue] or [RMValue] is modified inside of [block] it registers
         * itself using `GuiAnimator.`[current][current]`.`[add][add] and its pre-change value is saved as the
         * starting value of the animation.
         * - After the block finishes the value of each [GuiAnimatable] object that was registered is read as the
         * ending value of the animation.
         * - If the start and end are equal or either is null then no animations will be added for that value.
         *
         * @param duration The duration of the returned animation
         * @param easing The easing value of the returned animation
         * @param block The block that will be run and inside of which any changes will occur
         * @return The generated animation
         */
        @JvmOverloads
        fun animate(duration: Float, easing: Easing = Easing.linear, block: Runnable): Animation<*> {
            return current.animate(duration, easing, block)
        }

        /**
         * This method will create an animation that interpolates between the pre- and post-block values of any
         * [IMValues][IMValue] or [RMValues][RMValue] that are modified within the passed block.
         *
         * If you aren't sure whether you can animate a particular property with this, jump to the LibrarianLib
         * source code and check if the property delegates to an [IMValue] or [RMValue].
         * (because LibrarianLib is kotlin `SomeType getSomeProperty` in java will map to
         * `var/val someProperty: SomeType` in kotlin. Delegation is defined using the `by` keyword, so if
         * `by RMValue...` or `by someIMValueProperty` appear after the property declaration that means you can
         * animate it using this method.
         *
         * The internal process by which animations are created is as follows:
         * - The first time any given [IMValue] or [RMValue] is modified inside of [block] it registers
         * itself using `GuiAnimator.`[current][current]`.`[add][add] and its pre-change value is saved as the
         * starting value of the animation.
         * - After the block finishes the value of each [GuiAnimatable] object that was registered is read as the
         * ending value of the animation.
         * - If the start and end are equal or either is null then no animations will be added for that value.
         *
         * @param duration The duration of the returned animation
         * @param easing The easing value of the returned animation
         * @param block The block that will be run and inside of which any changes will occur
         * @return The generated animation
         */
        fun animate(duration: Float, easing: Easing = Easing.linear, block: () -> Unit): Animation<*> {
            return current.animate(duration, easing, block)
        }
    }

    private val animations = mutableMapOf<GuiAnimatable, Any?>()
    private var isAnimating = false

    /**
     * This method will create an animation that interpolates between the pre- and post-block values of any
     * [IMValues][IMValue] or [RMValues][RMValue] that are modified within the passed block.
     *
     * If you aren't sure whether you can animate a particular property with this, jump to the LibrarianLib
     * source code and check if the property delegates to an [IMValue] or [RMValue].
     * (because LibrarianLib is kotlin `SomeType getSomeProperty` in java will map to
     * `var/val someProperty: SomeType` in kotlin. Delegation is defined using the `by` keyword, so if
     * `by RMValue...` or `by someIMValueProperty` appear after the property declaration that means you can
     * animate it using this method.
     *
     * The internal process by which animations are created is as follows:
     * - The first time any given [IMValue] or [RMValue] is modified inside of [block] it registers
     * itself using `GuiAnimator.`[current][current]`.`[add][add] and its pre-change value is saved as the
     * starting value of the animation.
     * - After the block finishes the value of each [GuiAnimatable] object that was registered is read as the
     * ending value of the animation.
     * - If the start and end are equal or either is null then no animations will be added for that value.
     *
     * @param duration The duration of the returned animation
     * @param easing The easing value of the returned animation
     * @param block The block that will be run and inside of which any changes will occur
     * @return The generated animation
     */
    @JvmOverloads
    fun animate(duration: Float, easing: Easing = Easing.linear, block: Runnable): Animation<*> {
        animations.clear()
        isAnimating = true

        block.run()

        val valueAnimations = animations.mapNotNull {
            val start = it.value
            val end = it.key.getAnimatableValue()
            if(end == null || start == null || start == end)
                return@mapNotNull null

            GuiImplicitAnimation.ValueAnimation(it.key, start, end)
        }
        val anim = GuiImplicitAnimation(valueAnimations)
        anim.duration = duration
        anim.easing = easing

        isAnimating = false
        animations.clear()

        return anim
    }

    /**
     * This method will create an animation that interpolates between the pre- and post-block values of any
     * [IMValues][IMValue] or [RMValues][RMValue] that are modified within the passed block.
     *
     * If you aren't sure whether you can animate a particular property with this, jump to the LibrarianLib
     * source code and check if the property delegates to an [IMValue] or [RMValue].
     * (because LibrarianLib is kotlin `SomeType getSomeProperty` in java will map to
     * `var/val someProperty: SomeType` in kotlin. Delegation is defined using the `by` keyword, so if
     * `by RMValue...` or `by someIMValueProperty` appear after the property declaration that means you can
     * animate it using this method.
     *
     * The internal process by which animations are created is as follows:
     * - The first time any given [IMValue] or [RMValue] is modified inside of [block] it registers
     * itself using `GuiAnimator.`[current][current]`.`[add][add] and its pre-change value is saved as the
     * starting value of the animation.
     * - After the block finishes the value of each [GuiAnimatable] object that was registered is read as the
     * ending value of the animation.
     * - If the start and end are equal or either is null then no animations will be added for that value.
     *
     * @param duration The duration of the returned animation
     * @param easing The easing value of the returned animation
     * @param block The block that will be run and inside of which any changes will occur
     * @return The generated animation
     */
    fun animate(duration: Float, easing: Easing = Easing.linear, block: () -> Unit): Animation<*> {
        return this.animate(duration, easing, Runnable(block))
    }

    /**
     * Registers the given animatable to be included in the animation. See [animate] for details
     */
    fun add(target: GuiAnimatable) {
        if(!isAnimating) return
        animations.getOrPut(target) { target.getAnimatableValue() }
    }
}

@Suppress("UNCHECKED_CAST")
private class GuiImplicitAnimation(val targets: List<ValueAnimation>): Animation<Any>(
    PointlessAnimatableObject,
    AnimatableProperty.get(PointlessAnimatableObject::class.java, "field") as AnimatableProperty<Any>) {
    var easing: Easing = Easing.linear

    override fun update(time: Float) {
        val progress = easing(timeFraction(time))
        targets.forEach { it.update(progress) }
    }

    override fun complete() {
        targets.forEach { it.complete() }
        super.complete()
    }

    object PointlessAnimatableObject {
        var field = 0
    }

    class ValueAnimation(val target: GuiAnimatable, val start: Any, val end: Any) {
        private var lerper: Lerper<Any>
        init {
            var clazz: Class<*> = Any::class.java

            if(start.javaClass == end.javaClass) {
                clazz = start.javaClass
            } else {
                val startClasses = mutableSetOf<Class<*>>()
                var currentClass: Class<*>? = start.javaClass
                while (currentClass != null) {
                    startClasses.add(currentClass)
                    currentClass = currentClass.superclass
                }

                currentClass = end.javaClass
                while (currentClass != null) {
                    if (currentClass in startClasses) {
                        clazz = currentClass
                        break
                    }
                    currentClass = currentClass.superclass
                }
            }
            lerper = LerperHandler.getLerperOrError(clazz) as Lerper<Any>
        }

        var previousValue: Any? = null
        var previousCallback: Any? = null

        fun update(progress: Float) {
            val newValue = lerper.lerp(start, end, progress)

            val currentValue = target.getAnimatableValue()
            if(currentValue != previousValue) { // the value was manually set
                previousCallback = null
            }
            previousValue = currentValue
            val currentCallback = target.getAnimatableCallback()
            if(currentCallback != null && currentCallback != previousCallback) {
                previousCallback = currentCallback
            }
            target.setAnimatableValue(newValue)
        }

        fun complete() {
            previousCallback?.let { target.setAnimatableCallback(it) }
        }
    }
}

interface GuiAnimatable {
    fun getAnimatableValue(): Any?
    fun setAnimatableValue(value: Any?)
    fun getAnimatableCallback(): Any?
    fun setAnimatableCallback(supplier: Any)
}