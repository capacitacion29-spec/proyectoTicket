# Requerimientos Funcionales - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2024  
**Analista:** Equipo de Producto e Innovación

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requerimientos funcionales del Sistema Ticketero Digital, diseñado para modernizar la experiencia de atención en sucursales mediante:
- Digitalización completa del proceso de tickets
- Notificaciones automáticas en tiempo real vía Telegram
- Movilidad del cliente durante la espera
- Asignación inteligente de clientes a ejecutivos
- Panel de monitoreo para supervisión operacional

### 1.2 Alcance

Este documento cubre:
- ✅ 8 Requerimientos Funcionales (RF-001 a RF-008)
- ✅ 13 Reglas de Negocio (RN-001 a RN-013)
- ✅ Criterios de aceptación en formato Gherkin
- ✅ Modelo de datos funcional
- ✅ Matriz de trazabilidad

Este documento NO cubre:
- ❌ Arquitectura técnica (ver documento ARQUITECTURA.md)
- ❌ Tecnologías de implementación
- ❌ Diseño de interfaces de usuario

### 1.3 Definiciones

| Término | Definición |
|---------|------------|
| Ticket | Turno digital asignado a un cliente para ser atendido |
| Cola | Fila virtual de tickets esperando atención |
| Asesor/Ejecutivo | Empleado bancario que atiende clientes |
| Módulo | Estación de trabajo de un asesor (numerados 1-5) |
| Chat ID | Identificador único de usuario en Telegram |
| UUID | Identificador único universal para tickets |
| RUT/ID | Identificación nacional del cliente (ej: 12345678-9) |
| FIFO | First In, First Out - Primero en entrar, primero en salir |
| Backoff Exponencial | Estrategia de reintentos con tiempos crecientes |
| Message Broker | Sistema de mensajería asíncrona para desacoplar servicios |
| Evento | Notificación asíncrona de un cambio de estado en el sistema |
| Cifrado en Tránsito | Protección de datos durante transmisión (TLS/HTTPS) |
| Cifrado en Reposo | Protección de datos almacenados en base de datos |
| Enmascaramiento | Ocultación parcial de datos sensibles (ej: ****5678-9) |

---

## 2. Reglas de Negocio

Las siguientes reglas de negocio aplican transversalmente a todos los requerimientos funcionales:

**RN-001: Unicidad de Ticket Activo**  
Un cliente solo puede tener 1 ticket activo a la vez. Los estados activos son: EN_ESPERA, PROXIMO, ATENDIENDO. Si un cliente intenta crear un nuevo ticket teniendo uno activo, el sistema debe rechazar la solicitud con error HTTP 409 Conflict.

**Requisitos de Atomicidad:**
- La verificación de existencia de ticket activo y la creación del nuevo ticket deben ser operaciones atómicas
- El sistema debe prevenir condiciones de carrera cuando múltiples solicitudes llegan simultáneamente para el mismo nationalId
- La validación debe ser la "source of truth" única (no confiar solo en validaciones de cliente)
- En caso de solicitudes concurrentes, solo una debe tener éxito; las demás deben recibir HTTP 409

**Escenarios de Concurrencia:**
- Cliente presiona botón "Crear Ticket" dos veces rápidamente
- Dos terminales diferentes intentan crear ticket para el mismo nationalId
- Solicitud duplicada por timeout de red (cliente reintenta)

**Mecanismo de Garantía:**
El sistema debe implementar uno de los siguientes mecanismos:
- Bloqueo pesimista a nivel de base de datos (ej: SELECT FOR UPDATE)
- Constraint de unicidad en base de datos (UNIQUE INDEX en nationalId + estados activos)
- Lock distribuido a nivel de aplicación (ej: Redis distributed lock)
- Idempotency key para detectar reintentos del mismo cliente

**RN-002: Prioridad de Colas**  
Las colas tienen prioridades numéricas para asignación automática:
- GERENCIA: prioridad 4 (máxima)
- EMPRESAS: prioridad 3
- PERSONAL_BANKER: prioridad 2
- CAJA: prioridad 1 (mínima)

Cuando un asesor se libera, el sistema asigna primero tickets de colas con mayor prioridad.

**RN-003: Orden FIFO Dentro de Cola**  
Dentro de una misma cola, los tickets se procesan en orden FIFO (First In, First Out). El ticket más antiguo (createdAt menor) se asigna primero.

**RN-004: Balanceo de Carga Entre Asesores**  
Al asignar un ticket, el sistema selecciona el asesor AVAILABLE con menor valor de assignedTicketsCount, distribuyendo equitativamente la carga de trabajo.

**RN-005: Formato de Número de Ticket**  
El número de ticket sigue el formato: [Prefijo][Número secuencial 01-99]
- Prefijo: 1 letra según el tipo de cola
- Número: 2 dígitos, del 01 al 99, reseteado diariamente

Ejemplos: C01, P15, E03, G02

**RN-006: Prefijos por Tipo de Cola**  
- CAJA → C
- PERSONAL_BANKER → P
- EMPRESAS → E
- GERENCIA → G

**RN-007: Reintentos Automáticos de Mensajes**  
Si el envío de un mensaje a Telegram falla, el sistema reintenta automáticamente hasta 3 veces antes de marcarlo como FALLIDO.

**RN-008: Backoff Exponencial en Reintentos**  
Los reintentos de mensajes usan backoff exponencial:
- Intento 1: inmediato
- Intento 2: después de 30 segundos
- Intento 3: después de 60 segundos
- Intento 4: después de 120 segundos

**RN-009: Estados de Ticket**  
Un ticket puede estar en uno de estos estados:
- EN_ESPERA: esperando asignación a asesor
- PROXIMO: próximo a ser atendido (posición ≤ 3)
- ATENDIENDO: siendo atendido por un asesor
- COMPLETADO: atención finalizada exitosamente
- CANCELADO: cancelado por cliente o sistema
- NO_ATENDIDO: cliente no se presentó cuando fue llamado

**RN-010: Cálculo de Tiempo Estimado**  
El tiempo estimado de espera se calcula como:

```
tiempoEstimado = posiciónEnCola × tiempoPromedioCola
```

Donde tiempoPromedioCola varía por tipo:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**RN-011: Auditoría Obligatoria**  
Todos los eventos críticos del sistema deben registrarse en auditoría con: timestamp, tipo de evento, actor involucrado, entityId afectado, y cambios de estado.

**RN-014: Protección de Datos Sensibles**  
Los campos nationalId y telefono contienen información personal sensible y deben protegerse según normativas de privacidad.

**Requisitos de Seguridad:**
- **En Tránsito:** Toda comunicación debe usar cifrado TLS/HTTPS (obligatorio)
- **En Reposo:** Los datos sensibles deben cifrarse en almacenamiento persistente
- **Para Búsquedas:** El sistema debe permitir búsqueda por nationalId sin comprometer seguridad

**Mecanismos Sugeridos:**
- Hashing unidireccional para índices de búsqueda (ej: SHA-256 con salt)
- Cifrado reversible para almacenamiento del valor original
- Tokenización para referencias externas
- Enmascaramiento en logs y auditoría (mostrar solo últimos 4 dígitos)

**Ejemplo de Almacenamiento:**
```
nationalId (original): "12345678-9"
nationalId_hash (búsqueda): "a3f5b2c1..." (SHA-256)
nationalId_encrypted (almacenamiento): "enc_xyz123..." (AES-256)
nationalId_masked (logs): "****5678-9"
```

**RN-015: Cálculo de Posición de Alto Rendimiento**  
El cálculo de positionInQueue y estimatedWaitMinutes debe cumplir con requisito de performance < 1 segundo, incluso en escenarios de alta carga (25,000+ tickets/día en Fase Nacional).

**Requisitos de Performance:**
- Tiempo de respuesta: < 1 segundo para cálculo de posición
- Escalabilidad: Soportar 25,000+ tickets/día sin degradación
- Consistencia: Posición debe ser precisa en tiempo real

**Estrategia de Optimización:**
El sistema debe usar una de las siguientes estrategias:
- Caché de estado de colas en memoria de alta velocidad
- Contadores incrementales por cola (evitar COUNT(*) en BD transaccional)
- Snapshot periódico del estado de colas
- Índices optimizados para consultas de posición

**Invalidación de Caché:**
- Al crear nuevo ticket
- Al asignar ticket a asesor
- Al cambiar estado de ticket (COMPLETADO, CANCELADO)
- Máximo 5 segundos de desfase aceptable

**RN-012: Umbral de Pre-aviso**  
El sistema envía el Mensaje 2 (pre-aviso) cuando la posición del ticket es ≤ 3, indicando que el cliente debe acercarse a la sucursal.

**RN-013: Estados de Asesor**  
Un asesor puede estar en uno de estos estados:
- AVAILABLE: disponible para recibir asignaciones
- BUSY: atendiendo un cliente (no recibe nuevas asignaciones)
- OFFLINE: no disponible (almuerzo, capacitación, etc.)

**RN-016: Transactional Outbox Pattern**  
La creación de entidades y la publicación de eventos deben ser atómicas para prevenir inconsistencias. El sistema debe usar el patrón Transactional Outbox para garantizar que si una entidad se persiste en base de datos, su evento correspondiente se publicará eventualmente.

**Requisitos de Atomicidad:**
- La inserción del ticket y la inserción del evento en tabla outbox deben ocurrir en la misma transacción de base de datos
- Si la transacción falla, ni el ticket ni el evento se persisten
- Si la transacción tiene éxito, el evento se publicará eventualmente (garantía at-least-once)
- Un proceso separado (outbox publisher) lee eventos de la tabla outbox y los publica al message broker

**Escenarios Críticos:**
- Creación de ticket → TicketCreatedEvent
- Asignación de ticket → TicketAssignedEvent
- Cambio de posición → TicketPositionChangedEvent
- Cambio de estado de asesor → AdvisorAvailableEvent

**Tabla Outbox (Modelo de Datos):**
```
outbox_events
- id: BIGINT (PK, auto-increment)
- aggregate_type: VARCHAR (ej: "Ticket", "Advisor")
- aggregate_id: UUID (ej: ticket.codigoReferencia)
- event_type: VARCHAR (ej: "TicketCreated")
- payload: JSON (datos del evento)
- created_at: TIMESTAMP
- published_at: TIMESTAMP (nullable)
- published: BOOLEAN (default: false)
```

**Flujo de Publicación:**
1. Transacción DB: INSERT ticket + INSERT outbox_event (atómico)
2. Commit de transacción
3. Outbox Publisher (proceso separado) lee eventos con published = false
4. Publica evento al message broker
5. Marca published = true, published_at = NOW()

**Garantías:**
- **Atomicidad:** Ticket y evento se crean juntos o ninguno se crea
- **Durabilidad:** Evento persiste en BD antes de publicarse
- **At-least-once delivery:** Si falla publicación, se reintenta
- **Idempotencia:** Consumidores deben manejar eventos duplicados

---

## 3. Enumeraciones

### 3.1 QueueType

Tipos de cola disponibles en el sistema:

| Valor | Display Name | Tiempo Promedio | Prioridad | Prefijo |
|-------|--------------|-----------------|-----------|---------|
| CAJA | Caja | 5 min | 1 | C |
| PERSONAL_BANKER | Personal Banker | 15 min | 2 | P |
| EMPRESAS | Empresas | 20 min | 3 | E |
| GERENCIA | Gerencia | 30 min | 4 | G |

### 3.2 TicketStatus

Estados posibles de un ticket:

| Valor | Descripción | Es Activo? |
|-------|-------------|------------|
| EN_ESPERA | Esperando asignación | Sí |
| PROXIMO | Próximo a ser atendido | Sí |
| ATENDIENDO | Siendo atendido | Sí |
| COMPLETADO | Atención finalizada | No |
| CANCELADO | Cancelado | No |
| NO_ATENDIDO | Cliente no se presentó | No |

### 3.3 AdvisorStatus

Estados posibles de un asesor:

| Valor | Descripción | Recibe Asignaciones? |
|-------|-------------|----------------------|
| AVAILABLE | Disponible | Sí |
| BUSY | Atendiendo cliente | No |
| OFFLINE | No disponible | No |

### 3.4 MessageTemplate

Plantillas de mensajes para Telegram:

| Valor | Descripción | Momento de Envío |
|-------|-------------|------------------|
| totem_ticket_creado | Confirmación de creación | Inmediato al crear ticket |
| totem_proximo_turno | Pre-aviso | Cuando posición ≤ 3 |
| totem_es_tu_turno | Turno activo | Al asignar a asesor |

---

## 4. Requerimientos Funcionales

### RF-001: Crear Ticket Digital

**Descripción:**  
El sistema debe permitir al cliente crear un ticket digital para ser atendido en sucursal, ingresando su identificación nacional (RUT/ID), número de teléfono y seleccionando el tipo de atención requerida. El sistema generará un número único de ticket, calculará la posición actual en cola y el tiempo estimado de espera basado en datos reales de la operación.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Terminal de autoservicio disponible y funcional
- Sistema de gestión de colas operativo
- Conexión a base de datos activa

