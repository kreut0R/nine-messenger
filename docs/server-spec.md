# Nine server: specification for v1

Status: draft for the first Go server. The desktop client already has accounts and the encrypted vault; the
network part of the client is not written yet and will follow this document. If something here is wrong or
impractical, change the document first and tell the client side, so both stay in step.

The words MUST, SHOULD and MAY are meant in the usual way: MUST is required, SHOULD is a strong default, MAY is optional.

## 1. What the server is

A small relay. It lets two people who know each other's account ID exchange encrypted messages even when
one of them is offline. It cannot read the messages, does not know names or contact lists, and keeps as
little as possible.

The messages are encrypted on the clients with the Signal protocol (X3DH key agreement, then the Double Ratchet).
The server never runs any Signal code. It stores and forwards opaque bytes, so it needs no libsignal.

### In scope for v1

- Anonymous accounts: no phone number, no email, no password. An account is an Ed25519 public key.
- Invite-only registration (open and closed modes exist for testing).
- Storing Signal key bundles (identity key, signed prekey, one-time prekeys) as opaque blobs.
- Storing and delivering encrypted messages: an offline queue, live delivery, acknowledgements.
- Rate limits, size limits, retention limits.
- A single-binary deployment with one config file.

### Out of scope for v1 (planned later, do not build yet)

Sealed sender, group chats, attachments, push notifications, several devices per account, contact discovery,
federation between servers, voice and video, bots, an admin web UI.

### Trust model

- The server is untrusted for content: with the whole database in hand, an attacker gets ciphertext and the
  metadata listed in section 11, nothing else.
- The server is only "honest but curious" for metadata in v1: it sees who writes to whom and when. Section 11 says so openly.
- Clients do not trust the server for identity: they compare account IDs and safety numbers themselves.

## 2. Conventions

- Language Go (1.24 or newer), module `github.com/kreut0R/nine-messenger/server`, directory `server/` in this repository,
  license AGPL-3.0-or-later.
- Transport: HTTPS with TLS 1.3 only, plus a WebSocket on the same host. Plain HTTP is allowed only on a loopback
  address (for tests, or behind a Tor onion service or a reverse proxy that terminates TLS).
- Encoding: JSON in UTF-8, `Content-Type: application/json`. Unknown fields in requests MUST be ignored.
- Binary values (keys, signatures, ciphertext, tokens) are base64url without padding (RFC 4648 section 5), in JSON strings.
- Account ID: the lowercase hex SHA-256 of the raw 32-byte Ed25519 public key, 64 characters. The client already
  computes it exactly like this.
- Times: integer milliseconds since 1970-01-01 UTC. The server clock is the authority.
- Versioning: every path starts with `/v1`.
- Errors: an HTTP status plus a body
  `{"error": {"code": "machine_readable_code", "message": "short English text"}}`.
  Codes used below: `invalid_request` (400), `invalid_signature` (401), `invalid_credentials` (401),
  `unauthorized` (401), `invalid_invite` (403), `not_found` (404), `account_exists` (409),
  `payload_too_large` (413), `rate_limited` (429, with a `Retry-After` header in seconds), `queue_full` (429),
  `server_error` (500). Never put internal details in `message`.
- Domain separation: every signed string starts with a fixed label (`nine/v1/register`, `nine/v1/auth`), so a
  signature made for one purpose can never be replayed for another.

### Default limits (all configurable)

| What | Default |
|---|---|
| Request body | 128 KiB |
| Message content, decoded | 1 byte to 65,536 bytes |
| Identity key, decoded | up to 128 bytes |
| One prekey public key, decoded | up to 2,048 bytes (room for post-quantum keys later) |
| One-time prekeys per upload | up to 100 |
| One-time prekeys stored per account | up to 200 |
| Pending messages per recipient | 2,000 |
| Pending messages from one sender to one recipient | 500 |
| Undelivered message lifetime | 30 days |
| Auth token lifetime | 1 hour |
| Challenge lifetime | 60 seconds, single use |
| Idempotency memory for sends | 24 hours |
| Clock skew allowed in registration | 5 minutes |

## 3. Endpoints at a glance

