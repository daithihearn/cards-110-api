import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
	val kotlinVersion = "1.6.10"
	val springBootVersion = "2.7.7"
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
    id("org.springframework.boot") version "2.7.7"
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

group = "ie.daithi.cards"
java.sourceCompatibility = JavaVersion.VERSION_19

description = "api"

val springBootVersion: String = "2.7.7"
val swaggerVersion: String = "2.9.2"

dependencies {

	// Internal Dependencies

	// External Dependencies

	implementation("org.bouncycastle:bcprov-jdk15on:1.70")
	implementation("com.heroku.sdk:env-keystore:1.1.7")

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
	implementation("org.springframework.security:spring-security-oauth2-resource-server:5.7.3")
	implementation("org.springframework.security:spring-security-oauth2-jose:5.7.3")

	// Springfox
	implementation("io.springfox:springfox-swagger2:$swaggerVersion")
	implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

	// Other
	implementation("com.auth0:java-jwt:4.2.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("com.cloudinary:cloudinary-http44:1.33.0")

	//Test
	testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
	testImplementation("io.mockk:mockk:1.13.2")


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
	enabled = false
}

tasks.getByName<Jar>("jar") {
	enabled = true
}
