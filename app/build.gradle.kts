plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.plugin.serialization"); id("com.google.devtools.ksp"); id("com.google.dagger.hilt.android") }
android { namespace = "io.github.bei666qipan.rongban"; compileSdk = 35; defaultConfig { applicationId = "io.github.bei666qipan.rongban"; minSdk = 29; targetSdk = 35; versionCode = 1; versionName = "0.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }; buildFeatures { compose = true }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }; kotlinOptions { jvmTarget = "17" } }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("com.google.dagger:hilt-android:2.52")
 ksp("com.google.dagger:hilt-compiler:2.52")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
dependencies {
 implementation(project(":core")); implementation(project(":cards"))
 implementation("androidx.navigation:navigation-compose:2.8.5")
 implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
 implementation("com.airbnb.android:lottie-compose:6.6.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.room:room-runtime:2.6.1")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
