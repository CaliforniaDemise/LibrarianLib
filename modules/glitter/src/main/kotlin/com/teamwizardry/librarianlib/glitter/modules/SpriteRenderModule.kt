package com.teamwizardry.librarianlib.glitter.modules

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.teamwizardry.librarianlib.albedo.buffer.Primitive
import com.teamwizardry.librarianlib.core.mixin.IMatrix3f
import com.teamwizardry.librarianlib.core.mixin.IMatrix4f
import com.teamwizardry.librarianlib.core.rendering.BlendMode
import com.teamwizardry.librarianlib.core.rendering.DefaultRenderPhases
import com.teamwizardry.librarianlib.core.rendering.SimpleRenderLayers
import com.teamwizardry.librarianlib.core.util.Client
import com.teamwizardry.librarianlib.core.util.kotlin.builder
import com.teamwizardry.librarianlib.core.util.kotlin.normal
import com.teamwizardry.librarianlib.core.util.kotlin.unreachable
import com.teamwizardry.librarianlib.core.util.mixinCast
import com.teamwizardry.librarianlib.glitter.GlitterLightingCache
import com.teamwizardry.librarianlib.glitter.ParticleRenderModule
import com.teamwizardry.librarianlib.glitter.ParticleUpdateModule
import com.teamwizardry.librarianlib.glitter.ReadParticleBinding
import com.teamwizardry.librarianlib.glitter.bindings.ConstantBinding
import com.teamwizardry.librarianlib.math.floorInt
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3f
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.function.Consumer

/**
 * The bread-and-butter render module, a simple billboarded sprite.
 *
 * Particles are drawn as dynamically sized/colored sprites that are either billboarded or with an arbitrary facing
 * defined by [facingVector] (if any of facingVector's components are NaN the player's look vector will be used).
 * The particles are drawn as rectangles [size] blocks to a side and centered on the the particle's position.
 * One thing of note is that for some particle effects, particularly ones that should look consistent,
 * disabling interpolation by passing the current position for both [previousPosition] and [position] can make
 * the particles rock solid in their positions as opposed to jittering about slightly.
 */
