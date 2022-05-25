package ru.krirll.optimalmaps.presentation.adapters.additionalAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.krirll.optimalmaps.databinding.AddNewAdditionalPointBinding
import ru.krirll.optimalmaps.databinding.AdditionalPointItemBinding
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.adapters.diffCallBack.PointItemDiffCallBack
import ru.krirll.optimalmaps.presentation.adapters.viewHolder.PointItemViewHolder

class AdditionalPointsListAdapter :
    ListAdapter<PointItem, PointItemViewHolder>(PointItemDiffCallBack()) {

    private var onEditClickListener: ((PointItem) -> Unit)? = null
    private var onAddClickListener: (() -> Unit)? = null

    fun setOnEditClickListener(func: (PointItem) -> Unit) {
        onEditClickListener = func
    }

    fun setOnAddClickListener(func: () -> Unit) {
        onAddClickListener = func
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            when (viewType) {
                AdditionalPointViewType.ADD_NEW.layout -> AdditionalPointItemBinding.inflate(layoutInflater)
                AdditionalPointViewType.POINT_ITEM.layout -> AddNewAdditionalPointBinding.inflate(layoutInflater)
                else -> throw RuntimeException("Unknown view type $viewType")
            }

        return PointItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PointItemViewHolder, position: Int) {
        when (holder.binding) {
            is AddNewAdditionalPointBinding -> {
                holder.binding.addNewPointButton.setOnClickListener {
                    if (onAddClickListener == null)
                        throw RuntimeException("onAddClickListener == null")
                    else
                        onAddClickListener?.invoke()
                }
            }
            is AdditionalPointItemBinding -> {
                val item = getItem(position)
                holder.binding.editAdd.setOnClickListener {
                    if (onEditClickListener == null)
                        throw RuntimeException("onEditClickListener == null")
                    else
                        onEditClickListener?.invoke(item)
                }
                holder.binding.addText.setText(currentList[position].text)
            }
        }
    }

    override fun getItemCount() = currentList.size + 1

    override fun getItemViewType(position: Int) =
        when (position == currentList.size) {
            true -> AdditionalPointViewType.POINT_ITEM.layout
            false -> AdditionalPointViewType.ADD_NEW.layout
        }

    companion object {
        const val MAX_POOL_SIZE = 10
    }
}