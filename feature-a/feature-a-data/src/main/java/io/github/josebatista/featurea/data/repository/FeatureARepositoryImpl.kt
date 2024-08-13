package io.github.josebatista.featurea.data.repository

import io.github.josebatista.featurea.data.datasource.MessageDataSource
import io.github.josebatista.featurea.domain.repository.FeatureARepository
import javax.inject.Inject

class FeatureARepositoryImpl @Inject constructor(private val dataSource: MessageDataSource) :
    FeatureARepository {
    override fun invoke() {
        dataSource.getMessage()
    }
}