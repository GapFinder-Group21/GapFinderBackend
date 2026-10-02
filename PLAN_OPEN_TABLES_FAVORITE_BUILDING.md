# Plan: "Open tables en tu edificio favorito" (Flutter / Dart)

Pantalla (o sección) que muestra:

```
Edificio favorito calculado: Biblioteca
Has pasado 3 h 20 min aquí

Open tables recomendadas
 ┌─────────────────────────────────────┐
 │ Repaso de Cálculo II                │
 │ 10:00 – 11:30 · Máx. 6 personas     │
 └─────────────────────────────────────┘
 ┌─────────────────────────────────────┐
 │ Café y conversación en inglés       │
 │ 11:00 – 12:00 · Máx. 4 personas     │
 └─────────────────────────────────────┘
```

> Este documento es planeación del front. En la rama `weekly-gaps` (creada desde `origin/CLASS-BLOCKS`) ya se resolvieron dos problemas del backend: el `createdAt` de open tables (8.1) y el cálculo automático de gaps (8.3).
> Complementa a `PLAN_NEARBY_FRIENDS.md` (reutiliza el mismo `ApiClient`, sesión y manejo de errores).

---

## 1. Dónde está el backend

Se revisaron **todas** las ramas. La funcionalidad existe en 7 ramas remotas:

| Rama | Ruta del endpoint | Tope por intervalo |
|---|---|---|
| `origin/SMART-FEATURE`, `origin/CONTEXT-AWARE-SYSTEM`, `origin/GRUPAL-BQ-TYPE-2` | `GET /recommendations/open-tables/{userId}` (**vieja**) | 30 min |
| `origin/LOCATION-SENSOR-FUNCTIONS` | `GET /recommendations/open-tables/{userId}` (**vieja**) | 15 min |
| `origin/todos-los-controllers`, `origin/PATRON-OBSERVER`, `origin/CLASS-BLOCKS` | `GET /recommendations/user/{userId}/open-tables` (**actual**) | 15 min |

**Usar la ruta actual:** `GET /recommendations/user/{userId}/open-tables`.
No está en `main` ni en `googleImports`.

Archivos clave (en `origin/CLASS-BLOCKS`):

| Archivo | Qué hace |
|---|---|
| `controllers/RecommendationController.java` | Expone el endpoint |
| `services/RecommendationService.java` | Calcula el favorito y busca las open tables |
| `repositories/UserLocationLogRepository.java` → `findFavoriteBuilding` | SQL que calcula el edificio favorito |
| `repositories/OpenTableRepository.java` → `findMatchingOpenTables` | Filtra las open tables que te sirven |
| `dto/responses/OpenTableRecommendationResponseDTO.java` | Forma de la respuesta |

---

## 2. Cómo calcula el backend el edificio favorito

`findFavoriteBuilding(userId)` usa **tu historial de ubicaciones** (`user_location_log`):

1. Ordena tus registros de ubicación por fecha.
2. Para cada registro, calcula los minutos hasta tu **siguiente** registro, con un **tope de 15 min** (así, si cierras la app una noche, no suma 10 horas).
3. Suma esos minutos por edificio.
4. El edificio con más minutos es el **favorito**. Esos minutos son `totalMinutes`.

Consecuencias importantes para el front:

- **Los registros de ubicación solo se crean desde Nearby Friends** (`PUT /nearby-friends/user/{id}/location` o `/building/{buildingId}`). Si el usuario nunca usa esa pantalla, **no tiene edificio favorito**. Las dos funcionalidades están conectadas.
- Hacen falta **al menos 2 registros**: el último registro no suma minutos porque no tiene "siguiente".
- Considera **todo el historial**, no solo la última semana.

## 3. Qué open tables recomienda

`findMatchingOpenTables` devuelve las open tables que cumplen **todo** esto:

- Están en el **edificio favorito**.
- `status = OPEN` (no `FULL`, `COMPLETED` ni `EMPTY`).
- Todavía no han terminado (`endTime > ahora`).
- Se **cruzan en horario con algún gap tuyo que no haya terminado** (`gap.endTime > ahora` y los horarios se solapan).
- Ordenadas por `startTime`.

Consecuencia importante: **si el usuario no tiene gaps futuros o en curso, la lista sale vacía** aunque haya open tables en el edificio.
En la rama `weekly-gaps` los gaps se calculan solos a partir del horario (`class-blocks`) cada domingo, así que el usuario solo necesita tener su horario cargado (ver 8.3).

---

## 4. El endpoint

```
GET /recommendations/user/{userId}/open-tables
Authorization: Bearer <accessToken>
```

