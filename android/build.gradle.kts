// AGP 9 compiles Kotlin itself. Declaring the Compose and serialization
// plugins here also lifts the Kotlin compiler above the version AGP brings
// along, which the current AndroidX libraries need.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
