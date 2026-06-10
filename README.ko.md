# Live Coverage 플러그인

**언어:** [English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

JaCoCo 기반 IntelliJ IDEA 실시간 코드 커버리지 시각화 플러그인입니다.

## 기능

### 주요 기능
- **실시간 시각화**: 애플리케이션 실행 중 실제 실행 코드 자동 하이라이트
- **누적 커버리지 모드**: 실행 결과를 누적하고 "Clear Coverage"로 초기화 가능
- **JaCoCo Agent 내장**: agent JAR 내장, 별도 다운로드 불필요
- **툴 윈도우 제공**: 커버리지 통계 및 메서드 단위 실행 정보 표시
- **수동 제어**: Refresh / Clear(리셋) / 폴링 일시정지·재개
- **멀티 모듈 지원**: `Source output path` + `Classes output path` 다중 쌍 설정

### 해결하는 문제
- 유지보수가 어려운 복잡한 레거시 비즈니스 코드
- 증분 개발 및 요구사항 변경 시 영향 범위 분석
- 회귀 테스트 전 테스트 데이터 준비와 버그 위치 파악
- "이번 요청이 실제로 어떤 비즈니스 코드를 실행했는가?"를 빠르게 확인

### 핵심 가치
- **요청-코드 경로 가시화와 이해 비용 절감**: 실행된 코드 블록을 빠르게 식별
- **회귀 테스트 효율 향상**: 실제 실행 경로로 코드/요구사항 영향을 분석한 뒤 테스트 데이터를 준비
- **비즈니스 버그 위치 파악**: 레거시 동작을 더 빠르게 이해하고 문제가 되는 코드 조각을 신속히 찾음

## 설치

[JetBrains Marketplace](https://plugins.jetbrains.com)에서 설치(**Live Coverage** 검색). 30일 체험판 및 월간 구독을 제공합니다.

로컬 개발 빌드는 [빌드](#빌드) 섹션을 참고하세요.

## 설정

1. **플러그인 설정**:
   - `Settings` -> `Tools` -> `Live Coverage`
   - TCP Address 설정(기본값: 127.0.0.1)
   - TCP Port 설정(기본값: 6300)
   - 하나 이상의 경로 쌍 추가:
     - `Source output path` (Java 소스 루트 절대 경로)
     - `Classes output path` (컴파일된 `.class` 절대 경로)

2. **애플리케이션 실행**:
   - **자동 주입**: JVM 인수가 실행 구성에 자동 추가
   - Java 실행/디버그 시 JaCoCo JVM 인수 자동 주입
   - 수동 설정 불필요

## 사용법

### 자동 모드
프로젝트를 열면 자동으로 폴링이 시작되며, 코드 실행에 따라 커버리지가 실시간 누적됩니다.

### 수동 제어
`Tools` -> `Live Coverage`에서 사용:
- **Refresh Coverage**: 수동 커버리지 업데이트
- **Clear Coverage**: 하이라이트 전체 제거 및 JaCoCo agent 리셋
- **Pause/Resume Coverage**: 자동 폴링 중지/재개

### 툴 윈도우
하단 "Request Coverage"에서 확인 가능:
- 커버리지 통계
- 메서드 단위 실행 정보
- 연결 상태
- `Clear Coverage` 및 `Pause/Resume Coverage` 버튼

### 핵심 포인트
- ✅ **Agent 내장**: JaCoCo agent JAR 수동 다운로드 불필요
- ✅ **JVM 인수 자동 설정**: 실행 구성에 자동 반영
- ✅ **누적 모드**: 실행 결과 자동 누적
- ✅ **수동 리셋**: 언제든 초기화 가능
- ✅ **실시간 업데이트**: 실행 경로 즉시 확인
- ✅ **실무형 설계**: 스레드 안전·리소스 관리 아키텍처

## 빌드

```bash
./gradlew buildPlugin
```

결과물은 `build/distributions/`에 생성됩니다.

## 개발

### 프로젝트 구조
- **소스 코드**: `src/main/java/`
- **리소스**: `src/main/resources/`
- **빌드 설정**: `build.gradle.kts`

### 코드 스타일
- IntelliJ Platform 코딩 규약 준수
- Java 21 사용
- 모든 서비스는 스레드 안전 설계
- `@NotNull` / `@Nullable` 기반 null 안정성

## 문제 해결

### 커버리지가 표시되지 않을 때
1. JaCoCo agent 실행 및 접근 가능 여부 확인
2. TCP 주소/포트 설정 확인
3. classes output path 정확성 확인
4. source output path와 소스 구조 일치 여부 확인

### 연결 오류
- JaCoCo agent의 TCP 설정 확인
- 방화벽/네트워크 설정 확인
- IDE 로그 확인(Help -> Show Log in Finder/Explorer)

## 라이선스

Copyright (c) 2026 Showen. 최종 사용자 라이선스: [EULA.md](EULA.md).

## 감사의 글

기반 기술:
- IntelliJ Platform SDK
- JaCoCo Code Coverage Library