**Modelo de Datos (Campos del Ticket):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| codigoReferencia | UUID | Identificador único universal | "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6" |
| numero | String | Formato [Prefijo][01-99] | "C01", "P15", "E03", "G02" |
| nationalId | String | Identificación nacional del cliente | "12345678-9" |
| telefono | String | Número de teléfono para Telegram | "+56912345678" |
| branchOffice | String | Nombre de la sucursal | "Sucursal Centro" |
| queueType | Enum | Tipo de cola (ver 3.1) | CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA |
| status | Enum | Estado del ticket (ver 3.2) | EN_ESPERA, PROXIMO, ATENDIENDO, etc. |
| positionInQueue | Integer | Posición actual en cola (calculada) | 5 |
| estimatedWaitMinutes | Integer | Minutos estimados de espera | 25 |
| createdAt | Timestamp | Fecha/hora de creación | "2024-12-15T10:30:00Z" |
| assignedAdvisorId | UUID | ID del asesor asignado | null (inicialmente) |
| assignedModuleNumber | Integer | Número de módulo (1-5) | null (inicialmente) |

**Reglas de Negocio Aplicables:**
- **RN-001:** Un cliente solo puede tener 1 ticket activo a la vez (con atomicidad)
- **RN-005:** Número de ticket formato: [Prefijo][Número secuencial 01-99]
- **RN-006:** Prefijos por cola: C=Caja, P=Personal Banker, E=Empresas, G=Gerencia
- **RN-010:** Cálculo de tiempo estimado: posiciónEnCola × tiempoPromedioCola
- **RN-014:** Protección de datos sensibles (nationalId, telefono)
- **RN-015:** Cálculo de posición de alto rendimiento (< 1s)
- **RN-016:** Transactional Outbox Pattern (atomicidad ticket + evento)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Creación exitosa de ticket para cola de Caja**
```gherkin
Given el cliente con nationalId "12345678-9" no tiene tickets activos
And el terminal está en pantalla de selección de servicio
When el cliente ingresa:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | CAJA            |
Then el sistema genera un ticket con:
  | Campo                 | Valor Esperado                    |
  | codigoReferencia      | UUID válido                       |
  | numero                | "C[01-99]"                        |
  | status                | EN_ESPERA                         |
  | positionInQueue       | Número > 0                        |
  | estimatedWaitMinutes  | positionInQueue × 5               |
  | assignedAdvisor       | null                              |
  | assignedModuleNumber  | null                              |
And el sistema almacena el ticket en base de datos
And el sistema programa 3 mensajes de Telegram
And el sistema retorna HTTP 201 con JSON:
  {
    "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C01",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "queueType": "CAJA"
  }
```

**Escenario 2: Error - Cliente ya tiene ticket activo**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo:
  | numero | status     | queueType      |
  | P05    | EN_ESPERA  | PERSONAL_BANKER|
When el cliente intenta crear un nuevo ticket con queueType CAJA
Then el sistema rechaza la creación
And el sistema retorna HTTP 409 Conflict con JSON:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: P05",
    "ticketActivo": {
      "numero": "P05",
      "positionInQueue": 3,
      "estimatedWaitMinutes": 45
    }
  }
And el sistema NO crea un nuevo ticket
```

**Escenario 3: Validación - RUT/ID inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa nationalId vacío
Then el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "VALIDACION_FALLIDA",
    "campos": {
      "nationalId": "El RUT/ID es obligatorio"
    }
  }
And el sistema NO crea el ticket
```

**Escenario 4: Validación - Teléfono en formato inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa telefono "123"
Then el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "VALIDACION_FALLIDA",
    "campos": {
      "telefono": "Formato requerido: +56XXXXXXXXX"
    }
  }
And el sistema NO crea el ticket
```

**Escenario 5: Cálculo de posición - Primera persona en cola**
```gherkin
Given la cola de tipo PERSONAL_BANKER está vacía
When el cliente crea un ticket para PERSONAL_BANKER
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 15
And el número de ticket es "P01"
```

**Escenario 6: Cálculo de posición - Cola con tickets existentes**
```gherkin
Given la cola de tipo EMPRESAS tiene 4 tickets EN_ESPERA
When el cliente crea un nuevo ticket para EMPRESAS
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 100
And el cálculo es: 5 × 20min = 100min
And el número de ticket es "E05"
```

**Escenario 7: Creación sin teléfono (cliente no quiere notificaciones)**
```gherkin
Given el cliente no proporciona número de teléfono
When el cliente crea un ticket con:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | null            |
  | branchOffice | Sucursal Centro |
  | queueType    | GERENCIA        |
Then el sistema crea el ticket exitosamente
And el sistema NO programa mensajes de Telegram
And el sistema retorna HTTP 201 con JSON:
  {
    "identificador": "uuid-generado",
    "numero": "G01",
    "positionInQueue": 1,
    "estimatedWaitMinutes": 30,
    "queueType": "GERENCIA"
  }
```

**Escenario 8: Prevención de condición de carrera - Solicitudes concurrentes**
```gherkin
Given el cliente con nationalId "12345678-9" no tiene tickets activos
And el sistema recibe 2 solicitudes simultáneas para crear ticket:
  | Solicitud | nationalId  | queueType | Timestamp        |
  | Request-1 | 12345678-9  | CAJA      | 10:30:00.100     |
  | Request-2 | 12345678-9  | CAJA      | 10:30:00.105     |
When ambas solicitudes intentan verificar existencia de ticket activo
Then el sistema procesa las solicitudes de forma atómica
And solo UNA solicitud tiene éxito (Request-1 o Request-2)
And la solicitud exitosa retorna HTTP 201 con ticket creado
And la solicitud fallida retorna HTTP 409 Conflict con JSON:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: C01",
    "ticketActivo": {
      "numero": "C01",
      "positionInQueue": 1,
      "estimatedWaitMinutes": 5
    }
  }
And el sistema garantiza que solo existe 1 ticket en base de datos
```

**Escenario 9: Idempotencia - Cliente presiona botón dos veces rápidamente**
```gherkin
Given el cliente con nationalId "12345678-9" está en el terminal
When el cliente presiona "Crear Ticket" en t=0ms
And el cliente presiona "Crear Ticket" nuevamente en t=50ms (doble clic accidental)
Then la primera solicitud crea el ticket exitosamente (HTTP 201)
And la segunda solicitud es rechazada (HTTP 409)
And el sistema retorna el ticket ya creado en la respuesta 409:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: C01",
    "ticketActivo": {
      "numero": "C01",
      "positionInQueue": 1,
      "estimatedWaitMinutes": 5,
      "createdAt": "2024-12-15T10:30:00.100Z"
    }
  }
And el cliente ve el ticket creado en pantalla sin duplicados
```

**Escenario 10: Seguridad - Enmascaramiento de datos sensibles en respuesta**
```gherkin
Given el cliente con nationalId "12345678-9" crea un ticket exitosamente
When el sistema retorna la respuesta HTTP 201
Then el campo nationalIdMasked debe mostrar "****5678-9"
And el nationalId completo NO debe aparecer en la respuesta JSON
And el nationalId completo debe almacenarse cifrado en base de datos
And los logs del sistema deben mostrar nationalId enmascarado
And el evento de auditoría debe registrar nationalId enmascarado
```

**Escenario 11: Performance - Cálculo de posición en menos de 1 segundo**
```gherkin
Given la cola de tipo CAJA tiene 100 tickets EN_ESPERA
When el cliente crea un nuevo ticket para CAJA
Then el sistema calcula positionInQueue en menos de 1 segundo
And el cálculo usa caché de estado de colas (no COUNT(*) directo)
And el sistema retorna HTTP 201 con positionInQueue = 101
And estimatedWaitMinutes = 505 (101 × 5min)
```

**Escenario 12: Transactional Outbox - Atomicidad entre ticket y evento (RN-016)**
```gherkin
Given el cliente con nationalId "12345678-9" crea un ticket
When el sistema ejecuta la transacción de creación
Then el sistema inserta el ticket en tabla "tickets"
And el sistema inserta el evento en tabla "outbox_events" en la MISMA transacción:
  | aggregate_type | aggregate_id | event_type     | published |
  | Ticket         | uuid-123     | TicketCreated  | false     |
And ambas inserciones son atómicas (commit o rollback juntas)
And el sistema retorna HTTP 201 al cliente
And un proceso separado (Outbox Publisher) lee el evento de outbox_events
And el Outbox Publisher publica el evento al message broker
And el Outbox Publisher marca published = true
And el Servicio de Notificaciones recibe el evento y envía Mensaje 1
```

**Escenario 13: Transactional Outbox - Fallo de transacción previene evento huérfano**
```gherkin
Given el cliente intenta crear un ticket
When ocurre un error durante la inserción del ticket (ej: constraint violation)
Then la transacción hace ROLLBACK
And el ticket NO se persiste en base de datos
And el evento NO se persiste en tabla outbox_events
And el sistema retorna HTTP 500 Internal Server Error
And NO se publica ningún evento al message broker
And el cliente NO recibe Mensaje 1 (porque el ticket no existe)
```

**Escenario 14: Transactional Outbox - Reintento de publicación si falla message broker**
```gherkin
Given un ticket "C01" fue creado exitosamente
And el evento está en outbox_events con published = false
When el Outbox Publisher intenta publicar el evento
And el message broker está caído (error de conexión)
Then el evento permanece con published = false
And el Outbox Publisher reintenta en el siguiente ciclo (ej: 30s después)
When el message broker se recupera
And el Outbox Publisher reintenta la publicación
Then el evento se publica exitosamente
And el sistema marca published = true, published_at = NOW()
And el Servicio de Notificaciones recibe el evento (con retraso)
And el cliente recibe Mensaje 1 (eventualmente)
```

**Postcondiciones:**
- Ticket almacenado en base de datos con estado EN_ESPERA
- Datos sensibles (nationalId, telefono) cifrados según RN-014
- Evento "TicketCreatedEvent" insertado en tabla outbox_events (atómico con ticket según RN-016)
- Outbox Publisher publica evento al message broker (asíncrono)
- Evento de auditoría registrado: "TICKET_CREADO"
- Contador de secuencia incrementado para el tipo de cola
- Caché de estado de colas actualizado
- Garantía: Si ticket existe en BD, evento se publicará eventualmente (at-least-once)

**Evento Publicado (TicketCreatedEvent):**
```json
{
  "eventType": "TicketCreated",
  "timestamp": "2024-12-15T10:30:00Z",
  "payload": {
    "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C01",
    "telefono": "+56912345678",
    "queueType": "CAJA",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "branchOffice": "Sucursal Centro"
  }
}
```

**Nota Crítica de Desacoplamiento:** El campo `telefono` se incluye en el evento para que el servicio de notificaciones opere de forma autónoma sin consultar la BD de tickets. Esto evita reintroducir acoplamiento y mejora la resiliencia del sistema. El servicio de notificaciones solo necesita este evento para enviar el Mensaje 1 (confirmación).

**Endpoints HTTP:**
- `POST /api/tickets` - Crear nuevo ticket

**Request Body:**
```json
{
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA"
}
```

**Response 201 Created:**
```json
{
  "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "C01",
  "positionInQueue": 5,
  "estimatedWaitMinutes": 25,
  "queueType": "CAJA",
  "status": "EN_ESPERA",
  "createdAt": "2024-12-15T10:30:00Z",
  "nationalIdMasked": "****5678-9",
  "assignedAdvisorId": null,
  "assignedModuleNumber": null
}
```

**Nota de Seguridad:** El nationalId se retorna enmascarado en la respuesta HTTP para proteger datos sensibles (RN-014). El valor completo solo se almacena cifrado en base de datos. El campo nationalIdMasked muestra solo los últimos 4 dígitos para verificación del cliente.

**Response 409 Conflict:**
```json
{
  "error": "TICKET_ACTIVO_EXISTENTE",
  "mensaje": "Ya tienes un ticket activo: P05",
  "ticketActivo": {
    "numero": "P05",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 45
  }
}
```

**Response 400 Bad Request:**
```json
{
  "error": "VALIDACION_FALLIDA",
  "campos": {
    "nationalId": "El RUT/ID es obligatorio",
    "telefono": "Formato requerido: +56XXXXXXXXX"
  }
}
```

---

### RF-002: Enviar Notificaciones Automáticas vía Telegram

**Descripción:**  
El sistema debe enviar automáticamente tres mensajes vía Telegram al cliente durante el ciclo de vida de su ticket, manteniéndolo informado sobre el progreso de su turno sin necesidad de permanecer físicamente en la sucursal. Los mensajes se envían de forma asíncrona mediante un servicio de notificaciones que escucha eventos del sistema.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Actores Secundarios:** Servicio de Notificaciones, Telegram Bot API

**Precondiciones:**
- Ticket creado con número de teléfono válido
- Telegram Bot configurado y activo
- Cliente tiene cuenta de Telegram asociada al teléfono
- Message broker operativo para recibir eventos

**Modelo de Datos (Entidad Mensaje):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| id | BIGINT | Identificador único del mensaje | 12345 |
| ticketId | UUID | Referencia al ticket (FK) | "a1b2c3d4-e5f6..." |
| telefono | String | Número de teléfono destino | "+56912345678" |
| plantilla | Enum | Tipo de mensaje (ver 3.4) | totem_ticket_creado |
| estadoEnvio | Enum | Estado actual del envío | PENDIENTE, ENVIADO, FALLIDO |
| fechaProgramada | Timestamp | Cuándo debe enviarse | "2024-12-15T10:30:00Z" |
| fechaEnvio | Timestamp | Cuándo se envió realmente | "2024-12-15T10:30:05Z" (nullable) |
| telegramMessageId | String | ID retornado por Telegram API | "msg_xyz123" (nullable) |
| intentos | Integer | Contador de reintentos | 0, 1, 2, 3 (default: 0) |

**Nota de Desacoplamiento:** El campo `telefono` se incluye en la entidad Mensaje para evitar JOINs con la tabla de tickets durante reintentos. Esto mejora la performance y el desacoplamiento del servicio de notificaciones, permitiendo que opere de forma autónoma sin depender del servicio de tickets.

**Plantillas de Mensajes:**

**1. totem_ticket_creado (Mensaje 1 - Confirmación):**
```
✅ <b>Ticket Creado</b>

