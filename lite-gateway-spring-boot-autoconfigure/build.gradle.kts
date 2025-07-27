plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21" // Kotlinx Serialization 플러그인 추가

    // 코틀린에서 annotation processor를 실행하기 위한 플러그인
    // -> 동적으로 annotation processor를 실행하고, 생성된 코드를 컴파일할 수 있다.
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

repositories {
    mavenCentral()
}

dependencies {
    api(project(":lite-gateway-core"))
    api(project(":lite-gateway-server-webflux"))
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // KotlinFixture (data class 자동 생성)
    testImplementation("com.appmattus.fixture:fixture:1.2.0")
    // MockK (mocking 도구)
    testImplementation("io.mockk:mockk:1.13.17")
    // Kotlin reflection (Fixture 내부에서 필요할 수 있음)
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
}

sourceSets {
    test {
        java.srcDirs("src/test/kotlin")
    }
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
