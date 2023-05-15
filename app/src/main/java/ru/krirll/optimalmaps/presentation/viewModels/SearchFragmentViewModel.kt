package ru.krirll.optimalmaps.presentation.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.krirll.optimalmaps.data.repository.LocalRepositoryImpl
import ru.krirll.optimalmaps.data.repository.RemoteRepositoryImpl
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.LocalRepository
import ru.krirll.optimalmaps.domain.repository.RemoteRepository
import ru.krirll.optimalmaps.domain.useCases.GetPointsByQueryUseCase
import ru.krirll.optimalmaps.domain.useCases.LoadSearchHistoryUseCase
import ru.krirll.optimalmaps.domain.useCases.SavePointItemUseCase
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
    private val localRepository: LocalRepository = LocalRepositoryImpl(app)
    private val remoteRepository: RemoteRepository = RemoteRepositoryImpl()
    private val getSearchByQueryUseCase: GetPointsByQueryUseCase = GetPointsByQueryUseCase(remoteRepository)
    private val loadSearchHistoryUseCase: LoadSearchHistoryUseCase = LoadSearchHistoryUseCase(localRepository)
    private val savePointItemUseCase: SavePointItemUseCase = SavePointItemUseCase(localRepository)

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

    fun savePointItem(index: Int) {
        viewModelScope.launch {
            _pointItemList.value?.let { list ->
                if (list.isNotEmpty())
                    savePointItemUseCase.invoke(list[index])
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
                if (query != "") {
                    if (query != lastQuery) {
                        val result = getSearchByQueryUseCase.invoke(
                            query,
                            locale,
                            { onError(NetworkError.INCORRECT_QUERY) },
                            { onError(NetworkError.NO_INTERNET) }
                        )
                        lastQuery = query
                        _pointItemList.value = result
                    }
                } else {
                    _pointItemList.value = listOf()
                    loadSearchHistory()
                }
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