package ru.krirll.optimalmaps.presentation.adapters.diffCallBack

import androidx.recyclerview.widget.DiffUtil
import ru.krirll.optimalmaps.domain.model.RouteItem

class RouteItemDiffCallBack: DiffUtil.ItemCallback<RouteItem>() {
    override fun areItemsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
        return false
    }
}