### Respuesta 200 con favorito

```json
{
  "favoriteBuildingId": 1,
  "favoriteBuildingName": "Biblioteca",
  "totalMinutes": 200.0,
  "openTables": [
    {
      "id": 12,
      "title": "Repaso de Cálculo II",
      "description": "Traer ejercicios del parcial",
      "startTime": "2026-09-28T10:00:00",
      "endTime": "2026-09-28T11:30:00",
      "maxParticipants": 6,
      "status": "OPEN"
    }
  ]
}
```

### Respuesta 200 sin historial de ubicación

```json
{
  "favoriteBuildingId": null,
  "favoriteBuildingName": null,
  "totalMinutes": 0.0,
  "openTables": []
}
```

### Qué **no** trae cada open table

`OpenTableBasicDTO` no incluye: creador, edificio (se sobreentiende que es el favorito), actividad, ni cuántas personas se han unido. Si la tarjeta necesita "3/6 personas" o "Creada por Laura", eso requiere cambio en backend (ver sección 8).

### Errores

Igual que en Nearby Friends: texto plano (`404` si el usuario no existe, `500` error interno). Leer `response.body` como `String`.

---

## 5. Flujo de la pantalla

```
Abrir pantalla
   │
   ├─ GET /recommendations/user/{userId}/open-tables
   │
   ├─ favoriteBuildingId == null ──► Estado "Aún no tenemos tu edificio favorito"
   │                                 (CTA: ir a Nearby Friends / activar ubicación)
   │
   ├─ openTables vacío          ──► Header con el edificio favorito
   │                                 + "No hay open tables que coincidan con tus huecos libres"
   │                                 (CTA: registrar mis huecos / crear una open table)
   │
   └─ openTables con datos      ──► Header + lista de tarjetas

Refrescar:
   - Pull-to-refresh
   - Al volver a la pantalla (onResume / didPopNext)
   - No hace falta polling: el favorito cambia lento. Opcional: cada 2–5 min si la pantalla queda abierta
```

Tocar una tarjeta → detalle de la open table (`GET /open-tables/{id}`) → botón "Unirme" (ver sección 8, hoy tiene un bug).

---

## 6. Estructura sugerida en Dart

```
lib/
  features/recommended_open_tables/
    data/
      models/
        open_table.dart                      // id, title, description?, startTime, endTime, maxParticipants, status
        open_table_recommendation.dart       // favoriteBuildingId?, favoriteBuildingName?, totalMinutes, openTables
      recommendations_api.dart
      recommendations_repository.dart
    presentation/
      recommended_open_tables_controller.dart
      recommended_open_tables_screen.dart
      widgets/
        favorite_building_header.dart
        open_table_card.dart
        empty_states.dart
  core/                                       // compartido con Nearby Friends
    network/api_client.dart
    session/session_store.dart
```

### Modelos

```dart
enum OpenTableStatus { open, full, completed, empty }

class OpenTable {
  final int id;
  final String title;
  final String? description;
  final DateTime startTime;
  final DateTime endTime;
  final int maxParticipants;
  final OpenTableStatus status;

  factory OpenTable.fromJson(Map<String, dynamic> j) => OpenTable(
        id: j['id'],
        title: j['title'],
        description: j['description'],
        startTime: DateTime.parse(j['startTime']),
        endTime: DateTime.parse(j['endTime']),
        maxParticipants: j['maxParticipants'],
        status: OpenTableStatus.values.byName((j['status'] as String).toLowerCase()),
      );
}

class OpenTableRecommendation {
  final int? favoriteBuildingId;
  final String? favoriteBuildingName;
  final double totalMinutes;
  final List<OpenTable> openTables;

  bool get hasFavorite => favoriteBuildingId != null;

  factory OpenTableRecommendation.fromJson(Map<String, dynamic> j) => OpenTableRecommendation(
        favoriteBuildingId: j['favoriteBuildingId'],
        favoriteBuildingName: j['favoriteBuildingName'],
        totalMinutes: (j['totalMinutes'] as num?)?.toDouble() ?? 0,
        openTables: ((j['openTables'] as List?) ?? [])
            .map((e) => OpenTable.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}
```

Notas de parsing:
- `totalMinutes` viene como `200.0` o `200`: usar `as num` y `.toDouble()`.
- `favoriteBuildingId` puede ser `null`.
- Las fechas vienen **sin zona horaria** (`LocalDateTime`); `DateTime.parse` las toma como hora local.

### API

