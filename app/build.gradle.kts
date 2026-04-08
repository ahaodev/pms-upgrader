import org.gradle.language.nativeplatform.internal.Dimensions.applicationVariants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
fun Project.generateVersionCode(): Int {
    return providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toInt()
}

android {
    namespace = "com.ahao.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ahao.upgrader"
        minSdk = 24
        targetSdk = 36
        versionCode = generateVersionCode()
        versionName = "v0.1.$versionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePath = localProps["KEYSTORE_PATH"] as String?
    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = localProps["KEYSTORE_PASSWORD"] as String
                keyAlias = localProps["KEY_ALIAS"] as String
                keyPassword = localProps["KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

}

afterEvaluate {
    val versionName = android.defaultConfig.versionName
    tasks.named("packageRelease", com.android.build.gradle.tasks.PackageApplication::class) {
        doLast {
            val date = SimpleDateFormat("yyyyMMddHHmm").format(Date())
            outputDirectory.get().asFile.listFiles()
                ?.filter { it.name.endsWith(".apk") }
                ?.forEach { apk -> apk.renameTo(File(apk.parentFile, "$versionName-$date.apk")) }
        }
    }
}

dependencies {
    // Upgrader module
    implementation(project(":upgrader"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}