plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.plugin.serialization"); id("com.google.devtools.ksp"); id("com.google.dagger.hilt.android") }
android { namespace = "io.github.bei666qipan.rongban.core"; compileSdk = 35; defaultConfig { minSdk = 29; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }; buildFeatures { compose = true }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }; kotlinOptions { jvmTarget = "17" } }
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
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 ksp("androidx.room:room-compiler:2.6.1")
 implementation("androidx.datastore:datastore-preferences:1.1.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
 implementation("org.yaml:snakeyaml:2.2")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
