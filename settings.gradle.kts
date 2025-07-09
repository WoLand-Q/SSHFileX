pluginManagement {
    repositories { google(); mavenCentral() }
    plugins {
        id("com.android.application") version "8.3.2"
        id("org.jetbrains.kotlin.android") version "1.9.22"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SSH FileX"
include(":app")
