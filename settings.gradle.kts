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

val localProperties = java.util.Properties().apply {
    val file = rootDir.resolve("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val mapboxToken: String = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = mapboxToken // Access token from local.properties
            }
        }
    }
}

rootProject.name = "MapboxTest"
include(":app")