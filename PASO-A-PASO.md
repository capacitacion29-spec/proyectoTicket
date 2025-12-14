# 🎫 Sistema Ticketero - Guía Paso a Paso

Guía completa para probar el Sistema Ticketero con notificaciones automáticas vía Telegram.

## 📋 Prerrequisitos

```bash
# 1. Compilar el p# Sistema Ticketero - Guía Completa de APIs

## 🚀 Inicio Rápido

```bash
# 1. Compilar el proyecto
mvnw.cmd clean compile

# 2. Ejecutar la aplicación
mvnw.cmd spring-boot:run

# 3. Verificar que esté corriendo
curl http://localhost:8080/actuator/health

# 4. Verificar H2 Console (opcional)
# URL: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:ticketero
# Usuario: sa
# Contraseña: (vacía)
```

## 📱 Telegram Bot Configurado
- **Token**: 8086129023:AAGOHqV0ka76C6lMgCirIRmxYlEmMlneyjw
- **Chat ID**: 6527632523
- **Estado**: ✅ ACTIVO - Envía notificaciones reales

---

## 🔗 APIs Disponibles

### **🧪 Telegram Test**

#### Probar Conexión Telegram
```bash
curl -X POST "http://localhost:8080/api/telegram/test?message=Hola desde el sistema ticketero" 
```

**Respuesta esperada:**
```
✅ Message sent successfully
```

**📱 RESULTADO:** Recibirás un mensaje en Telegram: "🧪 TEST: Hola desde el sistema ticketero"

---

### **1. Asesores (Advisors)**

> **📊 Datos Iniciales**: El sistema carga automáticamente 5 asesores al iniciar:
> - María González (Módulo 1)
> - Juan Pérez (Módulo 2) 
> - Ana Silva (Módulo 3)
> - Carlos Rojas (Módulo 4)
> - Patricia Díaz (Módulo 5)

#### Listar Asesores
```bash
curl -X GET http://localhost:8080/api/advisors
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "name": "María González",
    "email": "maria.gonzalez@institucion.cl",
    "status": "AVAILABLE",
    "moduleNumber": 1,
    "assignedTicketsCount": 0
  },
  {
    "id": 2,
    "name": "Juan Pérez",
    "email": "juan.perez@institucion.cl",
    "status": "AVAILABLE",
    "moduleNumber": 2,
    "assignedTicketsCount": 0
  }
]
```

#### Crear Asesor
```bash
curl -X POST http://localhost:8080/api/advisors \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Luis Martínez",
    "email": "luis.martinez@institucion.cl"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 6,
  "name": "Luis Martínez",
  "email": "luis.martinez@institucion.cl",
  "status": "AVAILABLE",
  "moduleNumber": 6,
  "assignedTicketsCount": 0
}
```

#### Asignar Próximo Ticket (Manual)
```bash
curl -X POST http://localhost:8080/api/advisors/assign-next
```

**Respuesta esperada:**
```json
{
  "message": "Ticket asignado exitosamente",
  "ticketId": 1,
  "advisorName": "María González",
  "moduleNumber": 1
}
```

**📱 NOTIFICACIÓN AUTOMÁTICA:** Se envía mensaje "Es tu turno" al cliente vía Telegram.

#### Completar Atención
```bash
curl -X POST http://localhost:8080/api/advisors/complete/1
```

**Respuesta esperada:**
```json
{
  "message": "Ticket completado exitosamente",
  "ticketId": 1,
  "advisorName": "María González"
}
```

---

### **2. Tickets**

#### Crear Ticket
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678",
    "nombreCliente": "Ana Rodríguez",
    "telefono": "+56912345678"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "codigoReferencia": "TK-A1B2C3D4",
  "numero": "C001",
  "nationalId": "12345678",
  "nombreCliente": "Ana Rodríguez",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Principal",
  "queueType": "CAJA",
  "status": "EN_ESPERA",
  "positionInQueue": 1,
  "estimatedWaitMinutes": 5,
  "assignedAdvisorName": null,
  "assignedModuleNumber": null,
  "createdAt": "2024-12-14T15:30:00"
}
```

