package com.teamwizardry.librarianlib.features.particle

import com.teamwizardry.librarianlib.core.LibrarianLog
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.math.interpolate.InterpFunction
import com.teamwizardry.librarianlib.features.math.interpolate.InterpUnion
import com.teamwizardry.librarianlib.features.math.interpolate.StaticInterp
import com.teamwizardry.librarianlib.features.math.interpolate.position.InterpBezier3D
import com.teamwizardry.librarianlib.features.math.interpolate.position.InterpCircle
import com.teamwizardry.librarianlib.features.math.interpolate.position.InterpHelix
import com.teamwizardry.librarianlib.features.math.interpolate.position.InterpLine
import com.teamwizardry.librarianlib.features.particle.functions.*
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer

/**
 * Create a particle builder
 *
 * Particle builders are used to easily create particles and allow you to pass the
 * particle definition to various methods such as [ParticleSpawner.spawn]
 *
 * "Top quality glitter. Can't find glitter better than this, in fact, I'm really good friends with
 * the glitter people. Best pals. Can't get any better than this glitter" -Trump
 */
class ParticleBuilder(private var lifetime: Int) {
    // region Animation Start/End
    /**
     * Set the starting point of the animation (as a unit float).
     */
    fun setAnimStart(value: Float): ParticleBuilder {
        animStart = value
        return this
    }

    /**
     * Set the overflow amount of the animation (as a unit float).
     */
    fun setAnimEnd(value: Float): ParticleBuilder {
        animEnd = value
        return this
    }

    // endregion

    // region Render function and related interps

    // region Render function
    /**
     * Set the render function for the particle
     *
     * @see RenderFunctionBasic
     */
    fun setRenderFunction(value: RenderFunction): ParticleBuilder {
        renderFunc = value
        return this
    }

    /**
     * Shortcut for creating a basic render function
     */
    fun setRender(value: ResourceLocation): ParticleBuilder {
        renderFunc = RenderFunctionBasic(value, false)
        return this
    }

    /**
     * Shortcut for creating a basic render function
     */
    fun setRenderNormalLayer(value: ResourceLocation): ParticleBuilder {
        renderFunc = RenderFunctionBasic(value, true)
        return this
    }
    // endregion

    // region Color function

    /**
     * Set the color function for the particle.
     *
     * @see InterpColorComponents
     * @see InterpColorHSV
     */
    fun setColorFunction(value: InterpFunction<Color>): ParticleBuilder {
        colorFunc = value
        return this
    }

    /**
     * Shortcut for creating a static color
     */
    fun setColor(value: Color): ParticleBuilder {
        colorFunc = StaticInterp(value)
        return this
    }

    // endregion

    // region Alpha function
    /**
     * Set the alpha function for the particle.
     */
    fun setAlphaFunction(value: InterpFunction<Float>): ParticleBuilder {
        alphaFunc = value
        return this
    }

    /**
     * Shortcut for creating a static color
     */
    fun setAlpha(value: Float): ParticleBuilder {
        alphaFunc = StaticInterp(value)
        return this
    }

    // region shortcuts

    // TODO Add alpha functions and alpha function shortcuts

    // endregion

    // endregion

    // region Scale function
    /**
     * Set the scale function for the particle.
     */
    fun setScaleFunction(value: InterpFunction<Float>): ParticleBuilder {
        scaleFunc = value
        return this
    }

    /**
     * Shortcut for a static scale
     */
    fun setScale(value: Float): ParticleBuilder {
        scaleFunc = StaticInterp(value)
        return this
    }
    // endregion

    fun setRotation(value: Float): ParticleBuilder {
        rotationFunc = StaticInterp(value)
        return this
    }

    fun setRotation(interp: InterpFunction<Float>): ParticleBuilder {
        rotationFunc = interp
        return this
    }

    // endregion

    // region Position function

