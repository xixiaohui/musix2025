package com.xxh.ringbones.presentation.home

import androidx.lifecycle.ViewModel
import com.xxh.ringbones.domain.usecase.GetHomeCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 * Provides category data for the category grid and row.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase
) : ViewModel() {

    /** Category name string-res -> database type string. */
    val categories: Map<Int, String> = getHomeCategoriesUseCase()
}