**📱 NOTIFICACIÓN AUTOMÁTICA:** Se programa mensaje de confirmación para envío en 5 segundos:
```
🎫 Ticket Creado

Código: TK-A1B2C3D4
Número: C001
Cliente: Ana Rodríguez
Posición en cola: 1
Tiempo estimado: 5 minutos

⏰ Te notificaremos cuando sea tu turno.
```

#### Consultar Ticket por Código
```bash
curl -X GET http://localhost:8080/api/tickets/TK-A1B2C3D4
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "codigoReferencia": "TK-A1B2C3D4",
  "numero": "C001",
  "nationalId": "12345678",
  "nombreCliente": "Ana Rodríguez",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Principal",
  "queueType": "CAJA",
  "status": "EN_ESPERA",
  "positionInQueue": 1,
  "estimatedWaitMinutes": 5,
  "assignedAdvisorName": null,
  "assignedModuleNumber": null,
  "createdAt": "2024-12-14T15:30:00"
}
```

#### Listar Tickets Activos
```bash
curl -X GET http://localhost:8080/api/tickets
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "codigoReferencia": "TK-A1B2C3D4",
    "numero": "C001",
    "nombreCliente": "Ana Rodríguez",
    "status": "EN_ESPERA",
    "positionInQueue": 1
  }
]
```

---

### **3. Cola (Queue)**

#### Estado de la Cola
```bash
curl -X GET http://localhost:8080/api/queue/status
```

**Respuesta esperada:**
```json
{
  "totalTicketsInQueue": 3,
  "availableAdvisors": 4,
  "busyAdvisors": 1,
  "nextTickets": [
    {
      "id": 1,
      "numero": "C001",
      "nombreCliente": "Ana Rodríguez",
      "status": "EN_ESPERA",
      "positionInQueue": 1
    },
    {
      "id": 2,
      "numero": "C002",
      "nombreCliente": "Pedro Martínez",
      "status": "EN_ESPERA",
      "positionInQueue": 2
    }
  ],
  "averageWaitTimeMinutes": 8
}
```

### **4. Actuator (Monitoreo)**

#### Health Check
```bash
curl -X GET http://localhost:8080/actuator/health
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

#### Métricas del Sistema
```bash
curl -X GET http://localhost:8080/actuator/metrics
```

#### Información de la Aplicación
```bash
curl -X GET http://localhost:8080/actuator/info
```

---

## 🚀 Flujos Completos de Prueba

### **Escenario 1: Prueba de Telegram**

```bash
# PASO 1: Probar conexión con Telegram
echo "=== PASO 1: Probar Telegram ==="
curl -X POST "http://localhost:8080/api/telegram/test?message=Sistema iniciado correctamente"
echo "\n📱 Deberías recibir un mensaje en Telegram"
```

### **Escenario 2: Flujo Básico con Notificaciones Reales**

```bash
# PASO 1: Verificar asesores iniciales (5 pre-cargados)
echo "=== PASO 1: Verificar asesores ==="
curl -s http://localhost:8080/api/advisors | jq .

# PASO 2: Crear tickets (con notificaciones REALES vía Telegram)
echo "\n=== PASO 2: Crear tickets ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678", "nombreCliente": "Ana Rodríguez", "telefono": "+56912345678"}'
echo "\n📱 NOTIFICACIÓN REAL: Mensaje programado para Telegram (5 segundos)"

sleep 2

curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "87654321", "nombreCliente": "Pedro Martínez", "telefono": "+56987654321"}'
echo "\n📱 NOTIFICACIÓN REAL: Mensaje programado para Telegram (5 segundos)"

sleep 2

curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "11223344", "nombreCliente": "Laura Sánchez", "telefono": "+56911223344"}'
echo "\n📱 NOTIFICACIÓN REAL: Mensaje programado para Telegram (5 segundos)"

