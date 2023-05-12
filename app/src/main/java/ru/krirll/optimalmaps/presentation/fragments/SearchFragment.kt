package ru.krirll.optimalmaps.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentSearchBinding
import ru.krirll.optimalmaps.presentation.adapters.searchAdapter.PointItemViewType
import ru.krirll.optimalmaps.presentation.adapters.searchAdapter.PointListAdapter
import ru.krirll.optimalmaps.presentation.enums.Locale
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel
import ru.krirll.optimalmaps.presentation.viewModels.RouteConstructorViewModel
import ru.krirll.optimalmaps.presentation.viewModels.SearchFragmentViewModel

class SearchFragment : Fragment() {

    private var _viewBinding: FragmentSearchBinding? = null
    private val viewBinding: FragmentSearchBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentSearchBinding == null")

    private val searchViewModel by lazy {
        ViewModelProvider(this)[SearchFragmentViewModel::class.java]
    }

    //get current MapFragmentViewModel for setting point
    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private val routeConstructorViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteConstructorViewModel::class.java]
    }

    private var pointListAdapter: PointListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentSearchBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSearchView()
        initRecyclerView()
        setViewModelLocale()
        getSearchHistory()
        observeViewModel()
    }

    private fun setViewModelLocale() {
        val currentLocale = Locale.getLocale(LocaleListCompat.getDefault()[0]!!.toLanguageTag())
        searchViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun initRecyclerView() {
        viewBinding.historyRecyclerView.apply {
            //set max pool size for all types of view in RecyclerView
            recycledViewPool.setMaxRecycledViews(
                PointItemViewType.DEFAULT_POINT.layout,
                PointListAdapter.MAX_POOL_SIZE
            )
            recycledViewPool.setMaxRecycledViews(
                PointItemViewType.HISTORY_POINT.layout,
                PointListAdapter.MAX_POOL_SIZE
            )
            pointListAdapter = PointListAdapter().apply {
                setOnPointItemClickListener { item, index ->
                    val mode = routeConstructorViewModel.getPointMode()
                    if (mode == null) {
                        //set point in mapViewModel
                        mapViewModel.setPoint(item, PointMode.DEFAULT_POINT)
                    } else {
                        //set point in RouteConstructorViewModel
                        when (mode) {
                            PointMode.START_POINT -> routeConstructorViewModel.setStartPoint(
                                item,
                                false
                            )
                            PointMode.ADDITIONAL_POINT_ADD -> routeConstructorViewModel.addAdditionalPoint(item)
                            PointMode.ADDITIONAL_POINT_EDIT -> routeConstructorViewModel.editAdditionalPoint(item)
                            PointMode.FINISH_POINT -> routeConstructorViewModel.setFinishPoint(item)
                            else -> { /*nothing*/ }
                        }
                        routeConstructorViewModel.removePointMode()
                    }
                    searchViewModel.savePointItem(index)
                    findNavController().popBackStack()
                }
            }
            adapter = pointListAdapter
        }
    }

    private fun observeViewModel() {
        //observe api service result
        searchViewModel.pointItemList.observe(viewLifecycleOwner) {
            pointListAdapter?.submitList(it)
        }
        //observe local data result
        searchViewModel.pointHistoryList.observe(viewLifecycleOwner) {
            if (viewBinding.searchView.query.toString() == "")
                pointListAdapter?.submitList(it)
        }
        //collect errors from searchViewModel
        lifecycleScope.launchWhenStarted {
            searchViewModel.networkError.collect {
                val message = when (it) {
                    NetworkError.INCORRECT_QUERY -> getString(R.string.no_search_result)
                    NetworkError.NO_INTERNET -> getString(R.string.no_internet)
                    else -> ""
                }
                createSnackbar(message)
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

    private fun getSearchHistory() {
        if (viewBinding.searchView.query.toString() == "")
            searchViewModel.loadSearchHistory()
    }

    //init search view and open keyboard
    private fun initSearchView() {
        viewBinding.searchView.apply {
            //open by default
            isIconified = false
            //set search query listener
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query == "")
                        createSnackbar(getString(R.string.empty_query))
                    return true
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    query?.let { searchViewModel.saveLastSearchQuery(it) }
                    return true
                }

            })
            //close this fragment and go back to the MapFragment
            setOnCloseListener { findNavController().popBackStack() }
        }
    }

    //clean view binding
    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

}