package com.example.rickandmorty.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected fun launchWithErrorHandling(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    onError(throwable)
                }
        }
    }
}
