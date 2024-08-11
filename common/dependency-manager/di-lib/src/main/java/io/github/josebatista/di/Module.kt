package io.github.josebatista.di

interface Module {
    operator fun <T> get(requestedType: Class<T>): Factory<T>?
}
