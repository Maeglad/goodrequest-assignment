package com.goodrequest.hiring.ui

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
    state: SavedStateHandle,
    private val api: PokemonApi
) : ViewModel() {

    companion object {
        private const val UI_STATE_KEY = "Ui.UiState"
    }

    private val _uiState = state.getLiveData<UiState>(
        UI_STATE_KEY,
        InitialState(
            isError = false
        )
    )
    val uiState: LiveData<UiState> = _uiState

    init {
        val currentValue = _uiState.value
        if (currentValue is InitialState
            && !currentValue.isError) {
            load()
        }
    }

    fun retry() {
        _uiState.value =
            InitialState(isError = false)
        load()
    }

    fun load() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                api.getPokemons(page = 1)
            }
            result.fold(
                onSuccess = { pokemons ->
                    pokemons.map {
                        withContext(Dispatchers.IO) {
                            loadDetails(it)
                        }
                    }.also { pokemonsAndDetails ->
                        _uiState.value =
                            MainState(
                                pokemons = pokemonsAndDetails,
                                isError = false
                            )
                    }

                },
                onFailure = {
                    when (_uiState.value) {
                        is InitialState ->
                            _uiState.value = InitialState(
                                isError = true
                            )

                        is MainState -> {
                            val currentState = (_uiState.value as MainState)
                            _uiState.value =
                                MainState(
                                    pokemons = currentState.pokemons,
                                    isError = true
                                )
                        }

                        null -> {
                            // cannot happen
                            _uiState.value = InitialState(
                                isError = true
                            )
                        }
                    }
                })
        }
    }

    private suspend fun loadDetails(pokemon: Pokemon): Pokemon =
        pokemon.copy(
            detail = api.getPokemonDetail(pokemon).getOrNull()
        )

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