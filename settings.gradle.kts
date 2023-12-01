pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
val properties = java.util.Properties()
file("local.properties").inputStream().use { properties.load(it) }

// add to local.properties mapboxPrivateKey= ""
val mapboxPrivateKey: String? = properties.getProperty("mapboxPrivateKey")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")

            credentials {
                username = "mapbox"
                password = "sk.eyJ1Ijoib21uaWEyZyIsImEiOiJjbHA1dnRqeXYxc2xyMmtvNWMzMWsxOTBmIn0.fL7dajT7H6lbFeRUU0xMfw"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "MOBV Zadanie"
include(":app")
