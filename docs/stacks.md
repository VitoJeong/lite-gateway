# 기술 스택 

> 이 문서는 현재 개발 중인 프로젝트의 기술 스택을 정리한다. 각 기술의 역할과 고려사항을 포함한다.

| 항목                     | 구체화 / 보완 설명                                                              | 추가로 고려해볼 것                                              |
| ---------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------- |
| **Kotlin**             | - 주요 언어- 코루틴(필요시)으로 비동기 처리 가능                                            | - 코루틴을 어디까지 쓸지 (WebFlux와 충돌 X)                          |
| **Spring Boot**        | - 프레임워크- `3.x` 버전대 (Kotlin 친화적)                                          | - BOM 관리 (spring-boot-dependencies)                     |
| **Spring Actuator**    | - 메트릭스, 헬스체크, 관리 엔드포인트 제공                                                | - `/actuator/metrics` 외에 필요한 엔드포인트 (`/health`, `/info`) |
| **WebFlux** (Reactive) | - 논블로킹 처리, 높은 동시성                                                        | - 동기식(WebMVC)과의 비교도 간단히 문서화- 리액티브 체인 복잡도 관리             |
| **Gradle**             | - 빌드/의존성 관리- Kotlin DSL(`build.gradle.kts`) 활용 가능                        | - Multi-Module (예: core, gateway, mock-server) 구조 고려    |
| **Test**               | - 단위테스트: JUnit5, Mockito- 통합테스트: WebTestClient or RestAssured            | - k6/JMeter 부하테스트도 “트래픽 관리” 시각에서 함께 정리                  |
| **DDD**                | - Clean Architecture나 Hexagonal Architecture로 구조화- 도메인/어댑터/애플리케이션 레이어 분리 | - 작은 범위의 DDD 적용: 너무 과하지 않게, 핵심만!                        |
| **시각화/모니터링**           | - Prometheus: Actuator 메트릭 scrape- Grafana: 대시보드 구성- 로그 기반: ELK/Loki(선택) | - 로그 포맷 정리 (MDC 기반 JSON 등)- Alertmanager(선택)            |


## 라이브러리 의존성

| 목적                       | 의존성                                                | 설명                                             |
| ------------------------ | -------------------------------------------------- | ---------------------------------------------- |
| 리액티브 Core                | `spring-boot-starter-webflux`                      | 필수. 리액티브 기반 API 라우팅 및 필터링                      |
| JSON 직렬화                 | `jackson-module-kotlin`                            | Kotlin 사용 시 Jackson의 Kotlin 지원을 추가             |
| 코루틴 지원                   | `org.jetbrains.kotlinx:kotlinx-coroutines-reactor` | Reactor ↔ 코루틴 interop                          |
| validation               | `spring-boot-starter-validation`                   | Request/DTO 검증용. `@Valid`, `@Validated` 사용     |
| Spring Configuration DSL | `spring-boot-configuration-processor`              | `@ConfigurationProperties` 메타데이터 생성 (IDE 자동완성) |
