package io.github.josebatista.featurea.data.datasource

import javax.inject.Inject

class LocalDataSource @Inject constructor() : MessageDataSource {
    override fun getMessage() = println("===> Hello World!")
}