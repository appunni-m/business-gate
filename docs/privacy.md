# Privacy and local data

Business Gate keeps exact phone numbers, optional local names, choices, receiving-account/installation bindings, limited profile observations, action state and aggregate effort timings in the app's private, credential-protected SQLite database. Android's app sandbox and device storage protections apply; the database does not claim separate application-level encryption.

The app declares no Internet, contacts, SMS, call-log, camera, microphone, location, external-storage, general overlay, or gesture-injection permission. It has no analytics, advertising, login, billing or networking SDK. It does not synchronize contacts or messages.

Screen access can expose sensitive text. The production service starts with an inert package filter and an empty integration registry. When qualified, structural profile fields and supported controls must be the only inspected paths. No chat text, notification bodies, raw UI trees, images, authentication material or message hashes are persisted.

Notification-assisted discovery and sales hints have independent consent. The notification listener filters packages and verifies compatibility before touching notification metadata. It keeps only a capped, short-lived opaque in-memory hint cache. The bundled text classifier is local and bounded; its output is reason bits, never blocking authority. Notification-to-account binding is not qualified, so production notification-body scoring is unavailable in this build.

Optional reminders use a silent, low-importance channel, need the app's notification permission, and are capped at one useful review per rolling 30 days. They contain no sender identity or message text. Android notification access is a separate permission from the permission to post these reminders.

Local diagnostics can be previewed and deliberately copied to the clipboard. They include app/Android versions, consent and readiness flags, coarse counts and a controlled compatibility reason. They exclude names, numbers, receiving-account identities and message text. Other apps may access clipboard contents according to Android's own behavior after you copy.

Backup and device transfer are excluded through legacy and current Android rules. An installation marker in no-backup storage causes restored authority to be disarmed. Actual manufacturer transfer behavior still needs device testing. Retention preserves deliberate choices and business history; unreferenced regular-profile records age out after 90 idle days, resolved unknown records after 30, events after 30 days or 5,000 entries, and effort totals after 35 days. Maintenance runs at startup and during writes. Total account storage is capped at 50,000 records; adding an explicit choice can evict only unprotected optional cache records. Existing deliberate choices and business history are retained; inability to durably record work cannot authorize a click.

**Clear local data** resets local choices, history and consent. Uninstall removes this app's private data. Neither action unblocks numbers in the connected application. There is no automatic upload, recovery server or remote kill switch.

This text describes the development binary. A publisher must supply a public privacy-policy URL and real support contact, review the actual release behavior, and complete the required distribution declarations before selling the app.