| Method | Path | Needs token | Purpose |
|---|---|---|---|
| GET | `/healthz` | no | liveness, returns `ok` |
| POST | `/v1/accounts` | no (invite and signature) | register |
| DELETE | `/v1/accounts/me` | yes | delete the account and everything stored for it |
| POST | `/v1/auth/challenge` | no | get a one-time challenge |
| POST | `/v1/auth/token` | no (signature) | exchange a signed challenge for a token |
| DELETE | `/v1/auth/token` | yes | revoke the current token |
| PUT | `/v1/keys` | yes | upload or rotate key material |
| GET | `/v1/keys/count` | yes | how many one-time prekeys are left |
| GET | `/v1/keys/{accountId}` | yes | fetch someone's key bundle |
| PUT | `/v1/messages/{accountId}` | yes | send a message to that account |
| GET | `/v1/messages` | yes | list my pending messages |
| DELETE | `/v1/messages/{id}` | yes | acknowledge (delete) a delivered message |
| GET | `/v1/ws` | yes | live delivery over a WebSocket |

A token is sent as `Authorization: Bearer <token>`.

## 4. Accounts

### 4.1 Register: `POST /v1/accounts`

```json
{
  "publicKey": "A6EHv_POEL4dcN0Y50vAmWfk1jCbpQ1fHdyGZBJVMbg",
  "inviteCode": "7K3M-9QXA-2D5F-8HVN",
  "timestamp": 1793100000000,
  "signature": "RZUgB7fJXdZzY6qFFxpoF4y-6M34FvQKULhuQNPn23IIT3A3uSIqSmdAoFGvRy86nSwLwKWpa_y5BMjIoLZtAg"
}
```

The signature is Ed25519 over the UTF-8 bytes of these lines joined by a single `\n`, with no newline at the end:

```
nine/v1/register
<accountId>
<inviteCode>
<timestamp>
```

`<timestamp>` is the decimal number as sent. The signature proves the client owns the private key; without it anyone
could register somebody else's public key.

The server MUST check, in this order, and fail with the code in brackets:

1. The body is well formed, `publicKey` decodes to exactly 32 bytes, `signature` to exactly 64 (`invalid_request`).
2. The timestamp is within the allowed skew of the server clock (`invalid_request`).
3. The signature is valid for the derived account ID (`invalid_signature`).
4. Registration mode (see 9.2) allows it. In `invite` mode the code is known, unexpired and has uses left (`invalid_invite`).
   Use the single code `invalid_invite` for every reason, so a caller cannot tell "never existed" from "already used".
5. The account does not exist yet (`account_exists`, 409). In that case the invite is NOT spent.

Spending the invite and inserting the account MUST happen in one database transaction.

Success: `201` with `{"accountId": "<64 hex>"}`.

Invite code format: 16 characters of Crockford base32 (digits and letters without I, L, O, U), written in four
groups of four with dashes, upper case. The client sends it in exactly this form. The server stores only a hash of the
code (SHA-256 of the form without dashes).

### 4.2 Sign in: two steps

There are no passwords and no long-lived secrets on the server. A client proves itself by signing a fresh challenge.

`POST /v1/auth/challenge` with `{"accountId": "<64 hex>"}` returns `200` with

```json
{"challenge": "oKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr8", "expiresAt": 1793100060000}
```

The challenge is 32 random bytes. The server MUST answer `200` even for an account that does not exist (so the
endpoint is not a way to check which accounts exist), remember the challenge together with the account ID, and
forget it after 60 seconds or after one use. Keeping challenges in memory is fine for v1 (one server process).

`POST /v1/auth/token` with

```json
{
  "accountId": "56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c",
  "challenge": "oKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr8",
  "signature": "CTLRi7KcywBKwRwLkucdYAjA8s05LS70JrXG63MoVZ98uT4ZpU8_hg7nLqvovvlOJ6Y_2mdJvpWXF8is-h5pCA"
}
```

where the signature is Ed25519 over

```
nine/v1/auth
<accountId>
<challenge>
```

(the challenge exactly as the server sent it, base64url text). The server consumes the challenge first (so a failed
attempt also burns it), then checks that the account exists and the signature verifies. On any failure it answers
`401 invalid_credentials` and does not say which part failed. On success:

