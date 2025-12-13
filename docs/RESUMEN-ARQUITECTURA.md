# Resumen Ejecutivo - Arquitectura Sistema Ticketero

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2024  
**Estado:** ✅ COMPLETO - Listo para Implementación

---

## 📋 Documentos Generados

| Documento | Ubicación | Estado | Páginas |
|-----------|-----------|--------|---------|
| **Arquitectura Completa** | `docs/ARQUITECTURA.md` | ✅ Completo | ~40 |
| **Diagrama C4** | `docs/diagrams/01-context-diagram.puml` | ✅ Renderizable | - |
| **Diagrama Secuencia** | `docs/diagrams/02-sequence-diagram.puml` | ✅ Renderizable | - |
| **Modelo ER** | `docs/diagrams/03-er-diagram.puml` | ✅ Renderizable | - |

---

## 🎯 Decisiones Clave de Arquitectura

### Stack Tecnológico Seleccionado

| Componente | Tecnología | Justificación Principal |
|------------|------------|------------------------|
| **Backend** | Java 21 + Spring Boot 3.2.11 | Virtual Threads + Ecosistema maduro |
| **Base de Datos** | PostgreSQL 16 | ACID + Row-level locking |
| **Migraciones** | Flyway | Simplicidad + SQL plano |
| **HTTP Client** | RestTemplate | Suficiente para 0.9 msg/s |
| **Containerización** | Docker + Docker Compose | Paridad dev/prod |
| **Build** | Maven 3.9+ | Estándar empresarial |

### 5 ADRs Críticos

1. **ADR-001:** ❌ NO usar Circuit Breakers → Simplicidad 80/20
2. **ADR-002:** ✅ RestTemplate (no WebClient) → Volumen bajo
3. **ADR-003:** ✅ @Scheduled (no RabbitMQ) → Infraestructura simple
4. **ADR-004:** ✅ Flyway → Versionamiento automático
5. **ADR-005:** ✅ Bean Validation → Código declarativo

---

## 🏗️ Arquitectura en Capas

```
┌─────────────────────────────────────┐
│  PRESENTACIÓN (Controllers)         │  ← TicketController, AdminController
├─────────────────────────────────────┤
│  NEGOCIO (Services)                 │  ← TicketService, TelegramService, etc.
├─────────────────────────────────────┤
│  DATOS (Repositories)               │  ← TicketRepository, MensajeRepository
├─────────────────────────────────────┤
│  BASE DE DATOS (PostgreSQL)         │  ← ticket, mensaje, advisor
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  ASÍNCRONA (Schedulers)             │  ← MessageScheduler (60s), QueueProcessor (5s)
└─────────────────────────────────────┘
```

---

## 📊 Modelo de Datos (3 Tablas Core)

### ticket
- **PK:** id (BIGSERIAL)
- **UK:** codigo_referencia (UUID), numero (VARCHAR)
- **FK:** assigned_advisor_id
- **Campos clave:** status, queue_type, position_in_queue

### mensaje
- **PK:** id (BIGSERIAL)
- **FK:** ticket_id
- **Campos clave:** plantilla, estado_envio, fecha_programada, intentos

### advisor
- **PK:** id (BIGSERIAL)
- **UK:** email
- **Campos clave:** status, module_number, assigned_tickets_count

**Relaciones:**
- ticket (1) → mensaje (N)
- advisor (1) → ticket (N)

---

## 🔄 Flujo End-to-End (5 Fases)

1. **Creación de Ticket** (~200ms)
   - Cliente → Terminal → Controller → Service → DB
   - Genera número, calcula posición, programa 3 mensajes

2. **Mensaje 1: Confirmación** (~1-60s)
   - MessageScheduler detecta mensaje PENDIENTE
   - Envía vía Telegram: "✅ Ticket P01, posición #5, 75min"

3. **Progreso de Cola** (variable)
   - QueueProcessor recalcula posiciones cada 5s
   - Si posición ≤ 3 → status = PROXIMO

4. **Mensaje 2: Pre-aviso** (~5s)
   - MessageScheduler envía: "⏰ Pronto será tu turno P01"

5. **Asignación + Mensaje 3** (~5s)
   - QueueProcessor asigna asesor AVAILABLE
   - MessageScheduler envía: "🔔 ES TU TURNO P01! Módulo 3"

---

## 📈 Capacidad y Escalabilidad

### Fase 1 (Actual - Piloto)
- **Sucursales:** 1
- **Tickets/día:** 25,000
- **Mensajes/día:** 75,000 (0.9 msg/s)
- **Asesores concurrentes:** 5
- **Infraestructura:** PostgreSQL + @Scheduled

### Fase 2 (Expansión - 50 sucursales)
- **Tickets/día:** 1,250,000
- **Cambios:** RabbitMQ, Resilience4j, WebClient
- **Infraestructura:** Particionamiento de tablas