```dart
class RecommendationsApi {
  RecommendationsApi(this._client);
  final ApiClient _client;

  Future<OpenTableRecommendation> getRecommendedOpenTables(int userId) async {
    final json = await _client.get('/recommendations/user/$userId/open-tables');
    return OpenTableRecommendation.fromJson(json);
  }
}
```

### Estado

```dart
sealed class RecommendedTablesState {}
class RecLoading       extends RecommendedTablesState {}
class RecNoFavorite    extends RecommendedTablesState {}   // favoriteBuildingId == null
class RecLoaded        extends RecommendedTablesState {
  final String buildingName;
  final double totalMinutes;
  final List<OpenTable> tables;                            // puede estar vacía
}
class RecError         extends RecommendedTablesState { final String message; }
```

---

## 7. UI

### Header (`FavoriteBuildingHeader`)

- Etiqueta pequeña: **"Edificio favorito calculado"**.
- Título grande: `favoriteBuildingName`.
- Subtítulo: "Has pasado **3 h 20 min** aquí" (formatear `totalMinutes`: `< 60` → "45 min"; si no → "3 h 20 min").
- Ícono de info (opcional) con un tooltip: "Lo calculamos con el tiempo que has pasado en cada edificio del campus".

### Lista (`OpenTableCard`)

- Título.
- Horario: "10:00 – 11:30" (si no es hoy, agregar el día: "Mañana 10:00").
- Chip "En curso" si `startTime <= ahora < endTime`.
- "Máx. 6 personas".
- Descripción en 1–2 líneas (si existe).
- Tap → detalle.

### Estados vacíos

| Estado | Texto | Acción |
|---|---|---|
| Sin favorito | "Todavía no sabemos cuál es tu edificio favorito. Usa GapFinder en el campus y lo calcularemos." | Ir a Nearby Friends |
| Favorito sin tablas | "No hay open tables en Biblioteca que coincidan con tus huecos libres" | Ver mis huecos / Crear open table |
| Error | Mensaje del backend o "No pudimos cargar las recomendaciones" | Reintentar |

**Loading:** skeleton del header + 2–3 tarjetas.

---

## 8. Problemas encontrados en el backend (revisar antes de conectar)

1. ✅ **Resuelto en `weekly-gaps`: el backend no arrancaba en las 3 ramas más recientes.**
   `OpenTableRepository` tiene `countByCreatedAtGreaterThanEqual(...)` (lo usa `GET /open-tables/count`), pero `OpenTableModel` no tenía campo `createdAt`, y Spring Data falla al iniciar por eso.
   En `weekly-gaps` se agregó `createdAt` a `OpenTableModel` y se llena al crear la open table.
   Sigue pasando en: `origin/CLASS-BLOCKS`, `origin/PATRON-OBSERVER`, `origin/todos-los-controllers` (hasta que se una `weekly-gaps`).

2. ⚠️ **Hoy no se puede unirse a una open table desde la API.**
   `POST /open-table-participants` recibe `OpenTableParticipantBasicDTO`, que solo tiene `id` y `joinedAt`. El servicio exige `openTable.id` y `user.id`, así que siempre responde `400 "Debe indicar la open table (openTable)"`.
   → Pedir un endpoint tipo `POST /open-tables/{id}/join` (usuario sacado del token) o un DTO de request con `openTableId` y `userId`. Hasta entonces, el botón "Unirme" queda deshabilitado o con "Próximamente".

   **Lo mismo pasa al crear una open table:** `POST /open-tables` recibe `OpenTableBasicDTO` (sin `creator` ni `building`) y el servicio exige ambos → siempre `400 "Debe indicar el usuario creador (creator)"`. Hoy las open tables solo se pueden crear directo en la base de datos.
   → Pedir que el request acepte `creatorId`/`buildingId` (o `OpenTableCompleteDTO` con ids).

3. ✅ **Resuelto en `weekly-gaps`: los gaps ahora se generan solos a partir del horario.**
   - **Cada domingo a las 20:00 (Bogotá)** se calculan los gaps de lunes a sábado de la semana siguiente para todos los usuarios con clases (`WeeklyGapJob`).
   - **Regla:** un gap es el tiempo libre entre el final de una clase y el inicio de la siguiente, el mismo día, si dura **30 min o más**. Antes de la primera clase y después de la última **no** es gap; un día sin clases no tiene gaps. Franja del día: 06:30–21:30.
   - **Bajo demanda:** `POST /gaps/user/{userId}/generate-week?weekStart=2026-09-28` (`weekStart` es un lunes y es opcional: por defecto la semana actual, o la siguiente si es domingo). Devuelve los gaps creados.
   - Recalcular una semana reemplaza sus gaps, pero conserva los que ya tienen un match.
   → **Para el front:** después de que el usuario cargue o importe su horario (`/class-blocks` o importación de Google), llamar `POST /gaps/user/{userId}/generate-week` para que tenga gaps esa misma semana sin esperar al domingo.