```json
{"token": "<43 characters, 32 random bytes in base64url>", "expiresAt": 1793103600000}
```

The server stores only the SHA-256 of the token with the account ID and expiry. Tokens are compared in constant time.
`DELETE /v1/auth/token` revokes the token used for the call (`204`).

### 4.3 Delete: `DELETE /v1/accounts/me`

Removes the account, its keys, its pending messages, queued messages addressed to it, and all its tokens. `204`.
Messages it already delivered to others are, of course, on their devices.

### 4.4 One device per account

For v1 an account lives on one device. Restoring from a key backup on a new device logs into the same account
with the same login key; the new device then uploads fresh key material with `replaceOneTimePreKeys: true`
(section 5), and peers re-establish their Signal sessions. The server needs no special handling.

## 5. Key bundles

The Signal identity key is a different key from the account login key (Curve25519, made by libsignal on the client).
The server treats everything here as opaque bytes and enforces only sizes and counts.

`PUT /v1/keys`

```json
{
  "identityKey": "<base64url>",
  "signedPreKey": {"id": 7, "publicKey": "<base64url>", "signature": "<base64url>"},
  "oneTimePreKeys": [{"id": 101, "publicKey": "<base64url>"}, {"id": 102, "publicKey": "<base64url>"}],
  "replaceOneTimePreKeys": false
}
```

- `identityKey` and `signedPreKey` are required on the first upload and optional afterwards (the client rotates
  the signed prekey every few weeks and then sends only that).
- `oneTimePreKeys` are added to the stored ones. A repeated `id` replaces the old entry. If `replaceOneTimePreKeys` is
  true, all stored one-time prekeys are deleted first (used after a restore).
- Limits from section 2 apply; over the limit means `400 invalid_request`.
- `204` on success.

`GET /v1/keys/count` returns `{"oneTimePreKeys": 57}`. The client tops up when the number is low.

`GET /v1/keys/{accountId}` returns the bundle of that account:

```json
{
  "identityKey": "<base64url>",
  "signedPreKey": {"id": 7, "publicKey": "<base64url>", "signature": "<base64url>"},
  "oneTimePreKey": {"id": 101, "publicKey": "<base64url>"}
}
```

- It removes the returned one-time prekey from storage atomically (a second caller must never get the same one).
  If none are left, `oneTimePreKey` is `null` and the rest is still returned: the protocol allows this.
- `404 not_found` if the account is unknown or has uploaded no keys yet.
- Draining attack: anyone could fetch a victim's one-time prekeys until none remain. Hence the per-pair and
  per-caller limits in section 8. Dropping to the signed prekey only is acceptable and keeps working.

## 6. Messages

An envelope is what the server stores and forwards, never looks into:

```json
{"type": 1, "content": "<base64url ciphertext>"}
```

`type` is an integer from 1 to 255 chosen by the client (for example 1 = normal Signal message, 2 = first message
with a prekey). The server only checks the range.

### 6.1 Send: `PUT /v1/messages/{recipientId}`

```json
{"messageId": "0b3f2a1e-5c1d-4f07-9b1a-3c5d6e7f8091", "type": 1, "content": "<base64url>"}
```

- `messageId` is a UUID made by the client. It makes sending idempotent: a retry with the same `(sender, messageId)`
  within 24 hours returns the first result and stores nothing new.
- The sender is the account of the token. The server sets it; the client cannot choose it.
- `404 not_found` if the recipient does not exist. `413 payload_too_large` if the content is too big.
  `429 queue_full` if the recipient has too many pending messages, or this sender has too many pending to this recipient.
- Success: `201` with `{"id": "<server message id>", "receivedAt": 1793100000123}`. A repeated send: `200`, same body.
  The client shows its "sent" tick when it gets this answer.

The server id is 16 random bytes in lowercase hex.

### 6.2 Receive over HTTP

`GET /v1/messages?limit=100` returns the pending messages for the token's account, oldest first, without deleting them:

```json
{
  "messages": [
    {"id": "9f3c...", "senderId": "<64 hex>", "type": 1, "content": "<base64url>", "receivedAt": 1793100000123}
  ],
  "more": false
}
```

