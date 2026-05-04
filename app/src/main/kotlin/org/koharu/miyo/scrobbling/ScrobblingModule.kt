package org.koharu.miyo.scrobbling

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import okhttp3.OkHttpClient
import org.koharu.miyo.BuildConfig
import org.koharu.miyo.core.db.MangaDatabase
import org.koharu.miyo.core.network.BaseHttpClient
import org.koharu.miyo.core.network.CurlLoggingInterceptor
import org.koharu.miyo.scrobbling.anilist.data.AniListAuthenticator
import org.koharu.miyo.scrobbling.anilist.data.AniListInterceptor
import org.koharu.miyo.scrobbling.anilist.domain.AniListScrobbler
import org.koharu.miyo.scrobbling.common.data.ScrobblerStorage
import org.koharu.miyo.scrobbling.common.domain.Scrobbler
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblerService
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblerType
import org.koharu.miyo.scrobbling.kitsu.data.KitsuAuthenticator
import org.koharu.miyo.scrobbling.kitsu.data.KitsuInterceptor
import org.koharu.miyo.scrobbling.kitsu.data.KitsuRepository
import org.koharu.miyo.scrobbling.kitsu.domain.KitsuScrobbler
import org.koharu.miyo.scrobbling.mal.data.MALAuthenticator
import org.koharu.miyo.scrobbling.mal.data.MALInterceptor
import org.koharu.miyo.scrobbling.mal.domain.MALScrobbler
import org.koharu.miyo.scrobbling.shikimori.data.ShikimoriAuthenticator
import org.koharu.miyo.scrobbling.shikimori.data.ShikimoriInterceptor
import org.koharu.miyo.scrobbling.shikimori.domain.ShikimoriScrobbler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScrobblingModule {

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.SHIKIMORI)
	fun provideShikimoriHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: ShikimoriAuthenticator,
		@ScrobblerType(ScrobblerService.SHIKIMORI) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(ShikimoriInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MAL)
	fun provideMALHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: MALAuthenticator,
		@ScrobblerType(ScrobblerService.MAL) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(MALInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.ANILIST)
	fun provideAniListHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: AniListAuthenticator,
		@ScrobblerType(ScrobblerService.ANILIST) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(AniListInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	fun provideKitsuRepository(
		@ApplicationContext context: Context,
		@ScrobblerType(ScrobblerService.KITSU) storage: ScrobblerStorage,
		database: MangaDatabase,
		authenticator: KitsuAuthenticator,
		@BaseHttpClient baseHttpClient: OkHttpClient,
	): KitsuRepository {
		val okHttp = baseHttpClient.newBuilder().apply {
			authenticator(authenticator)
			addInterceptor(KitsuInterceptor(storage))
			if (BuildConfig.DEBUG) {
				addInterceptor(CurlLoggingInterceptor())
			}
		}.build()
		return KitsuRepository(context, okHttp, storage, database)
	}

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.ANILIST)
	fun provideAniListStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.ANILIST)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.SHIKIMORI)
	fun provideShikimoriStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.SHIKIMORI)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MAL)
	fun provideMALStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.MAL)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.KITSU)
	fun provideKitsuStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.KITSU)

	@Provides
	@ElementsIntoSet
	fun provideScrobblers(
		shikimoriScrobbler: ShikimoriScrobbler,
		aniListScrobbler: AniListScrobbler,
		malScrobbler: MALScrobbler,
		kitsuScrobbler: KitsuScrobbler
	): Set<@JvmSuppressWildcards Scrobbler> = setOf(shikimoriScrobbler, aniListScrobbler, malScrobbler, kitsuScrobbler)
}
