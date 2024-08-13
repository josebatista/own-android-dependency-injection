package io.github.josebatista.featurea.lib.di

import io.github.josebatista.di.Bind
import io.github.josebatista.di.Component
import io.github.josebatista.featurea.data.datasource.LocalDataSource
import io.github.josebatista.featurea.data.datasource.MessageDataSource
import io.github.josebatista.featurea.data.repository.FeatureARepositoryImpl
import io.github.josebatista.featurea.domain.repository.FeatureARepository
import io.github.josebatista.featurea.domain.usecase.FeatureAUseCase
import io.github.josebatista.featurea.domain.usecase.FeatureAUseCaseImpl
import io.github.josebatista.featurea.presentation.viewmodel.FeatureAViewModel

@Component(modules = [DiDomainBindsModule::class, DiDataBindsModule::class])
interface FeatureAComponent {
    val featureAViewModel: FeatureAViewModel
}

@Bind<FeatureARepository, FeatureARepositoryImpl>
@Bind<MessageDataSource, LocalDataSource>
interface DiDataBindsModule

@Bind<FeatureAUseCase, FeatureAUseCaseImpl>
interface DiDomainBindsModule