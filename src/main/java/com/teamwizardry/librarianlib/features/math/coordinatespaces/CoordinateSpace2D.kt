package com.teamwizardry.librarianlib.features.math.coordinatespaces

import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.math.Matrix3
import com.teamwizardry.librarianlib.features.math.Rect2d
import com.teamwizardry.librarianlib.features.math.Vec2d
import java.util.Collections
import java.util.IdentityHashMap

@JvmDefaultWithoutCompatibility
interface CoordinateSpace2D {
    /**
     * The parent coordinate space. All points in this space are transformed relative to its parent.
     */
    val parentSpace: CoordinateSpace2D?
    /**
     * The "normal" matrix.
     *
     * If the child space is embedded with an offset (x,y) within its parent, this will be `Matrix3().transform(x,y)`
     *
     * Counterintuitively, this matrix converts points from the child space to the parent space.
     */
    val matrix: Matrix3
    /**
     * The inverse of [matrix], or a best guess. The best way to get this is to apply the same transforms that were
     * used to create [matrix] while inverting both their values and order. This allows you to have an elegant failure
     * state when scaling by zero.
     */
    val inverseMatrix: Matrix3

    /**
     * Create a matrix that, when applied to a point in this coordinate space, returns the corresponding point in the
     * [other] coordinate space.
     */
    fun conversionMatrixTo(other: CoordinateSpace2D): Matrix3 {
        if(other === this) return Matrix3.identity
        if(other === this.parentSpace) return this.matrix.copy()
        if(other.parentSpace === this) return other.inverseMatrix.copy()

        val lca = lowestCommonAncestor(other) ?: throw UnrelatedCoordinateSpaceException(this, other)

        if(lca === other) return this.matrixToParent(other)
        if(lca === this) return other.matrixFromParent(this)

        val matrix = Matrix3()
        matrix *= other.matrixFromParent(lca)
        matrix *= this.matrixToParent(lca)
        return matrix
    }

    /**
     * Create a matrix that, when applied to a point in the [other] coordinate space, returns the corresponding point
     * in the this coordinate space.
     */
    fun conversionMatrixFrom(other: CoordinateSpace2D) = other.conversionMatrixTo(this)

    /**
     * Converts a point in this coordinate space into the corresponding point in the [other] coordinate space
     */
    fun convertPointTo(point: Vec2d, other: CoordinateSpace2D) = conversionMatrixTo(other) * point

    /**
     * Converts a point in the [other] coordinate space into the corresponding point in this coordinate space
     */
    fun convertPointFrom(point: Vec2d, other: CoordinateSpace2D) = other.convertPointTo(point, this)

    /**
     * Converts a rect in this coordinate space to the _**smallest bounding rectangle**_ around it in the [other]
     * coordinate space
     *
     * ## NOTE!
     *
     * This operation _**IS NOT REVERSIBLE**_. If there is any rotation returned rect will not equal the passed rect,
     * instead it will _contain_ it.
     */
    fun convertRectTo(rect: Rect2d, other: CoordinateSpace2D): Rect2d {
        var min = rect.min
        var max = rect.max
        var minmax = vec(min.x, max.y)
        var maxmin = vec(max.x, min.y)

        val matrix = conversionMatrixTo(other)
        min = matrix * min
        max = matrix * max
        minmax = matrix * minmax
        maxmin = matrix * maxmin

        val pos = Vec2d.min(
            Vec2d.min(min, max),
            Vec2d.min(minmax, maxmin)
        )
        val size = Vec2d.max(
            Vec2d.max(min, max),
            Vec2d.max(minmax, maxmin)
        ) - pos

        return Rect2d(pos, size)
    }

    /**
     * Converts a rect in the [other] coordinate space to the _**smallest bounding rectangle**_ around it in this
     * coordinate space
     *
     * ## NOTE!
     *
     * This operation _**IS NOT REVERSIBLE**_. If there is any rotation the returned rect will not equal the passed
     * rect, instead it will _contain_ it.
     */
    fun convertRectFrom(rect: Rect2d, other: CoordinateSpace2D) = other.convertRectTo(rect, this)

