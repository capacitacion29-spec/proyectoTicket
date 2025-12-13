# Mejoras Técnicas Aplicadas - Requerimientos Funcionales

**Proyecto:** Sistema Ticketero Digital  
**Documento:** REQUERIMIENTOS-FUNCIONALES.md  
**Fecha:** Diciembre 2024  
**Versión:** 1.1 (con mejoras técnicas)

---

## 📋 Resumen de Mejoras

Se han incorporado 5 mejoras técnicas críticas al documento de Requerimientos Funcionales, manteniendo el enfoque funcional sin prescribir tecnologías específicas.

---

## 🔒 MEJORA 1: Protección de Datos Sensibles (RN-014)

### Problema Identificado
El modelo de datos incluye `nationalId` (RUT/ID) y `telefono`, que son datos personales sensibles. El RNF-005 del documento de negocio exige encriptación, pero no estaba especificado en requerimientos funcionales.

### Solución Implementada

**Nueva Regla de Negocio: RN-014**
```
Protección de Datos Sensibles
- Cifrado en Tránsito: TLS/HTTPS obligatorio
- Cifrado en Reposo: Datos sensibles cifrados en BD
- Búsquedas Seguras: Hashing para índices sin comprometer seguridad
- Enmascaramiento: Logs y respuestas HTTP muestran datos parciales
```

**Mecanismos Sugeridos (sin prescribir tecnología):**
- Hashing unidireccional para índices de búsqueda
- Cifrado reversible para almacenamiento
- Tokenización para referencias externas
- Enmascaramiento en logs (****5678-9)

**Ejemplo de Almacenamiento:**
```
nationalId (original): "12345678-9"
nationalId_hash (búsqueda): "a3f5b2c1..." (SHA-256)
nationalId_encrypted (almacenamiento): "enc_xyz123..." (AES-256)
nationalId_masked (logs): "****5678-9"
```

### Impacto en RF-001

**Modelo de Datos:**
- Campo `nationalId` ahora especifica que se almacena cifrado
- Campo `telefono` también se cifra en reposo

**Respuesta HTTP 201:**
```json
{
  "identificador": "uuid",
  "numero": "C01",
  "nationalIdMasked": "****5678-9",  // ← NUEVO: enmascarado
  "assignedAdvisorId": null
}
```

**Nuevo Escenario Gherkin (Escenario 10):**
```gherkin
Escenario 10: Seguridad - Enmascaramiento de datos sensibles
Given el cliente crea un ticket exitosamente
Then el campo nationalIdMasked debe mostrar "****5678-9"
And el nationalId completo NO debe aparecer en respuesta JSON
And el nationalId completo debe almacenarse cifrado en BD
```

### Valor Agregado
- ✅ Cumplimiento de normativas de privacidad (GDPR, CCPA)
- ✅ Protección contra fugas de datos en logs
- ✅ Búsquedas eficientes sin comprometer seguridad
- ✅ Auditoría sin exponer datos sensibles

---

## ⚡ MEJORA 2: Cálculo de Posición de Alto Rendimiento (RN-015)

### Problema Identificado
En Fase Nacional (25,000+ tickets/día), calcular `positionInQueue` con `COUNT(*)` en PostgreSQL puede volverse lento y no cumplir con RNF-002 (< 1s para cálculo de posición).

### Solución Implementada

**Nueva Regla de Negocio: RN-015**
```
Cálculo de Posición de Alto Rendimiento
- Tiempo de respuesta: < 1 segundo
- Escalabilidad: 25,000+ tickets/día sin degradación
- Consistencia: Posición precisa en tiempo real
```

**Estrategias de Optimización Sugeridas:**
- Caché de estado de colas en memoria de alta velocidad
- Contadores incrementales por cola (evitar COUNT(*))
- Snapshot periódico del estado de colas
- Índices optimizados para consultas de posición

**Invalidación de Caché:**
- Al crear nuevo ticket
- Al asignar ticket a asesor
- Al cambiar estado (COMPLETADO, CANCELADO)
- Máximo 5 segundos de desfase aceptable

### Impacto en RF-001

**Postcondiciones:**
```
- Caché de estado de colas actualizado
```

**Nuevo Escenario Gherkin (Escenario 11):**
```gherkin
Escenario 11: Performance - Cálculo en menos de 1 segundo
Given la cola de tipo CAJA tiene 100 tickets EN_ESPERA
When el cliente crea un nuevo ticket
Then el sistema calcula positionInQueue en menos de 1 segundo
And el cálculo usa caché (no COUNT(*) directo)
```

### Valor Agregado
- ✅ Performance garantizada en alta carga
- ✅ Escalabilidad para Fase Nacional
- ✅ Experiencia de usuario fluida (< 1s)
- ✅ Reducción de carga en BD transaccional

