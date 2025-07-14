# 프로젝트 기술 스택 및 라이브러리 선정

> Kotlin + Spring Boot 기반 프로젝트의 기술 스택 버전 선정 과정과 주요 라이브러리 선택 이유를 정리한 문서이다.

## **에코 시스템 버전 선택**

### 1. Kotlin: 2.1.21
- 가장 최신의 안정적인 버전
- 24 10월 릴리즈 2.1.0 
- Jaa17 과의 호환성(안정적이고 검증된 조합)

### 2. Spring Boot: 3.3.12
- Spring 에코시스템에서 검증된 안정성
- 외부 시스템과의 호환성 고려

--- 

## 직렬화 라이브러리 선택 (Jackson vs kotlinx.serialization)

#### 결론: kotlinx.serialization

### 비교 요약

| 항목             | Jackson                           | kotlinx.serialization                   |
| -------------- | --------------------------------- | --------------------------------------- |
| Kotlin 최적화     | ❌ 보완 필요 (`jackson-module-kotlin`) | ✅ Kotlin 전용, null-safe, sealed class 지원 |
| 성능             | 😐 리플렉션 기반, 오버헤드 있음               | ✅ 정적 직렬화, 빠르고 경량                        |
| 컴파일 타임 안전성     | ❌ 대부분 런타임 오류                      | ✅ 프로퍼티 누락 등 컴파일 타임 검출                   |
| Spring Boot 통합 | ✅ 기본 포함                           | ❌ 커스텀 WebFlux 설정 필요                     |
| Kotlin 2.x 호환성 | ⚠️ 미흡 (실험적 수준)                    | ✅ 공식 지원, K2 대응 완료                       |

## 선택 이유
- 프로젝트는 Kotlin 2.1.21 기반
- 정적 타입 안전성과 고성능 직렬화가 중요
- Jackson은 Kotlin 2.x에서 호환성 이슈 존재


--- 

## 로깅 라이브러리 선택 (oshai vs microutils)

#### 결론: oshai + log4j2


| 항목                   | oshai                            | microutils (kotlin-logging)       |
|----------------------|--------------------------------|--------------------------------|
| 유지보수               | ✅ 활발함                       | ❌ 유지보수 중단 선언             |
| Kotlin Multiplatform 지원 | ✅ KMP 공식 지원                 | ❌ JVM 한정                      |
| 최신 SLF4J 대응         | ✅ SLF4J 2.x 호환               | ⚠️ SLF4J 2.x 일부 미지원          |
| 코드 간결성             | ✅ 람다 기반 lazy logging     | ✅ 동일 (기능적으로 유사)         |
| 외부 도입 사례           | 증가 추세                       | 주로 Spring 레거시에서 사용됨     |

## 선택 이유

- 프로젝트가 Kotlin 2.1.21 기반
- 최신 SLF4J 2.x와의 호환성을 고려
- K2 컴파일러와의 호환성을 확보하기 위해 oshai 선택
- microutils는 유지보수 중단 상태로, 신규 프로젝트에서 권장되지 않음
- log4j2는
    - 비동기 로깅 지원
    - 고부하 환경에서도 안정적인 성능
    - 설정 유연성 (XML, JSON, YAML 지원)
- oshai는 log4j2와 자연스럽게 통합 가능
- Spring Boot 3.3.x 에서 SLF4J 2.x와 호환성 확보


