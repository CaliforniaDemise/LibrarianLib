package com.teamwizardry.librarianlib.features.particlesystem

import com.teamwizardry.librarianlib.features.kotlin.whileNonNull
import com.teamwizardry.librarianlib.features.math.Vec2d
import com.teamwizardry.librarianlib.features.particlesystem.bindings.ConstantBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.StoredBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.VariableBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.DepthSortModule
import net.minecraft.util.math.Vec3d
import org.magicwerk.brownies.collections.GapList
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A system of particles with similar behavior.
 *
 * Particle systems define particle behavior through "modules", of which there are multiple kinds:
 *
 * * [updateModules] are called in every tick for each particle. They are called in sequence for each particle, meaning
 * any state set in one can be safely used in the next.
 * * [globalUpdateModules] are called every tick and passed the full list of particles. They are generally used for depth
 * sorting (see [DepthSortModule]) however they are useful for anything that requires the full particle list in one call.
 * * [renderPrepModules] are similar to [updateModules] but are called before each particle is rendered. They can be
 * used to initialize [VariableBinding]s so a complex computation does not have to be repeated in several of the render
 * module's input bindings.
 * * [renderModules] are in charge of actually rendering the particles into the world. You can add multiple and each
 * will render in order.
 *
 * The various module lists may be safely modified as long as the system isn't currently ticking or rendering, however
 * once any particles have been created it is no longer safe to create new bindings with [bind].
 *
 * When creating particle systems it is highly recommended to spawn enough to cause a moderate amount of lag and run
 * sampling to find which hotspots are slowing down the game. It is often surprising what small things have a large
 * impact.
 *
 * After creating a particle system, [addToGame] will add the system the game for rendering and updates directly. Calling
 * [removeFromGame] will, as the name implies, remove the particle system from the game, at which point it will no
 * longer render or receive updates
 */
abstract class ParticleSystem {

    init {
        addToGame()
    }

    /**
     * The modules that are called every tick for each particle. They are called in sequence for each particle, meaning
     * temporary state set in one module can be safely used in the subsequent modules.
     */
    val updateModules: MutableList<ParticleUpdateModule> = mutableListOf()
    /**
     * The modules that are called once at the end of each tick and passed the entire list of particles. Often used for
     * depth sorting with the [DepthSortModule]
     */
    val globalUpdateModules: MutableList<ParticleGlobalUpdateModule> = mutableListOf()
    /**
     * The modules that are called before each particle is rendered. They are similar to [updateModules] and are often
     * used to optimize computations that are reused in multiple places (say, calculating a normal vector) by storing
     * them in a local variable or [VariableBinding]. These will be called individually by each renderer, so they cannot
     * be used to optimize computations that are reused between renderers.
     */
    val renderPrepModules: MutableList<ParticleUpdateModule> = mutableListOf()
    /**
     * The modules that handle the rendering of the particles in this system. They are each passed the full list of
     * particles, and are free to use that information however they please. They are called in sequence, meaning any
     * depth sorting will only work within the confines of a single module. For example, depth sorting and having two
     * translucent renderer modules would result in the first module rendering, then the translucent particles in
     * the second module being occluded by any polygons the first renders.
     */
    val renderModules: MutableList<ParticleRenderModule> = mutableListOf()

    /**
     * The maximum number of particles in the particle reuse pool. The reuse pool is used to reduce the amount of memory
     * churn by retaining dead particle arrays and reusing them in [addParticle] as opposed to creating new ones each
     * time.
     */
    var poolSize = 1000

    private var systemInitialized: Boolean = false

    internal val queuedAdditions = ConcurrentLinkedQueue<DoubleArray>()
    internal val shouldQueue = AtomicBoolean(false)
    internal val particles: MutableList<DoubleArray> = GapList<DoubleArray>()
    private val particlePool = ArrayDeque<DoubleArray>(poolSize)

    /**
     * The built-in binding for particle lifetime. If the array in [age] is >= the array in [lifetime] the particle will
     * be removed during the next frame or update.
     */
    lateinit var lifetime: StoredBinding
        private set
    /**
     * The built-in binding for particle age. Upon spawning the array in this binding is initialized to 0, and every
     * tick thereafter its array is incremented. If the array in [age] is >= the array in [lifetime] the particle will
     * be removed during the next frame or update.
     */
    lateinit var age: StoredBinding
        private set

    /**
     * The required length of the particle arrays.
     */
    var fieldCount = 0
        private set

    var manuallyRender = false

    private var canBind = false

    /**
     * Configures the particle system, binding values and building module lists. This is called both when the system
     * is first added to the game and when resource packs are reloaded. The bindings and module lists are cleared
     * beforehand, so any existing bindings should be considered invalid and must be recreated.
     *
     * This method is the only valid place for bindings to be created with [bind].
     */
    abstract fun configure()

    /**
     * Creates a new [StoredBinding] of the specified size and allocates space for it at the end of the particle array,
     * increasing [fieldCount] to reflect the change.
     *
     * @throws IllegalStateException if called outside of [configure]
     */
    fun bind(size: Int): StoredBinding {
        if (!canBind)
            throw IllegalStateException("It is no longer safe to create new bindings, particles have already been " +
                    "created based on current field count.")
        val binding = StoredBinding(fieldCount, size)
        fieldCount += size
        return binding
    }