---

## 🔄 MEJORA 3: Flujo Asíncrono de Notificaciones (Evento TicketCreatedEvent)

### Problema Identificado
RF-001 finaliza con "programación de 3 mensajes de Telegram", pero no especificaba si era síncrono o asíncrono. Llamadas síncronas al servicio de notificaciones aumentan latencia y acoplamiento.

### Solución Implementada

**Postcondición Actualizada:**
```
- Evento "TicketCreatedEvent" publicado en message broker (asíncrono)
```

**Evento Publicado:**
```json
{
  "eventType": "TicketCreated",
  "timestamp": "2024-12-15T10:30:00Z",
  "payload": {
    "codigoReferencia": "uuid",
    "numero": "C01",
    "telefono": "+56912345678",
    "queueType": "CAJA",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "branchOffice": "Sucursal Centro"
  }
}
```

**Nota Explicativa:**
```
El servicio de notificaciones escuchará este evento para programar 
los 3 mensajes de Telegram de forma asíncrona, desacoplando la 
creación del ticket del envío de notificaciones.
```

### Impacto en Arquitectura

**Desacoplamiento:**
- Servicio de Tickets NO llama directamente a Servicio de Notificaciones
- Comunicación vía Message Broker (Kafka, RabbitMQ, etc.)
- Fallo en notificaciones NO afecta creación de ticket

**Nuevos Términos en Glosario:**
- Message Broker: Sistema de mensajería asíncrona
- Evento: Notificación asíncrona de cambio de estado

### Valor Agregado
- ✅ Reducción de latencia en creación de ticket
- ✅ Desacoplamiento entre microservicios
- ✅ Resiliencia: fallo en notificaciones no bloquea ticket
- ✅ Escalabilidad independiente de servicios

---

## 🏷️ MEJORA 4: Consistencia en Nombres de Campos (assignedAdvisorId)

### Problema Identificado
El campo `assignedAdvisor` era ambiguo: ¿es un ID o un objeto completo del asesor?

### Solución Implementada

**Antes:**
```
| assignedAdvisor | Relación | Referencia a entidad Advisor | null |
```

**Después:**
```
| assignedAdvisorId | UUID | ID del asesor asignado | null |
```

**Respuesta HTTP Actualizada:**
```json
{
  "assignedAdvisorId": null  // ← Claramente es un UUID, no un objeto
}
```

### Valor Agregado
- ✅ Claridad en contratos de API
- ✅ Sin ambigüedad para desarrolladores
- ✅ Consistencia con convenciones REST
- ✅ Facilita generación de código (Swagger/OpenAPI)

---

## 🔐 MEJORA 5: Atomicidad y Prevención de Condiciones de Carrera (RN-001 Ampliada)

### Problema Identificado
RN-001 especificaba unicidad de ticket activo, pero no garantizaba atomicidad ante solicitudes concurrentes (doble clic, múltiples terminales).

### Solución Implementada

**RN-001 Ampliada con:**

**Requisitos de Atomicidad:**
- Verificación + creación deben ser operaciones atómicas
- Prevenir condiciones de carrera para mismo nationalId
- Source of truth única (no confiar en validaciones de cliente)
- Solo una solicitud concurrente debe tener éxito

**Escenarios de Concurrencia:**
- Cliente presiona botón dos veces rápidamente
- Dos terminales diferentes para mismo nationalId
- Solicitud duplicada por timeout de red

**Mecanismos de Garantía Sugeridos:**
- Bloqueo pesimista (SELECT FOR UPDATE)
- Constraint de unicidad en BD
- Lock distribuido a nivel de aplicación
- Idempotency key para reintentos

### Impacto en RF-001

**Nuevos Escenarios Gherkin:**

**Escenario 8: Solicitudes concurrentes**
```gherkin
Given 2 solicitudes simultáneas para mismo nationalId
Then solo UNA tiene éxito (HTTP 201)
And la otra recibe HTTP 409
And solo existe 1 ticket en BD
```

**Escenario 9: Idempotencia (doble clic)**
```gherkin
Given cliente presiona "Crear Ticket" dos veces rápidamente
Then primera solicitud crea ticket (HTTP 201)
And segunda solicitud rechazada (HTTP 409)
And cliente ve ticket sin duplicados
```

### Valor Agregado
- ✅ Prevención de duplicación de tickets
- ✅ Sistema resiliente ante solicitudes concurrentes
- ✅ Comportamiento atómico verificable con tests
- ✅ Flexibilidad en implementación (4 opciones)

---

## 📊 Resumen de Cambios en RF-001

### Reglas de Negocio
| Antes | Después |
|-------|---------|
| 4 RN aplicadas | 6 RN aplicadas |
| RN-001, RN-005, RN-006, RN-010 | + RN-014 (Seguridad), RN-015 (Performance) |

