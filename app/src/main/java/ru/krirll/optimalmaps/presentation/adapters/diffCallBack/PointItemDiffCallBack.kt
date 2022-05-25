package ru.krirll.optimalmaps.presentation.adapters.searchAdapter

import androidx.recyclerview.widget.DiffUtil
import ru.krirll.optimalmaps.domain.model.PointItem

//comparison of list items
class PointItemDiffCallBack : DiffUtil.ItemCallback<PointItem>() {

    override fun areItemsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
        return oldItem.text == newItem.text
    }

    override fun areContentsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
        return oldItem == newItem
    }
}