Tu número de turno: <b>{numero}</b>
Posición en cola: <b>#{posicion}</b>
Tiempo estimado: <b>{tiempo} minutos</b>

Te notificaremos cuando estés próximo.
```

**Variables:**
- `{numero}`: Número de ticket (ej: "C01")
- `{posicion}`: Posición en cola (ej: 5)
- `{tiempo}`: Minutos estimados (ej: 25)

**2. totem_proximo_turno (Mensaje 2 - Pre-aviso):**
```
⏰ <b>¡Pronto será tu turno!</b>

Turno: <b>{numero}</b>
Faltan aproximadamente 3 turnos.

Por favor, acércate a la sucursal.
```

**Variables:**
- `{numero}`: Número de ticket (ej: "P05")

**3. totem_es_tu_turno (Mensaje 3 - Turno Activo):**
```
🔔 <b>¡ES TU TURNO {numero}!</b>

Dirígete al módulo: <b>{modulo}</b>
Asesor: <b>{nombreAsesor}</b>
```

**Variables:**
- `{numero}`: Número de ticket (ej: "E03")
- `{modulo}`: Número de módulo (ej: 3)
- `{nombreAsesor}`: Nombre del ejecutivo (ej: "María González")

**Reglas de Negocio Aplicables:**
- **RN-007:** 3 reintentos automáticos para mensajes fallidos
- **RN-008:** Backoff exponencial (30s, 60s, 120s)
- **RN-011:** Auditoría de envíos
- **RN-012:** Mensaje 2 cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Envío exitoso del Mensaje 1 (Confirmación)**
```gherkin
Given el sistema ha publicado un evento "TicketCreatedEvent" con:
  | codigoReferencia | numero | telefono     | positionInQueue | estimatedWaitMinutes |
  | uuid-123         | C01    | +56912345678 | 5               | 25                   |
And el evento incluye el campo telefono para evitar consultas a BD
And el servicio de notificaciones escucha el evento
When el servicio procesa el evento
Then el sistema crea un registro de mensaje con:
  | plantilla            | telefono     | estadoEnvio | intentos |
  | totem_ticket_creado  | +56912345678 | PENDIENTE   | 0        |
And el sistema envía el mensaje a Telegram API
And Telegram API retorna éxito con messageId "msg_xyz123"
Then el sistema actualiza el mensaje:
  | estadoEnvio | telegramMessageId | fechaEnvio          | intentos |
  | ENVIADO     | msg_xyz123        | 2024-12-15T10:30:05 | 1        |
And el sistema registra evento de auditoría: "MENSAJE_ENVIADO"
```

**Escenario 2: Envío exitoso del Mensaje 2 (Pre-aviso)**
```gherkin
Given un ticket con numero "P05" tiene positionInQueue = 4
When el sistema asigna otro ticket y la posición de "P05" cambia a 3
Then el sistema publica evento "TicketPositionChangedEvent"
And el servicio de notificaciones detecta posición ≤ 3
And el sistema crea mensaje con plantilla "totem_proximo_turno"
And el sistema envía el mensaje a Telegram
And el mensaje contiene: "⏰ ¡Pronto será tu turno! Turno: P05"
And el estadoEnvio se actualiza a ENVIADO
```

**Escenario 3: Envío exitoso del Mensaje 3 (Turno Activo)**
```gherkin
Given un ticket con numero "E03" es asignado al asesor "María González" en módulo 3
When el sistema publica evento "TicketAssignedEvent" con:
  | numero | moduleNumber | advisorName      |
  | E03    | 3            | María González |
Then el servicio de notificaciones crea mensaje con plantilla "totem_es_tu_turno"
And el sistema envía el mensaje a Telegram
And el mensaje contiene: "🔔 ¡ES TU TURNO E03! Dirígete al módulo: 3"
And el estadoEnvio se actualiza a ENVIADO
```

**Escenario 4: Descarte de notificación obsoleta - Ticket completado antes de envío (CASO CRÍTICO)**
```gherkin
Given el sistema publica "TicketAssignedEvent" para ticket "E03" en t=0s
And el evento entra en cola del message broker
And debido a alta carga, el evento se procesa con retraso de 30s
When en t=15s el ticket "E03" cambia a status = COMPLETADO (atención rápida)
And en t=30s el Servicio de Notificaciones intenta enviar Mensaje 3
Then el servicio verifica el estado actual del ticket en BD
And detecta status = COMPLETADO (ticket ya finalizado)
And el sistema NO envía el Mensaje 3 a Telegram
And el sistema actualiza el mensaje:
  | estadoEnvio | intentos | ultimoError                    |
  | FALLIDO     | 1        | "MENSAJE_DESCARTADO_OBSOLETO" |
And el sistema registra auditoría: "MENSAJE_DESCARTADO_OBSOLETO"
And el cliente NO recibe notificación de turno activo (evita confusión)
```

**Escenario 5: Descarte de notificación obsoleta - Ticket cancelado**
```gherkin
Given el sistema publica "TicketPositionChangedEvent" para ticket "P05" (posición = 3)
And el evento se procesa con retraso de 20s
When en t=10s el cliente cancela el ticket "P05" (status = CANCELADO)
And en t=20s el Servicio de Notificaciones intenta enviar Mensaje 2 (pre-aviso)
Then el servicio verifica el estado actual del ticket
And detecta status = CANCELADO
And el sistema NO envía el Mensaje 2
And el sistema marca el mensaje como FALLIDO con error "MENSAJE_DESCARTADO_OBSOLETO"
And el sistema registra auditoría con detalles del descarte
```

**Escenario 6: Validación de estado antes de envío - Estados válidos**
```gherkin
Given el Servicio de Notificaciones recibe evento para enviar mensaje
When el servicio verifica el estado del ticket antes de enviar
Then el sistema valida que el ticket esté en estado compatible:
  | Mensaje                 | Estados Válidos para Envío           |
  | totem_ticket_creado     | EN_ESPERA, PROXIMO                   |
  | totem_proximo_turno     | PROXIMO, EN_ESPERA                   |
  | totem_es_tu_turno       | ATENDIENDO                           |
And si el estado NO es válido, descarta el mensaje
And si el estado ES válido, procede con el envío
```

**Escenario 7: Fallo de red en primer intento, éxito en segundo (RN-008)**
```gherkin
Given el sistema intenta enviar Mensaje 1 para ticket "C01"
When Telegram API retorna error de red (timeout)
Then el sistema marca estadoEnvio = PENDIENTE
And incrementa intentos = 1
And el sistema espera 30 segundos (backoff exponencial)
When el sistema reintenta el envío (intento 2)
And Telegram API retorna éxito
Then el sistema actualiza estadoEnvio = ENVIADO
And intentos = 2
And el sistema registra auditoría: "MENSAJE_ENVIADO_TRAS_REINTENTO"
```

**Escenario 8: 3 reintentos fallidos → estado FALLIDO (RN-007)**
```gherkin
Given el sistema intenta enviar Mensaje 1 para ticket "G02"
When el intento 1 falla (error de red)
And el sistema espera 30s y reintenta (intento 2) → falla
And el sistema espera 60s y reintenta (intento 3) → falla
And el sistema espera 120s y reintenta (intento 4) → falla
Then el sistema marca estadoEnvio = FALLIDO
And intentos = 4
And el sistema NO reintenta más
And el sistema registra auditoría: "MENSAJE_FALLIDO_TRAS_3_REINTENTOS"
And el sistema envía alerta al equipo de soporte
```

**Escenario 9: Backoff exponencial entre reintentos (RN-008)**
```gherkin
Given el sistema tiene un mensaje PENDIENTE con intentos = 0
When el intento 1 falla en t=0s
Then el sistema programa reintento 2 para t=30s
When el intento 2 falla en t=30s
Then el sistema programa reintento 3 para t=90s (30s + 60s)
When el intento 3 falla en t=90s
Then el sistema programa reintento 4 para t=210s (90s + 120s)
And los tiempos de espera son: 30s, 60s, 120s (exponencial)
```

**Escenario 10: Cliente sin teléfono, no se programan mensajes**
```gherkin
Given un ticket se crea sin campo telefono (null)
When el sistema publica evento "TicketCreatedEvent" con telefono = null
Then el servicio de notificaciones ignora el evento
And NO se crean registros de mensaje
And NO se intenta envío a Telegram
And el ticket continúa su flujo normal sin notificaciones
```

**Escenario 11: Telegram API retorna error de número inválido**
```gherkin
Given el sistema intenta enviar Mensaje 1 a telefono "+56900000000"
When Telegram API retorna error "PHONE_NUMBER_INVALID"
Then el sistema marca estadoEnvio = FALLIDO
And intentos = 1
And el sistema NO reintenta (error no recuperable)
And el sistema registra auditoría: "MENSAJE_FALLIDO_TELEFONO_INVALIDO"
```

**Postcondiciones:**
- Mensaje insertado en BD con estado según resultado
- telegramMessageId almacenado si envío exitoso
- Contador de intentos incrementado en cada reintento
- Evento de auditoría registrado (MENSAJE_ENVIADO, MENSAJE_FALLIDO, o MENSAJE_DESCARTADO_OBSOLETO)
- Alerta generada si mensaje falla tras 3 reintentos
- Validación de estado del ticket antes de cada envío (previene notificaciones obsoletas)
- Mensajes descartados si ticket está en estado final (COMPLETADO, CANCELADO, NO_ATENDIDO)

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado por event listener)

**Eventos Consumidos:**

**TicketCreatedEvent:**
```json
{
  "eventType": "TicketCreated",
  "timestamp": "2024-12-15T10:30:00Z",
  "payload": {
    "codigoReferencia": "uuid-123",
    "numero": "C01",
    "telefono": "+56912345678",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25
  }
}
```

**TicketPositionChangedEvent:**
```json
{
  "eventType": "TicketPositionChanged",
  "timestamp": "2024-12-15T10:35:00Z",
  "payload": {
    "codigoReferencia": "uuid-456",
    "numero": "P05",
    "telefono": "+56912345679",
    "oldPosition": 4,
    "newPosition": 3
  }
}
```

**TicketAssignedEvent:**
```json
{
  "eventType": "TicketAssigned",
  "timestamp": "2024-12-15T10:40:00Z",
  "payload": {
    "codigoReferencia": "uuid-789",
    "numero": "E03",
    "telefono": "+56912345680",
    "moduleNumber": 3,
    "advisorName": "María González"
  }
}
```

**Ejemplo de Mensaje en Telegram:**

**Mensaje 1 (Confirmación):**
```
✅ Ticket Creado

Tu número de turno: C01
Posición en cola: #5
Tiempo estimado: 25 minutos

Te notificaremos cuando estés próximo.
```

**Mensaje 2 (Pre-aviso):**
```
⏰ ¡Pronto será tu turno!

Turno: P05
Faltan aproximadamente 3 turnos.

Por favor, acércate a la sucursal.
```

**Mensaje 3 (Turno Activo):**
```
🔔 ¡ES TU TURNO E03!

Dirígete al módulo: 3
Asesor: María González
```

**Notas Técnicas:**
- Los mensajes usan formato HTML de Telegram (`<b>` para negrita)
- El servicio de notificaciones es asíncrono y desacoplado del servicio de tickets
- Los reintentos se gestionan mediante un scheduler (ej: cron job cada 30s)
- Los errores no recuperables (PHONE_NUMBER_INVALID) no se reintentan
- Los errores recuperables (timeout, rate limit) se reintentan según RN-008
- **CRÍTICO:** El servicio SIEMPRE verifica el estado actual del ticket en BD antes de enviar, evitando notificaciones obsoletas en sistemas asíncronos con retrasos
- Estados finales (COMPLETADO, CANCELADO, NO_ATENDIDO) invalidan mensajes pendientes
- La validación de estado previene confusión del cliente (ej: recibir "es tu turno" después de ser atendido)

---


### RF-003: Calcular Posición y Tiempo Estimado en Tiempo Real

**Descripción:**  
El sistema debe calcular y actualizar en tiempo real la posición exacta del cliente en cola y el tiempo estimado de espera, permitiendo consultas bajo demanda. El cálculo debe considerar la cantidad de tickets activos adelante en la misma cola, el tiempo promedio de atención por tipo de servicio, y la cantidad de asesores disponibles. El sistema debe garantizar performance < 1 segundo incluso con alta carga.

**Prioridad:** Alta

**Actor Principal:** Cliente, Sistema (cálculo automático)

**Precondiciones:**
- Ticket existe en el sistema con estado activo
- Caché de estado de colas actualizado
- Sistema de colas operativo

**Algoritmo de Cálculo de Posición:**

```
positionInQueue = COUNT(tickets WHERE 
  queueType = ticket.queueType 
  AND status IN ('EN_ESPERA', 'PROXIMO')
  AND createdAt < ticket.createdAt
) + 1
```

**Algoritmo de Cálculo de Tiempo Estimado:**

```
estimatedWaitMinutes = positionInQueue × tiempoPromedioCola

