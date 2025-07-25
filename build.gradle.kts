plugins {
	kotlin("jvm") version "2.1.21"
	kotlin("plugin.spring") version "2.1.21"
	kotlin("plugin.serialization") version "2.1.21" // Kotlinx Serialization 플러그인 추가
	// 코틀린에서 annotation processor를 실행하기 위한 플러그인
	// -> 동으로 annotation processor를 실행하고, 생성된 코드를 컴파일할 수 있다.
	kotlin("kapt") version "2.1.21"

	id("org.springframework.boot") version "3.3.12"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.jazzybyte"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	// Kotlinx Serialization 의존성 추가(Jackson 대체)
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// 하위 모듈
	implementation(project(":lite-gateway-core"))
	implementation(project(":lite-gateway-spring-boot-autoconfigure"))
	implementation(project(":lite-gateway-spring-boot-starter"))
	implementation(project(":lite-gateway-server-webflux"))
	implementation(project(":lite-gateway-sample"))

	kapt("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(17)) // 💡 여기
	}
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