`DELETE /v1/messages/{id}` acknowledges one message and deletes it for good. `204`, also when it is already gone.
Only the recipient can acknowledge its own messages. A message is never deleted before it is acknowledged,
except by the retention limit.

Order is by `receivedAt`, then by insertion order.

### 6.3 Receive over a WebSocket: `GET /v1/ws`

The client upgrades the connection with the token in the `Authorization` header. All frames are JSON text.

Server to client:

```json
{"op": "message", "id": "9f3c...", "senderId": "<64 hex>", "type": 1, "content": "<base64url>", "receivedAt": 1793100000123}
{"op": "queue_empty"}
```

Client to server:

```json
{"op": "ack", "id": "9f3c..."}
```

Behavior:

1. After the upgrade the server sends every pending message in order, then `{"op": "queue_empty"}`, then each new message
   at once as it arrives.
2. An `ack` deletes the message, exactly like `DELETE /v1/messages/{id}`.
3. An unacknowledged message stays queued and is sent again on the next connection.
4. One live connection per account. A new connection closes the old one with code `4001` (replaced).
5. The server sends a WebSocket ping every 30 seconds and closes the connection if no pong arrives within 10 seconds.
6. When the token expires the server closes with `4002`; the client signs in again and reconnects.
   Rate limit trouble closes with `4003`.
7. Messages are never sent to a connection that has not finished the upgrade, and never to any other account.

The desktop client locks itself after a while and wipes its keys; at that moment it closes the connection and stops
acknowledging. The queue simply keeps the messages until the next unlock. That is why the offline queue is not optional.

### 6.4 What the server does not do

Read receipts, typing indicators, delivery confirmations to the sender and "last seen" are ordinary encrypted
messages between clients. The server has no idea they exist and MUST NOT add any such feature.

## 7. Storage

SQLite is the default (pure Go driver `modernc.org/sqlite`, so no cgo and a single static binary), behind a small Go
interface so PostgreSQL can be added later. A schema that works:

```sql
CREATE TABLE accounts (
  id          TEXT PRIMARY KEY,          -- 64 hex
  public_key  BLOB NOT NULL,             -- 32 bytes
  created_day INTEGER NOT NULL           -- days since 1970, not a precise time
);
CREATE TABLE invites (
  code_hash   BLOB PRIMARY KEY,          -- SHA-256 of the code without dashes
  uses_left   INTEGER NOT NULL,
  expires_at  INTEGER                    -- null means never
);
CREATE TABLE tokens (
  token_hash  BLOB PRIMARY KEY,
  account_id  TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  expires_at  INTEGER NOT NULL
);
CREATE TABLE key_bundles (
  account_id        TEXT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  identity_key      BLOB NOT NULL,
  signed_prekey_id  INTEGER NOT NULL,
  signed_prekey     BLOB NOT NULL,
  signed_prekey_sig BLOB NOT NULL
);
CREATE TABLE one_time_prekeys (
  account_id  TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  key_id      INTEGER NOT NULL,
  public_key  BLOB NOT NULL,
  PRIMARY KEY (account_id, key_id)
);
CREATE TABLE messages (
  id           TEXT PRIMARY KEY,         -- 32 hex
  recipient_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  sender_id    TEXT NOT NULL,
  type         INTEGER NOT NULL,
  content      BLOB NOT NULL,
  received_at  INTEGER NOT NULL
);
CREATE INDEX messages_by_recipient ON messages (recipient_id, received_at);
CREATE TABLE send_dedup (
  sender_id    TEXT NOT NULL,
  message_id   TEXT NOT NULL,
  server_id    TEXT NOT NULL,
  expires_at   INTEGER NOT NULL,
  PRIMARY KEY (sender_id, message_id)
);
```

Rules:

- `PRAGMA journal_mode = WAL`, `PRAGMA foreign_keys = ON`, and `PRAGMA secure_delete = ON` so deleted
  messages are overwritten in the file and not just unlinked.
