@file:Suppress("NOTHING_TO_INLINE")
package com.teamwizardry.librarianlib.albedo.base.buffer

import com.teamwizardry.librarianlib.albedo.buffer.RenderBuffer
import com.teamwizardry.librarianlib.albedo.buffer.VertexBuffer
import com.teamwizardry.librarianlib.albedo.shader.StandardUniforms
import com.teamwizardry.librarianlib.albedo.shader.attribute.VertexLayoutElement
import com.teamwizardry.librarianlib.albedo.shader.uniform.Mat4x4Uniform
import com.teamwizardry.librarianlib.albedo.shader.uniform.Uniform
import com.teamwizardry.librarianlib.math.Matrix4d

/**
 * The base class for most render buffers. It provides the basic vertex position functions and standard transform
 * matrices.
 *
 * In the shader, add `#include "liblib-albedo:base/transform.glsl"` to your attribute/uniform block and add
 * `gl_Position = albedo_base_transform();` to your `main()` body. e.g.
 * ```glsl
 * #version 150
 *
 * #include "liblib-albedo:base/transform.glsl"
 *
 * void main() {
 *     gl_Position = albedo_base_transform();
 * }
 * ```
 */
public abstract class BaseRenderBuffer<T : BaseRenderBuffer<T>>(vbo: VertexBuffer) : RenderBuffer(vbo) {
    protected val modelViewMatrix: Mat4x4Uniform = +Uniform.mat4.create("ModelViewMatrix")
    protected val projectionMatrix: Mat4x4Uniform = +Uniform.mat4.create("ProjectionMatrix")

    protected val position: VertexLayoutElement =
        +VertexLayoutElement("Position", VertexLayoutElement.FloatFormat.FLOAT, 3, false)

    override fun setupState() {
        super.setupState()
        StandardUniforms.setModelViewMatrix(modelViewMatrix)
        StandardUniforms.setProjectionMatrix(projectionMatrix)
    }

    public fun pos(x: Double, y: Double, z: Double): T {
        start(position)
        putFloat(x.toFloat())
        putFloat(y.toFloat())
        putFloat(z.toFloat())
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    public fun pos(matrix: Matrix4d, x: Double, y: Double, z: Double): T {
        return this.pos(matrix.transformX(x, y, z), matrix.transformY(x, y, z), matrix.transformZ(x, y, z))
    }

    // overloads for kotlin. The boxing gets inlined away.
    @JvmSynthetic
    public inline fun pos(x: Number, y: Number, z: Number): T {
        return pos(x.toDouble(), y.toDouble(), z.toDouble())
    }
    @JvmSynthetic
    public inline fun pos(matrix: Matrix4d, x: Number, y: Number, z: Number): T {
        return pos(matrix, x.toDouble(), y.toDouble(), z.toDouble())
    }
}