    /**
     * Set the position function for the particle.
     *
     * Positions are relative to the position specified in the [build] method
     *
     * @see StaticInterp
     * @see InterpLine
     * @see InterpHelix
     * @see InterpCircle
     * @see InterpBezier3D
     * @see InterpUnion
     */
    fun setPositionFunction(value: InterpFunction<Vec3d>): ParticleBuilder {
        positionEnabled = true
        positionFunc = value
        return this
    }

    // region Position enabled
    /**
     * Disable the position function
     */
    fun disablePosition(): ParticleBuilder {
        positionEnabled = false
        return this
    }

    /**
     * Enable the position function
     */
    fun enablePosition(): ParticleBuilder {
        positionEnabled = true
        return this
    }

    /**
     * Set the position function enabled flag
     */
    fun setPositionEnabled(value: Boolean): ParticleBuilder {
        positionEnabled = value
        return this
    }
    // endregion

    // region Movement Mode
    /**
     * Set the movement mode for the particle
     *
     * @see EnumMovementMode
     */
    fun setMovementMode(value: EnumMovementMode): ParticleBuilder {
        movementMode = value
        return this
    }
    // endregion

    // endregion

    // region Motion stuff

    // region Motion
    /**
     * Sets the tick
     *
     * Each tick while the particle is colliding with a block, it's tick is multiplied by this vector
     */
    fun setMotion(value: Vec3d): ParticleBuilder {
        motion = value
        return this
    }

    /**
     * Adds to the tick
     *
     * Each tick while the particle is colliding with a block, it's tick is multiplied by this vector
     */
    fun addMotion(value: Vec3d): ParticleBuilder {
        motion += value
        return this
    }
    // endregion

    // region Modifiers

    /**
     * Sets the collision flag.
     */
    fun setCollision(value: Boolean): ParticleBuilder {
        canCollide = value
        return this
    }

    /**
     * Sets the acceleration
     *
     * Each tick this value is added to the particle's tick
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun setAcceleration(value: Vec3d): ParticleBuilder {
        acceleration = value
        motionCalculation = true
        return this
    }

    /**
     * Adds to the acceleration
     *
     * Each tick this value is added to the particle's tick
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun addAcceleration(value: Vec3d): ParticleBuilder {
        acceleration += value
        motionCalculation = true
        return this
    }

    /**
     * Sets the deceleration
     *
     * Each tick the particle's tick is multiplied by this vector
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun setDeceleration(value: Vec3d): ParticleBuilder {
        deceleration = value
        motionCalculation = true
        return this
    }

    /**
     * Adds to the deceleration
     *
     * Each tick the particle's tick is multiplied by this vector
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun addDeceleration(value: Vec3d): ParticleBuilder {
        deceleration += value
        motionCalculation = true
        return this
    }

    /**
     * Sets the friction
     *
     * Each tick while the particle is colliding with a block, it's tick is multiplied by this vector
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun setFriction(value: Vec3d): ParticleBuilder {
        friction = value
        motionCalculation = true
        return this
    }

    /**
     * Adds to the friction
     *
     * Each tick while the particle is colliding with a block, it's tick is multiplied by this vector
     *
     * (calling this method enables standard particle tick calculations)
     */
    fun addFriction(value: Vec3d): ParticleBuilder {
        friction += value
        motionCalculation = true
        return this
    }
    // endregion

    fun setTick(value: TickFunction): ParticleBuilder {
        tickFunction = value
        return this
    }

    // region Motion enabled
    /**
     * Sets the tick enabled flag
     *
     * The tick enabled flag controls whether the particle uses the position function or traditional tick mechanics
     */
    fun setMotionCalculationEnabled(value: Boolean): ParticleBuilder {
        motionCalculation = value
        return this
    }

    /**
     * Sets the tick enabled flag to true
     *
     * The tick enabled flag controls whether the particle uses the position function or traditional tick mechanics
     */
    fun enableMotionCalculation(): ParticleBuilder {
        motionCalculation = true
        return this
    }

    /**
     * Sets the tick enabled flag to false
     *
     * The tick enabled flag controls whether the particle uses the position function or traditional tick mechanics
     */
    fun disableMotionCalculation(): ParticleBuilder {
        motionCalculation = false
        return this
    }
    // endregion