- Popping a one-time prekey is one statement or one transaction (`DELETE ... RETURNING`), never read-then-delete.
- A background job runs every hour: drops expired messages, tokens and dedup rows. Challenges live in memory.
- Plaintext secrets are never stored: invite codes and tokens only as hashes.

## 8. Limits against abuse

Use token buckets in memory. IP addresses are used only for these buckets, held in memory, never written to disk or logs.

| Bucket | Default |
|---|---|
| `POST /v1/accounts` per IP | 5 per hour |
| `POST /v1/auth/challenge` per IP | 30 per minute |
| `POST /v1/auth/token` per account ID | 10 per minute |
| `GET /v1/keys/{accountId}` per caller | 120 per hour |
| `GET /v1/keys/{accountId}` per caller and target | 10 per hour |
| `PUT /v1/messages/...` per sender | 60 per minute |
| WebSocket connections per account | 1 live, 10 attempts per minute |

On exceeding: `429 rate_limited` with `Retry-After`. Also enforce the queue limits from section 2.
Blocking a sender is done by the recipient's client (it drops the messages); the per-sender queue limit stops one
sender from filling a recipient's whole queue.

## 9. Running it

### 9.1 Commands

One binary, `nine-server`:

- `nine-server serve --config /etc/nine/config.yaml`
- `nine-server invite create [--uses 1] [--expires 7d]` prints the code once and stores only its hash
- `nine-server invite list` shows hashes' short prefixes, uses left and expiry, never the codes
- `nine-server account delete <accountId>`
- `nine-server stats` counts accounts and queued messages, nothing per person

There is no admin HTTP API in v1: fewer ways in.

### 9.2 Config

A single YAML file, every value can be overridden by an environment variable. At least:

```yaml
listen: ":8443"
tls: { cert: /etc/nine/fullchain.pem, key: /etc/nine/privkey.pem }   # or: insecure_loopback_http: true
database: /var/lib/nine/nine.db
registration: invite            # invite | open | closed
message_ttl: 720h
limits: {}                      # overrides for the tables above
log_level: info
```

`registration: open` MUST print a loud warning at start. `closed` rejects all registrations (accounts already there keep working).

### 9.3 Logging and health

- Log: start, stop, config problems, panics and errors, each with a random request id.
  Never log IP addresses, account IDs, tokens, invite codes, keys, message ids or any content.
- `GET /healthz` answers `200 ok` and nothing else (no version, no counts).
- Timeouts: read header 10 s, read body 30 s, idle 120 s. Cap the number of header bytes.
- Graceful shutdown on SIGTERM: stop accepting, let requests finish, close WebSockets with 1001.

## 10. Security checklist

- Use `crypto/ed25519` from the standard library for verification, `crypto/rand` for all random values,
  `crypto/subtle` for comparisons of secrets.
- Reject anything with a wrong size before doing any cryptography.
- Same error and similar timing for every failure of sign-in and invite checks.
- No CORS headers: this is not an API for web pages. Unknown paths give `404` with an empty body.
- Limit body sizes with `http.MaxBytesReader`; limit WebSocket read size to 16 KiB.
- Tokens are bearer secrets: never in URLs, never in logs.
- Few dependencies, pinned by `go.sum`. Run `go vet`, `staticcheck` and `govulncheck` in CI.
- Fuzz the JSON handlers and the base64 decoding (`go test -fuzz`).
- Because the database contains only ciphertext and the metadata in section 11, backups are low risk, but they
  still hold who talked to whom: protect them like the database.

## 11. What the server sees and what it does not (v1)

Honest list. Items marked "later" are planned improvements, not features of v1.

| The server sees | Notes |
|---|---|
| Each account's public key and the day it registered | needed for sign-in |
| Which account sends to which account, when, and how large the envelope is | needed to route in v1. Sealed sender (later) hides the sender. Clients should pad content to a multiple of 160 bytes. |
| Which accounts fetch whose key bundle | Same: sealed sender or anonymous fetch later. |
| Whether an account has a WebSocket open right now | Inherent to live delivery. |
| The IP address at connection time | Held in memory only; not stored. A Tor onion service removes it. |