Donde tiempoPromedioCola:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos
```

**Reglas de Negocio Aplicables:**
- **RN-003:** Orden FIFO dentro de cola (createdAt determina orden)
- **RN-010:** Fórmula de cálculo de tiempo estimado
- **RN-015:** Performance < 1 segundo usando caché

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consulta de posición para ticket en espera**
```gherkin
Given existe un ticket con numero "C05" en cola CAJA
And hay 4 tickets EN_ESPERA creados antes de "C05"
When el cliente consulta GET /api/tickets/C05/position
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 25 (5 × 5min)
And el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C05",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "queueType": "CAJA",
    "status": "EN_ESPERA",
    "ticketsAdelante": 4
  }
And el tiempo de respuesta es < 1 segundo
```

**Escenario 2: Actualización automática de posición al avanzar cola**
```gherkin
Given un ticket "P10" tiene positionInQueue = 8
When el sistema asigna el ticket "P02" (que estaba adelante)
And el ticket "P02" cambia a estado ATENDIENDO
Then el sistema recalcula posiciones de todos los tickets EN_ESPERA
And el ticket "P10" ahora tiene positionInQueue = 7
And estimatedWaitMinutes = 105 (7 × 15min)
And el sistema publica evento "TicketPositionChangedEvent"
```

**Escenario 3: Ticket próximo a ser atendido (posición ≤ 3)**
```gherkin
Given un ticket "E08" tiene positionInQueue = 4
When otro ticket es asignado y "E08" avanza a positionInQueue = 3
Then el sistema actualiza status = PROXIMO
And estimatedWaitMinutes = 60 (3 × 20min)
And el sistema publica evento "TicketPositionChangedEvent"
And el servicio de notificaciones envía Mensaje 2 (pre-aviso)
```

**Escenario 4: Consulta por UUID (codigoReferencia)**
```gherkin
Given existe un ticket con codigoReferencia "uuid-123" y numero "G02"
When el cliente consulta GET /api/tickets/uuid-123
Then el sistema retorna HTTP 200 con JSON:
  {
    "identificador": "uuid-123",
    "numero": "G02",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 60,
    "queueType": "GERENCIA",
    "status": "EN_ESPERA",
    "createdAt": "2024-12-15T10:00:00Z",
    "assignedAdvisorId": null
  }
```

**Escenario 5: Ticket ya asignado (posición = 0)**
```gherkin
Given un ticket "C03" tiene status = ATENDIENDO
And está asignado al asesor en módulo 2
When el cliente consulta GET /api/tickets/C03/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C03",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "queueType": "CAJA",
    "status": "ATENDIENDO",
    "assignedModuleNumber": 2,
    "assignedAdvisorId": "advisor-uuid-456"
  }
```

**Escenario 6: Performance con alta carga (100+ tickets en cola)**
```gherkin
Given la cola PERSONAL_BANKER tiene 150 tickets EN_ESPERA
When el cliente consulta posición del ticket "P151"
Then el sistema calcula positionInQueue = 151 usando caché
And el cálculo toma menos de 1 segundo
And estimatedWaitMinutes = 2265 (151 × 15min)
And el sistema NO ejecuta COUNT(*) en base de datos transaccional
```

**Escenario 7: Ticket no existe**
```gherkin
Given NO existe un ticket con numero "X99"
When el cliente consulta GET /api/tickets/X99/position
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "TICKET_NO_ENCONTRADO",
    "mensaje": "El ticket X99 no existe"
  }
