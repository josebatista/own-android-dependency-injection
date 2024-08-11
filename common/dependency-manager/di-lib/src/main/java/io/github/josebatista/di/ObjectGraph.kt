package io.github.josebatista.di

class ObjectGraph(private val modules: List<Module>) {

    constructor(vararg modules: Module) : this(modules.asList())

    private val factoryHolder = FactoryHolderModule()

    operator fun <T> get(requestedType: Class<T>): T {
        val knownFactoryOrNull = factoryHolder[requestedType]
        val factory = knownFactoryOrNull ?: modules
            .firstNotNullOf { module -> module[requestedType] }
            .also { factory -> factoryHolder.install(requestedType, factory) }
        return factory.get(this)
    }
}
