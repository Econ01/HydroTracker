import java.util.Properties
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.cemcakmak.hydrotracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cemcakmak.hydrotracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 28
        versionName = "1.0.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (file("../signing_keys/upload-keystore").exists()) {
            create("release") {
                storeFile = file("../signing_keys/upload-keystore")
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        // Robolectric reads the debug variant's merged assets, so expose the exported Room schemas
        // there for MigrationTestHelper. Scoped to debug only, so they never ship in the release APK.
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        ))
    }
}

ksp{
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (Bill of Materials) - Use the latest stable version
    implementation(platform(libs.compose.bom))

    // Core Compose UI dependencies (versions controlled by BOM)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material 3 with Expressive APIs - Override BOM for experimental features
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.material)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Icons (updated to use BOM version)
    implementation(libs.androidx.material.icons.extended)

    // Room (Database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (typed) + kotlinx serialization for the preferences store
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)

    // Health Connect
    implementation(libs.androidx.health.connect)

    // Google Play Store
    implementation(libs.review.ktx)
    implementation(libs.app.update.ktx)

    // Debug tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Unit testing (Robolectric runs Room MigrationTestHelper on the JVM — no emulator)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.sqlite.framework)
}

// ---------------------------------------------------------------------------------------------
// Third-party license collection (in-house replacement for Google's OSS Licenses plugin, which is
// incompatible with AGP 9). For each variant, resolves the runtime dependencies' POMs at build time
// and emits licenses.json as a generated asset, rendered by LicensesScreen. Uses only stable Gradle
// APIs, so it isn't affected by AGP variant-API churn.
// ---------------------------------------------------------------------------------------------
androidComponents {
    onVariants { variant ->
        val capName = variant.name.replaceFirstChar { it.uppercase() }
        val runtimeClasspath = configurations.getByName("${variant.name}RuntimeClasspath")
        val depHandler = dependencies
        val catalogFile = rootProject.layout.projectDirectory.file("gradle/libs.versions.toml")

        // Collect each runtime dependency's .pom via a detached "@pom" configuration, whose dependency
        // set is populated lazily in withDependencies {}. The task consumes pomFiles as @Internal and
        // resolves it inside its action, so the runtime classpath is resolved only at execution time —
        // never during the configuration phase (which would be a performance/config-cache penalty).
        // The version catalog is the task's tracked input, so it re-runs when dependencies change.
        val pomConfig = configurations.detachedConfiguration().apply {
            isTransitive = false
            withDependencies {
                runtimeClasspath.incoming.resolutionResult.allComponents
                    .mapNotNull { it.id as? ModuleComponentIdentifier }
                    .forEach { add(depHandler.create("${it.group}:${it.module}:${it.version}@pom")) }
            }
        }

        val licensesTask = tasks.register<GenerateLicensesTask>("generate${capName}LicensesJson") {
            outputDir.set(layout.buildDirectory.dir("generated/licenses/${variant.name}"))
            pomFiles.from(pomConfig)
            versionCatalog.set(catalogFile)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(licensesTask) { it.outputDir }
    }
}

abstract class GenerateLicensesTask : DefaultTask() {
    // @Internal (not @InputFiles): tracking this as an input would force the runtime classpath to
    // resolve while Gradle builds the task graph (configuration phase). We resolve it in the action.
    @get:Internal
    abstract val pomFiles: ConfigurableFileCollection

    // Tracked input so the task re-runs when dependencies change.
    @get:InputFile
    abstract val versionCatalog: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val factory = DocumentBuilderFactory.newInstance()
        val seen = mutableSetOf<String>()
        val entries = mutableListOf<Map<String, String>>()

        pomFiles.files.filter { it.name.endsWith(".pom") }.forEach { pom ->
            runCatching {
                val root = factory.newDocumentBuilder().parse(pom).documentElement
                val parent = directChild(root, "parent")
                val group = directChildText(root, "groupId")
                    ?: parent?.let { directChildText(it, "groupId") }
                val artifact = directChildText(root, "artifactId") ?: return@runCatching
                if (group == null) return@runCatching
                val key = "$group:$artifact"
                if (!seen.add(key)) return@runCatching

                val version = directChildText(root, "version")
                    ?: parent?.let { directChildText(it, "version") }
                    ?: ""
                val projectName = directChildText(root, "name")?.takeIf { it.isNotBlank() }
                val projectUrl = directChildText(root, "url").orEmpty()

                val licenseEl = directChild(root, "licenses")?.let { directChild(it, "license") }
                val override = LICENSE_OVERRIDES[group]
                val licenseName = licenseEl?.let { directChildText(it, "name") }
                    ?.takeIf { it.isNotBlank() } ?: override?.first ?: "Unknown"
                val licenseUrl = licenseEl?.let { directChildText(it, "url") }
                    ?.takeIf { it.isNotBlank() } ?: override?.second ?: projectUrl

                entries.add(
                    linkedMapOf(
                        "name" to (projectName ?: key),
                        "version" to version,
                        "license" to normalizeLicense(licenseName),
                        "url" to licenseUrl
                    )
                )
            }
        }

        entries.sortBy { (it["name"] ?: "").lowercase() }

        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("licenses.json").writeText(
            groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(entries))
        )
    }

    private fun directChild(parent: org.w3c.dom.Element, tag: String): org.w3c.dom.Element? {
        val nodes = parent.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is org.w3c.dom.Element && node.tagName == tag) return node
        }
        return null
    }

    private fun directChildText(parent: org.w3c.dom.Element, tag: String): String? =
        directChild(parent, tag)?.textContent?.trim()

    // Collapse the many vendor spellings of common licenses into clean SPDX-ish tags for display.
    private fun normalizeLicense(name: String): String {
        val n = name.lowercase()
        return when {
            "apache" in n -> "Apache-2.0"
            n == "mit" || "mit license" in n -> "MIT"
            "bsd" in n && "3" in n -> "BSD-3-Clause"
            "bsd" in n && "2" in n -> "BSD-2-Clause"
            "bsd" in n -> "BSD"
            else -> name
        }
    }

    companion object {
        // Fallback licenses for artifacts whose POMs declare the license only in a parent.
        private val LICENSE_OVERRIDES = mapOf(
            "com.google.guava" to ("Apache-2.0" to "https://www.apache.org/licenses/LICENSE-2.0.txt")
        )
    }
}