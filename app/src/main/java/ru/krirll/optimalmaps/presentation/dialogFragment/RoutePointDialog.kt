package ru.krirll.optimalmaps.presentation.dialogFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.RoutePointDialogBinding

class RoutePointDialog(
    private val onCurrentLocationClickListener: (() -> Unit)? = null,
    private val onChooseOnMapClickListener: () -> Unit,
    private val onSearchClickListener: () -> Unit,
    private val onDeleteClickListener: (() -> Unit)? = null
): DialogFragment() {

    private var _viewBinding: RoutePointDialogBinding? = null
    private val viewBinding: RoutePointDialogBinding
        get() = _viewBinding ?: throw RuntimeException("RoutePointDialogBinding == null")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawableResource(R.drawable.round_corner)
        _viewBinding = RoutePointDialogBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCurrentLocationButton()
        initChooseOnMapButton()
        initSearchButton()
        initDeleteButton()
        initCloseButton()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initCurrentLocationButton() {
        if (onCurrentLocationClickListener != null)
            viewBinding.currentLocationDialogButton.setOnClickListener {
                onCurrentLocationClickListener.invoke()
            }
        else
            viewBinding.currentLocationDialogButton.visibility = View.GONE
    }

    private fun initChooseOnMapButton() {
        viewBinding.chooseOnMapDialogButton.setOnClickListener {
            onChooseOnMapClickListener()
        }
    }

    private fun initDeleteButton() {
        if (onDeleteClickListener != null)
        viewBinding.deleteDialogButton.setOnClickListener {
            onDeleteClickListener.invoke()
        }
        else
            viewBinding.deleteDialogButton.visibility = View.GONE
    }

    private fun initSearchButton() {
        viewBinding.searchDialogButton.setOnClickListener {
            onSearchClickListener()
        }
    }

    private fun initCloseButton() {
        viewBinding.closeDialogButton.setOnClickListener {
            dialog?.dismiss()
        }
    }
}