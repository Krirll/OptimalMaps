package ru.krirll.optimalmaps.presentation.adapters.searchAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.krirll.optimalmaps.databinding.PointDefaultItemBinding
import ru.krirll.optimalmaps.databinding.PointHistoryItemBinding
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.adapters.diffCallBack.PointItemDiffCallBack
import ru.krirll.optimalmaps.presentation.adapters.viewHolder.PointItemViewHolder

class PointListAdapter : ListAdapter<PointItem, PointItemViewHolder>(PointItemDiffCallBack()) {

    private var onPointItemClickListener: ((PointItem, Int) -> Unit)? = null

    fun setOnPointItemClickListener(function: (PointItem, Int) -> Unit) {
        onPointItemClickListener = function
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            when (viewType) {
                PointItemViewType.DEFAULT_POINT.layout -> PointDefaultItemBinding.inflate(layoutInflater)
                PointItemViewType.HISTORY_POINT.layout -> PointHistoryItemBinding.inflate(layoutInflater)
                else -> throw RuntimeException("Unknown view type $viewType")
            }

        return PointItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PointItemViewHolder, position: Int) {
        val point = getItem(position)
        when (holder.binding) {
            is PointDefaultItemBinding -> {
                holder.binding.textPoint.apply {
                    text = point.text
                    setOnClickListener {
                        if (onPointItemClickListener == null)
                            throw RuntimeException("onPointItemClickListener == null")
                        else
                            onPointItemClickListener?.invoke(point, position)
                    }
                }
            }
            is PointHistoryItemBinding -> {
                holder.binding.textPoint.apply {
                    text = point.text
                    setOnClickListener {
                        if (onPointItemClickListener == null)
                            throw RuntimeException("onPointItemClickListener == null")
                        else
                            onPointItemClickListener?.invoke(point, position)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        when (getItem(position).isHistorySearch) {
            true -> PointItemViewType.HISTORY_POINT.layout
            false -> PointItemViewType.DEFAULT_POINT.layout
        }

    companion object {

        const val MAX_POOL_SIZE = 20
    }
}