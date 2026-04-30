package org.draken.usagi.core.ui.list

import android.view.View
import android.view.View.OnClickListener
<<<<<<< HEAD
import android.view.View.OnLongClickListener
import androidx.core.util.Function
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import org.draken.usagi.core.ui.OnContextClickListenerCompat
import org.draken.usagi.core.util.ext.setOnContextClickListenerCompat
=======
import android.view.View.OnContextClickListener
import android.view.View.OnLongClickListener
import androidx.core.util.Function
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6

class AdapterDelegateClickListenerAdapter<I, O>(
	private val adapterDelegate: AdapterDelegateViewBindingViewHolder<out I, *>,
	private val clickListener: OnListItemClickListener<O>,
	private val itemMapper: Function<I, O>,
<<<<<<< HEAD
) : OnClickListener, OnLongClickListener, OnContextClickListenerCompat {
=======
) : OnClickListener, OnLongClickListener, OnContextClickListener {
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6

	override fun onClick(v: View) {
		clickListener.onItemClick(mappedItem(), v)
	}

	override fun onLongClick(v: View): Boolean {
		return clickListener.onItemLongClick(mappedItem(), v)
	}

	override fun onContextClick(v: View): Boolean {
		return clickListener.onItemContextClick(mappedItem(), v)
	}

	private fun mappedItem(): O = itemMapper.apply(adapterDelegate.item)

	fun attach() = attach(adapterDelegate.itemView)

	fun attach(itemView: View) {
		itemView.setOnClickListener(this)
		itemView.setOnLongClickListener(this)
<<<<<<< HEAD
		itemView.setOnContextClickListenerCompat(this)
=======
		itemView.setOnContextClickListener(this)
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
	}

	companion object {

		operator fun <T> invoke(
			adapterDelegate: AdapterDelegateViewBindingViewHolder<out T, *>,
			clickListener: OnListItemClickListener<T>
		): AdapterDelegateClickListenerAdapter<T, T> = AdapterDelegateClickListenerAdapter(
			adapterDelegate = adapterDelegate,
			clickListener = clickListener,
			itemMapper = { x -> x },
		)
	}
}
