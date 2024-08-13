package io.github.josebatista.featurea.presentation.viewmodel

import io.github.josebatista.featurea.domain.usecase.FeatureAUseCase
import javax.inject.Inject

class FeatureAViewModel @Inject constructor(private val featureAUseCase: FeatureAUseCase) {
    fun getFeatureA() {
        featureAUseCase()
    }
}