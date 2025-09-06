
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

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        // log4j-slf4j2-impl 모듈을 사용하기 때문에 log4j-to-slf4j 모듈은 제외
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Kotlinx Serialization 의존성 추가(Jackson 대체)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.3")

    // 로깅 관련 의존성
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.1")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("io.github.classgraph:classgraph:4.8.179")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
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