# PASO 3: Ver estado de cola
echo "\n=== PASO 3: Estado de cola ==="
sleep 3
curl -s http://localhost:8080/api/queue/status | jq .

# PASO 4: Esperar asignación automática (QueueProcessorScheduler cada 5s)
echo "\n=== PASO 4: Esperando asignación automática ==="
echo "⏰ QueueProcessorScheduler asignará tickets automáticamente..."
echo "📱 Recibirás notificaciones 'Es tu turno' en Telegram"
sleep 8

# PASO 5: Verificar asignaciones
echo "\n=== PASO 5: Verificar asignaciones ==="
curl -s http://localhost:8080/api/advisors | jq .
curl -s http://localhost:8080/api/queue/status | jq .

# PASO 6: Completar algunas atenciones
echo "\n=== PASO 6: Completar atenciones ==="
curl -X POST http://localhost:8080/api/advisors/complete/1
echo "\nTicket 1 completado"
curl -X POST http://localhost:8080/api/advisors/complete/2
echo "\nTicket 2 completado"

# PASO 7: Estado final
echo "\n=== PASO 7: Estado final ==="
curl -s http://localhost:8080/api/queue/status | jq .
curl -s http://localhost:8080/api/advisors | jq .
```

### **Escenario 3: Consulta de Tickets**

```bash
# Crear un ticket y consultar por código
RESPONSE=$(curl -s -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "99887766", "nombreCliente": "Carlos Silva"}')
CODIGO=$(echo $RESPONSE | jq -r '.codigoReferencia')
echo "Ticket creado: $CODIGO"
echo "📱 Recibirás notificación de confirmación en Telegram"

# Consultar el ticket
curl -s http://localhost:8080/api/tickets/$CODIGO | jq .
```

### **Escenario 4: Flujo Manual de Asignación**

```bash
# PASO 1: Crear ticket
echo "=== PASO 1: Crear ticket ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "55443322", "nombreCliente": "María Fernández", "telefono": "+56955443322"}'

# PASO 2: Ver estado antes de asignación
echo "\n=== PASO 2: Estado antes de asignación ==="
curl -s http://localhost:8080/api/queue/status | jq .

# PASO 3: Asignar manualmente
echo "\n=== PASO 3: Asignación manual ==="
curl -X POST http://localhost:8080/api/advisors/assign-next
echo "\n📱 Cliente recibe notificación 'Es tu turno' en Telegram"

# PASO 4: Ver estado después de asignación
echo "\n=== PASO 4: Estado después de asignación ==="
curl -s http://localhost:8080/api/queue/status | jq .
curl -s http://localhost:8080/api/advisors | jq .
```

---

## 🚨 Casos de Error y Validaciones

### **Validaciones de Negocio**

```bash
# 1. Ticket duplicado (mismo nationalId)
echo "=== Error: Ticket duplicado ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678", "nombreCliente": "Otro Cliente"}'
# Respuesta: 400 Bad Request
# {
#   "error": "Bad Request",
#   "message": "Ya existe un ticket activo para este National ID"
# }

# 2. Ticket no encontrado
echo "\n=== Error: Ticket no encontrado ==="
curl -X GET http://localhost:8080/api/tickets/TK-NOEXISTE
# Respuesta: 404 Not Found
# {
#   "error": "Not Found",
#   "message": "Ticket no encontrado"
# }

# 3. Completar ticket inexistente
echo "\n=== Error: Completar ticket inexistente ==="
curl -X POST http://localhost:8080/api/advisors/complete/999
# Respuesta: 400 Bad Request
# {
#   "error": "Bad Request",
#   "message": "Ticket no encontrado"
# }

