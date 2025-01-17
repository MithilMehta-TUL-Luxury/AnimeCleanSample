package com.anime_clean_sample.presentation.vm

import androidx.lifecycle.SavedStateHandle
import com.anime_clean_sample.domain.di.IO
import com.anime_clean_sample.domain.usecase.anime.DeleteFavoriteAnimeUseCase
import com.anime_clean_sample.domain.usecase.anime.GetAnimeByIdUseCase
import com.anime_clean_sample.domain.usecase.anime.SaveAnimeToFavoriteUseCase
import com.anime_clean_sample.presentation.mapper.toAnime
import com.anime_clean_sample.presentation.mapper.toAnimeDetailsUiState
import com.anime_clean_sample.presentation.ui_state.AnimeDetailsUiState
import com.anime_clean_sample.presentation.vm.base.BaseViewModel
import com.anime_clean_sample.resource.constants.INVALID_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeDetailsVM @Inject constructor(
    private val getAnimeByIdUseCase: GetAnimeByIdUseCase,
    private val saveAnimeToFavoriteUseCase: SaveAnimeToFavoriteUseCase,
    private val deleteFavoriteAnimeUseCase: DeleteFavoriteAnimeUseCase,
    private val savedStateHandle: SavedStateHandle,
    @IO private val dispatcher: CoroutineDispatcher
) : BaseViewModel() {

    val id: StateFlow<Int>
        get() = savedStateHandle.getStateFlow("id", INVALID_ID)

    private val _animeDetailsUiState = MutableStateFlow(AnimeDetailsUiState())
    val animeDetailsUiState: StateFlow<AnimeDetailsUiState>
        get() = _animeDetailsUiState

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String>
        get() = _message

    init {
        id.filter {
            it != INVALID_ID
        }.map {
            getAnimeByIdUseCase(it)
                .single()
                .toAnimeDetailsUiState()
        }.onEach {
            _animeDetailsUiState.emit(it)
        }.flowOn(dispatcher)
            .launchIn(supervisorScope)
    }

    fun updateFavorite(save: Boolean) = supervisorScope.launch {
        (if (save) saveAnimeToFavoriteUseCase(animeDetailsUiState.value.toAnime())
        else deleteFavoriteAnimeUseCase(animeDetailsUiState.value.toAnime()))
            .flowOn(dispatcher)
            .collectLatest {
                _animeDetailsUiState.value = _animeDetailsUiState.value.copy(
                    isFavorite = save
                )
                _message.emit(it.data)
            }
    }
}