public class SpriteRenderModule private constructor(
    /**
     * The [SpriteRenderOptions] configuring the rendering of the particles.
     */
    public var renderOptions: SpriteRenderOptions,
    /**
     * The current position of the particle
     */
    public val position: ReadParticleBinding,
    /**
     * The position of the particle last tick, used to interpolate between ticks
     */
    public val previousPosition: ReadParticleBinding?,
    /**
     * The OpenGL color of the particle
     */
    public val color: ReadParticleBinding,
    /**
     * The width and height of the particle in meters. If this is a 1D binding the value is used for both width and
     * height, if it's a 2D binding the two axes are used as the width and height. Note that this does not affect UV
     * coordinates, so if you set this to non-square and have a square texture it will be distorted.
     */
    public val size: ReadParticleBinding,
    /**
     * If present, an artificial facing vector used instead of the player's look vector. This vector _does not need
     * to be normalized._
     */
    public val facingVector: ReadParticleBinding?,
    /**
     * The alpha multiplier for the color. Defaults to 1 if not present.
     */
    public val alphaMultiplier: ReadParticleBinding,
    /**
     * The size of the sprite sheet (must be a power of 2)
     */
    public val spriteSheetSize: Int,
    /**
     * The sprite index (indexed left-to-right, top-to-bottom)
     */
    public val spriteIndex: ReadParticleBinding,
    /**
     * If present, an artificial direction for the particle's "up" axis. This vector _does not need to be normalized._
     */
    public val upVector: ReadParticleBinding?,
    /**
     * If present, an artificial U/V texture size. When used in combination with sprite sheets, this will be a size
     * within the particle's specific sprite.
     */
    public val uvSize: ReadParticleBinding?,
    /**
     * If present, an offset to apply to the UV coordinates of the sprite. When used in combination with sprite sheets,
     * this will be an offset within the particle's specific sprite.
     */
    public val uvOffset: ReadParticleBinding?,
) : ParticleRenderModule {

    @Suppress("LocalVariableName")
    override fun renderDirect(
        context: WorldRenderContext,
        particles: List<DoubleArray>,
        prepModules: List<ParticleUpdateModule>
    ) {
        if(particles.isEmpty())
            return

        val stack = MatrixStack()
        val viewPos = Client.minecraft.gameRenderer.camera.pos
        stack.translate(-viewPos.x, -viewPos.y, -viewPos.z)

        val modelViewMatrix = stack.peek().model

        val normalMatrix = mixinCast<IMatrix3f>(context.matrixStack().peek().normal)
        val nm00 = normalMatrix.m00
        val nm01 = normalMatrix.m01
        val nm02 = normalMatrix.m02
        val nm10 = normalMatrix.m10
        val nm11 = normalMatrix.m11
        val nm12 = normalMatrix.m12
        val nm20 = normalMatrix.m20
        val nm21 = normalMatrix.m21
        val nm22 = normalMatrix.m22

        val transformMatrix = mixinCast<IMatrix4f>(modelViewMatrix)
        val tm00 = transformMatrix.m00
        val tm01 = transformMatrix.m01
        val tm02 = transformMatrix.m02
        val tm03 = transformMatrix.m03
        val tm10 = transformMatrix.m10
        val tm11 = transformMatrix.m11
        val tm12 = transformMatrix.m12
        val tm13 = transformMatrix.m13
        val tm20 = transformMatrix.m20
        val tm21 = transformMatrix.m21
        val tm22 = transformMatrix.m22
        val tm23 = transformMatrix.m23

        val camera = Client.minecraft.gameRenderer.camera

        val cameraX = camera.pos.x
        val cameraY = camera.pos.y
        val cameraZ = camera.pos.z

        val lookRightVec = Vec3f(-1f, 0f, 0f)
        val lookUpVec = Vec3f(0f, 1f, 0f)
        val lookVec = Vec3f(0f, 0f, -1f)

        lookRightVec.rotate(camera.rotation)
        lookUpVec.rotate(camera.rotation)
        lookVec.rotate(camera.rotation)

        val spriteSize = 1f / spriteSheetSize
        val spriteIndexMask = spriteSheetSize - 1
        val spriteSheetBits = MathHelper.log2(spriteSheetSize)

        val widthSizeIndex: Int = if (this.size.contents.size == 2) 1 else 0
        val computeNormal = renderOptions.diffuseLight

        val renderBuffer = SpriteRenderBuffer.SHARED
        renderBuffer.worldMatrix.set(modelViewMatrix)
        renderBuffer.texture.set(renderOptions.sprite)

        for(particle in particles) {
            for (i in prepModules.indices) {
                prepModules[i].update(particle)
            }

            position.load(particle)
            var posX = position.contents[0]
            var posY = position.contents[1]
            var posZ = position.contents[2]
            if (previousPosition != null) {
                previousPosition.load(particle)
                posX = Client.worldTime.interp(previousPosition.contents[0], posX)
                posY = Client.worldTime.interp(previousPosition.contents[1], posY)
                posZ = Client.worldTime.interp(previousPosition.contents[2], posZ)
            }

            var rightX = lookRightVec.x.toDouble()
            var rightY = lookRightVec.y.toDouble()
            var rightZ = lookRightVec.z.toDouble()

            var upX = lookUpVec.x.toDouble()
            var upY = lookUpVec.y.toDouble()
            var upZ = lookUpVec.z.toDouble()

            var normalX = lookVec.x.toDouble()
            var normalY = lookVec.y.toDouble()
            var normalZ = lookVec.z.toDouble()

            // compute local axes
            if (facingVector != null || upVector != null) {
                var facingX = cameraX - posX
                var facingY = cameraY - posY
                var facingZ = cameraZ - posZ
                if (facingVector != null) {
                    facingVector.load(particle)
                    facingX = facingVector.contents[0]
                    facingY = facingVector.contents[1]
                    facingZ = facingVector.contents[2]
                }

                // cross product formula reference: c = a x b
                // cX = aY * bZ - aZ * bY
                // cY = aZ * bX - aX * bZ
                // cZ = aX * bY - aY * bX
                if (upVector == null) {
                    // if we have a facing vector and no up vector, compute the up and right vectors using the Y axis
                    // as a reference
                    if (!facingX.isNaN() && !facingY.isNaN() && !facingZ.isNaN()) {
                        if (facingX == 0.0 && facingZ == 0.0) {
                            rightX = 1.0
                            rightY = 0.0
                            rightZ = 0.0

                            upX = 0.0
                            upY = 0.0
                            upZ = -1.0
                        } else {

                            // note: these cross products don't care about the input normalization. The output magnitude
                            // will be mangled whether we normalize the inputs or not, so there's no point doing the
                            // excess calculations

                            // compute the rightward vector using the cross product `facing x (0, 1, 0)`
                            // the zeros can be simplified away, leaving us with essentially a 2d perpendicular vector
                            // on the xz plane (x, z) -> (z, -x)
                            rightX = facingZ
                            rightY = 0.0
                            rightZ = -facingX
                            // the Y axis will always be zero here, so we can do a 2d normalization
                            val rightInvLength = MathHelper.fastInverseSqrt(rightX * rightX + rightZ * rightZ)
                            rightX *= rightInvLength
                            rightZ *= rightInvLength

                            // compute the upward vector using the cross product `facing x right`
                            // we can simplify some of the factors since `rightY` will always be zero
                            upX = facingY * rightZ
                            upY = facingZ * rightX - facingX * rightZ
                            upZ = -facingY * rightX
                            val upInvLength = MathHelper.fastInverseSqrt(upX * upX + upY * upY + upZ * upZ)
                            upX *= upInvLength
                            upY *= upInvLength
                            upZ *= upInvLength
                        }
                    }
                } else {
                    // if we have an up vector, compute the right vector based upon it and the facing vector
                    // if no custom facing vector is specified, the facingXYZ variables will be toward the camera
                    upVector.load(particle)
                    upX = upVector.contents[0]
                    upY = upVector.contents[1]
                    upZ = upVector.contents[2]
                    val upInvLength = MathHelper.fastInverseSqrt(upX * upX + upY * upY + upZ * upZ)
                    upX *= upInvLength
                    upY *= upInvLength
                    upZ *= upInvLength

                    // compute right axis using the cross product `facing x up`
                    rightX = facingY * upZ - facingZ * upY
                    rightY = facingZ * upX - facingX * upZ
                    rightZ = facingX * upY - facingY * upX
                    val rightInvLength = MathHelper.fastInverseSqrt(rightX * rightX + rightY * rightY + rightZ * rightZ)
                    rightX *= -rightInvLength
                    rightY *= -rightInvLength
                    rightZ *= -rightInvLength
                }

                if(computeNormal) {
                    // compute the normal using the cross product `right x up`
                    // two unit vectors at right angles will produce a unit vector output
                    normalX = rightY * upZ - rightZ * upY
                    normalY = rightZ * upX - rightX * upZ
                    normalZ = rightX * upY - rightY * upX
                }

                // both of those calculations spit out directions in world space. We need to transform those back into
                // the rendering space
                val _rightX = rightX
                val _rightY = rightY
                val _rightZ = rightZ
                rightX = tm00 * _rightX + tm01 * _rightY + tm02 * _rightZ
                rightY = tm10 * _rightX + tm11 * _rightY + tm12 * _rightZ
                rightZ = tm20 * _rightX + tm21 * _rightY + tm22 * _rightZ

                val _upX = upX
                val _upY = upY
                val _upZ = upZ
                upX = tm00 * _upX + tm01 * _upY + tm02 * _upZ
                upY = tm10 * _upX + tm11 * _upY + tm12 * _upZ
                upZ = tm20 * _upX + tm21 * _upY + tm22 * _upZ

                val _normalX = normalX
                val _normalY = normalY
                val _normalZ = normalZ
                normalX = nm00 * _normalX + nm01 * _normalY + nm02 * _normalZ
                normalY = nm10 * _normalX + nm11 * _normalY + nm12 * _normalZ
                normalZ = nm20 * _normalX + nm21 * _normalY + nm22 * _normalZ
            }

            size.load(particle)
            val width = size.contents[0] / 2
            val height = size.contents[widthSizeIndex] / 2

            val localRightX = rightX * width
            val localRightY = rightY * width
            val localRightZ = rightZ * width
            val localUpX = upX * height
            val localUpY = upY * height
            val localUpZ = upZ * height

            val x = tm00 * posX + tm01 * posY + tm02 * posZ + tm03 * 1
            val y = tm10 * posX + tm11 * posY + tm12 * posZ + tm13 * 1
            val z = tm20 * posX + tm21 * posY + tm22 * posZ + tm23 * 1

            color.load(particle)
            alphaMultiplier.load(particle)
            val r = color.contents[0].toFloat()
            val g = color.contents[1].toFloat()
            val b = color.contents[2].toFloat()
            val a = color.contents[3].toFloat() * alphaMultiplier.contents[0].toFloat()

            var minU = 0f
            var minV = 0f
            var uSize = 1f
            var vSize = 1f

            if (spriteSheetSize > 1) {
                spriteIndex.load(particle)
                val index = spriteIndex.contents[0].toInt()
                val uIndex = index and spriteIndexMask
                val vIndex = index ushr spriteSheetBits
                minU = spriteSize * uIndex
                minV = spriteSize * vIndex
                uSize = spriteSize
                vSize = spriteSize
            }

            if (uvOffset != null) {
                uvOffset.load(particle)
                minU += uSize * uvOffset.contents[0].toFloat()
                minV += vSize * uvOffset.contents[1].toFloat()
            }
            if (uvSize != null) {
                uvSize.load(particle)
                uSize *= uvSize.contents[0].toFloat()
                vSize *= uvSize.contents[1].toFloat()
            }

            val maxU = minU + uSize
            val maxV = minV + vSize

            val lightmap = if(renderOptions.worldLight) {
                val blockX = floorInt(posX)
                val blockY = floorInt(posY)
                val blockZ = floorInt(posZ)
                GlitterLightingCache.getCombinedLight(blockX, blockY, blockZ)
            } else {
                0
            }

            renderBuffer.point(x, y, z)
                .up(upX, upY, upZ)
                .right(rightX, rightY, rightZ)
                .color(r, g, b, a)
                .size(width, height)
                .tex(minU, minV, maxU, maxV)
                .endVertex()

        }

        GlStateManager._disableCull()
        renderBuffer.draw(Primitive.POINTS)
        GlStateManager._enableCull()
    }

    public companion object {
        /**
         * @param renderOptions The [SpriteRenderOptions] configuring the rendering of the particles
         * @param position The position binding for the particle (3D)
         */
        @JvmStatic
        public fun build(renderOptions: SpriteRenderOptions, position: ReadParticleBinding): Builder {
            return Builder(renderOptions, position)
        }

        /**
         * @param sprite The sprite texture to use. This defaults to normal blending and writing depth
         * @param position The position binding for the particle (3D)
         */
        @JvmStatic
        public fun build(sprite: Identifier, position: ReadParticleBinding): Builder {
            return Builder(SpriteRenderOptions.build(sprite).build(), position)
        }
    }

    /**
     * @param renderOptions The [SpriteRenderOptions] configuring the rendering of the particles
     * @param position The position binding for the particle (3D)
     */
    public class Builder(private val renderOptions: SpriteRenderOptions, private val position: ReadParticleBinding) {
        init {
            position.require(3)
        }

        private var previousPosition: ReadParticleBinding? = null
        private var color: ReadParticleBinding = ConstantBinding(1.0, 1.0, 1.0, 1.0)
        private var size: ReadParticleBinding = ConstantBinding(1.0)
        private var facingVector: ReadParticleBinding? = null
        private var upVector: ReadParticleBinding? = null
        private var alphaMultiplier: ReadParticleBinding = ConstantBinding(1.0)
        private var spriteSheetSize: Int = 1
        private var spriteIndex: ReadParticleBinding = ConstantBinding(0.0)
        private var uvSize: ReadParticleBinding? = null
        private var uvOffset: ReadParticleBinding? = null

        /**
         * The position of the particle last tick, used to interpolate between ticks
         */
        public fun previousPosition(value: ReadParticleBinding): Builder = builder {
            value.require(3)
            previousPosition = value
        }

        /**
         * The tint color of the particle
         */
        public fun color(value: ReadParticleBinding): Builder = builder {
            value.require(4)
            color = value
        }

        /**
         * The tint color of the particle
         */
        public fun color(value: Color): Builder = builder {
            color = ConstantBinding(value.red / 255.0, value.green / 255.0, value.blue / 255.0, value.alpha / 255.0)
        }

        /**
         * The tint color of the particle
         */
        public fun color(red: Double, green: Double, blue: Double, alpha: Double): Builder = builder {
            color = ConstantBinding(red, green, blue, alpha)
        }

        /**
         * The width and height of the particle in meters. If this is a 1D binding the value is used for both width and
         * height, if it's a 2D binding the two axes are used as the width and height. Note that this does not affect UV
         * coordinates, so if you set this to non-square and have a square texture it will be distorted.
         */
        public fun size(value: ReadParticleBinding): Builder = builder {
            value.require(1, 2)
            size = value
        }

        /**
         * The size of the particle in meters.
         */
        public fun size(value: Double): Builder = builder {
            size = ConstantBinding(value)
        }

        /**
         * The width and height of the particle in meters. Note that this does not affect UV coordinates, so if you set
         * this to non-square and have a square texture it will be distorted.
         */
        public fun size(width: Double, height: Double): Builder = builder {
            size = ConstantBinding(width, height)
        }

        /**
         * If present, an artificial facing vector used instead of the player's look vector. This vector _does not need
         * to be normalized._
         */
        public fun facingVector(value: ReadParticleBinding): Builder = builder {
            value.require(3)
            facingVector = value
        }

        /**
         * If present, an artificial direction for the particle's "up" axis. This vector _does not need to be normalized._
         */
        public fun upVector(value: ReadParticleBinding): Builder = builder {
            value.require(3)
            upVector = value
        }

        /**
         * The alpha multiplier for the color. Defaults to 1 if not present.
         */
        public fun alphaMultiplier(value: ReadParticleBinding): Builder = builder {
            value.require(1)
            alphaMultiplier = value
        }

        /**
         * Configures the sprite sheet.
         *
         * @param size The size of the sprite sheet (must be a power of 2)
         * @param index A binding for the sprite index (indexed left-to-right, top-to-bottom)
         */
        public fun spriteSheet(size: Int, index: ReadParticleBinding): Builder = builder {
            if (size and (size - 1) != 0) {
                throw IllegalArgumentException("Sprite sheet size $size is not a power of 2")
            }
            spriteSheetSize = size
            index.require(1)
            spriteIndex = index
        }

        /**
         * If present, an artificial U/V texture size. When used in combination with sprite sheets, this will be a size
         * within the particle's specific sprite.
         */
        public fun uvSize(value: ReadParticleBinding): Builder = builder {
            value.require(2)
            uvSize = value
        }

        /**
         * If present, an offset to apply to the UV coordinates of the sprite. When used in combination with sprite sheets,
         * this will be an offset within the particle's specific sprite.
         */
        public fun uvOffset(value: ReadParticleBinding): Builder = builder {
            value.require(2)
            uvOffset = value
        }

        public fun build(): SpriteRenderModule {
            return SpriteRenderModule(
                renderOptions,
                position,
                previousPosition,
                color,
                size,
                facingVector,
                alphaMultiplier,
                spriteSheetSize,
                spriteIndex,
                upVector,
                uvSize,
                uvOffset
            )
        }
    }
}

