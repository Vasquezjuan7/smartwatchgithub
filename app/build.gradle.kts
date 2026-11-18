import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Client ID de la OAuth App de GitHub (no es secreto). Se lee de local.properties:
//   github.clientId=Ov23li...
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val githubClientId: String = localProps.getProperty("github.clientId")
    ?: (findProperty("GITHUB_CLIENT_ID") as String?)
    ?: ""

// Edge Function de Supabase que genera los resúmenes con IA (URL y clave publicable, no son secretos).
val supabaseUrl: String = localProps.getProperty("supabase.url") ?: ""
val supabaseKey: String = localProps.getProperty("supabase.key") ?: ""

android {
    namespace = "com.example.githubmovil"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.githubmovil"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
        buildConfigField(
            "String", "AI_SUMMARY_URL",
            "\"${if (supabaseUrl.isBlank()) "" else supabaseUrl.trimEnd('/') + "/functions/v1/pr-summary"}\""
        )
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.wearable)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    implementation(libs.okhttp)
    implementation(libs.work.runtime.ktx)
    implementation(libs.core.ktx)
    implementation("androidx.compose.material:material-icons-core")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}