### Fase 3 (Nacional - 500+ sucursales)
- **Tickets/día:** 25,000,000+
- **Cambios:** Microservicios, Kafka, Redis, Kubernetes

---

## ⚠️ Limitaciones Conocidas

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| Polling cada 60s | Latencia hasta 60s | Aceptable. Fase 2: RabbitMQ |
| RestTemplate (blocking) | Menor throughput | Suficiente para 0.9 msg/s |
| Sin Circuit Breaker | Fallos Telegram no protegidos | Reintentos simples suficientes |
| Scheduler single-thread | Procesamiento secuencial | Suficiente para 25K tickets/día |

---

## 🚀 Configuración Rápida

### Variables de Entorno Requeridas

```bash
TELEGRAM_BOT_TOKEN=123456:ABC-DEF...
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=ticketero_user
DATABASE_PASSWORD=***
SPRING_PROFILES_ACTIVE=dev
```

### Levantar Entorno Local

```bash
# 1. Clonar repositorio
git clone <repo-url>

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tu TELEGRAM_BOT_TOKEN

# 3. Levantar servicios
docker-compose up -d

# 4. Verificar
curl http://localhost:8080/actuator/health
```

---

## 📝 Endpoints Principales

### API Pública

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/tickets` | Crear ticket |
| GET | `/api/tickets/{uuid}` | Obtener ticket |
| GET | `/api/tickets/{numero}/position` | Consultar posición |

### API Administrativa

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | Dashboard completo |
| GET | `/api/admin/queues/{type}` | Estado de cola |
| GET | `/api/admin/advisors` | Lista de asesores |
| PUT | `/api/admin/advisors/{id}/complete-ticket` | Completar atención |

---

## ✅ Checklist de Validación

### Antes de Implementar

- [ ] Revisar `docs/ARQUITECTURA.md` completo
- [ ] Validar diagramas PlantUML en http://www.plantuml.com/plantuml/
- [ ] Confirmar acceso a Telegram Bot API
- [ ] Aprobar ADRs con equipo técnico
- [ ] Validar volumen esperado (25K tickets/día)

### Durante Implementación

- [ ] Seguir estructura de paquetes definida
- [ ] Implementar Bean Validation en DTOs
- [ ] Configurar Flyway para migraciones
- [ ] Implementar schedulers con @Scheduled
- [ ] Agregar logging en componentes críticos

### Después de Implementar

- [ ] Pruebas de carga (25K tickets/día)
- [ ] Validar latencia de notificaciones (<60s)
- [ ] Monitorear uso de base de datos
- [ ] Documentar métricas de performance

---

## 🎓 Para Nuevos Desarrolladores

### Lectura Obligatoria (Orden)

1. **Este documento** (RESUMEN-ARQUITECTURA.md) - 10 minutos
2. **Sección 1-2** de ARQUITECTURA.md (Resumen + Stack) - 15 minutos
3. **Diagramas** (C4 + Secuencia + ER) - 20 minutos
4. **Sección 4-5** (Capas + Componentes) - 30 minutos
5. **ADRs** (Sección 6) - 15 minutos

**Total:** ~90 minutos para entender arquitectura completa

### Preguntas Frecuentes

**P: ¿Por qué no usamos WebClient si Spring Boot 3 lo recomienda?**  
R: Volumen bajo (0.9 msg/s). RestTemplate es más simple. Ver ADR-002.

**P: ¿Por qué no usamos RabbitMQ para mensajes?**  
R: PostgreSQL como queue es suficiente para 75K mensajes/día. Ver ADR-003.

**P: ¿Cómo se garantiza que un cliente no tenga 2 tickets activos?**  
R: Validación en TicketService (RN-001) + índice en national_id.

**P: ¿Qué pasa si Telegram falla?**  
R: Mensajes quedan PENDIENTES, reintentos automáticos (30s, 60s, 120s). Ver RN-007, RN-008.

---

## 📞 Contactos

- **Arquitecto:** Amazon Q Developer
- **Documento Principal:** `docs/ARQUITECTURA.md`
- **Diagramas:** `docs/diagrams/`
- **Requerimientos:** `docs/REQUERIMIENTOS-FUNCIONALES.md`

---

## 🔗 Enlaces Útiles

- **PlantUML Online:** http://www.plantuml.com/plantuml/
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **PostgreSQL 16 Docs:** https://www.postgresql.org/docs/16/
- **Telegram Bot API:** https://core.telegram.org/bots/api
- **Flyway Docs:** https://flywaydb.org/documentation/

---

**Última actualización:** Diciembre 2024  
**Próximo paso:** PROMPT 3 - Plan Detallado de Implementación

