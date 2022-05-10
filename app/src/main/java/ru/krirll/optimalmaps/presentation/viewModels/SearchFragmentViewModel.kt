package ru.krirll.optimalmaps.presentation.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.entities.GetPointsByQueryUseCase
import ru.krirll.optimalmaps.domain.entities.LoadSearchHistoryUseCase
import ru.krirll.optimalmaps.domain.entities.SavePointItemUseCase
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.NetworkError

class SearchFragmentViewModel(
    app: Application
) : AndroidViewModel(app) {

    //channel for sending search queries from SearchFragment
    private val searchQueryChannel by lazy { Channel<String>(Channel.CONFLATED) }

    init {
        //launch function, which will be collecting search queries
        viewModelScope.launch { getSearchResult() }
    }

    //init repository and use cases
    private val repository: PointRepositoryImpl = PointRepositoryImpl(app)
    private val getSearchByQueryUseCase: GetPointsByQueryUseCase = GetPointsByQueryUseCase(repository)
    private val loadSearchHistoryUseCase: LoadSearchHistoryUseCase = LoadSearchHistoryUseCase(repository)
    private val savePointItemUseCase: SavePointItemUseCase = SavePointItemUseCase(repository)

    private var locale: String = ""

    //list for api result
    private var _pointItemList = MutableLiveData<List<PointItem>>()
    val pointItemList: MutableLiveData<List<PointItem>>
        get() = _pointItemList

    //list for local result
    private var _pointHistoryList = MutableLiveData<List<PointItem>>()
    val pointHistoryList: MutableLiveData<List<PointItem>>
        get() = _pointHistoryList

    //channel for sending errors
    private var _networkError = Channel<NetworkError>()
    val networkError
        get() = _networkError.receiveAsFlow()

    private var lastQuery: String = "" //for saving last query

    fun setLocale(str: String) {
        locale = str
    }

    fun loadSearchHistory() {
        if (_pointHistoryList.value == null)
            _pointHistoryList =
                loadSearchHistoryUseCase.invoke() as MutableLiveData<List<PointItem>>
        else
            _pointHistoryList.postValue(_pointHistoryList.value)
    }

    fun savePointItem(item: PointItem) {
        viewModelScope.launch {
            _pointItemList.value?.let { list ->
                if (list.isNotEmpty())
                    savePointItemUseCase.invoke(list.first { it == item })
            }
        }
    }

    private fun onError(error: NetworkError) {
        viewModelScope.launch {
            _networkError.send(error)
        }
    }

    private suspend fun getSearchResult() {
        //get all search queries
        searchQueryChannel.receiveAsFlow().collect { query ->
            delay(300)
            if (searchQueryChannel.isEmpty) {
                if (query != "" && query != lastQuery) {
                    val result = getSearchByQueryUseCase.invoke(
                        query,
                        locale,
                        { onError(NetworkError.INCORRECT_QUERY) },
                        { onError(NetworkError.NO_INTERNET) }
                    )
                    lastQuery = query
                    _pointItemList.value = result
                } else
                    loadSearchHistory()
                delay(700)
            }
        }
    }

    fun saveLastSearchQuery(query: String) {
        viewModelScope.launch {
            //send last query to channel
            searchQueryChannel.send(query)
        }
    }
}