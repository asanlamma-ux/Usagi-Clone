package org.koharu.miyo.core.nav

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koharu.miyo.core.prefs.AppSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppRouterEntryPoint {

	val settings: AppSettings
}
