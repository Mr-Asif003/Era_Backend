# Era Backend

Spring Boot backend for the Era mobile app. This version focuses on exactly
two things, done properly: **authentication** and **real-time 1:1 chat**.
Pulse and the Era AI chatbot are intentionally out of scope for now (a few
harmless placeholder fields remain on the `User`/`Message` models so the
frontend types keep compiling, but there is no chatbot logic here).

## Stack (deliberately minimal)

| Concern              | Technology                                   |
|-----------------------|-----------------------------------------------|
| Language / runtime    | Java 17, Spring Boot 3.3                      |
| REST API              | Spring Web (MVC)                              |
| Real-time chat        | Spring WebSocket + STOMP over SockJS          |
| Auth                  | Spring Security + JWT (access + refresh)      |
| Database              | **MongoDB only**                              |
| Refresh tokens / presence | In-memory (no Redis needed)               |

The original project also wired up Kafka (for a notification stub that just
logged a line) and Redis (only used for refresh-token storage and online
presence). Neither was pulling its weight at this app's current scale, so
both were removed:

- **Kafka →** removed. The one thing it did (notify an offline user) is now
  a direct in-process call (`NotificationService`). Swap that class's
  internals for a real push provider (FCM, etc) when you're ready — no
  message broker required to get there.
- **Redis →** removed. Refresh-token rotation and online-presence tracking
  now live in two small in-memory `ConcurrentHashMap`-backed services
  (`RefreshTokenStore`, `PresenceService`). This is the right trade-off for
  a single backend instance. If you ever scale to multiple instances, swap
  those two classes for a shared store — nothing else in the codebase needs
  to change.

Net effect: **the only thing you need running locally is MongoDB.**

## Project layout

```
src/main/java/com/era/backend/
├── auth/            # register / login / refresh / logout, JWT issuing+validation
├── user/            # profile, search
├── conversation/    # direct & group conversations
├── message/         # sending, pagination, read receipts
├── websocket/        # STOMP config, JWT-on-CONNECT auth, presence
├── notification/     # push-notification placeholder (no-op logger for now)
├── config/           # Security, WebSocket, Mongo config
└── common/           # ApiResponse envelope, paging, global error handling
```

## Running it

### 1. Create a MongoDB Atlas cluster

1. In Atlas: **Database Access** → add a database user (username/password).
2. **Network Access** → add your current IP (or `0.0.0.0/0` while developing —
   just remember to lock it back down later).
3. **Database → Connect → Connect your application → Driver: Java** → copy
   the connection string. It looks like:
   ```
   mongodb+srv://<username>:<password>@<cluster-host>/eradb?retryWrites=true&w=majority
   ```
   URL-encode any special characters in the password (e.g. `@` → `%40`).

### 2. Configure the backend

```bash
cp .env.example .env
# then edit .env and paste in your real Atlas URI + a JWT secret
```

### 3. Run the backend

Requires Maven + JDK 17 installed locally (`mvn -v` to check).

Spring Boot doesn't auto-load `.env` files, so export the variables into
your shell first:

```bash
set -a; source .env; set +a
mvn spring-boot:run
```

(On Windows PowerShell: `Get-Content .env | ForEach-Object { if ($_ -match '^(.*?)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }`)

It starts on `http://localhost:8080`, with everything mounted under `/api`
(so the health check is `http://localhost:8080/api/health`) — this matches
the frontend's `API_BASE_URL` default of `http://localhost:8080/api/`.

Swagger UI: `http://localhost:8080/api/swagger-ui`

### Or run everything with Docker Compose

```bash
cp .env.example .env   # fill in your real Atlas URI + JWT secret
docker compose up -d
```

