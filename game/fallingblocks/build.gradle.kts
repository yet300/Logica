plugins { id("funfolio.miniapp") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.uikit)
            implementation(libs.bundles.mvi)
        }

        commonTest.dependencies {
            implementation(libs.compose.ui.test)
        }
    }
}