    /**
     * Converts a point in this coordinate space into the corresponding point in the parent coordinate space.
     *
     * If this space has no parent, this method returns the original point.
     */
    fun convertPointToParent(point: Vec2d) = parentSpace?.let { convertPointTo(point, it) } ?: point

    /**
     * Converts a point in the parent coordinate space into the corresponding point in this coordinate space.
     *
     * If this space has no parent, this method returns the original point.
     */
    fun convertPointFromParent(point: Vec2d) = parentSpace?.let { convertPointFrom(point, it) } ?: point

    /**
     * Converts a rect in this coordinate space to the _**smallest bounding rectangle**_ around it in the [other]
     * coordinate space
     *
     * If this space has no parent, this method returns the original rect.
     *
     * ## NOTE!
     *
     * This operation _**IS NOT REVERSIBLE**_. If there is any rotation returned rect will not equal the passed rect,
     * instead it will _contain_ it.
     */
    fun convertRectToParent(rect: Rect2d) = parentSpace?.let { convertRectTo(rect, it) } ?: rect

    /**
     * Converts a rect in the [other] coordinate space to the _**smallest bounding rectangle**_ around it in this
     * coordinate space
     *
     * If this space has no parent, this method returns the original rect.
     *
     * ## NOTE!
     *
     * This operation _**IS NOT REVERSIBLE**_. If there is any rotation the returned rect will not equal the passed
     * rect, instead it will _contain_ it.
     */
    fun convertRectFromParent(rect: Rect2d) = parentSpace?.let { convertRectFrom(rect, it) } ?: rect

    private fun lowestCommonAncestor(other: CoordinateSpace2D): CoordinateSpace2D? {
        // check for straight-line relationships next (doing both in parallel because that minimizes time
        // when the distance is short)
        var thisAncestor = this.parentSpace
        var otherAncestor = other.parentSpace
        while(thisAncestor != null || otherAncestor != null) {
            if(thisAncestor === other) return other
            if(otherAncestor === this) return this
            thisAncestor = thisAncestor?.parentSpace
            otherAncestor = otherAncestor?.parentSpace
        }

        val ancestors: MutableSet<CoordinateSpace2D> = Collections.newSetFromMap<CoordinateSpace2D>(IdentityHashMap())
        var ancestor = this.parentSpace
        while(ancestor != null) {
            ancestors.add(ancestor)
            ancestor = ancestor.parentSpace
        }
        ancestor = other.parentSpace
        while(ancestor != null) {
            if(ancestor in ancestors) return ancestor
            ancestor = ancestor.parentSpace
        }

        return null
    }

    /**
     * The matrix to get our coordinates back to [other]'s space. [other] is one of our ancestors
     */
    private fun matrixToParent(parent: CoordinateSpace2D): Matrix3 {
        val ancestors = mutableListOf<CoordinateSpace2D>()
        var space: CoordinateSpace2D = this
        while(space !== parent) {
            ancestors.add(space)
            space = space.parentSpace!!
        }

        val matrix = Matrix3()
        ancestors.reversed().forEach {
            matrix *= it.matrix
        }
        return matrix
    }

    /**
     * The matrix to get [other]'s coordinates down to our space. [other] is one of our ancestors
     */
    private fun matrixFromParent(other: CoordinateSpace2D): Matrix3 {
        val ancestors = mutableListOf<CoordinateSpace2D>()
        var space: CoordinateSpace2D = this
        while(space !== other) {
            ancestors.add(space)
            space = space.parentSpace!!
        }

        val matrix = Matrix3()
        ancestors.forEach {
            matrix *= it.inverseMatrix
        }
        return matrix
    }
}

class UnrelatedCoordinateSpaceException(val space1: CoordinateSpace2D, val space2: CoordinateSpace2D): RuntimeException()
