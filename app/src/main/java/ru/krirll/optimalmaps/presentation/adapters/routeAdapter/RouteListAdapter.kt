package ru.krirll.optimalmaps.presentation.adapters.routeAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.krirll.optimalmaps.databinding.RouteItemBinding
import ru.krirll.optimalmaps.domain.model.RouteItem
import ru.krirll.optimalmaps.presentation.adapters.diffCallBack.RouteItemDiffCallBack
import ru.krirll.optimalmaps.presentation.adapters.viewHolder.RouteItemViewHolder

class RouteListAdapter: ListAdapter<RouteItem, RouteItemViewHolder>(RouteItemDiffCallBack()) {

    private var onRouteItemClickListener: ((RouteItem) -> Unit)? = null

    fun setOnRouteItemClickListener(function: (RouteItem) -> Unit) {
        onRouteItemClickListener = function
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteItemViewHolder =
        RouteItemViewHolder(RouteItemBinding.inflate(LayoutInflater.from(parent.context)))


    override fun onBindViewHolder(holder: RouteItemViewHolder, position: Int) {
        holder.binding.text.text = currentList[position].points
        val item = getItem(position)
        if (onRouteItemClickListener == null)
            throw RuntimeException("onRouteItemClickListener == null")
        else
            holder.binding.text.setOnClickListener { onRouteItemClickListener?.invoke(item) }
    }
}