package org.koharu.miyo.details.domain

import org.koharu.miyo.core.util.LocaleStringComparator
import org.koharu.miyo.details.ui.model.MangaBranch

class BranchComparator : Comparator<MangaBranch> {

	private val delegate = LocaleStringComparator()

	override fun compare(o1: MangaBranch, o2: MangaBranch): Int = delegate.compare(o1.name, o2.name)
}