4. **La recomendación no excluye** las open tables que creó el propio usuario ni en las que ya se unió.
   → Opcional en el front: ocultar las que el usuario ya sabe que son suyas (no hay forma de saberlo con el DTO actual). Mejor pedirlo al backend.

5. **`openTables` es un `List` sin tipo** en el record Java. Funciona igual en JSON, pero si luego se cambia el DTO, revisar que el front siga parseando bien.

6. **Autenticación:** igual que Nearby Friends. Cuando se unan con `googleImports`, estas rutas exigirán `Authorization: Bearer <accessToken>`. Mandarlo desde ya.

---

## 9. Cómo probar de punta a punta (Postman)

Con el backend de la rama `weekly-gaps` levantado:

1. Tener un usuario `U` y dos edificios `A` y `B`.
2. Generar historial de ubicación de `U`, espaciado unos minutos entre llamadas:
   - `PUT /nearby-friends/user/U/building/A` (varias veces)
   - `PUT /nearby-friends/user/U/building/B` (una vez)
   → `A` debería ser el favorito.
   *(Los minutos se calculan con la hora real de cada llamada; si las haces seguidas, `totalMinutes` será casi 0, pero igual hay favorito.)*
3. Generar gaps de `U` desde su horario:
   - Cargar dos clases el mismo día con un hueco entre ellas, ej. `POST /class-blocks/user/U` → `{ "subject": "Cálculo", "dayOfWeek": "MON", "startTime": "08:00", "endTime": "10:00" }` y otra de `12:00` a `14:00`.
   - `POST /gaps/user/U/generate-week` → debe crear un gap de 10:00 a 12:00 ese lunes.
   - Alternativa rápida: `POST /gaps` → `{ "startTime": "<hoy 10:00>", "endTime": "<hoy 12:00>", "user": { "id": U } }`.
4. Crear una open table `OPEN` en `A` entre 10:30 y 11:30. Por el problema 8.2, hoy hay que insertarla **directo en la base de datos** (tabla `open_table`: `creator_id`, `building_id`, `title`, `start_time`, `end_time`, `max_participants`, `status = 'OPEN'`).
5. `GET /recommendations/user/U/open-tables` → debe devolver `A` y la open table.
6. Casos borde: usuario sin logs (sin favorito), sin gaps (lista vacía), open table `FULL` (no aparece), open table ya terminada (no aparece).

---

## 10. Checklist por fases

### Fase 0: Backend
- [ ] Usar la rama `weekly-gaps` (o esperar a que se una con `main`).
- [x] Resolver el `createdAt` (8.1) para que arranque.
- [x] Calcular gaps automáticamente desde el horario (8.3).
- [ ] En el front, llamar `POST /gaps/user/{userId}/generate-week` después de guardar o importar el horario.
- [ ] Probar el flujo de la sección 9 en Postman.
- [ ] Pedir que se pueda crear una open table y unirse a una desde la API (8.2).

### Fase 1: UI con datos simulados
- [ ] Modelos `OpenTable` y `OpenTableRecommendation` con `fromJson` + tests con los 2 JSON de la sección 4.
- [ ] `FavoriteBuildingHeader` con formato de minutos.
- [ ] `OpenTableCard` con horario y chip "En curso".
- [ ] Pantalla con los estados de la sección 6 usando un repositorio falso.

### Fase 2: Conectar
- [ ] `RecommendationsApi` usando el `ApiClient` compartido (Bearer + refresh en 401).
- [ ] Pull-to-refresh y recargar al volver a la pantalla.
- [ ] CTA "Ir a Nearby Friends" en el estado sin favorito.

### Fase 3: Detalle y unirse
- [ ] Pantalla de detalle con `GET /open-tables/{id}`.
- [ ] Botón "Unirme" (cuando exista el endpoint del punto 8.2).

### Fase 4: Pulido
- [ ] Skeleton loading.
- [ ] Tests de widget de cada estado.
- [ ] Textos finales y tooltip explicando el cálculo.

### Futuro
- Mostrar "3/6 personas" y el creador (requiere ampliar el DTO en backend).
- Recomendar también en el edificio **actual** (combinar con Nearby Friends).
- Notificación cuando se abre una open table en tu edificio favorito durante un gap.
