package com.goodrequest.hiring.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.ui.UiState.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class PokemonViewModel(
    private val state: SavedStateHandle,
    private val api: PokemonApi
) : ViewModel() {

    companion object {
        private const val UI_STATE_KEY = "Ui.UiState"
        private const val NEXT_PAGE_KEY = "Ui.NextPage"
        private const val IS_PAGE_ERROR_KEY = "Ui.IsPageError"
    }

    private val _uiState = state.getLiveData<UiState>(
        UI_STATE_KEY,
        InitialState(
            isError = false
        )
    )
    val uiState: LiveData<UiState> = _uiState

    private val _isPageError = state.getLiveData(
        IS_PAGE_ERROR_KEY,
        false
    )
    val isPageError: LiveData<Boolean> = _isPageError
    private val nextPage get() = state.get<Int>(NEXT_PAGE_KEY) ?: 1

    private var isLoadingPage = false

    init {
        val currentValue = _uiState.value
        if (currentValue is InitialState
            && !currentValue.isError) {
            reloadData()
        }
    }

    fun retryInitialLoad() {
        _uiState.value = InitialState(isError = false)
        reloadData()
    }

    fun retryPage() {
        _isPageError.value = false
        loadNextPage()
    }

    fun reloadData() {
        viewModelScope.launch {
            load(true)
            _isPageError.value = false
        }
    }

    fun loadNextPage() {
        // already loading page
        if (isLoadingPage) return
        // load new page
        isLoadingPage = true
        viewModelScope.launch {
            load(false)
            isLoadingPage = false
        }
    }


    private suspend fun load(isReset: Boolean) {

        val loadPage = if (isReset) {
            1
        } else {
            nextPage
        }
            val result = withContext(Dispatchers.IO) {
                api.getPokemons(page = loadPage)
            }
            result.fold(
                onSuccess = { pokemons: List<Pokemon> ->
                    onSuccess(pokemons, isReset)
                    state[NEXT_PAGE_KEY] = loadPage + 1
                },
                onFailure = {
                    Log.e(PokemonViewModel::class.java.name, "error while loading pokemons", it)
                    onFailure(isReset)
                })
    }

    private suspend fun loadDetails(pokemon: Pokemon): Pokemon =
        pokemon.copy(
            detail = api.getPokemonDetail(pokemon).fold(
                onSuccess = { it },
                onFailure = {
                    Log.e(PokemonViewModel::class.java.name, "error while laoding details", it)
                    null
                }
            )
        )

    private suspend fun onSuccess(pokemons: List<Pokemon>, isReset: Boolean) {
        pokemons.map {
            // load details
            withContext(Dispatchers.IO) {
                loadDetails(it)
            }
        }.also { pokemonsAndDetails ->
            _uiState.value =
                MainState(
                    pokemons =
                    if (isReset) {
                        // reset pokemon data and only keep new data
                        pokemonsAndDetails
                    } else {
                        // check if current state is main state
                        // if not return empty list
                        val currentState = _uiState.value
                        val currentPokemons = if (currentState is MainState) {
                            currentState.pokemons
                        } else {
                            listOf()
                        }
                        // merge current pokemons with new ones
                        currentPokemons.plus(pokemonsAndDetails)
                    },
                    isError = false
                )
        }
    }

    private fun onFailure(isReset: Boolean) {
        when (_uiState.value) {
            is InitialState ->
                // update initial state to display error
                _uiState.value = InitialState(
                    isError = true
                )

            is MainState -> {
                // update current state to update with error
                // keep existing pokemons
                val currentState = (_uiState.value as MainState)
                _uiState.value =
                    MainState(
                        pokemons = currentState.pokemons,
                        isError = isReset
                    )
                _isPageError.value = !isReset
            }

            null -> {
                // cannot happen
            }
        }
    }

}


data class Pokemon(
    val id     : String,
    val name   : String,
    val detail : PokemonDetail? = null
) : Serializable

data class PokemonDetail(
    val image  : String,
    val move   : String,
    val weight : Int
) : Serializable