# 🎫 Sistema Ticketero - Guía Paso a Paso

Guía completa para probar el Sistema Ticketero con notificaciones automáticas vía Telegram.

## 📋 Prerrequisitos

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

## 🌐 Demo Web Interface

### Interfaz Web para Generar Tickets
```
URL: file:///[ruta-proyecto]/demo-ticketero-web/index.html
```

**Características:**
- ✅ Formulario intuitivo con validaciones
- ✅ Integración directa con API REST
- ✅ Pop-ups de éxito y error
- ✅ Diseño responsive y moderno
- ✅ Manejo de errores detallado

**Campos del formulario:**
- **RUT/ID Nacional**: Identificación del cliente (requerido)
- **Nombre Completo**: Nombre del cliente (requerido)
- **Teléfono**: Para notificaciones Telegram (opcional)
- **Tipo de Atención**: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA (requerido) ⭐ **NUEVO**

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

### **1. Tickets**

#### Crear Ticket ⭐ **ACTUALIZADO**
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "nombreCliente": "Ana Rodríguez",
    "telefono": "+56912345678",
    "queueType": "CAJA"
  }'
```

> **🔄 CAMBIO IMPORTANTE:** El campo `queueType` ahora es **OBLIGATORIO**. 
> Valores válidos: `CAJA`, `PERSONAL_BANKER`, `EMPRESAS`, `GERENCIA`
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "codigoReferencia": "TK-A1B2C3D4",
  "numero": "C001",
  "nationalId": "12345678-9",
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
  "nationalId": "12345678-9",
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

---

### **2. Asesores (Advisors)**

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

#### Asignar Próximo Ticket (Manual)
```bash
curl -X POST http://localhost:8080/api/advisors/assign-next
```

**📱 NOTIFICACIÓN AUTOMÁTICA:** Se envía mensaje "Es tu turno" al cliente vía Telegram.

#### Completar Atención
```bash
curl -X POST http://localhost:8080/api/advisors/complete/1
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

### **Escenario 1: Prueba con Interfaz Web**

```bash
# PASO 1: Abrir la interfaz web
# Navegar a: file:///[ruta-proyecto]/demo-ticketero-web/index.html

# PASO 2: Completar formulario
# - RUT/ID: 12345678-9
# - Nombre: Ana Rodríguez
# - Teléfono: +56912345678 (opcional)
# - Tipo de Atención: CAJA (OBLIGATORIO) ⭐

# PASO 3: Hacer clic en "Generar Ticket"
# - Si es exitoso: Pop-up con detalles del ticket
# - Si hay error: Pop-up con mensaje de error detallado

# PASO 4: Verificar notificación Telegram (si se proporcionó teléfono)
# 📱 Recibirás mensaje de confirmación en 5 segundos
```

### **Escenario 2: Prueba de Telegram**

```bash
# PASO 1: Probar conexión con Telegram
echo "=== PASO 1: Probar Telegram ==="
curl -X POST "http://localhost:8080/api/telegram/test?message=Sistema iniciado correctamente"
echo "\n📱 Deberías recibir un mensaje en Telegram"
```

### **Escenario 3: Flujo Básico con Notificaciones Reales**

```bash
# PASO 1: Verificar asesores iniciales (5 pre-cargados)
echo "=== PASO 1: Verificar asesores ==="
curl -s http://localhost:8080/api/advisors | jq .

# PASO 2: Crear tickets (con notificaciones REALES vía Telegram)
echo "\n=== PASO 2: Crear tickets ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678-9", "nombreCliente": "Ana Rodríguez", "telefono": "+56912345678", "queueType": "CAJA"}'
echo "\n📱 NOTIFICACIÓN REAL: Mensaje programado para Telegram (5 segundos)"

sleep 2

curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "87654321-0", "nombreCliente": "Pedro Martínez", "telefono": "+56987654321", "queueType": "PERSONAL_BANKER"}'
echo "\n📱 NOTIFICACIÓN REAL: Mensaje programado para Telegram (5 segundos)"

sleep 2

curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "11223344-5", "nombreCliente": "Laura Sánchez", "telefono": "+56911223344", "queueType": "EMPRESAS"}'
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

### **Escenario 4: Consulta de Tickets**

```bash
# Crear un ticket y consultar por código
RESPONSE=$(curl -s -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "99887766-7", "nombreCliente": "Carlos Silva", "queueType": "GERENCIA"}')
CODIGO=$(echo $RESPONSE | jq -r '.codigoReferencia')
echo "Ticket creado: $CODIGO"
echo "📱 Recibirás notificación de confirmación en Telegram (si proporcionaste teléfono)"

