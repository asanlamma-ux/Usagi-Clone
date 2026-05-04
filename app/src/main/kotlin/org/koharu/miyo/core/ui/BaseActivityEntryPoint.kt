package org.koharu.miyo.core.ui

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koharu.miyo.core.exceptions.resolve.ExceptionResolver
import org.koharu.miyo.core.prefs.AppSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseActivityEntryPoint {

	val settings: AppSettings

	val exceptionResolverFactory: ExceptionResolver.Factory
}