    // region Motion Function

    // see tick function property

    // endregion

    // endregion

    // region Plain ol' position stuff

    // region Position offset

    /**
     * An offset to add to the position passed in to [build()]
     */
    fun setPositionOffset(value: Vec3d): ParticleBuilder {
        positionOffset = value
        return this
    }

    /**
     * An offset to add to the position passed in to [build()]
     */
    fun addPositionOffset(value: Vec3d): ParticleBuilder {
        positionOffset += value
        return this
    }

    // endregion

    // endregion

    // region Randomization

    /**
     * Set jitter amount.
     *
     * Each tick there is a 1 in [chance] chance of `rand(-1 to 1) *` each of [value]'s components being added
     * to the particle's tick.
     */
    fun setJitter(chance: Int, value: Vec3d): ParticleBuilder {
        jitterMagnitude = value
        jitterChance = 1f / chance
        return this
    }

    // region Randomization lambdas
    /**
     * Clear the randomization lambdas
     */
    fun clearRandomizationLambdas(): ParticleBuilder {
        randomizationLambdas.clear()
        return this
    }

    /**
     * Add a randomization lambda
     */
    fun addRandomizationLambda(r: Consumer<ParticleBuilder>): ParticleBuilder {
        randomizationLambdas.add(r)
        return this
    }

    /**
     * Add a randomization lambda
     */
    fun addRandomizationLambda(r: (ParticleBuilder) -> Unit): ParticleBuilder {
        randomizationLambdas.add(Consumer<ParticleBuilder>(r))
        return this
    }
    // endregion

    // region Default randomizations

    /**
     * Sets the default randomization flag
     *
     * The default randomization flag controls whether to randomize some of the varibles automatically when building
     * a particle. (by default it randomizes Position, Lifetime, Anim Start, Anim End, and Motion)
     */
    fun setRandomEnabled(value: Boolean): ParticleBuilder {
        defaultRandomizations = value
        return this
    }

    /**
     * Sets the default randomization flag to true
     *
     * The default randomization flag controls whether to randomize some of the varibles automatically when building
     * a particle. (by default it randomizes Position, Lifetime, Anim Start, Anim End, and Motion)
     */
    fun enableRandom(): ParticleBuilder {
        defaultRandomizations = true
        return this
    }

    /**
     * Sets the default randomization flag to false
     *
     * The default randomization flag controls whether to randomize some of the varibles automatically when building
     * a particle. (by default it randomizes Position, Lifetime, Anim Start, Anim End, and Motion)
     */
    fun disableRandom(): ParticleBuilder {
        defaultRandomizations = false
        return this
    }

    // endregion

    // endregion

    // anim start/end
    var animStart: Float = 0f
        private set
    var animEnd: Float = 1f
        private set

    // render stuff
    var renderFunc: RenderFunction? = null
        private set
    var colorFunc: InterpFunction<Color>? = null
        private set
    var alphaFunc: InterpFunction<Float> = StaticInterp(1f)
        private set
    var scaleFunc: InterpFunction<Float> = StaticInterp(1f)
        private set
    var rotationFunc: InterpFunction<Float> = StaticInterp(0f)
        private set

    // pos func
    var positionEnabled: Boolean = false
        private set
    var positionFunc: InterpFunction<Vec3d>? = null
        private set
    var movementMode: EnumMovementMode = EnumMovementMode.IN_DIRECTION
        private set

    // tick stuff
    var motionCalculation: Boolean = false
        private set
    var motion: Vec3d = Vec3d.ZERO
        private set
    var acceleration: Vec3d = vec(0.0, -0.01, 0.0)
        private set
    var deceleration: Vec3d = vec(0.95, 0.95, 0.95)
        private set
    var friction: Vec3d = vec(0.9, 1.0, 0.9)
        private set
    var tickFunction: TickFunction? = null
        private set

    // plain ol' position
    var positionOffset: Vec3d = Vec3d.ZERO
        private set
    var canCollide: Boolean = false
        private set
    var canBounce: Boolean = false
    var bounceMagnitude: Double = 0.9

