package com.teamwizardry.librarianlib.foundation.tileentity

import com.teamwizardry.librarianlib.foundation.util.SerializationMarker

@Target(AnnotationTarget.FIELD)
@SerializationMarker
annotation class Save

@Target(AnnotationTarget.FIELD)
@SerializationMarker
annotation class Sync