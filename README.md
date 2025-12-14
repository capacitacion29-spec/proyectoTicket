# Sistema Ticketero

Sistema de gestión de tickets con notificaciones en tiempo real vía Telegram.

## Características

- ✅ Creación de tickets con código único
- ✅ Gestión de cola automática
- ✅ Asignación automática a asesores disponibles
- ✅ Notificaciones en tiempo real vía Telegram
- ✅ API REST completa
- ✅ Base de datos H2 para desarrollo

## Endpoints Principales

### Tickets
- `POST /api/tickets` - Crear nuevo ticket
- `GET /api/tickets/{codigo}` - Consultar ticket por código

### Asesores
- `GET /api/advisors` - Listar asesores
- `POST /api/advisors/assign-next` - Asignar próximo ticket
- `POST /api/advisors/complete/{ticketId}` - Completar atención

### Cola
- `GET /api/queue/status` - Estado actual de la cola

## Configuración Telegram

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    username: ${TELEGRAM_BOT_USERNAME}
    chat-id: ${TELEGRAM_CHAT_ID}
```

## Ejecutar

```bash
mvn spring-boot:run
```

## Base de Datos H2

- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:ticketero
- Usuario: sa
- Contraseña: (vacía)

## Estados de Ticket

- `EN_ESPERA` - Esperando asignación
- `PROXIMO` - Próximo a ser atendido (posición <= 3)
- `ATENDIENDO` - Siendo atendido por un asesor
- `COMPLETADO` - Atención finalizada
- `CANCELADO` - Cancelado
- `NO_ATENDIDO` - Cliente no se presentó

## Estados de Asesor

- `AVAILABLE` - Disponible para atender
- `BUSY` - Atendiendo un cliente
- `OFFLINE` - No disponible