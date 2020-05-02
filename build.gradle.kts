import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
	val kotlinVersion = "1.3.50"
	val springBootVersion = "2.2.6.RELEASE"
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
	id("org.springframework.boot") version "2.2.6.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id("maven-publish")
	kotlin("jvm") version "1.3.61"
	kotlin("plugin.spring") version "1.3.71"
}

apply(plugin = "maven")
apply(plugin = "org.springframework.boot")
apply(plugin = "kotlin")

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

repositories {
	mavenLocal()
	mavenCentral()
	maven(url = "https://jitpack.io")
	flatDir {
		dirs("libs")
	}
}

group = "ie.daithi.cards"
version = "0.0.1-SNAPSHOT"

description = "api"

val springBootVersion: String = "2.2.6.RELEASE"
val swaggerVersion: String = "2.9.2"

dependencies {

	// Internal Dependencies
	implementation("ie.daithi.cards:cards-110-frontend:$version")

	//External Dependencies

	//Kotlin dependencies
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.72")
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")

	//Spring dependencies
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-websocket:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-data-redis:$springBootVersion")
	testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

	//Springfox
	implementation("io.springfox:springfox-swagger2:$swaggerVersion")
	implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

	// Other
	implementation("com.sendgrid:sendgrid-java:4.4.7")
	implementation("com.auth0:java-jwt:3.10.2")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
	implementation("org.apache.commons:commons-text:1.8")


}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "12"
	}
}