# 4. Asesor duplicado (mismo email)
echo "\n=== Error: Asesor duplicado ==="
curl -X POST http://localhost:8080/api/advisors -H "Content-Type: application/json" -d '{"nombre": "Otro Juan", "email": "maria.gonzalez@institucion.cl"}'
# Respuesta: 400 Bad Request
# {
#   "error": "Bad Request",
#   "message": "Ya existe un asesor con este email"
# }
```

### **Validaciones de Entrada**

```bash
# 1. Campos requeridos faltantes
echo "=== Error: Campos faltantes ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": ""}'
# Respuesta: 400 Bad Request con detalles de validación
# {
#   "error": "Validation Failed",
#   "details": [
#     "nationalId: no debe estar vacío",
#     "nombreCliente: no debe estar vacío"
#   ]
# }

# 2. Email inválido
echo "\n=== Error: Email inválido ==="
curl -X POST http://localhost:8080/api/advisors -H "Content-Type: application/json" -d '{"nombre": "Test", "email": "email-invalido"}'
# Respuesta: 400 Bad Request
# {
#   "error": "Validation Failed",
#   "details": ["email: debe ser una dirección de correo electrónico válida"]
# }
```

---

## 🔍 Base de Datos H2

- **URL:** http://localhost:8080/h2-console
- **JDBC URL:** jdbc:h2:mem:ticketero
- **Usuario:** sa
- **Contraseña:** (vacía)

### **Consultas Útiles**

```sql
-- Ver todos los tickets
SELECT * FROM ticket ORDER BY created_at;

-- Ver asesores y su estado
SELECT name, status, module_number, assigned_tickets_count FROM advisor;

-- Ver mensajes programados
SELECT t.numero, m.plantilla, m.estado_envio, m.fecha_programada 
FROM mensaje m 
JOIN ticket t ON m.ticket_id = t.id 
ORDER BY m.fecha_programada;

-- Estadísticas de cola
SELECT status, COUNT(*) as cantidad 
FROM ticket 
GROUP BY status;
```

---

## 🤖 Automatización del Sistema

### **Schedulers Activos**

1. **MessageScheduler** (cada 60 segundos)
   - Procesa mensajes pendientes de Telegram
   - Reintenta envíos fallidos (máximo 3 intentos)
   - Log: `🔄 Processing pending messages...`

2. **QueueProcessorScheduler** (cada 5 segundos)
   - Asigna automáticamente tickets a asesores disponibles
   - Envía notificación "Es tu turno" al cliente
   - Log: `🎯 Processing queue for automatic assignment...`

### **Flujo de Notificaciones Telegram 📱**

1. **Ticket Creado** → Mensaje programado (+5 segundos)
   ```
   🎫 Ticket Creado
   
   Código: TK-A1B2C3D4
   Número: C001
   Cliente: Ana Rodríguez
   Posición en cola: 1
   Tiempo estimado: 5 minutos
   
   ⏰ Te notificaremos cuando sea tu turno.
   ```

2. **Ticket Asignado** → Mensaje "Es tu turno" (+2 segundos)
   ```
   🔔 ¡ES TU TURNO!
   
   Número: C001
   Cliente: Ana Rodríguez
   Módulo: 1
   Asesor: María González
   
   🏃‍♀️ Dirígete al módulo ahora.
   ```

3. **Próximo Turno** → Mensaje de preparación
   ```
   ⏰ ERES EL PRÓXIMO
   
   Número: C002
   Cliente: Pedro Martínez
   Posición: 1
   
   💼 Prepárate, serás llamado pronto.
   ```

4. **MessageScheduler** → Procesa y envía mensajes REALES vía Telegram API

---

## 📊 Estados del Sistema

### **Estados de Tickets**
- `EN_ESPERA`: Esperando asignación
- `PROXIMO`: Próximo a ser atendido (posición ≤ 3)
- `ATENDIENDO`: Siendo atendido por un asesor
- `COMPLETADO`: Atención finalizada
- `CANCELADO`: Cancelado por cliente o sistema
- `NO_ATENDIDO`: Cliente no se presentó

### **Estados de Asesores**
- `AVAILABLE`: Disponible para atender
- `BUSY`: Atendiendo un cliente
- `OFFLINE`: No disponible

### **Estados de Mensajes**
- `PENDIENTE`: Esperando ser enviado
- `ENVIADO`: Enviado exitosamente
- `FALLIDO`: Falló el envío (después de 3 intentos)

---

## 🎯 Características Implementadas

✅ **RF-001**: Creación de tickets con código único  
✅ **RF-002**: Consulta de tickets por código  
✅ **RF-003**: Gestión de cola automática  
✅ **RF-004**: Asignación automática a asesores  
✅ **RF-005**: Notificaciones programadas vía Telegram  
✅ **RF-006**: Estado de cola en tiempo real  
✅ **RF-007**: Gestión de asesores  
✅ **RF-008**: Completar atención de tickets  

---

## 🧪 Pruebas Completas del Sistema

### **Test 1: Verificación de Telegram**
```bash
# Probar que Telegram funciona
curl -X POST "http://localhost:8080/api/telegram/test?message=Sistema funcionando correctamente"
# Resultado esperado: Mensaje en tu Telegram
```

### **Test 2: Flujo Completo End-to-End**
```bash
#!/bin/bash
echo "🚀 INICIANDO PRUEBA COMPLETA DEL SISTEMA"

