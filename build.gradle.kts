plugins {
	id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT"
	`maven-publish`
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
val mc = providers.gradleProperty("minecraft_version").get()
val loader = providers.gradleProperty("loader_version").get()
val fabricApi = providers.gradleProperty("fabric_api_version").get()
val yacl = property("yacl_version") as String
val modmenu = property("modmenu_version") as String

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

repositories {
	maven("https://maven.fabricmc.net/")
	maven("https://maven.2b2t.vc/remote")
	maven("https://api.modrinth.com/maven") {
		content {
			includeGroup("maven.modrinth")
		}
	}
	maven("https://maven.isxander.dev/releases")
}

loom {
	accessWidenerPath = file("src/main/resources/wdlx.accesswidener")
	runs {
		getByName("client") {
			client()
			programArgs("--username", "test")
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:$mc")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loader")
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")
	implementation(include("net.lenni0451:LambdaEvents:2.4.2")!!)
	implementation(include("net.lenni0451:Reflect:1.6.3")!!)

	modImplementation("maven.modrinth:modmenu:$modmenu")
	modImplementation("dev.isxander:yet-another-config-lib:$yacl+$mc-fabric")

	compileOnly("org.projectlombok:lombok:1.18.46")
	annotationProcessor("org.projectlombok:lombok:1.18.46")
}

tasks {
	processResources {
		val version = version
		inputs.property("version", version)

		filesMatching("fabric.mod.json") {
			expand("version" to version)
		}
	}
	jar {
		val projectName = project.name
		archiveVersion = "$version+fabric-$mc"
		inputs.property("projectName", projectName)

		from("LICENSE") {
			rename { "${it}_$projectName" }
		}
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
	repositories {

	}
}
