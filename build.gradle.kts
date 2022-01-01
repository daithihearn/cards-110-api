import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

buildscript {
	val kotlinVersion = "1.6.10"
	val springBootVersion = "2.6.2"
	repositories {
		mavenLocal()
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
	}
}

plugins {
    id("org.springframework.boot") version "2.6.2"
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
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

base {
	archivesBaseName = "cards-110-api"
}

//val versionFile = Properties()
//versionFile.load(FileInputStream(".env"))

group = "ie.daithi.cards"
//version = "${versionFile.getProperty("CARDS_API_VERSION")}"
java.sourceCompatibility = JavaVersion.VERSION_17

description = "api"

val springBootVersion: String = "2.6.2"
val swaggerVersion: String = "3.0.0"

dependencies {

	// Internal Dependencies

	// External Dependencies

	implementation("org.bouncycastle:bcprov-jdk15on:1.65")
	implementation("com.heroku.sdk:env-keystore:1.1.6")

	// Kotlin dependencies
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")

	// Spring dependencies
	implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-data-redis:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-websocket:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
	implementation("org.springframework.security:spring-security-oauth2-resource-server:5.3.3.RELEASE")
	implementation("org.springframework.security:spring-security-oauth2-jose:5.3.3.RELEASE")

	// Springfox
	implementation("io.springfox:springfox-swagger2:$swaggerVersion")
	implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

	// Other
	implementation("com.sendgrid:sendgrid-java:4.4.7")
	implementation("com.auth0:java-jwt:3.10.2")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
	implementation("org.apache.commons:commons-text:1.8")

	//Test
	testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
	testImplementation("io.mockk:mockk:1.10.2")


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