pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Own Dependency Manager"
include(":app")
include(":common:dependency-manager:di-lib")
include(":common:dependency-manager:di-processor")
include(":feature-a:feature-a-data")
include(":feature-a:feature-a-domain")
include(":feature-a:feature-a-presentation")
include(":feature-a:feature-a-lib")
