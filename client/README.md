# Nine: desktop client

A prototype client in Kotlin and Compose Multiplatform. For now it builds for desktop (Windows, macOS, Linux).
Android and iOS will be added as new targets in the same module, the shared code lives in `commonMain`.

There is no server yet. Chats and messages live only on this device, are sent nowhere and are stored
encrypted (see "Security"). The Signal protocol for conversations between people will be connected together with the server.
There are no made-up contacts in the app: the chat list is empty until you create your own.

## What it does

- create a chat (the "+" button), open it and send a message: Enter sends, Shift+Enter inserts a line break;
- the "All", "Unread" and "Archive" tabs, search over chats, search inside a conversation;
- move a chat to the archive and back, pick the color of your own messages;
- the conversations are saved to an encrypted file and restored on the next launch;
- an account is created on the device without a phone number or email, sign-in with an access code, a key backup in a separate file;
- a profile menu (profile, settings, lock, sign out) and full-screen settings with a grouped sidebar; the name, autolock, access code change and clipboard clearing work, the other items are marked "Soon".

## How to run

JDK 17 or newer is needed. Gradle downloads itself.

```sh
cd client
./gradlew :composeApp:run        # run the app
./gradlew :composeApp:desktopTest # run the tests
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Where the data lives: three files in the app folder.
Windows: `%APPDATA%\Nine`, macOS: `~/Library/Application Support/Nine`, Linux: `~/.local/share/nine`.
To start from a clean slate, choose "Sign out" in the profile menu or delete the folder.
A plaintext `messenger.json` from earlier versions is moved inside and erased when an account is created.

## How the code is organized

The code is split into layers. Each one knows only about the ones below it.

```
composeApp/src
├── commonMain/kotlin/app/nine
│   ├── domain/        models (chat, message) and the time source, with no dependency on the UI
│   ├── data/          MessengerRepository: the only place where chats change; MessengerStore: where they are kept
│   ├── security/      Vault: the account, the access code, the key backup; Lockout: the pause after failed attempts
│   ├── presentation/  pure screen logic: filters, the feed with days and groups, time formats, the selection state
│   ├── ui/            Compose: theme, components, lock (sign-in), profile (menu), settings, chatlist, conversation, contact, dialog
│   ├── NineRoot.kt    the very top: the sign-in screen until an account is open, then the messenger
│   └── NineApp.kt     three columns and the new-chat and sign-out dialogs
├── desktopMain        the Main.kt entry point, FileVault (encryption, Argon2id, AES-GCM), the file chooser
├── commonTest         logic tests, no window needed
└── desktopTest        tests of the encrypted store and of the file one (the old, plaintext one)
```

Once there is a server, sending a message will go through `MessengerRepository.sendMessage`:
the message is first saved with the `Delivery.Local` status, then goes to the server and changes status.
The UI does not change: the ticks on messages already know how to show `Sent` and `Read`.

## Look and feel

There is one theme, dark: a black background, neutral panels with a thin light outline, green only on the selected
chat, own messages and ticks. There are no gradients. The Inter font lives in the resources (SIL OFL license in `licenses/`). The icons come from the Phosphor Icons set (MIT license, also in `licenses/`). In settings the tiles are colored by meaning: green, blue, amber and red for the ones that work, gray only for those that do not exist yet.
Colors and text sizes are collected in `ui/theme`.

Compose Multiplatform has no real background blur under glass yet, so the glass is conveyed by an outline, a highlight
on the top edge and a translucent header and input field.

## Security

What is done. An account is a pair of Ed25519 keys created on the device. The sign-in key and all conversations are stored in files
only as ciphertext (AES-256-GCM, every record has its own random nonce). The master key is encrypted with a key derived from the access
code (Argon2id, 64 MiB, 3 passes). The code itself is stored nowhere: a correct code is recognized by the master key
decrypting without an error. There is no home-made cryptography, only AES-GCM from the JDK and Argon2id and Ed25519 from BouncyCastle.

Files in the data folder: `vault.json` (the encrypted master key, the Argon2id parameters, the failed-attempt counter),
`identity.enc` (the sign-in key), `messenger.enc` (the conversations).

What this gives and what it does not:

- A person who gets only a powered-off device or a copy of the folder can read the conversations only by guessing the code.
  The minimum is eight characters, a phrase of several words is better. A short numeric PIN is guessed quickly against a copy of the files,
  so a quick PIN will come later, together with binding the key to the system keystore (Keystore, Keychain, DPAPI).
- The pause after five failed attempts (30 seconds, then twice as long each time, no more than an hour) stops guessing inside the
  app itself. The counter lives in the plaintext `vault.json`, so anyone with a copy of the files bypasses it.
  Against that kind of guessing only the cost of Argon2id and the length of the code help.
- Erasing overwrites the files with zeros and deletes them. On SSDs and journaling file systems this is not a guarantee:
  the reliability comes from the encryption, not from the overwrite.
- While the app is open, the keys are in RAM. Whoever controls a running computer sees everything
  you see. Autolock (settings → "Security") wipes the keys from memory: immediately on minimize or
  after 1, 5 or 15 minutes without mouse and keyboard, 5 minutes by default. After a lock the store is closed for writing,
  so that a delayed action cannot corrupt the file.
- Changing the code changes only what the master key is wrapped with. The master key itself stays the same, so the new code does not
  revoke the old one: whoever managed to copy the files and learned the old code will open everything they copied. A full key
  rotation will be a separate step. A wrong current code during a change counts toward the same attempt limit as at sign-in.
- A forgotten code cannot be recovered. What remains is the key backup (settings → "Profile and sign-in" → "Save") with a separate
  password. It brings the account back, but not the conversations: the conversations live only on the device.

What is not there yet: the Signal protocol for conversations with other people (needs the server), a duress code, wipe after
failed attempts, a panic button, binding to the system keystore. They are shown in settings marked "Soon".
