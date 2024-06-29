package com.teamwizardry.librarianlib.features.facade.provided.pastry

/**
 * Used for highly volatile APIs in Pastry.
 *
 * The entirety of Pastry is experimental on some level, but including experimental annotations literally everywhere
 * would get tiresome pretty quick, so much of it is unannotated. However, some components are so volatile that they
 * necessitate a compile-time check, so that's what this is for.
 */
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalPastryAPI