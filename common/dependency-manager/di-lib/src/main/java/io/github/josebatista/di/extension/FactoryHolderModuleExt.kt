package io.github.josebatista.di.extension

import io.github.josebatista.di.FactoryHolderModule
import io.github.josebatista.di.ObjectGraph
import io.github.josebatista.di.singleton

inline fun <reified T> FactoryHolderModule.install(noinline factory: ObjectGraph.() -> T) =
    install(T::class.java, factory)

inline fun <reified T> FactoryHolderModule.installSingleton(noinline factory: ObjectGraph.() -> T) =
    install(T::class.java, singleton(factory))

inline fun <reified REQUESTED, reified PROVIDED : REQUESTED> FactoryHolderModule.bind() {
    install(REQUESTED::class.java) { objectGraph ->
        objectGraph[PROVIDED::class.java]
    }
}
