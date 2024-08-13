package io.github.josebatista.featurea.lib

import io.github.josebatista.featurea.lib.di.GeneratedFeatureAComponent

object FeatureALib {
    fun sayHello() {
        GeneratedFeatureAComponent().featureAViewModel.getFeatureA()
    }
}