    // randomization
    var jitterMagnitude: Vec3d = vec(0.05, 0.05, 0.05)
        private set
    var jitterChance: Float = 0.0f
        private set
    var randomizationLambdas: MutableList<Consumer<ParticleBuilder>> = mutableListOf()
        private set
    var defaultRandomizations = true
        private set
    var posRandMultiplier = 0.1f
    var motionRandMultiplier = 0.01f
    var lifetimeRandMultiplier = 0.1
    var animRandMultiplier = 0.05

    /**
     * Set the number of ticks the particle will live
     */
    fun setLifetime(value: Int): ParticleBuilder {
        lifetime = value
        return this
    }

    /**
     * Build an instance of the particle.
     *
     * Returns null and prints a warning if the color function or render function are null.
     */
    fun build(world: World, pos: Vec3d): ParticleBase? {
        randomizationLambdas.forEach { it.accept(this) }

        var pos_ = pos + positionOffset
        var lifetime_ = lifetime
        var animStart_ = animStart
        var animEnd_ = animEnd
        var motion_ = motion

        if (defaultRandomizations) {

            pos_ += vec(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * posRandMultiplier,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * posRandMultiplier,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * posRandMultiplier
            )

            motion_ += vec(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandMultiplier,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandMultiplier,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandMultiplier
            )

            lifetime_ += ((ThreadLocalRandom.current().nextDouble() - 0.5) * lifetime * lifetimeRandMultiplier).toInt()

            animStart_ += ((ThreadLocalRandom.current().nextDouble() - 0.5) * animRandMultiplier).toFloat()
            animEnd_ += ((ThreadLocalRandom.current().nextDouble() - 0.5) * animRandMultiplier).toFloat()
        }

        val renderFunc_ = renderFunc

        if (renderFunc_ == null) {
            LibrarianLog.warn("Particle render function was null!!")
            return null
        }

        return ParticleBase(world, pos_, lifetime_, animStart_, animEnd_,
                positionFunc ?: StaticInterp(Vec3d.ZERO), colorFunc ?: StaticInterp(Color.WHITE), alphaFunc,
                renderFunc_, tickFunction, movementMode, scaleFunc, rotationFunc,
                motionCalculation, positionEnabled, canCollide, motion_, acceleration, deceleration, friction, jitterMagnitude, jitterChance,
                canBounce, bounceMagnitude)
    }

    /**
     * Clones this builder.
     */
    fun clone(): ParticleBuilder {
        val v = ParticleBuilder(lifetime)

        cloneTo(v)

        return v
    }

    /**
     * Copies all the values from this builder to the other builder.
     *
     * All properies point to the same objects (not a deep copy) except for the randomizationLambdas list. The list in
     * the other object is cleared and the lambdas from this object are copied in. Modifying one list won't modify the
     * other one.
     */
    fun cloneTo(v: ParticleBuilder) {
        v.animStart = this.animStart
        v.animEnd = this.animEnd

        // render stuff
        v.renderFunc = this.renderFunc
        v.colorFunc = this.colorFunc
        v.alphaFunc = this.alphaFunc
        v.scaleFunc = this.scaleFunc

        // pos func
        v.positionEnabled = this.positionEnabled
        v.positionFunc = this.positionFunc
        v.movementMode = this.movementMode

        // tick stuff
        v.motionCalculation = this.motionCalculation
        v.motion = this.motion
        v.acceleration = this.acceleration
        v.deceleration = this.deceleration
        v.friction = this.friction

        // plain ol' position
        v.positionOffset = this.positionOffset
        v.canCollide = this.canCollide
        v.canBounce = this.canBounce
        v.bounceMagnitude = this.bounceMagnitude

        // randomization
        v.jitterMagnitude = this.jitterMagnitude
        v.jitterChance = this.jitterChance

        v.randomizationLambdas.clear()
        v.randomizationLambdas.addAll(this.randomizationLambdas)

        v.defaultRandomizations = this.defaultRandomizations
    }
}
