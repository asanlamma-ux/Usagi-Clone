package org.koharu.miyo.list.ui.adapter

import org.koharu.miyo.list.domain.ListFilterOption

interface QuickFilterClickListener {

	fun onFilterOptionClick(option: ListFilterOption)
}
