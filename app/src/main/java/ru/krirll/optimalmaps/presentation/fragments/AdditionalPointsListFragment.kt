package ru.krirll.optimalmaps.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentAdditionalPointsListBinding
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.adapters.additionalAdapter.AdditionalPointViewType
import ru.krirll.optimalmaps.presentation.adapters.additionalAdapter.AdditionalPointsListAdapter
import ru.krirll.optimalmaps.presentation.dialogFragment.RoutePointDialog
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel
import ru.krirll.optimalmaps.presentation.viewModels.RouteConstructorViewModel

class AdditionalPointsListFragment : Fragment() {

    private val routeConstructorViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteConstructorViewModel::class.java]
    }

    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private var listAdapter: AdditionalPointsListAdapter? = null

    private var dialog: RoutePointDialog? = null
    private var isDialogOpened = false

    private var _viewBinding: FragmentAdditionalPointsListBinding? = null
    private val viewBinding: FragmentAdditionalPointsListBinding
        get() = _viewBinding
            ?: throw RuntimeException("FragmentAdditionalPointsListBinding == null")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentAdditionalPointsListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getSavedValues(savedInstanceState)
        initRecyclerView()
        observeMapViewModel()
        observeRouteConstructorViewModel()
    }

    private fun initRecyclerView() {
        viewBinding.additionalPointsRecyclerView.apply {
            //set max pool for point item view type
            recycledViewPool.setMaxRecycledViews(
                AdditionalPointViewType.POINT_ITEM.layout,
                AdditionalPointsListAdapter.MAX_POOL_SIZE
            )
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            //set adapter
            listAdapter = AdditionalPointsListAdapter().apply {
                setOnAddClickListener {
                    createAddDialog()
                }
                setOnEditClickListener { point ->
                    createEditDialog(point)
                }
            }
            adapter = listAdapter
        }
    }

    private fun createEditDialog(point: PointItem) {
        routeConstructorViewModel.setCurrentItemIndex(point)
        showDialog(
            {
                //get point by map
                clearDialog()
                mapViewModel.removePoint()
                routeConstructorViewModel.setPointMode(PointMode.ADDITIONAL_POINT_EDIT)
                findNavController().navigate(R.id.action_additionalPointsListFragment_to_mapPointChoiceFragment)
            },
            {
                //get point by search
                clearDialog()
                routeConstructorViewModel.setPointMode(PointMode.ADDITIONAL_POINT_EDIT)
                findNavController().navigate(R.id.action_additionalPointsListFragment_to_searchFragment)
            },
            {
                clearDialog()
                routeConstructorViewModel.removeAdditionalPoint(point)
                routeConstructorViewModel.removeCurrentItemIndex()
            }
        )
    }

    private fun createAddDialog() {
        showDialog(
            {
                //get point by map
                clearDialog()
                mapViewModel.removePoint()
                routeConstructorViewModel.setPointMode(PointMode.ADDITIONAL_POINT_ADD)
                findNavController().navigate(R.id.action_additionalPointsListFragment_to_mapPointChoiceFragment)
            },
            {
                //get point by search
                clearDialog()
                routeConstructorViewModel.setPointMode(PointMode.ADDITIONAL_POINT_ADD)
                findNavController().navigate(R.id.action_additionalPointsListFragment_to_searchFragment)
            }
        )
    }

    private fun showDialog(
        onChooseOnMapClickListener: () -> Unit,
        onSearchClickListener: () -> Unit,
        onDeleteClickListener: (() -> Unit)? = null
    ) {
        dialog = RoutePointDialog(
            onChooseOnMapClickListener = onChooseOnMapClickListener,
            onSearchClickListener = onSearchClickListener,
            onDeleteClickListener = onDeleteClickListener
        )
        dialog?.isCancelable = false
        dialog?.show(requireActivity().supportFragmentManager, DIALOG)
        isDialogOpened = true
    }

    private fun observeMapViewModel() {
        mapViewModel.point.observe(viewLifecycleOwner) {
            if (it.first != null && it.second != null) {
                when (it.first) /*it.first is a mode of point*/ {
                    PointMode.ADDITIONAL_POINT_ADD -> {
                        routeConstructorViewModel.addAdditionalPoint(it.second!!)
                        mapViewModel.removePoint()
                    }
                    PointMode.ADDITIONAL_POINT_EDIT -> {
                        routeConstructorViewModel.apply {
                            editAdditionalPoint(it.second!!)
                            removeCurrentItemIndex()
                            mapViewModel.removePoint()
                        }
                    }
                    else -> {
                        /*this observe is only for additional points*/
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel.networkError.collect {
                    val message = when (it) {
                        NetworkError.NO_INTERNET -> getString(R.string.no_internet)
                        NetworkError.NO_INFO_ABOUT_POINT -> getString(R.string.no_info_about_point)
                        else -> ""
                    }
                    createSnackbar(message)
                }
            }
        }
    }

    private fun createSnackbar(message: String) {
        view?.let {
            Snackbar.make(
                it,
                message,
                Snackbar.LENGTH_LONG
            ).apply {
                animationMode = Snackbar.ANIMATION_MODE_SLIDE
            }.show()
        }
    }

    private fun observeRouteConstructorViewModel() {
        routeConstructorViewModel.additionalPoints.observe(viewLifecycleOwner) {
            listAdapter?.submitList(it.toList())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routeConstructorViewModel.pointError.collect {
                    createSnackbar(getString(R.string.double_additional_point))
                }
            }
        }
    }

    override fun onPause() {
        if (dialog?.dialog == null)
            isDialogOpened = false
        dialog?.dismiss()
        super.onPause()
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    private fun clearDialog() {
        dialog?.dismiss()
        isDialogOpened = false
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isDialogOpened = savedInstanceState.getBoolean(IS_DIALOG_OPENED)
            if (isDialogOpened) {
                val currentItem = routeConstructorViewModel.getCurrentItem()
                if (currentItem == null)
                    createAddDialog()
                else
                    createEditDialog(currentItem)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_DIALOG_OPENED, isDialogOpened)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val IS_DIALOG_OPENED = "IS_DIALOG_OPENED"
        private const val DIALOG = "DIALOG"
    }
}