# 1. Health Check
echo "\n1. 🔍 Health Check"
curl -s http://localhost:8080/actuator/health | jq .status

# 2. Verificar asesores
echo "\n2. 👥 Asesores disponibles"
curl -s http://localhost:8080/api/advisors | jq 'length'

# 3. Crear ticket con notificación
echo "\n3. 🎫 Creando ticket..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678", "nombreCliente": "Test User", "telefono": "+56912345678"}')
TICKET_ID=$(echo $RESPONSE | jq -r '.id')
CODIGO=$(echo $RESPONSE | jq -r '.codigoReferencia')
echo "Ticket creado: $CODIGO (ID: $TICKET_ID)"
echo "📱 Revisa tu Telegram - deberías recibir notificación"

# 4. Esperar y asignar
echo "\n4. ⏰ Esperando 10 segundos para asignación automática..."
sleep 10

# 5. Verificar asignación
echo "\n5. 🔄 Verificando asignación"
curl -s http://localhost:8080/api/tickets/$CODIGO | jq '.status, .assignedAdvisorName, .assignedModuleNumber'
echo "📱 Si fue asignado, deberías recibir notificación 'Es tu turno'"

# 6. Completar ticket
echo "\n6. ✅ Completando ticket"
curl -s -X POST http://localhost:8080/api/advisors/complete/$TICKET_ID

# 7. Estado final
echo "\n7. 📊 Estado final"
curl -s http://localhost:8080/api/queue/status | jq '.totalTicketsInQueue, .availableAdvisors'

echo "\n✅ PRUEBA COMPLETA FINALIZADA"
```

### **Test 3: Carga de Múltiples Tickets**
```bash
# Crear múltiples tickets para probar la cola
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d "{\"nationalId\": \"1234567$i\", \"nombreCliente\": \"Cliente $i\", \"telefono\": \"+5691234567$i\"}"
  echo "\nTicket $i creado - 📱 Notificación enviada"
  sleep 1
done

echo "\n📊 Estado de la cola:"
curl -s http://localhost:8080/api/queue/status | jq .
```

---

## 🚀 Próximos Pasos

### **✅ Completado**
- ✅ Telegram Bot configurado y funcionando
- ✅ Notificaciones reales vía Telegram
- ✅ Sistema de cola automático
- ✅ API REST completa
- ✅ Base de datos H2 funcional

### **🔄 Mejoras Futuras**
1. **UI Web**
   - Dashboard para asesores
   - Pantalla de cola para clientes
   - Métricas en tiempo real

2. **Escalabilidad**
   - Migrar a PostgreSQL
   - Implementar Redis para cache
   - Microservicios con Spring Cloud

3. **Funcionalidades Avanzadas**
   - Webhook de Telegram para respuestas
   - Notificaciones por SMS
   - Integración con sistemas externos