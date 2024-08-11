package io.github.josebatista

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import io.github.josebatista.di.Binds
import io.github.josebatista.di.Component
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass


class ComponentProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ComponentProcessor(environment.codeGenerator)
    }

    data class ComponentFactory(
        val type: KSClassDeclaration,
        val constructorParameters: List<KSDeclaration>,
        val isSingleton: Boolean
    )

    data class EntryPoint(
        val propertyDeclaration: KSPropertyDeclaration,
        val propertyType: KSDeclaration
    )

    class ComponentModel(
        val packageName: String,
        val imports: Set<String>,
        val className: String,
        val componentInterfaceName: String,
        val factories: List<ComponentFactory>,
        val binds: Map<KSDeclaration, KSDeclaration>,
        val entryPoints: List<EntryPoint>
    )

    class ComponentProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {
        override fun process(resolver: Resolver): List<KSAnnotated> {
            val annotatedSymbols = resolver.getSymbolsWithAnnotation(Component::class.java.name)
            val unprocessedSymbols = annotatedSymbols.filter { !it.validate() }.toList()
            annotatedSymbols
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.validate() && it.classKind == ClassKind.INTERFACE }
                .forEach { it.accept(ComponentVisitor(), Unit) }
            return unprocessedSymbols
        }

        @OptIn(KspExperimental::class)
        inner class ComponentVisitor : KSVisitorVoid() {
            override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
                val componentInterfaceName = classDeclaration.simpleName.asString()
                val packageName = classDeclaration.containingFile!!.packageName.asString()
                val className = "Generated$componentInterfaceName"
                val componentAnnotation = classDeclaration.annotations.single { it isInstance Component::class }
                val entryPoints = readEntryPoints(classDeclaration)
                val entryPointTypes = entryPoints.map { it.propertyType }
                val binds = readBinds(componentAnnotation)
                val bindProvidedTypes = binds.values
                val factories = traverseDependencyGraph(entryPointTypes + bindProvidedTypes)
                val importDeclarations = entryPointTypes + bindProvidedTypes + factories.map { it.type }
                val actualImports = importDeclarations
                    .filter { it.packageName != classDeclaration.packageName }
                    .map { it.qualifiedName!!.asString() }.toSet() +
                        if (factories.any { it.isSingleton }) {
                            setOf("io.github.josebatista.di.componentSingleton")
                        } else emptySet()
                val model = ComponentModel(
                    packageName = packageName,
                    imports = actualImports,
                    className = className,
                    componentInterfaceName = componentInterfaceName,
                    factories = factories,
                    binds = binds,
                    entryPoints = entryPoints,
                )
                codeGenerator.createNewFile(
                    Dependencies(true, classDeclaration.containingFile!!), packageName, className
                ).use { ktFile ->
                    generateComponent(model, ktFile)
                }
            }

            private fun readEntryPoints(classDeclaration: KSClassDeclaration) = classDeclaration
                .getDeclaredProperties().map { property ->
                    val resolvedPropertyType = property.type.resolve().declaration
                    EntryPoint(property, resolvedPropertyType)
                }.toList()

            private fun readBinds(componentAnnotation: KSAnnotation): Map<KSDeclaration, KSDeclaration> {
                @Suppress("UNCHECKED_CAST")
                val bindsModules = componentAnnotation.getArgument("modules").value as List<KSType>
                val binds = bindsModules
                    .map { it.declaration as KSClassDeclaration }
                    .flatMap { it.getDeclaredFunctions() }
                    .filter { it.isAnnotationPresent(Binds::class) }
                    .associate { function ->
                        val resolvedReturnType = function.returnType!!.resolve().declaration
                        val resolvedParamType = function.parameters.single().type.resolve().declaration
                        resolvedReturnType to resolvedParamType
                    }
                return binds
            }

            private fun traverseDependencyGraph(factoryEntryPoint: List<KSDeclaration>): List<ComponentFactory> {
                val typesToProcess = mutableListOf<KSDeclaration>()
                typesToProcess += factoryEntryPoint
                val factories = mutableListOf<ComponentFactory>()
                val typedVisited = mutableListOf<KSDeclaration>()
                while (typesToProcess.isNotEmpty()) {
                    val visitedClassDeclaration = typesToProcess.removeFirst() as KSClassDeclaration
                    if (visitedClassDeclaration !in typedVisited) {
                        typedVisited += visitedClassDeclaration
                        val injectConstructors = visitedClassDeclaration.getConstructors()
                            .filter { it.isAnnotationPresent(Inject::class) }
                            .toList()
                        check(injectConstructors.size < 2) {
                            "There should be a most one @Inject constructor"
                        }
                        if (injectConstructors.isNotEmpty()) {
                            val injectConstructor = injectConstructors.first()
                            val constructorParams = injectConstructor.parameters.map { it.type.resolve().declaration }
                            typesToProcess += constructorParams
                            val isSingleton = visitedClassDeclaration.isAnnotationPresent(Singleton::class)
                            factories += ComponentFactory(visitedClassDeclaration, constructorParams, isSingleton)
                        }
                    }
                }
                return factories
            }

            private fun generateComponent(model: ComponentModel, ktFile: OutputStream) {
                with(model) {
                    ktFile.appendLine("package $packageName")
                    ktFile.appendLine()
                    imports.forEach { import -> ktFile.appendLine("import $import") }
                    ktFile.appendLine()
                    ktFile.appendLine("class $className : $componentInterfaceName {")
                    factories.forEach { (classDeclaration, parameterDeclarations, isSingleton) ->
                        val name = classDeclaration.simpleName.asString()
                        val parameters = parameterDeclarations.map { requestedType ->
                            val providedType = binds[requestedType] ?: requestedType
                            providedType.simpleName.asString()
                        }
                        val singleton = if (isSingleton) "componentSingleton" else ""
                        ktFile.appendLine("    private val provide$name = $singleton {")
                        ktFile.appendLine("        $name(${parameters.joinToString(", ") { "provide$it()" }})")
                        ktFile.appendLine("    }")
                    }
                    entryPoints.forEach { (propertyDeclaration, type) ->
                        val entryPointName = propertyDeclaration.simpleName.asString()
                        val typeSimpleName = type.simpleName.asString()
                        ktFile.appendLine("    override val $entryPointName: $typeSimpleName")
                        ktFile.appendLine("        get() = provide$typeSimpleName()")
                    }
                    ktFile.appendLine("}")
                }
            }
        }
    }
}

infix fun KSAnnotation.isInstance(annotationKClass: KClass<*>): Boolean {
    return shortName.getShortName() == annotationKClass.simpleName &&
            annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName
}

fun KSAnnotation.getArgument(name: String): KSValueArgument {
    return arguments.single { it.name?.asString() == name }
}
