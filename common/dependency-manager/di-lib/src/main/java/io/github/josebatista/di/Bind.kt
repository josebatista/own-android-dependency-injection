package io.github.josebatista.di

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Bind<REQUESTED : Any, PROVIDED : REQUESTED>
