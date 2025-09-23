package com.chris.culsi.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItemCatalogViewModel(app: Application): AndroidViewModel(app) {
    private val repo = ItemCatalogRepository(app)

    private val _currentCategory = MutableStateFlow(CatalogCategory.BREAKFAST)
    val currentCategory: StateFlow<CatalogCategory> = _currentCategory

    val items: StateFlow<List<String>> =
        _currentCategory.flatMapLatest { repo.mergedSortedItems(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lastLoggedBy: StateFlow<String> =
        repo.lastLoggedBy().stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setCategory(cat: CatalogCategory) {
        _currentCategory.value = cat
    }

    fun addCustom(item: String) {
        val cat = _currentCategory.value
        viewModelScope.launch { repo.addCustom(cat, item) }
    }

    fun rememberLastLoggedBy(name: String) {
        viewModelScope.launch { repo.setLastLoggedBy(name) }
    }
}
