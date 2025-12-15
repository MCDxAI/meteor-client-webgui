plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Minecraft
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor Client
    modImplementation(libs.meteor.client)
    compileOnly(libs.orbit)

    // NanoHTTPD for HTTP server and WebSocket support
    modImplementation(libs.nanohttpd.core)
    include(libs.nanohttpd.core)
    modImplementation(libs.nanohttpd.websocket)
    include(libs.nanohttpd.websocket)

    // JSON serialization for WebSocket messages
    modImplementation(libs.gson)
    include(libs.gson)

    // Testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get(),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    test {
        useJUnitPlatform()
    }

    // Install WebUI dependencies
    register<Exec>("installWebUI") {
        group = "build"
        description = "Install Vue.js WebUI dependencies"

        val webuiDir = file("webui")
        val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"

        workingDir = webuiDir
        commandLine(npmCommand, "install")

        onlyIf {
            !file("webui/node_modules").exists()
        }
    }

    // Build WebUI with npm
    register<Exec>("buildWebUI") {
        group = "build"
        description = "Build the Vue.js WebUI"
        dependsOn("installWebUI")

        val webuiDir = file("webui")
        val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"

        workingDir = webuiDir
        commandLine(npmCommand, "run", "build")
    }

    // Copy built WebUI to resources
    register<Copy>("copyWebUI") {
        group = "build"
        description = "Copy built WebUI files to resources"
        dependsOn("buildWebUI")

        doFirst {
            project.delete("src/main/resources/webui")
        }

        from("webui/dist")
        into("src/main/resources/webui")
    }

    // Make processResources depend on copyWebUI
    processResources {
        dependsOn("copyWebUI")
    }

    // Clean generated WebUI resources
    clean {
        delete("src/main/resources/webui")
    }
}