```

**Escenario 8: Recálculo masivo al cambiar estado de múltiples tickets**
```gherkin
Given la cola EMPRESAS tiene 20 tickets EN_ESPERA
When el sistema completa 3 tickets simultáneamente (COMPLETADO)
Then el sistema recalcula posiciones de los 17 tickets restantes
And cada ticket reduce su positionInQueue en 3
And el sistema actualiza caché de estado de colas
And el recálculo completo toma menos de 2 segundos
```

**Postcondiciones:**
- Posición calculada y almacenada en caché
- Tiempo estimado actualizado
- Evento "TicketPositionChangedEvent" publicado si posición cambió
- Caché de colas sincronizado con base de datos
- Auditoría registrada si posición ≤ 3 (dispara Mensaje 2)

**Endpoints HTTP:**
- `GET /api/tickets/{codigoReferencia}` - Consultar ticket por UUID
- `GET /api/tickets/{numero}/position` - Consultar posición por número

**Response 200 OK (GET /api/tickets/{numero}/position):**
```json
{
  "numero": "C05",
  "positionInQueue": 5,
  "estimatedWaitMinutes": 25,
  "queueType": "CAJA",
  "status": "EN_ESPERA",
  "ticketsAdelante": 4,
  "createdAt": "2024-12-15T10:15:00Z",
  "nationalIdMasked": "****5678-9"
}
```

**Response 200 OK (GET /api/tickets/{uuid}):**
```json
{
  "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "P10",
  "positionInQueue": 7,
  "estimatedWaitMinutes": 105,
  "queueType": "PERSONAL_BANKER",
  "status": "EN_ESPERA",
  "createdAt": "2024-12-15T10:20:00Z",
  "nationalIdMasked": "****5678-9",
  "assignedAdvisorId": null,
  "assignedModuleNumber": null
}
```

**Nota de Seguridad:** Todas las respuestas HTTP incluyen nationalIdMasked en lugar del nationalId completo, cumpliendo con RN-014 (Protección de Datos Sensibles).

**Response 404 Not Found:**
```json
{
  "error": "TICKET_NO_ENCONTRADO",
  "mensaje": "El ticket X99 no existe"
}
```

**Evento Publicado (TicketPositionChangedEvent):**
```json
{
  "eventType": "TicketPositionChanged",
  "timestamp": "2024-12-15T10:35:00Z",
  "payload": {
    "codigoReferencia": "uuid-456",
    "numero": "P05",
    "telefono": "+56912345679",
    "oldPosition": 4,
    "newPosition": 3,
    "estimatedWaitMinutes": 45,
    "queueType": "PERSONAL_BANKER"
  }
}
```

**Nota:** Este evento dispara el Mensaje 2 (pre-aviso) cuando newPosition ≤ 3.

---

### RF-004: Asignar Ticket a Asesor

**Descripción:**  
El sistema debe asignar automáticamente tickets en espera a asesores disponibles, siguiendo reglas de prioridad de colas y balanceo de carga. La asignación se realiza cuando un asesor cambia su estado a AVAILABLE, seleccionando el ticket de mayor prioridad que lleva más tiempo esperando (FIFO dentro de cada cola).

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Actores Secundarios:** Asesor, Panel de Gestión

**Precondiciones:**
- Asesor con estado AVAILABLE
- Al menos un ticket con estado EN_ESPERA o PROXIMO
- Sistema de colas operativo

**Algoritmo de Asignación:**

```
1. Filtrar asesores con status = AVAILABLE
2. Seleccionar asesor con menor assignedTicketsCount (balanceo)
3. Buscar ticket con mayor prioridad de cola (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
4. Dentro de la cola de mayor prioridad, seleccionar ticket más antiguo (MIN(createdAt))
5. Asignar ticket al asesor:
   - ticket.status = ATENDIENDO
   - ticket.assignedAdvisorId = asesor.id
   - ticket.assignedModuleNumber = asesor.moduleNumber
   - asesor.status = BUSY
   - asesor.assignedTicketsCount += 1
6. Publicar evento TicketAssignedEvent
7. Recalcular posiciones de tickets restantes en cola
```

**Reglas de Negocio Aplicables:**
- **RN-002:** Prioridad de colas (GERENCIA=4, EMPRESAS=3, PERSONAL_BANKER=2, CAJA=1)
- **RN-003:** Orden FIFO dentro de cola
- **RN-004:** Balanceo de carga entre asesores
- **RN-013:** Estados de asesor (AVAILABLE, BUSY, OFFLINE)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asignación exitosa con un solo ticket en espera**
```gherkin
Given existe un asesor "María González" con:
  | id          | moduleNumber | status    | assignedTicketsCount |
  | advisor-001 | 3            | AVAILABLE | 0                    |
And existe un ticket "C01" con:
  | numero | queueType | status    | createdAt           |
  | C01    | CAJA      | EN_ESPERA | 2024-12-15T10:00:00 |
When el sistema ejecuta el proceso de asignación automática
Then el sistema asigna "C01" al asesor "María González"
And el ticket "C01" se actualiza:
  | status      | assignedAdvisorId | assignedModuleNumber |
  | ATENDIENDO  | advisor-001       | 3                    |
And el asesor "María González" se actualiza:
  | status | assignedTicketsCount |
  | BUSY   | 1                    |
And el sistema publica evento "TicketAssignedEvent"
And el servicio de notificaciones envía Mensaje 3 (turno activo)
```

**Escenario 2: Prioridad de colas - GERENCIA antes que CAJA**
```gherkin
Given existe un asesor AVAILABLE
And existen tickets en espera:
  | numero | queueType | createdAt           | prioridad |
  | C01    | CAJA      | 2024-12-15T10:00:00 | 1         |
  | G01    | GERENCIA  | 2024-12-15T10:05:00 | 4         |
When el sistema ejecuta asignación automática
Then el sistema asigna "G01" (mayor prioridad)
And "C01" permanece EN_ESPERA
And el ticket "G01" cambia a ATENDIENDO
```

**Escenario 3: FIFO dentro de misma cola**
```gherkin
Given existe un asesor AVAILABLE
And existen tickets en cola PERSONAL_BANKER:
  | numero | createdAt           |
  | P03    | 2024-12-15T10:00:00 |
  | P05    | 2024-12-15T10:05:00 |
  | P02    | 2024-12-15T09:55:00 |
When el sistema ejecuta asignación automática
Then el sistema asigna "P02" (createdAt más antiguo)
And "P03" y "P05" permanecen EN_ESPERA
```

**Escenario 4: Balanceo de carga entre múltiples asesores**
```gherkin
Given existen asesores AVAILABLE:
  | id          | moduleNumber | assignedTicketsCount |
  | advisor-001 | 1            | 3                    |
  | advisor-002 | 2            | 1                    |
  | advisor-003 | 3            | 5                    |
And existe un ticket "E05" EN_ESPERA
When el sistema ejecuta asignación automática
Then el sistema asigna "E05" a advisor-002 (menor carga: 1)
And advisor-002.assignedTicketsCount = 2
```

**Escenario 5: No hay asesores disponibles**
```gherkin
Given todos los asesores tienen status = BUSY o OFFLINE
And existen 10 tickets EN_ESPERA
When el sistema ejecuta asignación automática
Then el sistema NO asigna ningún ticket
And todos los tickets permanecen EN_ESPERA
And el sistema registra evento de auditoría: "ASIGNACION_PENDIENTE_SIN_ASESORES"
```

**Escenario 6: Asesor completa atención y se libera (Arquitectura Orientada a Eventos)**
```gherkin
Given un asesor "Juan Pérez" tiene:
  | status | assignedTicketsCount | currentTicket |
  | BUSY   | 1                    | C05           |
When el asesor marca el ticket "C05" como COMPLETADO
Then el sistema actualiza el asesor:
  | status    | assignedTicketsCount |
  | AVAILABLE | 0                    |
And el sistema publica evento "AdvisorAvailableEvent"
And el Servicio de Asignación escucha el evento
And el Servicio de Asignación decide si asignar nuevo ticket
And si hay tickets EN_ESPERA, asigna el siguiente según prioridad
```

**Nota de Arquitectura:** El servicio de gestión de asesores NO llama directamente al servicio de asignación. En su lugar, publica un evento AdvisorAvailableEvent que el servicio de asignación consume de forma asíncrona. Esto evita acoplamiento y mejora la escalabilidad del sistema.

**Escenario 7: Recálculo de posiciones tras asignación**
```gherkin
Given la cola CAJA tiene tickets:
  | numero | positionInQueue | status    |
  | C01    | 1               | EN_ESPERA |
  | C02    | 2               | EN_ESPERA |
  | C03    | 3               | EN_ESPERA |
When el sistema asigna "C01" a un asesor
Then el sistema recalcula posiciones:
  | numero | positionInQueue | status      |
  | C01    | 0               | ATENDIENDO  |
  | C02    | 1               | EN_ESPERA   |
  | C03    | 2               | EN_ESPERA   |
And el sistema publica eventos "TicketPositionChangedEvent" para C02 y C03
```

**Escenario 8: Asignación con múltiples colas activas**
```gherkin
Given existe un asesor AVAILABLE
And existen tickets en espera:
  | numero | queueType       | createdAt           | prioridad |
  | C10    | CAJA            | 2024-12-15T09:00:00 | 1         |
  | P08    | PERSONAL_BANKER | 2024-12-15T09:30:00 | 2         |
  | E05    | EMPRESAS        | 2024-12-15T10:00:00 | 3         |
When el sistema ejecuta asignación automática
Then el sistema asigna "E05" (prioridad 3, mayor)
And "P08" y "C10" permanecen EN_ESPERA
```

**Postcondiciones:**
- Ticket asignado con estado ATENDIENDO
- Asesor marcado como BUSY
- Contador assignedTicketsCount incrementado
- Evento "TicketAssignedEvent" publicado
- Evento "AdvisorAvailableEvent" publicado cuando asesor completa ticket
- Mensaje 3 (turno activo) enviado vía Telegram
- Posiciones de tickets restantes recalculadas
- Evento de auditoría registrado

**Endpoints HTTP:**
- `POST /api/advisors/{advisorId}/complete-ticket` - Asesor completa atención
- `PUT /api/advisors/{advisorId}/status` - Cambiar estado de asesor

**Request Body (Complete Ticket):**
```json
{
  "ticketNumero": "C05",
  "completionNotes": "Cliente atendido satisfactoriamente"
}
```

**Response 200 OK:**
```json
{
  "ticketNumero": "C05",
  "status": "COMPLETADO",
  "completedAt": "2024-12-15T10:45:00Z",
  "advisorStatus": "AVAILABLE",
  "nextTicketAssigned": "C06"
}
```

**Evento Publicado (TicketAssignedEvent):**
```json
{
  "eventType": "TicketAssigned",
  "timestamp": "2024-12-15T10:40:00Z",
  "payload": {
    "codigoReferencia": "uuid-789",
    "numero": "E03",
    "telefono": "+56912345680",
    "moduleNumber": 3,
    "advisorId": "advisor-001",
    "advisorName": "María González",
    "queueType": "EMPRESAS",
    "assignedAt": "2024-12-15T10:40:00Z"
  }
}
```

**Nota:** Este evento dispara el Mensaje 3 (turno activo) vía Telegram.

**Evento Publicado (AdvisorAvailableEvent):**
```json
{
  "eventType": "AdvisorAvailable",
  "timestamp": "2024-12-15T10:45:00Z",
  "payload": {
    "advisorId": "advisor-001",
    "advisorName": "Juan Pérez",
    "moduleNumber": 2,
    "assignedTicketsCount": 0,
    "previousTicket": "C05",
    "availableSince": "2024-12-15T10:45:00Z"
  }
}
```

**Nota de Desacoplamiento:** El Servicio de Asignación escucha este evento y ejecuta el algoritmo de asignación de forma asíncrona. Esto evita que el servicio de gestión de asesores tenga una dependencia directa con el servicio de asignación, mejorando la escalabilidad y resiliencia del sistema.

---

### RF-006: Gestión de Mensajes de Telegram

**Descripción:**  
El sistema debe gestionar el ciclo de vida completo de los mensajes de Telegram, incluyendo programación, envío, reintentos automáticos, y seguimiento de estado. El servicio de notificaciones opera de forma asíncrona y desacoplada, consumiendo eventos del sistema y ejecutando la lógica de envío con manejo robusto de errores.

**Prioridad:** Alta

**Actor Principal:** Sistema (Servicio de Notificaciones)

**Actores Secundarios:** Telegram Bot API, Message Broker

**Precondiciones:**
- Telegram Bot configurado con token válido
- Message broker operativo
- Tabla de mensajes creada en base de datos
- Cliente tiene cuenta de Telegram asociada al teléfono

**Modelo de Datos (Entidad Mensaje - Extendido):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| id | BIGINT | Identificador único del mensaje | 12345 |
| ticketId | UUID | Referencia al ticket (FK) | "a1b2c3d4-e5f6..." |
| telefono | String | Número de teléfono destino | "+56912345678" |
| plantilla | Enum | Tipo de mensaje (ver 3.4) | totem_ticket_creado |
| estadoEnvio | Enum | Estado actual del envío | PENDIENTE, ENVIADO, FALLIDO |
| fechaProgramada | Timestamp | Cuándo debe enviarse | "2024-12-15T10:30:00Z" |
| fechaEnvio | Timestamp | Cuándo se envió realmente | "2024-12-15T10:30:05Z" (nullable) |
| telegramMessageId | String | ID retornado por Telegram API | "msg_xyz123" (nullable) |
| intentos | Integer | Contador de reintentos | 0, 1, 2, 3 (default: 0) |
| ultimoError | String | Mensaje de error del último intento | "Network timeout" (nullable) |
| proximoReintento | Timestamp | Cuándo se reintentará | "2024-12-15T10:30:30Z" (nullable) |
| contenidoMensaje | Text | Mensaje renderizado con variables | "✅ Ticket Creado\n\nTu número..." |

**Estados de Envío (Enum):**

| Valor | Descripción | Es Final? |
|-------|-------------|----------|
| PENDIENTE | Esperando envío o reintento | No |
| ENVIADO | Enviado exitosamente | Sí |
| FALLIDO | Falló tras 3 reintentos | Sí |

**Reglas de Negocio Aplicables:**
- **RN-007:** 3 reintentos automáticos para mensajes fallidos
- **RN-008:** Backoff exponencial (30s, 60s, 120s)
- **RN-011:** Auditoría de envíos
- **RN-012:** Mensaje 2 cuando posición ≤ 3
- **RN-016:** Transactional Outbox Pattern (eventos)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Programación de 3 mensajes al crear ticket**
```gherkin
Given el sistema publica evento "TicketCreatedEvent" con:
  | codigoReferencia | numero | telefono     | positionInQueue |
  | uuid-123         | C01    | +56912345678 | 5               |
When el Servicio de Notificaciones consume el evento
Then el sistema crea 3 registros de mensaje:
  | plantilla            | estadoEnvio | fechaProgramada | intentos |
  | totem_ticket_creado  | PENDIENTE   | NOW()           | 0        |
  | totem_proximo_turno  | PENDIENTE   | NULL            | 0        |
  | totem_es_tu_turno    | PENDIENTE   | NULL            | 0        |
And solo el Mensaje 1 tiene fechaProgramada = NOW()
And los Mensajes 2 y 3 esperan eventos futuros
```

**Escenario 2: Envío exitoso del Mensaje 1 en primer intento**
```gherkin
Given existe un mensaje con:
  | id    | plantilla           | estadoEnvio | intentos | telefono     |
  | 12345 | totem_ticket_creado | PENDIENTE   | 0        | +56912345678 |
When el Scheduler ejecuta el proceso de envío
And el sistema renderiza la plantilla con variables del ticket
And el sistema llama a Telegram Bot API: sendMessage()
And Telegram API retorna HTTP 200 con:
  {
    "ok": true,
    "result": {
      "message_id": "msg_xyz123"
    }
  }
Then el sistema actualiza el mensaje:
  | estadoEnvio | telegramMessageId | fechaEnvio          | intentos |
  | ENVIADO     | msg_xyz123        | 2024-12-15T10:30:05 | 1        |
And el sistema registra auditoría: "MENSAJE_ENVIADO"
And el mensaje NO se reintenta más
```

**Escenario 3: Fallo de red en primer intento, éxito en segundo (RN-008)**
```gherkin
Given existe un mensaje PENDIENTE con intentos = 0
When el sistema intenta enviar a Telegram API
And Telegram API retorna error de timeout (HTTP 504)
Then el sistema actualiza el mensaje:
  | estadoEnvio | intentos | ultimoError      | proximoReintento        |
  | PENDIENTE   | 1        | "Network timeout" | NOW() + 30 segundos     |
And el sistema registra auditoría: "MENSAJE_REINTENTO_PROGRAMADO"
When el Scheduler ejecuta 30 segundos después
And el sistema reintenta el envío (intento 2)
And Telegram API retorna HTTP 200 (éxito)
Then el sistema actualiza:
  | estadoEnvio | telegramMessageId | intentos | fechaEnvio          |
  | ENVIADO     | msg_abc456        | 2        | 2024-12-15T10:30:35 |
And el sistema registra auditoría: "MENSAJE_ENVIADO_TRAS_REINTENTO"
```

**Escenario 4: 4 intentos fallidos → estado FALLIDO (RN-007)**
```gherkin
Given existe un mensaje PENDIENTE con intentos = 0
When el intento 1 falla (timeout) en t=0s
Then proximoReintento = t+30s, intentos = 1
When el intento 2 falla (timeout) en t=30s
Then proximoReintento = t+90s (30s + 60s), intentos = 2
When el intento 3 falla (timeout) en t=90s
Then proximoReintento = t+210s (90s + 120s), intentos = 3
When el intento 4 falla (timeout) en t=210s
Then el sistema actualiza:
  | estadoEnvio | intentos | proximoReintento | ultimoError      |
  | FALLIDO     | 4        | NULL             | "Network timeout" |
And el sistema NO programa más reintentos
And el sistema registra auditoría: "MENSAJE_FALLIDO_TRAS_4_INTENTOS"
And el sistema envía alerta al equipo de soporte
```

**Escenario 5: Backoff exponencial - Tiempos de espera correctos (RN-008)**
```gherkin
Given un mensaje falla en el intento 1 en t=0s
Then proximoReintento = t+30s
When falla en el intento 2 en t=30s
Then proximoReintento = t+90s (espera 60s adicionales)
When falla en el intento 3 en t=90s
Then proximoReintento = t=210s (espera 120s adicionales)
And los tiempos de espera son: 30s, 60s, 120s (exponencial)
```

**Escenario 6: Error no recuperable - Teléfono inválido**
```gherkin
Given existe un mensaje con telefono "+56900000000"
When el sistema intenta enviar a Telegram API
And Telegram API retorna HTTP 400 con:
  {
    "ok": false,
    "error_code": 400,
    "description": "Bad Request: phone number invalid"
  }
Then el sistema actualiza:
  | estadoEnvio | intentos | ultimoError                | proximoReintento |
  | FALLIDO     | 1        | "Phone number invalid"     | NULL             |
And el sistema NO programa reintentos (error no recuperable)
And el sistema registra auditoría: "MENSAJE_FALLIDO_TELEFONO_INVALIDO"
```

**Escenario 7: Renderizado de plantilla con variables**
```gherkin
Given existe un mensaje con plantilla "totem_ticket_creado"
And el ticket asociado tiene:
  | numero | positionInQueue | estimatedWaitMinutes |
  | C01    | 5               | 25                   |
When el sistema renderiza la plantilla
Then el contenidoMensaje generado es:
  """
  ✅ <b>Ticket Creado</b>
  
  Tu número de turno: <b>C01</b>
  Posición en cola: <b>#5</b>
  Tiempo estimado: <b>25 minutos</b>
  
  Te notificaremos cuando estés próximo.
  """
And el sistema almacena contenidoMensaje en la BD
And el sistema envía este texto a Telegram API con parse_mode="HTML"
```

**Escenario 8: Mensaje 2 (pre-aviso) se programa al cambiar posición**
```gherkin
Given un ticket "P05" tiene positionInQueue = 4
And existe un mensaje con:
  | plantilla           | estadoEnvio | fechaProgramada |
  | totem_proximo_turno | PENDIENTE   | NULL            |
When el sistema publica evento "TicketPositionChangedEvent" con:
  | numero | oldPosition | newPosition |
  | P05    | 4           | 3           |
And el Servicio de Notificaciones detecta newPosition ≤ 3
Then el sistema actualiza el mensaje:
  | fechaProgramada | estadoEnvio |
  | NOW()           | PENDIENTE   |
And el Scheduler envía el mensaje en el siguiente ciclo
And el mensaje contiene: "⏰ ¡Pronto será tu turno! Turno: P05"
```

**Escenario 9: Mensaje 3 (turno activo) se envía al asignar asesor**
```gherkin
Given un ticket "E03" es asignado al asesor "María González" en módulo 3
And existe un mensaje con:
  | plantilla         | estadoEnvio | fechaProgramada |
  | totem_es_tu_turno | PENDIENTE   | NULL            |
When el sistema publica evento "TicketAssignedEvent" con:
  | numero | moduleNumber | advisorName      |
  | E03    | 3            | María González |
Then el sistema actualiza el mensaje:
  | fechaProgramada | estadoEnvio |
  | NOW()           | PENDIENTE   |
And el sistema renderiza la plantilla con variables:
  | {numero} | {modulo} | {nombreAsesor}  |
  | E03      | 3        | María González |
And el Scheduler envía el mensaje
And el mensaje contiene: "🔔 ¡ES TU TURNO E03! Dirígete al módulo: 3"
```

**Escenario 10: Consulta de historial de mensajes de un ticket**
```gherkin
Given un ticket "C01" tiene 3 mensajes:
  | plantilla            | estadoEnvio | fechaEnvio          | intentos |
  | totem_ticket_creado  | ENVIADO     | 2024-12-15T10:30:05 | 1        |
  | totem_proximo_turno  | ENVIADO     | 2024-12-15T10:35:10 | 2        |
  | totem_es_tu_turno    | ENVIADO     | 2024-12-15T10:40:00 | 1        |
When el sistema consulta GET /api/tickets/C01/messages
Then el sistema retorna HTTP 200 con JSON:
  {
    "ticketNumero": "C01",
    "mensajes": [
      {
        "plantilla": "totem_ticket_creado",
        "estadoEnvio": "ENVIADO",
        "fechaEnvio": "2024-12-15T10:30:05Z",
        "intentos": 1
      },
      {
        "plantilla": "totem_proximo_turno",
        "estadoEnvio": "ENVIADO",
        "fechaEnvio": "2024-12-15T10:35:10Z",
        "intentos": 2
      },
      {
        "plantilla": "totem_es_tu_turno",
        "estadoEnvio": "ENVIADO",
        "fechaEnvio": "2024-12-15T10:40:00Z",
        "intentos": 1
      }
    ]
  }
```

**Postcondiciones:**
- Mensaje almacenado en BD con estado actualizado
- telegramMessageId registrado si envío exitoso
- Contador de intentos incrementado en cada reintento
- proximoReintento calculado según backoff exponencial
- Evento de auditoría registrado
- Alerta generada si mensaje falla tras 4 intentos
- Cliente recibe notificación en Telegram (si envío exitoso)

**Endpoints HTTP:**
- `GET /api/tickets/{numero}/messages` - Consultar historial de mensajes

**Response 200 OK:**
```json
{
  "ticketNumero": "C01",
  "mensajes": [
    {
      "id": 12345,
      "plantilla": "totem_ticket_creado",
      "estadoEnvio": "ENVIADO",
      "fechaEnvio": "2024-12-15T10:30:05Z",
      "intentos": 1,
      "telegramMessageId": "msg_xyz123"
    }
  ]
}
```

---

### RF-007: Panel de Monitoreo en Tiempo Real

**Descripción:**  
El sistema debe proporcionar un panel de monitoreo en tiempo real para supervisores y gerentes, mostrando el estado actual de todas las colas, asesores, y tickets activos. El panel debe actualizarse automáticamente sin recargar la página, permitiendo toma de decisiones operacionales inmediatas.

**Prioridad:** Media

**Actor Principal:** Supervisor, Gerente de Sucursal

**Actores Secundarios:** Sistema (actualización automática)

**Precondiciones:**
- Usuario autenticado con rol de supervisor o gerente
- Sistema de colas operativo
- WebSocket o Server-Sent Events configurado para actualizaciones en tiempo real

**Modelo de Datos (Vista Agregada):**

**Dashboard Summary:**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| totalTicketsActivos | Integer | Tickets EN_ESPERA + PROXIMO + ATENDIENDO | 45 |
| totalTicketsHoy | Integer | Tickets creados hoy (todas las colas) | 127 |
| totalTicketsCompletados | Integer | Tickets COMPLETADO hoy | 82 |
| tiempoEsperaPromedio | Integer | Promedio de minutos de espera (hoy) | 18 |
| asesoresDisponibles | Integer | Asesores con status AVAILABLE | 3 |
| asesoresOcupados | Integer | Asesores con status BUSY | 5 |
| asesoresOffline | Integer | Asesores con status OFFLINE | 2 |

**Queue Status (por cada cola):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| queueType | Enum | Tipo de cola | CAJA, PERSONAL_BANKER |
| ticketsEnEspera | Integer | Tickets EN_ESPERA en esta cola | 12 |
| ticketsProximos | Integer | Tickets PROXIMO (posición ≤ 3) | 3 |
| ticketsAtendiendo | Integer | Tickets ATENDIENDO en esta cola | 4 |
| tiempoEsperaPromedio | Integer | Minutos promedio de espera | 25 |
| ticketMasAntiguo | String | Número del ticket más antiguo | "C05" |
| minutosEsperaMaximo | Integer | Minutos del ticket más antiguo | 45 |

**Advisor Status (por cada asesor):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| advisorId | UUID | ID del asesor | "advisor-001" |
| nombre | String | Nombre del asesor | "María González" |
| moduleNumber | Integer | Número de módulo | 3 |
| status | Enum | Estado actual | AVAILABLE, BUSY, OFFLINE |
| currentTicket | String | Ticket actual (si BUSY) | "C05" (nullable) |
| ticketsAtendidosHoy | Integer | Tickets completados hoy | 15 |
| tiempoPromedioAtencion | Integer | Minutos promedio por ticket | 8 |

**Reglas de Negocio Aplicables:**
- **RN-002:** Prioridad de colas (para ordenar visualización)
- **RN-009:** Estados de ticket (para contadores)
- **RN-011:** Auditoría de accesos al panel
- **RN-013:** Estados de asesor (para dashboard)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Visualización inicial del dashboard**
```gherkin
Given un supervisor autenticado accede al panel de monitoreo
When el sistema carga el dashboard
Then el sistema muestra:
  | Métrica                  | Valor |
  | totalTicketsActivos     | 45    |
  | totalTicketsHoy         | 127   |
  | totalTicketsCompletados | 82    |
  | asesoresDisponibles     | 3     |
  | asesoresOcupados        | 5     |
  | asesoresOffline         | 2     |
And el sistema muestra 4 secciones de colas (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
And el sistema muestra lista de 10 asesores con su estado actual
And el tiempo de carga es < 2 segundos
```

**Escenario 2: Actualización en tiempo real al crear ticket**
```gherkin
Given el supervisor está viendo el dashboard
And la cola CAJA tiene 12 tickets EN_ESPERA
When un cliente crea un nuevo ticket para CAJA
Then el dashboard se actualiza automáticamente sin recargar
And la sección CAJA muestra:
  | ticketsEnEspera | 13 |
And totalTicketsActivos incrementa en 1
And totalTicketsHoy incrementa en 1
And la actualización ocurre en < 3 segundos
```

**Escenario 3: Actualización al asignar ticket a asesor**
```gherkin
Given el dashboard muestra:
  | asesoresDisponibles | 3 |
  | asesoresOcupados    | 5 |
And la cola PERSONAL_BANKER tiene 8 tickets EN_ESPERA
When el sistema asigna ticket "P05" al asesor "María González"
Then el dashboard actualiza:
  | asesoresDisponibles | 2 |
  | asesoresOcupados    | 6 |
And la cola PERSONAL_BANKER muestra:
  | ticketsEnEspera   | 7 |
  | ticketsAtendiendo | 5 |
And el asesor "María González" muestra:
  | status        | BUSY |
  | currentTicket | P05  |
```

**Escenario 4: Alerta de ticket con espera prolongada**
```gherkin
Given la cola EMPRESAS tiene un ticket "E03" esperando 60 minutos
And el umbral de alerta es 45 minutos
When el dashboard se actualiza
Then el sistema resalta la cola EMPRESAS en color rojo
And el sistema muestra:
  | ticketMasAntiguo    | E03 |
  | minutosEsperaMaximo | 60  |
And el sistema envía notificación al supervisor
```

**Escenario 5: Filtrado por cola específica**
```gherkin
Given el supervisor está en el dashboard
When el supervisor selecciona filtro "Solo GERENCIA"
Then el sistema muestra solo datos de cola GERENCIA:
  | ticketsEnEspera   | 2  |
  | ticketsAtendiendo | 1  |
  | ticketMasAntiguo  | G02 |
And el sistema oculta las otras 3 colas
And los contadores globales permanecen visibles
```

**Escenario 6: Consulta de historial de asesor**
```gherkin
Given el supervisor hace clic en asesor "Juan Pérez"
When el sistema muestra detalle del asesor
Then el sistema retorna:
  {
    "advisorId": "advisor-002",
    "nombre": "Juan Pérez",
    "moduleNumber": 2,
    "status": "BUSY",
    "currentTicket": "C10",
    "ticketsAtendidosHoy": 18,
    "tiempoPromedioAtencion": 7,
    "historialHoy": [
      {"ticket": "C09", "duracion": 6, "completadoEn": "10:45:00"},
      {"ticket": "C08", "duracion": 8, "completadoEn": "10:38:00"}
    ]
  }
```

**Escenario 7: Dashboard sin tickets activos**
```gherkin
Given es inicio de jornada (8:00 AM)
And no hay tickets creados aún
When el supervisor accede al dashboard
Then el sistema muestra:
  | totalTicketsActivos     | 0 |
  | totalTicketsHoy         | 0 |
  | totalTicketsCompletados | 0 |
And el sistema muestra mensaje: "No hay tickets activos en este momento"
And las 4 colas muestran ticketsEnEspera = 0
```

**Escenario 8: Performance con alta carga (100+ tickets activos)**
```gherkin
Given existen 150 tickets activos distribuidos en 4 colas
And hay 10 asesores activos
When el supervisor accede al dashboard
Then el sistema carga todos los datos en < 2 segundos
And el sistema usa agregaciones precalculadas (no consultas complejas)
And las actualizaciones en tiempo real no degradan performance
```

**Postcondiciones:**
- Dashboard cargado con datos en tiempo real
- WebSocket/SSE establecido para actualizaciones automáticas
- Evento de auditoría registrado: "DASHBOARD_ACCEDIDO"
- Métricas agregadas calculadas y cacheadas
- Alertas generadas si hay tickets con espera > umbral

**Endpoints HTTP:**
- `GET /api/dashboard/summary` - Resumen general del dashboard
- `GET /api/dashboard/queues` - Estado de todas las colas
- `GET /api/dashboard/advisors` - Estado de todos los asesores
- `GET /api/dashboard/advisors/{advisorId}/history` - Historial de asesor
- `WS /api/dashboard/realtime` - WebSocket para actualizaciones en tiempo real

**Response 200 OK (GET /api/dashboard/summary):**
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "totalTicketsActivos": 45,
  "totalTicketsHoy": 127,
  "totalTicketsCompletados": 82,
  "tiempoEsperaPromedio": 18,
  "asesoresDisponibles": 3,
  "asesoresOcupados": 5,
  "asesoresOffline": 2
}
```

**Response 200 OK (GET /api/dashboard/queues):**
```json
{
  "queues": [
    {
      "queueType": "CAJA",
      "ticketsEnEspera": 12,
      "ticketsProximos": 3,
      "ticketsAtendiendo": 4,
      "tiempoEsperaPromedio": 25,
      "ticketMasAntiguo": "C05",
      "minutosEsperaMaximo": 45,
      "alertaEsperaProlongada": true
    },
    {
      "queueType": "PERSONAL_BANKER",
      "ticketsEnEspera": 8,
      "ticketsProximos": 2,
      "ticketsAtendiendo": 3,
      "tiempoEsperaPromedio": 35,
      "ticketMasAntiguo": "P08",
      "minutosEsperaMaximo": 38,
      "alertaEsperaProlongada": false
    }
  ]
}
```

**Response 200 OK (GET /api/dashboard/advisors):**
```json
{
  "advisors": [
    {
      "advisorId": "advisor-001",
      "nombre": "María González",
      "moduleNumber": 3,
      "status": "BUSY",
      "currentTicket": "E03",
      "ticketsAtendidosHoy": 15,
      "tiempoPromedioAtencion": 8
    },
    {
      "advisorId": "advisor-002",
      "nombre": "Juan Pérez",
      "moduleNumber": 2,
      "status": "AVAILABLE",
      "currentTicket": null,
      "ticketsAtendidosHoy": 18,
      "tiempoPromedioAtencion": 7
    }
  ]
}
```

**WebSocket Message (Actualización en Tiempo Real):**
```json
{
  "eventType": "DashboardUpdate",
  "timestamp": "2024-12-15T10:30:05Z",
  "updates": {
    "summary": {
      "totalTicketsActivos": 46,
      "totalTicketsHoy": 128
    },
    "queues": [
      {
        "queueType": "CAJA",
        "ticketsEnEspera": 13
      }
    ],
    "advisors": [
      {
        "advisorId": "advisor-001",
        "status": "BUSY",
        "currentTicket": "C11"
      }
    ]
  }
}
```

**Notas Técnicas:**
- El dashboard usa WebSocket o Server-Sent Events para actualizaciones en tiempo real
- Las métricas agregadas se calculan cada 30 segundos y se cachean
- Las alertas de espera prolongada se evalúan cada minuto
- El umbral de alerta por defecto es 45 minutos (configurable)
- El sistema publica eventos DashboardUpdate cuando cambia estado de tickets o asesores
- El frontend debe manejar reconexiones automáticas del WebSocket

---

### RF-008: Auditoría y Trazabilidad

**Descripción:**  
El sistema debe registrar todos los eventos críticos del negocio para garantizar trazabilidad completa, cumplimiento normativo, y capacidad de análisis forense. Los registros de auditoría deben ser inmutables, consultables, y retenidos según políticas de la organización.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Actores Secundarios:** Auditor, Supervisor, Compliance Officer

**Precondiciones:**
- Sistema de auditoría configurado
- Tabla de auditoría creada en base de datos
- Políticas de retención definidas

**Modelo de Datos (Entidad AuditLog):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| id | BIGINT | Identificador único del registro | 123456 |
| timestamp | Timestamp | Fecha/hora del evento (UTC) | "2024-12-15T10:30:00Z" |
| eventType | String | Tipo de evento auditado | "TICKET_CREADO" |
| entityType | String | Tipo de entidad afectada | "Ticket", "Advisor", "Mensaje" |
| entityId | String | ID de la entidad afectada | "uuid-123" |
| actor | String | Quién ejecutó la acción | "system", "advisor-001", "admin-001" |
| actorType | Enum | Tipo de actor | SYSTEM, USER, ADVISOR, ADMIN |
| action | String | Acción ejecutada | "CREATE", "UPDATE", "DELETE" |
| previousState | JSON | Estado anterior (nullable) | {"status": "EN_ESPERA"} |
| newState | JSON | Estado nuevo | {"status": "ATENDIENDO"} |
| metadata | JSON | Información adicional | {"ip": "192.168.1.10", "userAgent": "..."} |
| severity | Enum | Nivel de severidad | INFO, WARNING, ERROR, CRITICAL |

**Tipos de Eventos Auditados:**

| Evento | EntityType | Severidad | Descripción |
|--------|------------|-----------|-------------|
| TICKET_CREADO | Ticket | INFO | Cliente crea nuevo ticket |
| TICKET_ASIGNADO | Ticket | INFO | Ticket asignado a asesor |
| TICKET_COMPLETADO | Ticket | INFO | Asesor completa atención |
| TICKET_CANCELADO | Ticket | WARNING | Ticket cancelado (timeout o manual) |
| MENSAJE_ENVIADO | Mensaje | INFO | Mensaje Telegram enviado exitosamente |
| MENSAJE_FALLIDO | Mensaje | ERROR | Mensaje falló tras 4 reintentos |
| ASESOR_DISPONIBLE | Advisor | INFO | Asesor cambia a AVAILABLE |
| ASESOR_OFFLINE | Advisor | INFO | Asesor cambia a OFFLINE |
| ASESOR_OFFLINE_FORZADO | Advisor | WARNING | Supervisor fuerza OFFLINE |
| DASHBOARD_ACCEDIDO | Dashboard | INFO | Usuario accede al panel de monitoreo |
| DATOS_SENSIBLES_ACCEDIDOS | Ticket | CRITICAL | Acceso a nationalId o telefono sin enmascarar |
| CONFIGURACION_MODIFICADA | System | WARNING | Cambio en configuración del sistema |

**Reglas de Negocio Aplicables:**
- **RN-011:** Auditoría obligatoria de eventos críticos
- **RN-014:** Enmascaramiento de datos sensibles en logs

**Políticas de Retención:**
- **Eventos INFO:** 90 días en base de datos activa, 2 años en archivo
- **Eventos WARNING:** 180 días en base de datos activa, 3 años en archivo
- **Eventos ERROR/CRITICAL:** 365 días en base de datos activa, 5 años en archivo
- **Eventos con datos sensibles:** Cifrados en reposo, retención según normativa

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Auditoría de creación de ticket**
```gherkin
Given un cliente crea un ticket "C01" exitosamente
When el sistema persiste el ticket en base de datos
Then el sistema registra evento de auditoría:
  | Campo       | Valor                      |
  | eventType   | TICKET_CREADO              |
  | entityType  | Ticket                     |
  | entityId    | uuid-123                   |
  | actor       | system                     |
  | actorType   | SYSTEM                     |
  | action      | CREATE                     |
  | newState    | {"numero": "C01", "status": "EN_ESPERA"} |
  | severity    | INFO                       |
And el registro incluye timestamp en UTC
And el registro es inmutable (no se puede modificar)
```

**Escenario 2: Auditoría de cambio de estado de ticket**
```gherkin
Given un ticket "P05" tiene status = EN_ESPERA
When el sistema asigna el ticket al asesor "María González"
Then el sistema registra evento de auditoría:
  | Campo         | Valor                                    |
  | eventType     | TICKET_ASIGNADO                          |
  | entityType    | Ticket                                   |
  | entityId      | uuid-456                                 |
  | actor         | system                                   |
  | action        | UPDATE                                   |
  | previousState | {"status": "EN_ESPERA", "assignedAdvisorId": null} |
  | newState      | {"status": "ATENDIENDO", "assignedAdvisorId": "advisor-001"} |
  | severity      | INFO                                     |
```

**Escenario 3: Auditoría de mensaje fallido**
```gherkin
Given un mensaje falla tras 4 intentos de envío
When el sistema marca el mensaje como FALLIDO
Then el sistema registra evento de auditoría:
  | Campo      | Valor                                  |
  | eventType  | MENSAJE_FALLIDO                        |
  | entityType | Mensaje                                |
  | entityId   | 12345                                  |
  | actor      | system                                 |
  | severity   | ERROR                                  |
  | metadata   | {"intentos": 4, "ultimoError": "Network timeout"} |
And el sistema envía alerta al equipo de soporte
```

**Escenario 4: Auditoría de acción de supervisor**
```gherkin
Given un supervisor "admin-001" fuerza OFFLINE al asesor "Juan Pérez"
When el sistema cambia el estado del asesor
Then el sistema registra evento de auditoría:
  | Campo      | Valor                                  |
  | eventType  | ASESOR_OFFLINE_FORZADO                 |
  | entityType | Advisor                                |
  | entityId   | advisor-002                            |
  | actor      | admin-001                              |
  | actorType  | ADMIN                                  |
  | action     | UPDATE                                 |
  | severity   | WARNING                                |
  | metadata   | {"motivo": "Emergencia", "ticketAfectado": "C10"} |
```

**Escenario 5: Consulta de auditoría por ticket**
```gherkin
Given existen eventos de auditoría para ticket "C01":
  | eventType         | timestamp           | actor  |
  | TICKET_CREADO     | 2024-12-15T10:00:00 | system |
  | TICKET_ASIGNADO   | 2024-12-15T10:15:00 | system |
  | TICKET_COMPLETADO | 2024-12-15T10:25:00 | advisor-001 |
When el auditor consulta GET /api/audit/ticket/C01
Then el sistema retorna HTTP 200 con JSON:
  {
    "entityId": "uuid-123",
    "entityType": "Ticket",
    "eventos": [
      {
        "eventType": "TICKET_CREADO",
        "timestamp": "2024-12-15T10:00:00Z",
        "actor": "system",
        "action": "CREATE"
      },
      {
        "eventType": "TICKET_ASIGNADO",
        "timestamp": "2024-12-15T10:15:00Z",
        "actor": "system",
        "action": "UPDATE"
      },
      {
        "eventType": "TICKET_COMPLETADO",
        "timestamp": "2024-12-15T10:25:00Z",
        "actor": "advisor-001",
        "action": "UPDATE"
      }
    ]
  }
```

**Escenario 6: Consulta de auditoría por rango de fechas**
```gherkin
Given el auditor quiere ver eventos del día 2024-12-15
When el auditor consulta GET /api/audit?startDate=2024-12-15&endDate=2024-12-15&severity=ERROR
Then el sistema retorna eventos con severity = ERROR del día especificado
And el sistema pagina los resultados (máximo 100 por página)
And el sistema incluye total de registros encontrados
```

**Escenario 7: Enmascaramiento de datos sensibles en auditoría (RN-014)**
```gherkin
Given un ticket se crea con nationalId "12345678-9"
When el sistema registra evento TICKET_CREADO
Then el campo newState contiene nationalIdMasked: "****5678-9"
And el nationalId completo NO aparece en el registro de auditoría
And el telefono aparece enmascarado: "+569****5678"
```

**Escenario 8: Inmutabilidad de registros de auditoría**
```gherkin
Given existe un registro de auditoría con id = 123456
When un usuario intenta modificar el registro vía API
Then el sistema rechaza la solicitud con HTTP 403 Forbidden
And el sistema retorna mensaje: "Los registros de auditoría son inmutables"
And el sistema registra intento de modificación como evento CRITICAL
```

**Postcondiciones:**
- Evento de auditoría almacenado en base de datos
- Registro inmutable (no se puede modificar ni eliminar)
- Datos sensibles enmascarados según RN-014
- Timestamp en UTC para consistencia global
- Índices creados para consultas eficientes (entityId, timestamp, eventType)

**Endpoints HTTP:**
- `GET /api/audit/ticket/{numero}` - Auditoría de un ticket específico
- `GET /api/audit/advisor/{advisorId}` - Auditoría de un asesor
- `GET /api/audit?startDate={date}&endDate={date}&severity={level}` - Consulta por rango
- `GET /api/audit/stats` - Estadísticas de auditoría

**Response 200 OK (GET /api/audit/ticket/{numero}):**
```json
{
  "entityId": "uuid-123",
  "entityType": "Ticket",
  "numero": "C01",
  "eventos": [
    {
      "id": 123456,
      "eventType": "TICKET_CREADO",
      "timestamp": "2024-12-15T10:00:00Z",
      "actor": "system",
      "actorType": "SYSTEM",
      "action": "CREATE",
      "newState": {
        "numero": "C01",
        "status": "EN_ESPERA",
        "nationalIdMasked": "****5678-9"
      },
      "severity": "INFO"
    },
    {
      "id": 123457,
      "eventType": "TICKET_ASIGNADO",
      "timestamp": "2024-12-15T10:15:00Z",
      "actor": "system",
      "actorType": "SYSTEM",
      "action": "UPDATE",
      "previousState": {
        "status": "EN_ESPERA"
      },
      "newState": {
        "status": "ATENDIENDO",
        "assignedAdvisorId": "advisor-001"
      },
      "severity": "INFO"
    }
  ]
}
```

**Response 200 OK (GET /api/audit/stats):**
```json
{
  "periodo": "2024-12-15",
  "totalEventos": 1247,
  "porSeveridad": {
    "INFO": 1150,
    "WARNING": 85,
    "ERROR": 10,
    "CRITICAL": 2
  },
  "porTipo": {
    "TICKET_CREADO": 127,
    "TICKET_ASIGNADO": 120,
    "TICKET_COMPLETADO": 82,
    "MENSAJE_ENVIADO": 360,
    "ASESOR_DISPONIBLE": 45
  },
  "eventosRecientes": [
    {
      "eventType": "TICKET_CREADO",
      "timestamp": "2024-12-15T10:30:00Z",
      "severity": "INFO"
    }
  ]
}
```

**Notas Técnicas:**
- Los registros de auditoría son append-only (solo inserción, no actualización ni eliminación)
- Los datos sensibles se enmascaran antes de almacenar en auditoría
- Los índices en (entityId, timestamp, eventType) optimizan consultas
- La retención de datos se gestiona mediante jobs programados
- Los eventos CRITICAL generan alertas inmediatas al equipo de seguridad
- El sistema usa particionamiento por fecha para mejorar performance de consultas históricas

---

## 5. Matriz de Trazabilidad

### 5.1 Mapeo Reglas de Negocio → Requerimientos Funcionales

| Regla de Negocio | RF-001 | RF-002 | RF-003 | RF-004 | RF-005 | RF-006 | RF-007 | RF-008 |
|------------------|--------|--------|--------|--------|--------|--------|--------|--------|
| RN-001: Unicidad Ticket | ✅ | | | | | | | |
| RN-002: Prioridad Colas | | | | ✅ | | | ✅ | |
| RN-003: Orden FIFO | | | ✅ | ✅ | | | | |
| RN-004: Balanceo Carga | | | | ✅ | ✅ | | | |
| RN-005: Formato Número | ✅ | | | | | | | |
| RN-006: Prefijos Cola | ✅ | | | | | | | |
| RN-007: 3 Reintentos | | ✅ | | | | ✅ | | |
| RN-008: Backoff Exponencial | | ✅ | | | | ✅ | | |
| RN-009: Estados Ticket | ✅ | | ✅ | ✅ | | | ✅ | |
| RN-010: Cálculo Tiempo | ✅ | | ✅ | | | | | |
| RN-011: Auditoría | ✅ | ✅ | | ✅ | ✅ | ✅ | ✅ | ✅ |
| RN-012: Umbral Pre-aviso | | ✅ | ✅ | | | ✅ | | |
| RN-013: Estados Asesor | | | | ✅ | ✅ | | ✅ | |
| RN-014: Protección Datos | ✅ | | | | | | | ✅ |
| RN-015: Performance < 1s | ✅ | | ✅ | | | | ✅ | |
| RN-016: Transactional Outbox | ✅ | | | | | ✅ | | |

### 5.2 Mapeo Requerimientos Funcionales → Eventos

| Requerimiento | Eventos Publicados | Eventos Consumidos |
|---------------|--------------------|-----------------|
| RF-001: Crear Ticket | TicketCreatedEvent | - |
| RF-002: Notificaciones | - | TicketCreatedEvent, TicketPositionChangedEvent, TicketAssignedEvent |
| RF-003: Calcular Posición | TicketPositionChangedEvent | TicketCreatedEvent, TicketAssignedEvent |
| RF-004: Asignar Ticket | TicketAssignedEvent | AdvisorAvailableEvent |
| RF-005: Estados Asesor | AdvisorAvailableEvent, AdvisorStatusChangedEvent | - |
| RF-006: Mensajes Telegram | - | TicketCreatedEvent, TicketPositionChangedEvent, TicketAssignedEvent |
| RF-007: Panel Monitoreo | DashboardUpdateEvent | Todos los eventos |
| RF-008: Auditoría | - | Todos los eventos |

### 5.3 Mapeo Requerimientos Funcionales → Endpoints HTTP

| Requerimiento | Método | Endpoint | Descripción |
|---------------|--------|----------|-------------|
| RF-001 | POST | /api/tickets | Crear ticket |
| RF-003 | GET | /api/tickets/{uuid} | Consultar ticket por UUID |
| RF-003 | GET | /api/tickets/{numero}/position | Consultar posición |
| RF-004 | POST | /api/advisors/{id}/complete-ticket | Completar atención |
| RF-004 | PUT | /api/advisors/{id}/status | Cambiar estado asesor |
| RF-005 | PUT | /api/advisors/{id}/status | Cambiar estado asesor |
| RF-005 | GET | /api/advisors/{id}/status | Consultar estado |
| RF-005 | POST | /api/advisors/{id}/force-offline | Forzar OFFLINE |
| RF-006 | GET | /api/tickets/{numero}/messages | Historial mensajes |
| RF-007 | GET | /api/dashboard/summary | Resumen dashboard |
| RF-007 | GET | /api/dashboard/queues | Estado colas |
| RF-007 | GET | /api/dashboard/advisors | Estado asesores |
| RF-007 | GET | /api/dashboard/advisors/{id}/history | Historial asesor |
| RF-007 | WS | /api/dashboard/realtime | WebSocket tiempo real |
| RF-008 | GET | /api/audit/ticket/{numero} | Auditoría ticket |
| RF-008 | GET | /api/audit/advisor/{id} | Auditoría asesor |
| RF-008 | GET | /api/audit | Consulta por rango |
| RF-008 | GET | /api/audit/stats | Estadísticas |

### 5.4 Cobertura de Casos de Prueba

| Requerimiento | Escenarios Gherkin | Happy Path | Errores | Edge Cases | Performance |
|---------------|-------------------|------------|---------|------------|-------------|
| RF-001 | 14 | ✅ | ✅ | ✅ | ✅ |
| RF-002 | 9 | ✅ | ✅ | ✅ | ❌ |
| RF-003 | 8 | ✅ | ✅ | ✅ | ✅ |
| RF-004 | 8 | ✅ | ✅ | ✅ | ❌ |
| RF-005 | 8 | ✅ | ✅ | ✅ | ❌ |
| RF-006 | 10 | ✅ | ✅ | ✅ | ❌ |
| RF-007 | 8 | ✅ | ✅ | ✅ | ✅ |
| RF-008 | 8 | ✅ | ✅ | ✅ | ❌ |
| **TOTAL** | **73** | **8/8** | **8/8** | **8/8** | **3/8** |

---

## 6. Resumen Ejecutivo

### Métricas del Documento

| Métrica | Valor |
|---------|-------|
| Requerimientos Funcionales | 8 |
| Reglas de Negocio | 16 |
| Enumeraciones | 4 |
| Escenarios Gherkin | 73 |
| Endpoints HTTP | 18 |
| Eventos Publicados | 6 |
| Páginas Generadas | ~52 |

### Cobertura de Requerimientos de Negocio

| Requerimiento Negocio | RF Asociados | Estado |
|-----------------------|--------------|--------|
| Digitalización de tickets | RF-001 | ✅ Completo |
| Notificaciones Telegram | RF-002, RF-006 | ✅ Completo |
| Movilidad del cliente | RF-002, RF-003 | ✅ Completo |
| Asignación inteligente | RF-004, RF-005 | ✅ Completo |
| Panel de monitoreo | RF-007 | ✅ Completo |
| Auditoría y trazabilidad | RF-008 | ✅ Completo |

### Próximos Pasos

1. **Revisión de Stakeholders:** Validar RF-001 a RF-008 con equipo de negocio
2. **Diseño de Arquitectura:** Crear documento ARQUITECTURA.md basado en estos RF
3. **Diseño de Base de Datos:** Crear esquema DDL a partir de modelos de datos
4. **Definición de APIs:** Generar especificación OpenAPI/Swagger
5. **Plan de Pruebas:** Convertir 73 escenarios Gherkin en casos de prueba automatizados

---

**Documento:** REQUERIMIENTOS-FUNCIONALES.md  
**Versión:** 1.0  
**Estado:** ✅ COMPLETADO  
**Fecha:** Diciembre 2024  
**Páginas:** 52

### RF-005: Gestionar Estados de Asesor

**Descripción:**  
El sistema debe permitir a los asesores cambiar su estado de disponibilidad (AVAILABLE, BUSY, OFFLINE) para controlar la recepción de nuevas asignaciones. Los cambios de estado deben ser inmediatos y reflejarse en el sistema de asignación automática. El sistema debe validar transiciones de estado válidas y prevenir cambios inconsistentes.

**Prioridad:** Alta

**Actor Principal:** Asesor

**Actores Secundarios:** Sistema (automatización), Supervisor

**Precondiciones:**
- Asesor autenticado en el sistema
- Panel de gestión operativo

**Modelo de Datos (Entidad Asesor):**

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|----------|
| id | UUID | Identificador único del asesor | "advisor-001" |
| nombre | String | Nombre completo del asesor | "María González" |
| moduleNumber | Integer | Número de módulo asignado (1-5) | 3 |
| status | Enum | Estado actual (ver 3.3) | AVAILABLE, BUSY, OFFLINE |
| assignedTicketsCount | Integer | Contador de tickets asignados hoy | 5 |
| currentTicketId | UUID | Ticket actualmente en atención | "uuid-123" (nullable) |
| lastStatusChange | Timestamp | Última vez que cambió estado | "2024-12-15T10:30:00Z" |

**Transiciones de Estado Válidas:**

```
AVAILABLE → BUSY (automático al asignar ticket)
AVAILABLE → OFFLINE (manual por asesor)
BUSY → AVAILABLE (automático al completar ticket)
BUSY → OFFLINE (manual, requiere completar ticket primero)
OFFLINE → AVAILABLE (manual por asesor)
```

**Transiciones Inválidas:**
- BUSY → AVAILABLE (sin completar ticket actual)
- Cualquier estado → BUSY (solo el sistema puede marcar BUSY)

**Reglas de Negocio Aplicables:**
- **RN-004:** Balanceo de carga (assignedTicketsCount)
- **RN-011:** Auditoría de cambios de estado
- **RN-013:** Estados de asesor definidos

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asesor se marca como AVAILABLE al iniciar jornada**
```gherkin
Given un asesor "María González" tiene status = OFFLINE
And el asesor inicia sesión en el panel de gestión
When el asesor presiona botón "Iniciar Atención"
Then el sistema actualiza status = AVAILABLE
And assignedTicketsCount = 0
And currentTicketId = null
And lastStatusChange = timestamp actual
And el sistema registra auditoría: "ASESOR_DISPONIBLE"
And el sistema publica evento "AdvisorAvailableEvent"
And el Servicio de Asignación escucha el evento y asigna ticket si hay EN_ESPERA
```

**Escenario 2: Sistema marca asesor como BUSY al asignar ticket**
```gherkin
Given un asesor "Juan Pérez" tiene status = AVAILABLE
And assignedTicketsCount = 2
When el sistema asigna ticket "C05" al asesor
Then el sistema actualiza automáticamente:
  | status | assignedTicketsCount | currentTicketId |
  | BUSY   | 3                    | uuid-C05        |
And lastStatusChange = timestamp de asignación
And el asesor NO puede cambiar manualmente a AVAILABLE
```

**Escenario 3: Asesor completa ticket y vuelve a AVAILABLE (Arquitectura Orientada a Eventos)**
```gherkin
Given un asesor "María González" tiene:
  | status | currentTicketId | assignedTicketsCount |
  | BUSY   | uuid-C05        | 3                    |
When el asesor marca ticket "C05" como COMPLETADO
Then el sistema actualiza automáticamente:
  | status    | currentTicketId | assignedTicketsCount |
  | AVAILABLE | null            | 2                    |
And lastStatusChange = timestamp de completado
And el sistema publica evento "AdvisorAvailableEvent"
And el Servicio de Asignación escucha el evento de forma asíncrona
And el Servicio de Asignación decide si asignar nuevo ticket
And si hay tickets EN_ESPERA, asigna el siguiente según prioridad
```

**Nota de Desacoplamiento:** El cambio de estado a AVAILABLE dispara un evento que el Servicio de Asignación consume de forma asíncrona, evitando llamadas síncronas entre servicios.

**Escenario 4: Asesor se marca como OFFLINE para almuerzo**
```gherkin
Given un asesor "Juan Pérez" tiene status = AVAILABLE
And currentTicketId = null (no está atendiendo)
When el asesor presiona botón "Pausar Atención"
And selecciona motivo "Almuerzo"
Then el sistema actualiza status = OFFLINE
And lastStatusChange = timestamp actual
And el sistema registra auditoría: "ASESOR_OFFLINE - Motivo: Almuerzo"
And el asesor NO recibe nuevas asignaciones
```

**Escenario 5: Error - Asesor intenta ir a OFFLINE mientras atiende**
```gherkin
Given un asesor "María González" tiene:
  | status | currentTicketId |
  | BUSY   | uuid-C05        |
When el asesor intenta cambiar a OFFLINE
Then el sistema rechaza el cambio
And el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "TRANSICION_INVALIDA",
    "mensaje": "Debes completar el ticket C05 antes de pausar atención",
    "currentTicket": "C05"
  }
And el status permanece BUSY
```

**Escenario 6: Supervisor fuerza cambio de estado**
```gherkin
Given un asesor "Juan Pérez" tiene status = BUSY
And el asesor no responde (emergencia)
When un supervisor ejecuta "Forzar OFFLINE" con motivo "Emergencia"
Then el sistema actualiza status = OFFLINE
And el ticket actual se marca como NO_ATENDIDO
And el ticket vuelve a cola EN_ESPERA
And el sistema registra auditoría: "ASESOR_OFFLINE_FORZADO - Supervisor: admin-001"
```

**Escenario 7: Balanceo de carga - Asesor con menos tickets recibe primero**
```gherkin
Given existen asesores AVAILABLE:
  | nombre          | assignedTicketsCount |
  | María González  | 5                    |
  | Juan Pérez      | 2                    |
  | Ana Martínez    | 8                    |
And existe un ticket "C10" EN_ESPERA
When el sistema ejecuta asignación automática
Then el sistema asigna "C10" a "Juan Pérez" (menor carga: 2)
And Juan Pérez.assignedTicketsCount = 3
And Juan Pérez.status = BUSY
```

**Escenario 8: Consulta de estado actual del asesor**
```gherkin
Given un asesor "María González" tiene:
  | status    | assignedTicketsCount | currentTicketId |
  | AVAILABLE | 3                    | null            |
When el panel consulta GET /api/advisors/advisor-001/status
Then el sistema retorna HTTP 200 con JSON:
  {
    "id": "advisor-001",
    "nombre": "María González",
    "moduleNumber": 3,
    "status": "AVAILABLE",
    "assignedTicketsCount": 3,
    "currentTicketId": null,
    "lastStatusChange": "2024-12-15T10:30:00Z"
  }
```

**Postcondiciones:**
- Estado de asesor actualizado en base de datos
- Evento de auditoría registrado con motivo del cambio
- Sistema de asignación automática notificado del cambio
- Si cambio a AVAILABLE, se ejecuta asignación inmediata
- Si cambio a OFFLINE, asesor removido del pool de asignación

**Endpoints HTTP:**
- `PUT /api/advisors/{advisorId}/status` - Cambiar estado de asesor
- `GET /api/advisors/{advisorId}/status` - Consultar estado actual
- `POST /api/advisors/{advisorId}/force-offline` - Supervisor fuerza OFFLINE

**Request Body (PUT /api/advisors/{advisorId}/status):**
```json
{
  "newStatus": "OFFLINE",
  "reason": "Almuerzo"
}
```

**Response 200 OK:**
```json
{
  "id": "advisor-001",
  "nombre": "María González",
  "status": "OFFLINE",
  "previousStatus": "AVAILABLE",
  "lastStatusChange": "2024-12-15T12:00:00Z",
  "reason": "Almuerzo"
}
```

**Response 400 Bad Request (Transición Inválida):**
```json
{
  "error": "TRANSICION_INVALIDA",
  "mensaje": "Debes completar el ticket C05 antes de pausar atención",
  "currentStatus": "BUSY",
  "requestedStatus": "OFFLINE",
  "currentTicket": "C05"
}
```

**Evento de Auditoría:**
```json
{
  "eventType": "AdvisorStatusChanged",
  "timestamp": "2024-12-15T12:00:00Z",
  "payload": {
    "advisorId": "advisor-001",
    "advisorName": "María González",
    "previousStatus": "AVAILABLE",
    "newStatus": "OFFLINE",
    "reason": "Almuerzo",
    "changedBy": "advisor-001",
    "forced": false
  }
}
```

---

