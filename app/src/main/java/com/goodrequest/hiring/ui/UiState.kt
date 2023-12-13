package com.goodrequest.hiring.ui

import java.io.Serializable

sealed class UiState : Serializable {
    data class InitialState(val isError: Boolean) : UiState()
    data class MainState(val pokemons: List<Pokemon>, val isError: Boolean) : UiState()
}