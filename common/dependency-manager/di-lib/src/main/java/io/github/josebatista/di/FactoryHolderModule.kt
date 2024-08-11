package io.github.josebatista.di

class FactoryHolderModule : Module {
    private val factories = mutableMapOf<Class<out Any?>, Factory<out Any?>>()

    override fun <T> get(requestedType: Class<T>): Factory<T>? = factories[requestedType] as Factory<T>?

    fun <T> install(requestedType: Class<T>, factory: Factory<T>) {
        factories[requestedType] = factory
    }
}