| The server never sees | Why |
|---|---|
| Message text, files, display names, avatars | End-to-end encrypted, names travel inside messages |
| Contact lists, who is in someone's address book | Not uploaded; there is no directory or search, the only way to reach a person is to know the full account ID |
| Phone numbers, emails, real names | Not part of the system |
| Read receipts, typing, delivery to the other side | They are ordinary encrypted messages |

## 12. What the client will do (so you know what to expect)

- Makes the Ed25519 key on the device (done), derives the account ID (done), keeps the seed encrypted at rest (done).
- Registers once with an invite code, then signs in when the app is unlocked. Locking closes the connection.
- Uploads key material, tops up one-time prekeys when `count` is low, rotates the signed prekey.
- Uses `messageId` on every send and retries after failures with exponential backoff, so duplicates must be harmless.
- Sends no display name or avatar to the server, ever.
- Maps delivery states like this: created locally means `Local`, a `201` or `200` from `PUT /v1/messages/...` means `Sent`,
  a read receipt message from the other client means `Read`.

## 13. Test vectors

The same values are checked by a client test (`IdentityVectorTest`), by Go's standard library and by Python, so
they agree across languages. All strings are UTF-8, lines joined by a single `\n`, no trailing newline.

| Name | Value |
|---|---|
| Seed (hex) | `000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f` |
| Public key (hex) | `03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8` |
| Public key (base64url) | `A6EHv_POEL4dcN0Y50vAmWfk1jCbpQ1fHdyGZBJVMbg` |
| Account ID | `56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c` |
| Invite code | `7K3M-9QXA-2D5F-8HVN` |
| Timestamp | `1793100000000` |
| Register signature (base64url) | `RZUgB7fJXdZzY6qFFxpoF4y-6M34FvQKULhuQNPn23IIT3A3uSIqSmdAoFGvRy86nSwLwKWpa_y5BMjIoLZtAg` |
| Challenge | `oKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr8` (bytes `a0 a1 ... bf`) |
| Auth signature (base64url) | `CTLRi7KcywBKwRwLkucdYAjA8s05LS70JrXG63MoVZ98uT4ZpU8_hg7nLqvovvlOJ6Y_2mdJvpWXF8is-h5pCA` |

The signed strings are:

```
nine/v1/register
56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c
7K3M-9QXA-2D5F-8HVN
1793100000000
```

```
nine/v1/auth
56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c
oKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr8
```

A first Go test should load these and check that the account ID derivation and both signatures verify.

## 14. Order of work

Each step can be tried with `curl` before the next one starts.

1. **Accounts and sign-in.** `serve`, `/healthz`, `invite create`, `POST /v1/accounts`, challenge, token, delete account.
   Unit tests with the vectors above.
2. **Key bundles.** `PUT /v1/keys`, `count`, fetch with an atomic pop.
3. **Messages over HTTP.** Send, list, acknowledge, the queue limits, idempotency, the retention job.
4. **WebSocket delivery.** Flush on connect, `queue_empty`, ack, ping, replacement of old connections.
5. **Hardening.** Rate limits, all size limits, fuzz tests, CI with `govulncheck`, a systemd unit and a Dockerfile
   as examples, a `server/README.md` with the run instructions.

Later: attachments (an encrypted blob store with an upload token and a size cap), sealed sender, push through a
notification relay that learns nothing, groups.

### House rules for the code

Readable and boring: small packages (`api`, `auth`, `store`, `limits`, `config`), no clever abstractions, comments
only on things that are easy to misread, nothing extra in the prototype, everything real (no fake data, no stubs
that pretend to work). Work on a branch and open a pull request into `main`; do not commit to `main` directly.

## 15. Decisions still open

Better to settle these before the first release, none blocks step 1:

1. Invite model: single-use codes only, or also multi-use codes for a team? (v1 supports both through `uses_left`.)
2. Defaults: 30 days of retention, 2,000 pending messages per recipient. Right for a small closed team?
3. Hosting: one VPS with Let's Encrypt, or behind a reverse proxy, and whether a Tor onion address ships on day one.
4. SQLite for good, or PostgreSQL later for bigger teams.
5. Sealed sender: it changes what clients store about senders, so decide before the client keeps sender ids for real.
6. Attachments: the size cap and the storage budget.