### Escenarios Gherkin
| Antes | Después |
|-------|---------|
| 9 escenarios | 11 escenarios |
| - | + Escenario 10 (Enmascaramiento) |
| - | + Escenario 11 (Performance < 1s) |

### Modelo de Datos
| Campo | Antes | Después |
|-------|-------|---------|
| assignedAdvisor | Relación (ambiguo) | assignedAdvisorId (UUID claro) |
| nationalId | String | String (cifrado en reposo) |
| telefono | String | String (cifrado en reposo) |

### Respuesta HTTP 201
| Campo | Antes | Después |
|-------|-------|---------|
| nationalId | "12345678-9" (completo) | nationalIdMasked: "****5678-9" |
| assignedAdvisor | null | assignedAdvisorId: null |

### Postcondiciones
| Antes | Después |
|-------|---------|
| 3 mensajes programados | Evento TicketCreatedEvent publicado (asíncrono) |
| - | Datos sensibles cifrados |
| - | Caché de colas actualizado |

---

## 🎯 Impacto por Stakeholder

### Para Desarrolladores
- ✅ **Seguridad:** Especificaciones claras de cifrado y enmascaramiento
- ✅ **Performance:** Estrategias de optimización documentadas
- ✅ **Arquitectura:** Flujo asíncrono con eventos explícito
- ✅ **Atomicidad:** 4 opciones de implementación para prevenir race conditions

### Para QA/Testers
- ✅ **Escenario 10:** Validar enmascaramiento de datos sensibles
- ✅ **Escenario 11:** Validar performance < 1s con 100+ tickets
- ✅ **Escenarios 8-9:** Validar atomicidad y prevención de duplicados

### Para Arquitectos
- ✅ **RN-014:** Guía para diseño de seguridad de datos
- ✅ **RN-015:** Requisitos de caché y optimización
- ✅ **Eventos:** Patrón de comunicación asíncrona entre servicios

### Para Compliance/Legal
- ✅ **RN-014:** Cumplimiento de normativas de privacidad
- ✅ **Enmascaramiento:** Protección en logs y auditoría
- ✅ **Cifrado:** En tránsito (TLS) y en reposo

---

## 📈 Métricas Actualizadas

| Métrica | Antes | Después | Incremento |
|---------|-------|---------|------------|
| Reglas de Negocio | 13 | 15 | +2 (RN-014, RN-015) |
| Escenarios RF-001 | 9 | 11 | +2 (Seguridad, Performance) |
| Términos en Glosario | 9 | 14 | +5 (Message Broker, Evento, Cifrado, etc.) |
| Páginas Documento | ~14 | ~16 | +2 |

---

## ✅ Checklist de Validación

### Seguridad (RN-014)
- [x] Cifrado en tránsito especificado (TLS/HTTPS)
- [x] Cifrado en reposo especificado
- [x] Mecanismos de búsqueda segura documentados
- [x] Enmascaramiento en respuestas HTTP
- [x] Escenario Gherkin para validar enmascaramiento

### Performance (RN-015)
- [x] Requisito < 1s documentado
- [x] Estrategias de optimización sugeridas
- [x] Invalidación de caché especificada
- [x] Escenario Gherkin para validar performance

### Arquitectura Asíncrona
- [x] Evento TicketCreatedEvent documentado
- [x] Payload del evento especificado
- [x] Desacoplamiento de servicios explicado
- [x] Términos agregados al glosario

### Atomicidad (RN-001)
- [x] Requisitos de atomicidad documentados
- [x] Escenarios de concurrencia identificados
- [x] 4 mecanismos de garantía sugeridos
- [x] 2 escenarios Gherkin para validar

### Consistencia de Nombres
- [x] assignedAdvisor → assignedAdvisorId
- [x] Tipo UUID especificado
- [x] Respuestas HTTP actualizadas

---

## 🔄 Próximos Pasos

### Para RF-002 (Notificaciones Telegram)
- Documentar consumo del evento TicketCreatedEvent
- Especificar RN-014 para cifrado de telefono en mensajes
- Aplicar RN-007 y RN-008 (reintentos y backoff)

### Para RF-003 (Calcular Posición)
- Aplicar RN-015 (caché de alto rendimiento)
- Especificar endpoint GET /api/tickets/{numero}/position
- Validar performance < 1s en escenarios Gherkin

### Para RF-004 (Asignar Ticket)
- Documentar evento TicketAssignedEvent
- Aplicar RN-002 (prioridad de colas)
- Aplicar RN-004 (balanceo de carga)

---

**Documento generado:** Diciembre 2024  
**Última actualización:** Después de aplicar 5 mejoras técnicas  
**Estado:** Listo para revisión de arquitectura

