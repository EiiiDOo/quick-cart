import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-kapt")
    id("com.apollographql.apollo") version "4.0.1"
    alias(libs.plugins.google.gms.google.services)
}

val properties= Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = "com.senseicoder.quickcart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.senseicoder.quickcart"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField ("String","shopify_admin_api_access_token","\"${properties.getProperty("shopify_admin_api_access_token")}\"")
        buildConfigField ("String","shopify_api_key","\"${properties.getProperty("shopify_api_key")}\"")
        buildConfigField ("String","shopify_secret_key","\"${properties.getProperty("shopify_secret_key")}\"")
        buildConfigField ("String","shopify_store_front_api_access_token","\"${properties.getProperty("shopify_store_front_api_access_token")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

apollo {
    service("Admin") {
        // Adds the given directory as a GraphQL source root
        srcDir("src/main/graphql")
        // The package name for the generated models
        packageName.set("com.admin")
        // Warn if using a deprecated field
        warnOnDeprecatedUsages.set(true)
        // Whether to generate Kotlin or Java models
        generateKotlinModels.set(true)
        // wire the generated models to the "test" source set
//        outputDirConnection {
//            connectToKotlinSourceSet("test")
//        }
        // This creates a downloadAdminApolloSchemaFromIntrospection task
        introspection {
            headers.put("X-Shopify-Access-Token", properties["shopify_admin_api_access_token"].toString())
            endpointUrl.set("https://android-alex-team5.myshopify.com/admin/api/2024-10/graphql.json")
            // The path is interpreted relative to the current project
            schemaFile.set(file("src/main/graphql/com/admin/schema.graphqls"))
        }
        //Make IDEA aware of codegen and will run it during your Gradle Sync, default: false
        generateSourcesDuringGradleSync.set(true)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //idk if this would be needed
    implementation("androidx.preference:preference:1.2.1")
    //legacy support for NestedRecyclerView
    implementation(libs.androidx.legacy.support.v4)
    //Room
    implementation (libs.androidx.room.ktx)
    implementation (libs.androidx.room.runtime)
    kapt (libs.androidx.room.compiler)
    //Work Manager
    implementation(libs.androidx.work.runtime.ktx)
    //Apollo (GraphQL)
    implementation(libs.apollo.runtime)
    //Retrofit
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.okhttp)
    implementation (libs.logging.interceptor)
    //GSON
    implementation (libs.gson)
    //animation
    implementation (libs.lottie)
    //facebook shimmer
    implementation (libs.shimmer)
    //Glide
    implementation (libs.glide)
    kapt(libs.glide.compiler)
    //SwipeRefreshLayout
    implementation(libs.androidx.swiperefreshlayout)
    //zoomable image
    implementation (libs.touchimageview)
    //Testing
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("org.robolectric:robolectric:4.8")

    // AndroidX Test - JVM Testing
    testImplementation("androidx.test:core-ktx:1.4.0")
    testImplementation("androidx.test.ext:junit-ktx:1.1.3")

    /*    // AndroidX Test - Instrumented Testing
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")*/

    // InstantTaskExecutorRule
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.1.0")

    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")

    // Mokito
    testImplementation("org.mockito:mockito-core:3.10.0")
    androidTestImplementation("org.mockito:mockito-android:3.10.0")

    // Hamcrest for assertions
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    androidTestImplementation("org.hamcrest:hamcrest:2.2")
    androidTestImplementation("org.hamcrest:hamcrest-library:2.2")
}