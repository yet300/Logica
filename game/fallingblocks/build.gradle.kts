plugins { id("logica.miniapp") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(libs.bundles.mvi)
        }
    }
}
