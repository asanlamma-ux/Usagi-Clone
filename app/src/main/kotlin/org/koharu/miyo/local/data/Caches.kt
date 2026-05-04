package org.koharu.miyo.local.data

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PageCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FaviconCache
