package io.github.josebatista.di.extension

import io.github.josebatista.di.ObjectGraph

inline fun <reified T> ObjectGraph.get() = get(T::class.java)
