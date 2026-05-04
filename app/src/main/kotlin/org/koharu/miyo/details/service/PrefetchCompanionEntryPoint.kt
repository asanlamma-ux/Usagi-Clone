package org.koharu.miyo.details.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koharu.miyo.core.prefs.AppSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrefetchCompanionEntryPoint {
	val settings: AppSettings
}
