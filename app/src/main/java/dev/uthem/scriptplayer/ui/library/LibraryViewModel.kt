package dev.uthem.scriptplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.uthem.scriptplayer.data.AddResult
import dev.uthem.scriptplayer.data.ScriptRepository
import dev.uthem.scriptplayer.data.ScriptSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 보관함 화면의 상태.
 *
 * [loading] 을 따로 둔다. 목록이 비어 있는 것과 아직 안 읽어 온 것을 구분하지 않으면,
 * 켤 때마다 "아직 대본이 없습니다" 가 한 번 번쩍인다.
 */
data class LibraryUiState(
    val scripts: List<ScriptSummary> = emptyList(),
    val loading: Boolean = true,
)

/** 담기 시도의 결과를 화면에 한 번만 알린다. */
sealed interface LibraryEvent {
    data object NothingToRead : LibraryEvent
}

class LibraryViewModel(private val repository: ScriptRepository) : ViewModel() {

    val state: StateFlow<LibraryUiState> = repository.observeLibrary()
        .map { LibraryUiState(scripts = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private val _events = MutableStateFlow<LibraryEvent?>(null)
    val events: StateFlow<LibraryEvent?> = _events.asStateFlow()

    fun add(raw: String) = viewModelScope.launch {
        if (repository.add(raw) is AddResult.NothingToRead) {
            _events.value = LibraryEvent.NothingToRead
        }
    }

    fun rename(id: String, title: String) = viewModelScope.launch {
        repository.rename(id, title)
    }

    fun delete(id: String) = viewModelScope.launch {
        repository.delete(id)
    }

    fun consumeEvent() {
        _events.value = null
    }
}
