import net.fabricmc.loom.task.FabricModJsonV1Task

plugins {
	id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
val mc = providers.gradleProperty("minecraft_version").get()
val loader = providers.gradleProperty("loader_version").get()
val fabricApi = providers.gradleProperty("fabric_api_version").get()
val yacl = providers.gradleProperty("yacl_version").get()
val modmenu = providers.gradleProperty("modmenu_version").get()

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
	jar {
		val projectName = project.name
		archiveVersion = "$version+fabric-$mc"
		inputs.property("projectName", projectName)

		from("LICENSE") {
			rename { "${it}_$projectName" }
		}
	}
	val fmjTask = register<FabricModJsonV1Task>("createModJson") {
		group = "fabric"
		outputFile = file(project.layout.buildDirectory.file("resources/main/fabric.mod.json").get().asFile)
		json {
			modId = "wdlx"
			version = project.version as String
			name = "WorldDownloadX"
			description = "Download multiplayer worlds into a singleplayer world"
			author("rfresh2")
			contactInformation.put("homepage", "https://github.com/rfresh2/WorldDownloadX")
			contactInformation.put("sources", "https://github.com/rfresh2/WorldDownloadX")
			licenses.add("LGPL-3.0")
			icon {
				path = "assets/wdlx/icon.png"
			}
			client()
			entrypoint("client", "wdlx.WorldDownloadX")
			entrypoint("modmenu", "wdlx.config.ModMenuScreen")
			mixin {
				environment = "client"
				value = "wdlx.mixins.json"
			}
			depends("fabricloader", ">=0.19.2")
			depends("minecraft", "1.21.4")
			depends("fabric-api", "*")
			depends("yet_another_config_lib_v3", "*")
			accessWidener = loom.accessWidenerPath.get().asFile.name
		}
	}
	processResources {
		dependsOn(fmjTask)
	}
}