This builds and runs just the backend container (MongoDB is on Atlas now,
so there's nothing else to run locally) — it reads `.env` automatically.

### Environment variables

All of these go in `.env` (for Docker) or your shell (for `mvn spring-boot:run`):

| Variable                    | Example                                                        | Notes                                |
|------------------------------|-------------------------------------------------------------------|-----------------------------------------|
| `SPRING_DATA_MONGODB_URI`    | `mongodb+srv://user:pass@cluster.mongodb.net/eradb?retryWrites=true&w=majority` | Your Atlas connection string     |
| `JWT_SECRET`                 | output of `openssl rand -base64 32`                                | **Never reuse the placeholder value beyond local dev** |
| `JWT_ACCESS_EXPIRY`          | `900000` (15 min, ms)                                              | optional, has a default                 |
| `JWT_REFRESH_EXPIRY`         | `604800000` (7 days, ms)                                           | optional, has a default                 |

## Connecting the mobile app

The frontend's `.env` already points at the right place for local dev:

```
EXPO_PUBLIC_API_URL=http://localhost:8080/api
EXPO_PUBLIC_WS_URL=http://localhost:8080/api/ws
```

**Testing on a physical device / Expo Go:** `localhost` means the phone
itself, not your computer. Uncomment the LAN-IP lines in `.env` and put in
your machine's local IP (e.g. `192.168.x.x`), and make sure the phone is on
the same Wi-Fi network as the backend.

## API surface

All REST responses are wrapped as:
```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

### Auth (public)
| Method | Path                    | Body                                                          |
|--------|--------------------------|-----------------------------------------------------------------|
| POST   | `/auth/register`        | `{ username, email, password, displayName, number }`            |
| POST   | `/auth/login`            | `{ email, password }`                                            |
| POST   | `/auth/refresh-token`    | `{ refreshToken }`                                               |
| POST   | `/auth/logout`           | `{ refreshToken }`                                               |

All three of `register` / `login` / `refresh-token` return
`{ user, accessToken, refreshToken }`.

### Conversations (JWT required)
| Method | Path                        | Notes                                              |
|--------|------------------------------|-----------------------------------------------------|
| GET    | `/conversations`             | All conversations for the current user             |
| GET    | `/conversations/:id`          | One conversation (must be a member)                 |
| POST   | `/conversations/direct`       | `{ email }` — finds or creates a 1:1 conversation   |
| POST   | `/conversations/group`        | `{ name, memberIds }`                               |
| DELETE | `/conversations/:id`          | Deletes the conversation (and its messages)         |

Each conversation's `members[]` is enriched at read time with the other
user's live `fullName` / `avatarUrl` / `avatarColor` / `online` /
`lastSeen` — not just their raw id.

### Messages (JWT required)
| Method | Path                              | Notes                                    |
|--------|-------------------------------------|---------------------------------------------|
| GET    | `/conversations/:id/messages`        | `?page=0&size=30`, newest-first              |
| POST   | `/messages`                          | `{ conversationId, text, type?, replyToId? }`|
| PUT    | `/messages/:id/read`                 | Marks read, notifies sender                  |
| DELETE | `/messages/:id`                      | Soft delete (sender only)                    |

### Real-time (STOMP over SockJS at `/ws`)

Connect with `Authorization: Bearer <accessToken>` as a STOMP CONNECT
header. Once connected, subscribe to:

- `/user/queue/messages` — new messages in any of your conversations
- `/user/queue/delivery` — read-receipt updates
- `/user/queue/typing` — typing indicators
- `/topic/presence` — broadcast online/offline events

Sending a message is done over plain REST (`POST /messages`); the backend
fans it out over these queues to the other participant(s) in real time.

## What's deliberately not here yet

- **Pulse** and the **Era AI chatbot** — out of scope for this pass, per the
  brief. A `MessageType.ERA` enum value and a couple of `User` fields
  (`eraVoice`, `eraLanguage`) are kept only because the frontend's TypeScript
  types already reference them; there's no chatbot logic behind them.
- **Contacts sync** (`POST /contacts/sync`) — the frontend has this wired up
  in `contacts.api.ts`, but the screen that would call it isn't linked into
  navigation yet, so it wasn't implemented. Straightforward to add later:
  hash/normalize phone numbers, look up matching `User.number`s, return the
  map.
- **Real email verification** — accounts are marked verified on
  registration since there's no email provider wired up. `User.isVerified`
  and the `/auth/verify` contract are there for whenever that's added.
