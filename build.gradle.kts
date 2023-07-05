import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.yaml.snakeyaml.Yaml
import java.io.FileReader

buildscript {
	repositories {
		mavenLocal()
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.22")
		classpath("org.yaml:snakeyaml:1.29")
	}
}

plugins {
	id("org.springframework.boot") version "3.0.5"
    id("maven-publish")
	id("com.diffplug.spotless") version "6.19.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

spotless {
	kotlin {
		ktfmt().googleStyle().configure {
			it.setBlockIndent(4)
			it.setContinuationIndent(4)
		}
	}
}


apply(plugin = "maven-publish")
apply(plugin = "io.spring.dependency-management")

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

repositories {
	mavenLocal()
	mavenCentral()
	flatDir {
		dirs("libs")
	}
}

val yaml = Yaml()
val obj: Map<String, Any> = yaml.load(FileReader("src/main/resources/application.yaml"))
version = ((obj["app"] as Map<*, *>)["version"]).toString()

group = "ie.daithi.cards"
java.sourceCompatibility = JavaVersion.VERSION_17

description = "api"

dependencies {

	// Internal Dependencies

	// External Dependencies

	implementation("org.bouncycastle:bcprov-jdk15on:1.70")
	implementation("com.heroku.sdk:env-keystore:1.1.7")

	// Kotlin dependencies
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")

	// Spring dependencies
	implementation("org.springframework.boot:spring-boot-starter:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-web:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-data-redis:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-websocket:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-security:3.1.1")
	implementation("org.springframework.boot:spring-boot-starter-actuator:3.1.1")
	implementation("org.springframework.security:spring-security-oauth2-resource-server:6.0.2")
	implementation("org.springframework.security:spring-security-oauth2-jose:6.0.2")

	// Springfox
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

	// Other
	implementation("com.auth0:java-jwt:4.3.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("com.cloudinary:cloudinary-http44:1.33.0")

	//Test
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")
	testImplementation("io.mockk:mockk:1.13.4")


}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.getByName<BootJar>("bootJar") {
	enabled = true
}

tasks.getByName<Jar>("jar") {
	enabled = false
}