public class SpriteRenderOptions private constructor(
    public val sprite: Identifier,
    public val cull: Boolean,
    public val worldLight: Boolean,
    public val diffuseLight: Boolean,
) {
    public companion object {
        @JvmStatic
        public fun build(sprite: Identifier): Builder {
            return Builder(sprite)
        }
    }

    public class Builder(private val sprite: Identifier) {
        private var blendMode: BlendMode? = BlendMode.NORMAL
        private var writeDepth: Boolean = true
        private var blur: Boolean = false

        private var cull: Boolean = true
        private var worldLight: Boolean = false
        private var diffuseLight: Boolean = false

        /**
         * Disable OpenGL blending
         */
        public fun disableBlending(): Builder = builder {
            blendMode = null
        }

        /**
         * The OpenGL source/dest enableBlend factors.
         */
        public fun blendMode(value: BlendMode): Builder = builder {
            blendMode = value
        }

        /**
         * Use additive blending
         */
        public fun additiveBlending(): Builder = blendMode(BlendMode.ADDITIVE)

        /**
         * Whether to write to the depth buffer
         */
        public fun writeDepth(value: Boolean): Builder = builder {
            writeDepth = value
        }

        /**
         * Whether to blur the texture (i.e. interpolate colors vs. use nearest-neighbor scaling)
         */
        public fun blur(value: Boolean): Builder = builder {
            blur = value
        }

        /**
         * Whether to enable backface culling (defaults to true)
         */
        public fun cull(value: Boolean): Builder = builder {
            cull = value
        }

        /**
         * Whether to apply world light (i.e. block and sky light) to the particles. Note: At large scales this may have
         * some performance impact.
         */
        public fun worldLight(value: Boolean): Builder = builder {
            worldLight = value
        }

        /**
         * Whether to apply diffuse lighting (e.g. particles appear darker from below) to the particles. Note: At large
         * scales this may have some performance impact.
         */
        public fun diffuseLight(value: Boolean): Builder = builder {
            diffuseLight = value
        }


        public fun build(): SpriteRenderOptions {
//            val renderState = RenderLayer.MultiPhaseParameters.builder()
//                .texture(RenderPhase.Texture(sprite, blur, false))
//                .alpha(DefaultRenderPhases.ONE_TENTH_ALPHA)
//
//            if (cull || diffuseLight) {
//                // when using diffuse lighting we always cull. If culling is disabled we render two quads with different normals
//                renderState.cull(DefaultRenderPhases.ENABLE_CULLING)
//            } else {
//                renderState.cull(DefaultRenderPhases.DISABLE_CULLING)
//            }
//
//            blendMode?.let { blendMode ->
//                renderState.transparency(RenderPhase.Transparency("particle_transparency", {
//                    RenderSystem.enableBlend()
//                    blendMode.glApply()
//                }, {
//                    RenderSystem.disableBlend()
//                    RenderSystem.defaultBlendFunc()
//                }))
//            }
//
//            if (worldLight) renderState.lightmap(DefaultRenderPhases.ENABLE_LIGHTMAP)
//            if (diffuseLight) renderState.diffuseLighting(DefaultRenderPhases.ENABLE_DIFFUSE_LIGHTING)
//            if (!writeDepth) renderState.writeMaskState(DefaultRenderPhases.COLOR_MASK)
//
//            extraConfig?.accept(renderState)
//
//            val renderType = SimpleRenderLayers.makeType(
//                "particle_type",
//                vertexFormatForLighting(worldLight, diffuseLight),
//                GL11.GL_QUADS,
//                256,
//                false,
//                false,
//                renderState.build(false)
//            )

            return SpriteRenderOptions(sprite, cull, worldLight, diffuseLight)
        }
    }
}