    /**
     * Creates a new [ConstantBinding] that is INDEPENDENT of the system and CANNOT BE MODIFIED NOR MANIPULATED.
     * This creates READ ONLY bindings that are STATIC and UNCHANGING.
     * This is very useful for specifying values that you never want changed in your particles or never want them or
     * have no intention of having them be configurable.
     *
     * Use [bind] if you want to make a array that changes in your particle like a colorPrimary binding that changes array
     * in a transition, or a velocity that changes.
     *
     * Really ANYTHING you want that your particle will change, you have
     * to bind. If not, you can use this method to reduce load on the particle's storage and for incredibly useful
     * convenience.
     */
    fun constant(vararg args: Double): ConstantBinding {
        return ConstantBinding(*args)
    }

    /**
     * Convenience method for [constant].
     */
    fun constant(color: Color) = constant(color.red / 255.0, color.green / 255.0, color.blue / 255.0, color.alpha / 255.0)

    /**
     * Convenience method for [constant].
     */
    fun constant(vec: Vec3d) = constant(vec.x, vec.y, vec.z)

    /**
     * Convenience method for [constant].
     */
    fun constant(vec: Vec2d) = constant(vec.x, vec.y)

    /**
     * Convenience method for [constant].
     */
    fun constant(i: Int) = constant(i.toDouble())

    /**
     * Convenience method for [constant].
     */
    fun constant(f: Float) = constant(f.toDouble())

    /**
     * Convenience method for [constant].
     * Renamed with a D at the end because it conflicts with [constant] otherwise.
     */
    fun constantD(d: Double) = constant(d)

    /**
     * Creates a particle and initializes it with the passed values. Any missing values will be set to 0.
     *
     * [params] should be populated with the values for each binding the order they were bound. For example, if
     * a `position` binding was created with a size of 3, then a `colorPrimary` with a size of 4, and then a `size` with a
     * size of 1, [params] should be populated in the following order: `x, y, z, r, g, b, a, size`. It is recommended
     * that a subclass be created with a method that populates [params] based upon more meaningfully named arguments.
     * An example based on the aforementioned particle would be:
     *
     * ```kotlin
     * double[] spawn(double lifetime, Vec3d position, Color colorPrimary, double size) {
     *     return this.addParticle(lifetime,
     *             position.x, position.y, position.z,
     *             colorPrimary.red/255.0, colorPrimary.green/255.0, colorPrimary.blue/255.0, colorPrimary.alpha/255.0,
     *             size
     *     )
     * }
     * ```
     *
     * @param lifetime the lifetime of the particle in ticks
     * @param params an array of values to initialize the particle array with.
     */
    fun addParticle(lifetime: Double, vararg params: Double): DoubleArray {
        if (!systemInitialized) {
            reload()
            systemInitialized = true
        }

        val particle = particlePool.poll() ?: DoubleArray(fieldCount)
        particle[0] = lifetime
        particle[1] = 0.0
        params.copyInto(particle, 2)

        if (shouldQueue.get()) {
            queuedAdditions.add(particle)
        } else {
            particles.add(particle)
        }

        for (i in 0 until updateModules.size) {
            updateModules[i].init(particle)
        }
        return particle
    }

    /**
     * Adds the particle system to the game for rendering and updates.
     */
    fun addToGame() {
        GameParticleSystems.add(this)
    }

    /**
     * Removes the particle system from the game, meaning it will no longer render or receive updates.
     */
    fun removeFromGame() {
        GameParticleSystems.remove(this)
    }

    /**
     * Reloads the particle system. This involves clearing the particle and module lists, then calling [configure] to
     * re-bind values and rebuild the module lists.
     */
    fun reload() {
        // this.particles.clear()
        // this.particlePool.clear()
        this.updateModules.clear()
        this.globalUpdateModules.clear()
        this.renderPrepModules.clear()
        this.renderModules.clear()

        this.particles.clear()
        this.particlePool.clear()
        this.canBind = true
        this.fieldCount = 0

        this.lifetime = bind(1)
        this.age = bind(1)
        this.configure()

        this.canBind = false
    }

    fun update() {
        shouldQueue.set(true)
        whileNonNull({ queuedAdditions.poll() }) { particle ->
            particles.add(particle)
        }
        val iter = particles.iterator()

        for (particle in iter) {

            this.lifetime.load(particle)
            this.age.load(particle)

            val lifetime = this.lifetime.getValue(0)
            val age = this.age.getValue(0)
            // round age up and lifetime down so age is never greater than lifetime
            if (ceil(age) >= floor(lifetime)) {
                iter.remove()
                if (particlePool.size < poolSize)
                    particlePool.push(particle)
                continue
            }

            this.age.setValue(0, age + 1)
            this.age.store(particle)
            update(particle)
        }

        for (i in 0 until globalUpdateModules.size) {
            globalUpdateModules[i].update(particles)
        }
        shouldQueue.set(false)
    }

    private fun update(particle: DoubleArray) {
        for (i in 0 until updateModules.size) {
            updateModules[i].update(particle)
        }
    }

    fun render() {
        shouldQueue.set(true)
        whileNonNull({ queuedAdditions.poll() }) { particle ->
            particles.add(particle)
        }
        for (i in 0 until renderModules.size) {
            renderModules[i].render(particles, renderPrepModules)
        }
        shouldQueue.set(false)
    }
}
