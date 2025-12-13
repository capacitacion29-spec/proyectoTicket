# Plan Detallado de Implementación - Sistema Ticketero

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2024  
**Tech Lead:** Amazon Q Developer  
**Tiempo Estimado Total:** 11 horas

---

## 📋 Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Estructura del Proyecto](#2-estructura-del-proyecto)
3. [Configuración Inicial](#3-configuración-inicial)
4. [Migraciones de Base de Datos](#4-migraciones-de-base-de-datos)
5. [Implementación por Fases](#5-implementación-por-fases)
6. [Orden de Ejecución Recomendado](#6-orden-de-ejecución-recomendado)
7. [Comandos Útiles](#7-comandos-útiles)
8. [Troubleshooting](#8-troubleshooting)
9. [Checklist Final de Validación](#9-checklist-final-de-validación)

---

## 1. Introducción

### 1.1 Objetivo del Plan

Este documento proporciona un plan de implementación **paso a paso** para construir el Sistema Ticketero completo. Está diseñado para que un desarrollador mid-level pueda seguirlo sin necesidad de consultar documentación adicional.

### 1.2 Alcance

**Incluye:**
- ✅ Estructura completa de carpetas y paquetes Java
- ✅ 3 migraciones SQL (Flyway)
- ✅ Configuración completa (Maven, Spring Boot, Docker)
- ✅ 8 fases de implementación con checklists
- ✅ Ejemplos de código para patrones clave
- ✅ Comandos ejecutables

**No incluye:**
- ❌ Código Java completo de todas las clases (ver PROMPT 4)
- ❌ Tests exhaustivos (solo ejemplos básicos)

### 1.3 Prerequisitos

**Herramientas Requeridas:**
- Java 21 (OpenJDK o Eclipse Temurin)
- Maven 3.9+
- Docker Desktop
- IDE (IntelliJ IDEA / VS Code con extensiones Java)
- Git
- PostgreSQL Client (opcional, para debugging)

**Conocimientos Requeridos:**
- Spring Boot 3.x
- JPA/Hibernate
- REST APIs
- SQL básico
- Docker básico

### 1.4 Cronograma Estimado

| Día | Fases | Tiempo | Entregable |
|-----|-------|--------|------------|
| **Día 1** | Fase 0-4 | 4 horas | Estructura + Entities + DTOs + Repositories |
| **Día 2** | Fase 5-6 | 5 horas | Services + Controllers |
| **Día 3** | Fase 7 | 2 horas | Schedulers + Testing E2E |
| **Total** | 8 fases | **11 horas** | Sistema completo funcional |

---

## 2. Estructura del Proyecto

### 2.1 Árbol de Directorios Completo

```
ticketero/
├── pom.xml                                    # Maven configuration
├── .env                                       # Variables de entorno (gitignored)
├── .gitignore                                 # Git ignore rules
├── docker-compose.yml                         # PostgreSQL + API
├── Dockerfile                                 # Multi-stage build
├── README.md                                  # Instrucciones del proyecto
│
├── src/
│   ├── main/
│   │   ├── java/com/example/ticketero/
│   │   │   │
│   │   │   ├── TicketeroApplication.java    # Main class con @EnableScheduling
│   │   │   │
│   │   │   ├── controller/                   # REST Controllers
│   │   │   │   ├── TicketController.java
│   │   │   │   └── AdminController.java
│   │   │   │
│   │   │   ├── service/                      # Business Logic
│   │   │   │   ├── TicketService.java
│   │   │   │   ├── TelegramService.java
│   │   │   │   ├── QueueManagementService.java
│   │   │   │   ├── AdvisorService.java
│   │   │   │   └── NotificationService.java
│   │   │   │
│   │   │   ├── repository/                   # Data Access
│   │   │   │   ├── TicketRepository.java
│   │   │   │   ├── MensajeRepository.java
│   │   │   │   └── AdvisorRepository.java
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── entity/                   # JPA Entities
│   │   │   │   │   ├── Ticket.java
│   │   │   │   │   ├── Mensaje.java
│   │   │   │   │   └── Advisor.java
│   │   │   │   │
│   │   │   │   ├── dto/                      # DTOs
│   │   │   │   │   ├── TicketCreateRequest.java
│   │   │   │   │   ├── TicketResponse.java
│   │   │   │   │   ├── QueuePositionResponse.java
│   │   │   │   │   ├── DashboardResponse.java
│   │   │   │   │   └── QueueStatusResponse.java
│   │   │   │   │
│   │   │   │   └── enums/                    # Enumerations
│   │   │   │       ├── QueueType.java
│   │   │   │       ├── TicketStatus.java
│   │   │   │       ├── AdvisorStatus.java
│   │   │   │       └── MessageTemplate.java
│   │   │   │
│   │   │   ├── scheduler/                    # Scheduled Tasks
│   │   │   │   ├── MensajeScheduler.java
│   │   │   │   └── QueueProcessorScheduler.java
│   │   │   │
│   │   │   ├── config/                       # Configuration
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   └── TelegramConfig.java
│   │   │   │
│   │   │   └── exception/                    # Exception Handling
│   │   │       ├── TicketNotFoundException.java
│   │   │       ├── TicketActivoExistenteException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot config
│   │       ├── application-dev.yml           # Dev profile
│   │       ├── application-prod.yml          # Prod profile
│   │       │
│   │       └── db/migration/                 # Flyway migrations
│   │           ├── V1__create_ticket_table.sql
│   │           ├── V2__create_mensaje_table.sql
│   │           └── V3__create_advisor_table.sql
│   │
│   └── test/
│       └── java/com/example/ticketero/
│           ├── service/
│           │   ├── TicketServiceTest.java
│           │   └── TelegramServiceTest.java
│           │
│           └── controller/
│               └── TicketControllerTest.java
│
└── docs/                                      # Documentación
    ├── REQUERIMIENTOS-NEGOCIO.md
    ├── REQUERIMIENTOS-FUNCIONALES.md
    ├── ARQUITECTURA.md
    ├── PLAN-IMPLEMENTACION.md
    └── diagrams/
        ├── 01-context-diagram.puml
        ├── 02-sequence-diagram.puml
        └── 03-er-diagram.puml
```

### 2.2 Conteo de Archivos

| Categoría | Cantidad | Descripción |
|-----------|----------|-------------|
| **Entities** | 3 | Ticket, Mensaje, Advisor |
| **Repositories** | 3 | JpaRepository interfaces |
| **Services** | 5 | Lógica de negocio |
| **Controllers** | 2 | REST endpoints |
| **DTOs** | 5 | Request/Response objects |
| **Enums** | 4 | QueueType, TicketStatus, AdvisorStatus, MessageTemplate |
| **Schedulers** | 2 | Procesamiento asíncrono |
| **Config** | 2 | RestTemplate, Telegram |
| **Exceptions** | 3 | Custom + GlobalHandler |
| **Migraciones SQL** | 3 | Flyway scripts |
| **Config Files** | 6 | pom.xml, yml, docker, env |
| **Tests** | 3 | Ejemplos básicos |
| **TOTAL** | **41 archivos** | Proyecto completo |

---

## 3. Configuración Inicial

### 3.1 pom.xml (Maven)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.11</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ticketero</artifactId>
    <version>1.0.0</version>
    <name>Ticketero API</name>
    <description>Sistema de Gestión de Tickets con Notificaciones en Tiempo Real</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway for Database Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```


### 3.2 application.yml

```yaml
spring:
  application:
    name: ticketero-api

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ticketero}
    username: ${DATABASE_USERNAME:dev}
    password: ${DATABASE_PASSWORD:dev123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway maneja el schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Telegram Configuration
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# Logging
logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 3.3 .env (Template)

```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

**⚠️ IMPORTANTE:** Agregar `.env` al `.gitignore`

### 3.4 docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticketero-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev -d ticketero"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ticketero-api
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      DATABASE_USERNAME: dev
      DATABASE_PASSWORD: dev123
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
```

### 3.5 Dockerfile (Multi-stage)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.6 .gitignore

```
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml

# IDE
.idea/
*.iml
.vscode/
.classpath
.project
.settings/

# Environment
.env
*.log

# OS
.DS_Store
Thumbs.db
```

---

## 4. Migraciones de Base de Datos

### 4.1 V1__create_ticket_table.sql

**Ubicación:** `src/main/resources/db/migration/V1__create_ticket_table.sql`

```sql
-- V1__create_ticket_table.sql
-- Tabla principal de tickets

CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    position_in_queue INTEGER NOT NULL,
    estimated_wait_minutes INTEGER NOT NULL,
    assigned_advisor_id BIGINT,
    assigned_module_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);

-- Comentarios para documentación
COMMENT ON TABLE ticket IS 'Tickets de atención en sucursales';
COMMENT ON COLUMN ticket.codigo_referencia IS 'UUID único para referencias externas';
COMMENT ON COLUMN ticket.numero IS 'Número visible del ticket (C01, P15, etc.)';
COMMENT ON COLUMN ticket.position_in_queue IS 'Posición actual en cola (calculada en tiempo real)';
COMMENT ON COLUMN ticket.estimated_wait_minutes IS 'Tiempo estimado de espera en minutos';
```

### 4.2 V2__create_mensaje_table.sql

**Ubicación:** `src/main/resources/db/migration/V2__create_mensaje_table.sql`

```sql
-- V2__create_mensaje_table.sql
-- Tabla de mensajes programados para Telegram

CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) 
        ON DELETE CASCADE
);

-- Índices para performance del scheduler
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);

-- Comentarios
COMMENT ON TABLE mensaje IS 'Mensajes programados para envío vía Telegram';
COMMENT ON COLUMN mensaje.plantilla IS 'Tipo de mensaje: totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno';
COMMENT ON COLUMN mensaje.estado_envio IS 'Estado: PENDIENTE, ENVIADO, FALLIDO';
COMMENT ON COLUMN mensaje.intentos IS 'Cantidad de reintentos de envío';
```

### 4.3 V3__create_advisor_table.sql

**Ubicación:** `src/main/resources/db/migration/V3__create_advisor_table.sql`

```sql
-- V3__create_advisor_table.sql
-- Tabla de asesores/ejecutivos

CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_module_number CHECK (module_number BETWEEN 1 AND 5),
    CONSTRAINT chk_assigned_count CHECK (assigned_tickets_count >= 0)
);

-- Índice para búsqueda de asesores disponibles
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);

-- Foreign key de ticket a advisor (se agrega ahora que advisor existe)
ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_advisor 
    FOREIGN KEY (assigned_advisor_id) 
    REFERENCES advisor(id) 
    ON DELETE SET NULL;

-- Datos iniciales: 5 asesores
INSERT INTO advisor (name, email, status, module_number) VALUES
    ('María González', 'maria.gonzalez@institucion.cl', 'AVAILABLE', 1),
    ('Juan Pérez', 'juan.perez@institucion.cl', 'AVAILABLE', 2),
    ('Ana Silva', 'ana.silva@institucion.cl', 'AVAILABLE', 3),
    ('Carlos Rojas', 'carlos.rojas@institucion.cl', 'AVAILABLE', 4),
    ('Patricia Díaz', 'patricia.diaz@institucion.cl', 'AVAILABLE', 5);

-- Comentarios
COMMENT ON TABLE advisor IS 'Asesores/ejecutivos que atienden clientes';
COMMENT ON COLUMN advisor.status IS 'Estado: AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN advisor.module_number IS 'Número de módulo de atención (1-5)';
COMMENT ON COLUMN advisor.assigned_tickets_count IS 'Cantidad de tickets actualmente asignados';
```

---

## 5. Implementación por Fases

### FASE 0: Setup del Proyecto (30 minutos)

**Objetivo:** Configurar el proyecto base y verificar que compila

#### Tareas

```
[ ] 1. Crear proyecto Maven con estructura de carpetas
[ ] 2. Configurar pom.xml con todas las dependencias
[ ] 3. Crear application.yml con configuración base
[ ] 4. Crear .env con variables de entorno
[ ] 5. Crear docker-compose.yml para PostgreSQL
[ ] 6. Levantar base de datos: docker-compose up -d postgres
[ ] 7. Crear clase principal TicketeroApplication.java
[ ] 8. Verificar compilación: mvn clean compile
[ ] 9. Verificar que conecta a BD: mvn spring-boot:run
```

#### Código: TicketeroApplication.java

```java
package com.example.ticketero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketeroApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketeroApplication.class, args);
    }
}
```

#### Criterios de Aceptación

```
✅ Proyecto compila sin errores
✅ Aplicación inicia y conecta a PostgreSQL
✅ Logs muestran: "Started TicketeroApplication"
✅ Actuator health endpoint responde: curl http://localhost:8080/actuator/health
```

#### Comandos de Validación

```bash
# Compilar
mvn clean compile

# Levantar PostgreSQL
docker-compose up -d postgres

# Verificar PostgreSQL
docker ps | grep ticketero-db

# Ejecutar aplicación
mvn spring-boot:run

# En otra terminal, verificar health
curl http://localhost:8080/actuator/health
```

**Resultado Esperado:**
```json
{
  "status": "UP"
}
```

---

### FASE 1: Migraciones y Enumeraciones (45 minutos)

**Objetivo:** Crear esquema de base de datos y enumeraciones Java

#### Tareas

```
[ ] 1. Crear V1__create_ticket_table.sql
[ ] 2. Crear V2__create_mensaje_table.sql
[ ] 3. Crear V3__create_advisor_table.sql
[ ] 4. Crear enum QueueType.java
[ ] 5. Crear enum TicketStatus.java
[ ] 6. Crear enum AdvisorStatus.java
[ ] 7. Crear enum MessageTemplate.java
[ ] 8. Reiniciar aplicación y verificar migraciones
[ ] 9. Verificar tablas creadas: \dt en psql
[ ] 10. Verificar datos iniciales: SELECT * FROM advisor;
```

#### Código: QueueType.java

```java
package com.example.ticketero.model.enums;

public enum QueueType {
    CAJA("Caja", 5, 1),
    PERSONAL_BANKER("Personal Banker", 15, 2),
    EMPRESAS("Empresas", 20, 3),
    GERENCIA("Gerencia", 30, 4);

    private final String displayName;
    private final int avgTimeMinutes;
    private final int priority;

    QueueType(String displayName, int avgTimeMinutes, int priority) {
        this.displayName = displayName;
        this.avgTimeMinutes = avgTimeMinutes;
        this.priority = priority;
    }

    public String getDisplayName() { return displayName; }
    public int getAvgTimeMinutes() { return avgTimeMinutes; }
    public int getPriority() { return priority; }
}
```

#### Código: TicketStatus.java

```java
package com.example.ticketero.model.enums;

public enum TicketStatus {
    EN_ESPERA,
    PROXIMO,
    ATENDIENDO,
    COMPLETADO,
    CANCELADO
}
```

#### Código: AdvisorStatus.java

```java
package com.example.ticketero.model.enums;

public enum AdvisorStatus {
    AVAILABLE,
    BUSY,
    OFFLINE
}
```

#### Código: MessageTemplate.java

```java
package com.example.ticketero.model.enums;

public enum MessageTemplate {
    TOTEM_TICKET_CREADO,
    TOTEM_PROXIMO_TURNO,
    TOTEM_ES_TU_TURNO
}
```

#### Criterios de Aceptación

```
✅ Flyway ejecuta las 3 migraciones exitosamente
✅ Tabla flyway_schema_history muestra 3 versiones
✅ Tablas ticket, mensaje, advisor existen
✅ 5 asesores iniciales insertados en advisor
✅ 4 enums creadas con valores correctos
```

#### Comandos de Validación

```bash
# Reiniciar aplicación
mvn spring-boot:run

# Conectar a PostgreSQL
docker exec -it ticketero-db psql -U dev -d ticketero

# Verificar migraciones
SELECT * FROM flyway_schema_history;

# Verificar tablas
\dt

# Verificar asesores
SELECT * FROM advisor;

# Salir
\q
```

---

### FASE 2: Entities (1 hora)

**Objetivo:** Crear las 3 entidades JPA mapeadas a las tablas

#### Tareas

```
[ ] 1. Crear Ticket.java con todas las anotaciones JPA
[ ] 2. Crear Mensaje.java con relación a Ticket
[ ] 3. Crear Advisor.java con relación a Ticket
[ ] 4. Usar Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder
[ ] 5. Mapear enums con @Enumerated(EnumType.STRING)
[ ] 6. Configurar relaciones: @OneToMany, @ManyToOne
[ ] 7. Agregar @PrePersist para codigo_referencia UUID
[ ] 8. Compilar y verificar sin errores
```

#### Código: Ticket.java

```java
package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_referencia", nullable = false, unique = true)
    private UUID codigoReferencia;

    @Column(name = "numero", nullable = false, unique = true, length = 10)
    private String numero;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "branch_office", nullable = false, length = 100)
    private String branchOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "position_in_queue", nullable = false)
    private Integer positionInQueue;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private Integer estimatedWaitMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    private Advisor assignedAdvisor;

    @Column(name = "assigned_module_number")
    private Integer assignedModuleNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        codigoReferencia = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### Código: Mensaje.java

```java
package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.MessageTemplate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "plantilla", nullable = false, length = 50)
    private MessageTemplate plantilla;

    @Column(name = "estado_envio", nullable = false, length = 20)
    private String estadoEnvio;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "telegram_message_id", length = 50)
    private String telegramMessageId;

    @Column(name = "intentos", nullable = false)
    private Integer intentos;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estadoEnvio == null) {
            estadoEnvio = "PENDIENTE";
        }
        if (intentos == null) {
            intentos = 0;
        }
    }
}
```

#### Código: Advisor.java

```java
package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.AdvisorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "advisor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advisor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdvisorStatus status;

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    @Column(name = "assigned_tickets_count", nullable = false)
    private Integer assignedTicketsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assignedTicketsCount == null) {
            assignedTicketsCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### Criterios de Aceptación

```
✅ 3 entities creadas con anotaciones JPA correctas
✅ Relaciones bidireccionales configuradas
✅ Proyecto compila sin errores
✅ Hibernate valida el schema al iniciar (no crea tablas por ddl-auto=validate)
```

#### Comandos de Validación

```bash
# Compilar
mvn clean compile

# Ejecutar aplicación
mvn spring-boot:run

# Verificar logs - debe mostrar:
# "Validated JPA managed types"
# "Started TicketeroApplication"
```

---

### FASE 3: DTOs (45 minutos)

**Objetivo:** Crear DTOs para request/response

#### Tareas

```
[ ] 1. Crear TicketCreateRequest.java con Bean Validation
[ ] 2. Crear TicketResponse.java como record
[ ] 3. Crear QueuePositionResponse.java
[ ] 4. Crear DashboardResponse.java
[ ] 5. Crear QueueStatusResponse.java
[ ] 6. Agregar validaciones: @NotBlank, @NotNull, @Pattern
[ ] 7. Compilar y verificar
```

#### Código: TicketCreateRequest.java

```java
package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TicketCreateRequest(
    
    @NotBlank(message = "El RUT/ID es obligatorio")
    String nationalId,
    
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Teléfono debe tener formato +56XXXXXXXXX")
    String telefono,
    
    @NotBlank(message = "La sucursal es obligatoria")
    String branchOffice,
    
    @NotNull(message = "El tipo de cola es obligatorio")
    QueueType queueType
) {}
```

#### Código: TicketResponse.java

```java
package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID codigoReferencia,
    String numero,
    String nationalId,
    String branchOffice,
    QueueType queueType,
    TicketStatus status,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    LocalDateTime createdAt
) {}
```

#### Código: QueuePositionResponse.java

```java
package com.example.ticketero.model.dto;

public record QueuePositionResponse(
    String numero,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    String status
) {}
```

#### Código: DashboardResponse.java

```java
package com.example.ticketero.model.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
    Integer totalTicketsEnEspera,
    Integer totalTicketsAtendiendo,
    Integer totalTicketsCompletadosHoy,
    Map<String, Integer> ticketsPorCola,
    List<AdvisorInfo> asesores
) {
    public record AdvisorInfo(
        String name,
        String status,
        Integer moduleNumber,
        Integer assignedTicketsCount
    ) {}
}
```

#### Código: QueueStatusResponse.java

```java
package com.example.ticketero.model.dto;

import java.util.List;

public record QueueStatusResponse(
    String queueType,
    Integer totalEnEspera,
    Integer avgWaitMinutes,
    List<TicketInfo> tickets
) {
    public record TicketInfo(
        String numero,
        Integer positionInQueue,
        Integer estimatedWaitMinutes
    ) {}
}
```

#### Criterios de Aceptación

```
✅ 5 DTOs creados
✅ Validaciones Bean Validation configuradas
✅ Records usados donde sea apropiado (inmutabilidad)
✅ Proyecto compila sin errores
```

---

### FASE 4: Repositories (30 minutos)

**Objetivo:** Crear interfaces de acceso a datos

#### Tareas

```
[ ] 1. Crear TicketRepository.java extends JpaRepository
[ ] 2. Crear MensajeRepository.java
[ ] 3. Crear AdvisorRepository.java
[ ] 4. Agregar queries custom con @Query
[ ] 5. Métodos: findByCodigoReferencia, findByNationalIdAndStatusIn, etc.
```

#### Código: TicketRepository.java

```java
package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);

    Optional<Ticket> findByNumero(String numero);

    @Query("SELECT t FROM Ticket t WHERE t.nationalId = :nationalId AND t.status IN :statuses")
    Optional<Ticket> findByNationalIdAndStatusIn(
        @Param("nationalId") String nationalId, 
        @Param("statuses") List<TicketStatus> statuses
    );

    @Query("SELECT t FROM Ticket t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Ticket> findByStatusOrderByCreatedAtAsc(@Param("status") TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.queueType = :queueType AND t.status = :status ORDER BY t.createdAt ASC")
    List<Ticket> findByQueueTypeAndStatusOrderByCreatedAtAsc(
        @Param("queueType") QueueType queueType,
        @Param("status") TicketStatus status
    );

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status AND t.createdAt >= :startDate")
    Long countByStatusAndCreatedAtAfter(
        @Param("status") TicketStatus status,
        @Param("startDate") LocalDateTime startDate
    );
}
```

#### Código: MensajeRepository.java

```java
package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("SELECT m FROM Mensaje m WHERE m.estadoEnvio = :estado AND m.fechaProgramada <= :fecha")
    List<Mensaje> findByEstadoEnvioAndFechaProgramadaLessThanEqual(
        @Param("estado") String estado,
        @Param("fecha") LocalDateTime fecha
    );

    List<Mensaje> findByTicketId(Long ticketId);

    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.estadoEnvio = 'FALLIDO' AND m.intentos >= 3")
    Long countMensajesFallidos();
}
```

#### Código: AdvisorRepository.java

```java
package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {

    List<Advisor> findByStatus(AdvisorStatus status);

    @Query("SELECT a FROM Advisor a WHERE a.status = :status ORDER BY a.assignedTicketsCount ASC")
    List<Advisor> findByStatusOrderByAssignedTicketsCountAsc(@Param("status") AdvisorStatus status);

    Optional<Advisor> findByModuleNumber(Integer moduleNumber);

    @Query("SELECT COUNT(a) FROM Advisor a WHERE a.status = :status")
    Long countByStatus(@Param("status") AdvisorStatus status);
}
```

#### Criterios de Aceptación

```
✅ 3 repositories creados
✅ Queries custom documentadas
✅ Proyecto compila sin errores
✅ Métodos siguen convenciones de Spring Data JPA
```

---

### FASE 5: Services (3 horas)

**Objetivo:** Implementar toda la lógica de negocio

#### Tareas

```
[ ] 1. Crear TelegramService.java (envío de mensajes)
[ ] 2. Crear TicketService.java (crear ticket, calcular posición)
[ ] 3. Crear QueueManagementService.java (asignación automática)
[ ] 4. Crear AdvisorService.java (gestión de asesores)
[ ] 5. Crear NotificationService.java (coordinar notificaciones)
[ ] 6. Implementar lógica según RN-001 a RN-013
[ ] 7. Agregar @Transactional donde corresponda
[ ] 8. Logging con @Slf4j
```

#### Orden de Implementación

1. **TelegramService** (sin dependencias)
2. **AdvisorService** (solo repository)
3. **TicketService** (usa TelegramService)
4. **QueueManagementService** (usa TicketService, AdvisorService)
5. **NotificationService** (usa TelegramService)

#### Código: TelegramService.java (Ejemplo Simplificado)

```java
package com.example.ticketero.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final RestTemplate restTemplate;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.api-url}")
    private String apiUrl;

    public String enviarMensaje(String telefono, String mensaje) {
        log.info("Enviando mensaje a {}", telefono);

        String url = apiUrl + botToken + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", telefono);
        body.put("text", mensaje);
        body.put("parse_mode", "HTML");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            var response = restTemplate.postForObject(url, request, Map.class);
            log.info("Mensaje enviado exitosamente");
            return response != null ? response.get("message_id").toString() : null;
        } catch (Exception e) {
            log.error("Error enviando mensaje: {}", e.getMessage());
            throw new RuntimeException("Error al enviar mensaje de Telegram", e);
        }
    }

    public String construirMensajeTicketCreado(String numero, Integer posicion, Integer tiempo) {
        return String.format(
            "✅ <b>Ticket Creado</b>\n\n" +
            "Tu número de turno: <b>%s</b>\n" +
            "Posición en cola: <b>#%d</b>\n" +
            "Tiempo estimado: <b>%d minutos</b>\n\n" +
            "Te notificaremos cuando estés próximo.",
            numero, posicion, tiempo
        );
    }

    public String construirMensajeProximoTurno(String numero) {
        return String.format(
            "⏰ <b>Próximo Turno</b>\n\n" +
            "Tu turno <b>%s</b> está próximo.\n" +
            "Por favor, acércate al área de espera.",
            numero
        );
    }

    public String construirMensajeTurnoActivo(String numero, Integer modulo, String nombreAsesor) {
        return String.format(
            "🔔 <b>Es Tu Turno</b>\n\n" +
            "Turno: <b>%s</b>\n" +
            "Módulo: <b>%d</b>\n" +
            "Asesor: <b>%s</b>\n\n" +
            "Por favor, dirígete al módulo indicado.",
            numero, modulo, nombreAsesor
        );
    }
}
```

#### Código: TicketService.java (Ejemplo Simplificado)

```java
package com.example.ticketero.service;

import com.example.ticketero.exception.TicketActivoExistenteException;
import com.example.ticketero.model.dto.TicketCreateRequest;
import com.example.ticketero.model.dto.TicketResponse;
import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.MensajeRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final MensajeRepository mensajeRepository;

    @Transactional
    public TicketResponse crearTicket(TicketCreateRequest request) {
        log.info("Creando ticket para nationalId: {}", request.nationalId());

        // RN-001: Validar ticket activo existente
        validarTicketActivoExistente(request.nationalId());

        // Generar número según RN-005, RN-006
        String numero = generarNumeroTicket(request.queueType());

        // Calcular posición según RN-010
        int posicion = calcularPosicionEnCola(request.queueType());
        int tiempoEstimado = calcularTiempoEstimado(posicion, request.queueType());

        // Crear y guardar ticket
        Ticket ticket = Ticket.builder()
            .nationalId(request.nationalId())
            .telefono(request.telefono())
            .branchOffice(request.branchOffice())
            .queueType(request.queueType())
            .status(TicketStatus.EN_ESPERA)
            .numero(numero)
            .positionInQueue(posicion)
            .estimatedWaitMinutes(tiempoEstimado)
            .build();

        ticket = ticketRepository.save(ticket);

        // Programar 3 mensajes (si hay teléfono)
        if (request.telefono() != null && !request.telefono().isBlank()) {
            programarMensajes(ticket);
        }

        log.info("Ticket creado: {}", ticket.getNumero());

        return toResponse(ticket);
    }

    private void validarTicketActivoExistente(String nationalId) {
        List<TicketStatus> estadosActivos = List.of(
            TicketStatus.EN_ESPERA, 
            TicketStatus.PROXIMO, 
            TicketStatus.ATENDIENDO
        );
        
        ticketRepository.findByNationalIdAndStatusIn(nationalId, estadosActivos)
            .ifPresent(t -> {
                throw new TicketActivoExistenteException(
                    "Ya tienes un ticket activo: " + t.getNumero()
                );
            });
    }

    private String generarNumeroTicket(QueueType queueType) {
        // RN-005: Prefijo según tipo de cola
        String prefijo = switch (queueType) {
            case CAJA -> "C";
            case PERSONAL_BANKER -> "P";
            case EMPRESAS -> "E";
            case GERENCIA -> "G";
        };

        // RN-006: Contador secuencial (simplificado)
        long count = ticketRepository.count() + 1;
        return String.format("%s%02d", prefijo, count % 100);
    }

    private int calcularPosicionEnCola(QueueType queueType) {
        // RN-010: Contar tickets EN_ESPERA en la misma cola
        List<Ticket> ticketsEnEspera = ticketRepository
            .findByQueueTypeAndStatusOrderByCreatedAtAsc(queueType, TicketStatus.EN_ESPERA);
        
        return ticketsEnEspera.size() + 1;
    }

    private int calcularTiempoEstimado(int posicion, QueueType queueType) {
        // RN-010: posición × tiempo promedio
        return posicion * queueType.getAvgTimeMinutes();
    }

    private void programarMensajes(Ticket ticket) {
        LocalDateTime ahora = LocalDateTime.now();

        // Mensaje 1: Confirmación inmediata
        Mensaje mensaje1 = Mensaje.builder()
            .ticket(ticket)
            .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
            .estadoEnvio("PENDIENTE")
            .fechaProgramada(ahora)
            .intentos(0)
            .build();

        mensajeRepository.save(mensaje1);

        log.info("Mensaje de confirmación programado para ticket {}", ticket.getNumero());
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getCodigoReferencia(),
            ticket.getNumero(),
            ticket.getNationalId(),
            ticket.getBranchOffice(),
            ticket.getQueueType(),
            ticket.getStatus(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getCreatedAt()
        );
    }

    public TicketResponse obtenerTicketPorCodigo(UUID codigoReferencia) {
        Ticket ticket = ticketRepository.findByCodigoReferencia(codigoReferencia)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        return toResponse(ticket);
    }
}
```

#### Criterios de Aceptación

```
✅ 5 services implementados
✅ Reglas de negocio RN-001 a RN-013 aplicadas
✅ Transacciones configuradas correctamente
✅ Logging apropiado en operaciones clave
✅ Manejo de excepciones básico
```

---

### FASE 6: Controllers (2 horas)

**Objetivo:** Exponer API REST

#### Tareas

```
[ ] 1. Crear TicketController.java (endpoints públicos)
[ ] 2. Crear AdminController.java (endpoints administrativos)
[ ] 3. Configurar @RestController, @RequestMapping
[ ] 4. Usar @Valid para validación automática
[ ] 5. ResponseEntity con códigos HTTP apropiados
[ ] 6. Crear GlobalExceptionHandler.java para errores
```

#### Endpoints a Implementar

**TicketController:**
- `POST /api/tickets` - Crear ticket
- `GET /api/tickets/{uuid}` - Obtener ticket
- `GET /api/tickets/{numero}/position` - Consultar posición

**AdminController:**
- `GET /api/admin/dashboard` - Dashboard completo
- `GET /api/admin/queues/{type}` - Estado de cola
- `GET /api/admin/advisors` - Lista asesores

#### Código: TicketController.java

```java
package com.example.ticketero.controller;

import com.example.ticketero.model.dto.TicketCreateRequest;
import com.example.ticketero.model.dto.TicketResponse;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(
        @Valid @RequestBody TicketCreateRequest request
    ) {
        log.info("POST /api/tickets - Creando ticket para {}", request.nationalId());
        
        TicketResponse response = ticketService.crearTicket(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{codigoReferencia}")
    public ResponseEntity<TicketResponse> obtenerTicket(
        @PathVariable UUID codigoReferencia
    ) {
        log.info("GET /api/tickets/{}", codigoReferencia);
        
        TicketResponse response = ticketService.obtenerTicketPorCodigo(codigoReferencia);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

#### Código: GlobalExceptionHandler.java

```java
package com.example.ticketero.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketActivoExistenteException.class)
    public ResponseEntity<Map<String, Object>> handleTicketActivoExistente(
        TicketActivoExistenteException ex
    ) {
        log.warn("Ticket activo existente: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        body.put("errors", errors);
        
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error no controlado: ", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "Error interno del servidor");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
```

#### Código: TicketActivoExistenteException.java

```java
package com.example.ticketero.exception;

public class TicketActivoExistenteException extends RuntimeException {
    public TicketActivoExistenteException(String message) {
        super(message);
    }
}
```

#### Criterios de Aceptación

```
✅ 2 controllers implementados
✅ Validación automática funciona
✅ Manejo de errores centralizado
✅ Códigos HTTP correctos (200, 201, 400, 404, 409)
✅ Logs apropiados en cada endpoint
```

---

### FASE 7: Schedulers (1.5 horas)

**Objetivo:** Implementar procesamiento asíncrono

#### Tareas

```
[ ] 1. Crear MensajeScheduler.java (@Scheduled fixedRate=60000)
[ ] 2. Crear QueueProcessorScheduler.java (@Scheduled fixedRate=5000)
[ ] 3. Configurar @EnableScheduling en clase principal
[ ] 4. Implementar lógica de reintentos (RN-007, RN-008)
[ ] 5. Implementar asignación automática (RN-002, RN-003, RN-004)
[ ] 6. Logging detallado
```

#### Código: MensajeScheduler.java

```java
package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MensajeScheduler {

    private final MensajeRepository mensajeRepository;
    private final TelegramService telegramService;

    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    @Transactional
    public void procesarMensajesPendientes() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Mensaje> mensajesPendientes = mensajeRepository
            .findByEstadoEnvioAndFechaProgramadaLessThanEqual("PENDIENTE", ahora);

        if (mensajesPendientes.isEmpty()) {
            log.debug("No hay mensajes pendientes");
            return;
        }

        log.info("Procesando {} mensajes pendientes", mensajesPendientes.size());

        for (Mensaje mensaje : mensajesPendientes) {
            try {
                enviarMensaje(mensaje);
            } catch (Exception e) {
                log.error("Error procesando mensaje {}: {}", mensaje.getId(), e.getMessage());
            }
        }
    }

    private void enviarMensaje(Mensaje mensaje) {
        Ticket ticket = mensaje.getTicket();
        
        // Validar que el ticket tenga teléfono
        if (ticket.getTelefono() == null || ticket.getTelefono().isBlank()) {
            log.warn("Ticket {} no tiene teléfono, marcando mensaje como FALLIDO", ticket.getNumero());
            mensaje.setEstadoEnvio("FALLIDO");
            mensajeRepository.save(mensaje);
            return;
        }

        // Construir mensaje según plantilla
        String textoMensaje = construirMensaje(mensaje, ticket);

        try {
            // Enviar mensaje
            String messageId = telegramService.enviarMensaje(ticket.getTelefono(), textoMensaje);
            
            // Actualizar mensaje como ENVIADO
            mensaje.setEstadoEnvio("ENVIADO");
            mensaje.setFechaEnvio(LocalDateTime.now());
            mensaje.setTelegramMessageId(messageId);
            mensajeRepository.save(mensaje);
            
            log.info("Mensaje {} enviado exitosamente", mensaje.getId());
            
        } catch (Exception e) {
            // RN-007, RN-008: Manejo de reintentos
            manejarReintento(mensaje, e);
        }
    }

    private String construirMensaje(Mensaje mensaje, Ticket ticket) {
        return switch (mensaje.getPlantilla()) {
            case TOTEM_TICKET_CREADO -> telegramService.construirMensajeTicketCreado(
                ticket.getNumero(),
                ticket.getPositionInQueue(),
                ticket.getEstimatedWaitMinutes()
            );
            case TOTEM_PROXIMO_TURNO -> telegramService.construirMensajeProximoTurno(
                ticket.getNumero()
            );
            case TOTEM_ES_TU_TURNO -> telegramService.construirMensajeTurnoActivo(
                ticket.getNumero(),
                ticket.getAssignedModuleNumber(),
                ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : "N/A"
            );
        };
    }

    private void manejarReintento(Mensaje mensaje, Exception e) {
        int intentosActuales = mensaje.getIntentos();
        
        // RN-007: Máximo 3 reintentos
        if (intentosActuales >= 3) {
            log.error("Mensaje {} falló después de 3 intentos", mensaje.getId());
            mensaje.setEstadoEnvio("FALLIDO");
            mensajeRepository.save(mensaje);
            
            // RN-008: Notificar a soporte (simplificado)
            log.error("ALERTA: Mensaje {} requiere intervención manual", mensaje.getId());
            return;
        }

        // Incrementar contador de intentos
        mensaje.setIntentos(intentosActuales + 1);
        
        // RN-007: Backoff exponencial (30s, 60s, 120s)
        int delaySeconds = switch (intentosActuales + 1) {
            case 1 -> 30;
            case 2 -> 60;
            case 3 -> 120;
            default -> 0;
        };
        
        mensaje.setFechaProgramada(LocalDateTime.now().plusSeconds(delaySeconds));
        mensajeRepository.save(mensaje);
        
        log.warn("Mensaje {} reintentará en {}s (intento {}/3)", 
            mensaje.getId(), delaySeconds, intentosActuales + 1);
    }
}
```

#### Código: QueueProcessorScheduler.java (Simplificado)

```java
package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorScheduler {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;

    @Scheduled(fixedRate = 5000) // Cada 5 segundos
    @Transactional
    public void procesarColas() {
        log.debug("Procesando colas...");

        // Buscar asesores disponibles
        List<Advisor> asesoresDisponibles = advisorRepository
            .findByStatusOrderByAssignedTicketsCountAsc(AdvisorStatus.AVAILABLE);

        if (asesoresDisponibles.isEmpty()) {
            log.debug("No hay asesores disponibles");
            return;
        }

        // Buscar tickets en espera
        List<Ticket> ticketsEnEspera = ticketRepository
            .findByStatusOrderByCreatedAtAsc(TicketStatus.EN_ESPERA);

        if (ticketsEnEspera.isEmpty()) {
            log.debug("No hay tickets en espera");
            return;
        }

        // Asignar tickets a asesores (RN-002, RN-003, RN-004)
        for (Advisor asesor : asesoresDisponibles) {
            if (ticketsEnEspera.isEmpty()) break;

            Ticket ticket = ticketsEnEspera.remove(0);
            asignarTicketAsesor(ticket, asesor);
        }
    }

    private void asignarTicketAsesor(Ticket ticket, Advisor asesor) {
        log.info("Asignando ticket {} a asesor {} (módulo {})", 
            ticket.getNumero(), asesor.getName(), asesor.getModuleNumber());

        // Actualizar ticket
        ticket.setStatus(TicketStatus.ATENDIENDO);
        ticket.setAssignedAdvisor(asesor);
        ticket.setAssignedModuleNumber(asesor.getModuleNumber());
        ticketRepository.save(ticket);

        // Actualizar asesor
        asesor.setAssignedTicketsCount(asesor.getAssignedTicketsCount() + 1);
        asesor.setStatus(AdvisorStatus.BUSY);
        advisorRepository.save(asesor);

        log.info("Ticket {} asignado exitosamente", ticket.getNumero());
    }
}
```

#### Criterios de Aceptación

```
✅ MensajeScheduler procesa mensajes pendientes cada 60s
✅ QueueProcessorScheduler asigna tickets cada 5s
✅ Reintentos funcionan (30s, 60s, 120s backoff)
✅ Asignación respeta prioridades y FIFO
✅ Logging detallado en operaciones clave
```

---

## 6. Orden de Ejecución Recomendado

### Día 1 (4 horas)

```
09:00 - 09:30  │ FASE 0: Setup del Proyecto
09:30 - 10:15  │ FASE 1: Migraciones y Enumeraciones
10:15 - 10:30  │ ☕ Break
10:30 - 11:30  │ FASE 2: Entities
11:30 - 12:15  │ FASE 3: DTOs
12:15 - 13:00  │ 🍽️ Almuerzo
13:00 - 13:30  │ FASE 4: Repositories
```

**Entregable Día 1:** Estructura completa + Modelo de datos + Acceso a datos

### Día 2 (5 horas)

```
09:00 - 12:00  │ FASE 5: Services (3 horas)
12:00 - 13:00  │ 🍽️ Almuerzo
13:00 - 15:00  │ FASE 6: Controllers (2 horas)
```

**Entregable Día 2:** Lógica de negocio + API REST funcional

### Día 3 (2 horas)

```
09:00 - 10:30  │ FASE 7: Schedulers
10:30 - 11:00  │ Testing E2E
11:00 - 11:30  │ Documentación final
```

**Entregable Día 3:** Sistema completo funcional

---

## 7. Comandos Útiles

### 7.1 Maven

```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar (sin tests)
mvn clean package -DskipTests

# Ejecutar aplicación
mvn spring-boot:run

# Limpiar target
mvn clean
```

### 7.2 Docker

```bash
# Levantar PostgreSQL solo
docker-compose up -d postgres

# Ver logs de PostgreSQL
docker-compose logs -f postgres

# Levantar todo (PostgreSQL + API)
docker-compose up --build

# Detener servicios
docker-compose down

# Detener y limpiar volúmenes
docker-compose down -v

# Ver contenedores activos
docker ps

# Entrar al contenedor de PostgreSQL
docker exec -it ticketero-db bash
```

### 7.3 PostgreSQL

```bash
# Conectar a base de datos
docker exec -it ticketero-db psql -U dev -d ticketero

# Ver tablas
\dt

# Describir tabla
\d ticket

# Ver migraciones
SELECT * FROM flyway_schema_history;

# Ver asesores
SELECT * FROM advisor;

# Ver tickets
SELECT numero, status, queue_type FROM ticket;

# Salir
\q
```

### 7.4 Testing Manual (cURL)

```bash
# Health check
curl http://localhost:8080/actuator/health

# Crear ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }' | jq

# Obtener ticket (reemplazar UUID)
curl http://localhost:8080/api/tickets/UUID-AQUI | jq

# Dashboard (cuando esté implementado)
curl http://localhost:8080/api/admin/dashboard | jq
```

### 7.5 Git

```bash
# Inicializar repositorio
git init

# Agregar archivos
git add .

# Commit inicial
git commit -m "feat: setup inicial del proyecto"

# Ver estado
git status

# Ver logs
git log --oneline
```

---

## 8. Troubleshooting

### 8.1 Problemas Comunes

#### Error: "Failed to configure a DataSource"

**Causa:** PostgreSQL no está corriendo o credenciales incorrectas

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker ps | grep ticketero-db

# Si no está corriendo, levantarlo
docker-compose up -d postgres

# Verificar variables de entorno en .env
cat .env
```

#### Error: "Flyway migration failed"

**Causa:** Schema inconsistente o migración con errores

**Solución:**
```bash
# Conectar a PostgreSQL
docker exec -it ticketero-db psql -U dev -d ticketero

# Limpiar schema (CUIDADO: borra todo)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

# Salir y reiniciar aplicación
\q
mvn spring-boot:run
```

#### Error: "Port 8080 already in use"

**Causa:** Otra aplicación usando el puerto

**Solución:**
```bash
# Opción 1: Cambiar puerto en application.yml
server:
  port: 8081

# Opción 2: Matar proceso en puerto 8080 (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Opción 3: Matar proceso en puerto 8080 (Linux/Mac)
lsof -ti:8080 | xargs kill -9
```

#### Error: "Lombok not working"

**Causa:** Plugin de Lombok no instalado en IDE

**Solución:**
```bash
# IntelliJ IDEA
# 1. File > Settings > Plugins
# 2. Buscar "Lombok"
# 3. Instalar y reiniciar

# VS Code
# 1. Instalar extensión "Lombok Annotations Support"
# 2. Reiniciar VS Code
```

### 8.2 Verificación de Salud del Sistema

```bash
# Script de verificación completa
#!/bin/bash

echo "=== Verificando Sistema Ticketero ==="

# 1. PostgreSQL
echo "1. PostgreSQL..."
docker ps | grep ticketero-db && echo "✅ PostgreSQL OK" || echo "❌ PostgreSQL DOWN"

# 2. API Health
echo "2. API Health..."
curl -s http://localhost:8080/actuator/health | grep UP && echo "✅ API OK" || echo "❌ API DOWN"

# 3. Tablas
echo "3. Tablas..."
docker exec ticketero-db psql -U dev -d ticketero -c "\dt" | grep ticket && echo "✅ Tablas OK" || echo "❌ Tablas MISSING"

# 4. Migraciones
echo "4. Migraciones..."
docker exec ticketero-db psql -U dev -d ticketero -c "SELECT COUNT(*) FROM flyway_schema_history" | grep 3 && echo "✅ Migraciones OK" || echo "❌ Migraciones INCOMPLETE"

echo "=== Verificación Completa ==="
```

---

## 9. Checklist Final de Validación

### 9.1 Funcionalidad Core

```
[ ] Crear ticket con datos válidos retorna 201
[ ] Crear ticket con RUT duplicado retorna 409
[ ] Crear ticket sin teléfono funciona (sin mensajes)
[ ] Obtener ticket por UUID retorna datos correctos
[ ] Número de ticket sigue formato correcto (C01, P15, etc.)
[ ] Posición en cola se calcula correctamente
[ ] Tiempo estimado se calcula correctamente
```

### 9.2 Notificaciones

```
[ ] Mensaje de confirmación se programa al crear ticket
[ ] MensajeScheduler procesa mensajes cada 60s
[ ] Reintentos funcionan con backoff exponencial
[ ] Mensajes fallidos se marcan como FALLIDO después de 3 intentos
```

### 9.3 Asignación Automática

```
[ ] QueueProcessorScheduler asigna tickets cada 5s
[ ] Tickets se asignan a asesores disponibles
[ ] Asignación respeta orden FIFO
[ ] Estado de asesor cambia a BUSY al asignar ticket
```

### 9.4 Base de Datos

```
[ ] 3 migraciones ejecutadas exitosamente
[ ] 5 asesores iniciales insertados
[ ] Índices creados correctamente
[ ] Foreign keys funcionan
```

### 9.5 Configuración

```
[ ] application.yml configurado correctamente
[ ] .env con variables de entorno
[ ] docker-compose levanta PostgreSQL
[ ] Dockerfile construye imagen correctamente
```

### 9.6 Código

```
[ ] 3 entities con anotaciones JPA
[ ] 3 repositories con queries custom
[ ] 5 services con lógica de negocio
[ ] 2 controllers con endpoints REST
[ ] 2 schedulers con procesamiento asíncrono
[ ] GlobalExceptionHandler maneja errores
```

### 9.7 Testing

```
[ ] mvn clean compile sin errores
[ ] mvn test pasa tests básicos
[ ] mvn spring-boot:run inicia aplicación
[ ] curl health endpoint retorna UP
[ ] Crear ticket vía cURL funciona
```

---

## 10. Próximos Pasos

### Después de Completar Este Plan

1. **PROMPT 4:** Implementación de código completo de todas las clases
2. **Testing Exhaustivo:** Tests unitarios y de integración
3. **Documentación API:** Swagger/OpenAPI
4. **Monitoreo:** Métricas con Actuator + Prometheus
5. **CI/CD:** Pipeline de despliegue automatizado

### Mejoras Futuras

- Autenticación y autorización (Spring Security)
- Rate limiting para API pública
- Cache con Redis
- Mensajería con RabbitMQ/Kafka
- Frontend con React/Angular
- Despliegue en AWS/Azure

---

**Documento generado:** Diciembre 2024  
**Versión:** 1.0  
**Estado:** Completo y listo para ejecución

