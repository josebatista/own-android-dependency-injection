package io.github.josebatista.featurea.domain.usecase

import io.github.josebatista.featurea.domain.repository.FeatureARepository
import javax.inject.Inject

class FeatureAUseCaseImpl @Inject constructor(private val repository: FeatureARepository) :
    FeatureAUseCase {
    override fun invoke() = repository()
}