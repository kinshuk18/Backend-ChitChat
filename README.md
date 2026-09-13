# ChitChat — Secure Real-time Messaging Backend

A high-performance, secure real-time messaging backend built with **Spring Boot 3**, **WebSockets**, **PostgreSQL**, and cryptographic guarantees (AES-GCM encryption & Ed25519 digital signatures).

---

## ✨ Features

- **🔐 Cryptographic Security at Rest**:
  - **AES-GCM (256-bit)** message content encryption with unique 96-bit random nonces.
  - **Ed25519 Digital Signatures**: Every message is digitally signed with the sender's private key and verified with their public key before decryption.
  - **Key Encryption Key (KEK)**: User private signing keys are encrypted at rest with AES-GCM before storage.
- **🛡️ Authentication & Authorization**:
  - **BCrypt Password Hashing**: Passwords stored securely with `@JsonIgnore` protection.
  - **Stateless JWT Authentication**: Access and refresh tokens for REST API and WebSocket handshakes.
  - **Object-Level Authorization**: Enforces strict room membership checks to prevent Broken Object Level Authorization (BOLA).
- **⚡ Real-Time WebSocket Infrastructure**:
  - Text-based duplex communication for instant message dispatch and room broadcasting.
  - Granular delivery and read receipt status updates (`SENT` ➔ `DELIVERED` ➔ `READ`).
  - Automatic session cleanup on connection disconnect.
- **💾 Full Chat History Persistence**:
  - Seamless pagination and message history retrieval with instant decryption upon membership verification.

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3
- **Language**: Java 17+
- **Database**: PostgreSQL / Spring Data JPA & Hibernate
- **Security**: Spring Security 6, Nimbus JOSE + JWT, BCrypt
- **Cryptography**: AES-GCM (256-bit), Ed25519 (RFC 8032)
- **Protocol**: Raw WebSockets (`org.springframework.web.socket`)
- **Build Tool**: Maven

---

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL running locally or via Docker

### 1. Configure Environment

Copy the example configuration:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

### 2. Build & Test

```bash
./mvnw clean test
```

### 3. Run Application

```bash
./mvnw spring-boot:run
```

The backend starts at `http://localhost:8080`.

---

## 📡 API Endpoints

### Authentication & Users
- `POST /user/create` — Register new user & generate Ed25519 keypair.
- `POST /auth/login` — Authenticate and receive JWT tokens.
- `POST /auth/refresh` — Refresh access token using refresh token.

### Rooms
- `POST /room/create` — Create a new room with participants.
- `GET /room/all` — List all joined rooms with members.
- `POST /room/join/{roomId}` — Join an existing room.
- `DELETE /room/leave/{roomId}` — Leave a room.

### Messages & Receipts
- `GET /rooms/{roomId}/messages/recent` — Fetch and decrypt room message history.
- `GET /rooms/{messageId}` — Fetch delivery/read receipts for a message.

### WebSocket
- `ws://localhost:8080/ws?token=<JWT_ACCESS_TOKEN>`
  - Types: `JOIN_ROOM`, `SEND_MESSAGE`, `MESSAGE_DELIVERED`, `MESSAGE_READ`.

---

## 🧪 Testing

Run the automated cryptographic and integration test suite:
```bash
./mvnw test -Dtest=MessageCryptoServiceTest
```
