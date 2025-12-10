plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"
description = "Town of Babel - software quality metrics visualisation tool"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // Swagger / OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.opencsv:opencsv:5.8")

	implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
	implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
	implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.1.202505142326-r")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}