# Consultar el ticket
curl -s http://localhost:8080/api/tickets/$CODIGO | jq .
```

### **Escenario 5: Flujo Manual de Asignación**

```bash
# PASO 1: Crear ticket
echo "=== PASO 1: Crear ticket ==="
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "55443322-1", "nombreCliente": "María Fernández", "telefono": "+56955443322", "queueType": "CAJA"}'

# PASO 2: Asignar manualmente
echo "\n=== PASO 2: Asignar manualmente ==="
curl -X POST http://localhost:8080/api/advisors/assign-next

# PASO 3: Verificar asignación
echo "\n=== PASO 3: Verificar asignación ==="
curl -s http://localhost:8080/api/advisors | jq .
```

---

## 🎯 Casos de Prueba Específicos

### **Caso 1: Error - Cliente ya tiene ticket activo**
```bash
# Crear primer ticket
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678-9", "nombreCliente": "Ana Rodríguez", "queueType": "CAJA"}'

# Intentar crear segundo ticket con mismo RUT (debería fallar)
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678-9", "nombreCliente": "Ana Rodríguez", "queueType": "PERSONAL_BANKER"}'

# Respuesta esperada: HTTP 409 Conflict
```

### **Caso 2: Validación de datos** ⭐ **ACTUALIZADO**
```bash
# Datos inválidos (sin RUT)
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nombreCliente": "Ana Rodríguez", "queueType": "CAJA"}'

# Datos inválidos (sin queueType - NUEVO REQUERIMIENTO)
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678-9", "nombreCliente": "Ana Rodríguez"}'

# Datos inválidos (queueType inválido)
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "12345678-9", "nombreCliente": "Ana Rodríguez", "queueType": "INVALIDO"}'

# Respuesta esperada: HTTP 400 Bad Request
```

### **Caso 3: Ticket sin teléfono (sin notificaciones)**
```bash
# Crear ticket sin teléfono
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d '{"nationalId": "99887766-7", "nombreCliente": "Carlos Silva", "queueType": "GERENCIA"}'

# No se enviarán notificaciones Telegram
```

---

## 📊 Estados del Sistema

### **Estados de Ticket**
- `EN_ESPERA` - Esperando asignación
- `PROXIMO` - Próximo a ser atendido (posición <= 3)
- `ATENDIENDO` - Siendo atendido por un asesor
- `COMPLETADO` - Atención finalizada
- `CANCELADO` - Cancelado
- `NO_ATENDIDO` - Cliente no se presentó

### **Estados de Asesor**
- `AVAILABLE` - Disponible para atender
- `BUSY` - Atendiendo un cliente
- `OFFLINE` - No disponible

### **Tipos de Cola**
- `CAJA` - Transacciones básicas (5 min promedio)
- `PERSONAL_BANKER` - Productos financieros (15 min promedio)
- `EMPRESAS` - Clientes corporativos (20 min promedio)
- `GERENCIA` - Casos especiales (30 min promedio)

---

## 🔧 Troubleshooting

### **Problema: Error de conexión en interfaz web**
```
Error: No se pudo conectar con el servidor
Solución: Verificar que la aplicación esté ejecutándose en http://localhost:8080
```

### **Problema: No llegan notificaciones Telegram**
```
Verificar:
1. Token del bot configurado correctamente
2. Chat ID válido
3. Campo telefono proporcionado en el ticket
4. Revisar logs de la aplicación
```

### **Problema: H2 Console no carga**
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:ticketero
Usuario: sa
Contraseña: (vacía)
```

### **Problema: Tickets no se asignan automáticamente**
```
Verificar:
1. Hay asesores con status AVAILABLE
2. QueueProcessorScheduler está ejecutándose (cada 5s)
3. Revisar logs para errores
```

---

## 📁 Estructura de Archivos

```
proyectoTicket/
├── demo-ticketero-web/
│   └── index.html              # Interfaz web para generar tickets
├── src/main/java/
│   └── com/example/ticketero/
│       ├── controller/         # Controladores REST
│       ├── service/           # Lógica de negocio
│       ├── repository/        # Acceso a datos
│       ├── model/            # Entidades y DTOs
│       └── scheduler/        # Tareas programadas
├── docs/                     # Documentación
├── PASO-A-PASO.md           # Esta guía
└── README.md                # Información general
```

---

## 🎉 ¡Listo para Probar!

1. **Ejecutar aplicación**: `mvnw.cmd spring-boot:run`
2. **Abrir interfaz web**: `demo-ticketero-web/index.html`
3. **Generar tickets** usando el formulario web
4. **Verificar notificaciones** en Telegram
5. **Monitorear estado** con las APIs REST

**¡El sistema está completamente funcional y listo para demostración!**

---

**Versión:** 1.0  
**Última actualización:** Diciembre 2024  
**Estado:** ✅ COMPLETADO