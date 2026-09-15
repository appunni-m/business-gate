# Displayed sender filtering and queued business cleanup

The owner authorized exact displayed-name notification filtering on 14 September 2026. This supersedes the earlier requirement to wait for a visible native session before every notification dismissal. It does not relax the identity and Stop requirements for native Block or Unblock.

## Product contract

After explicit setup consent, a supported direct notification whose displayed sender exactly matches the bundled public-profile catalogue, supplied sender-name rules or a locally learned business name is dismissed automatically unless that name is whitelisted. Matching is NFC plus trimmed edge whitespace; there is no fuzzy, substring or case-insensitive classification. Display names are rules, not authenticated identities: a personal sender using the same name can match. Unknown names remain visible until a supported profile inspection confirms a business. Personal, group and unsupported profiles never receive automatic native Block.

The public-profile starter catalogue records the actual name visible on each publicly accessible business profile, its business-owned contact source and observation date. Published profile names are not represented as a comprehensive directory or as measured notification payloads. No third-party marketing database, message body, logo or account credential is bundled. Updates ship with the APK; the app adds no network permission.

Verified business profiles add their business names to a local, receiving-account-scoped learned list. The existing whitelist remains authoritative and is never overwritten by the catalogue or learning. Exact-number choices remain intact. A name-only notification with an unresolved conflicting number exception stays visible until its identity can be checked.

## Persistence and notification handling

An additional local store owns filter consent, learned names and a durable list of hidden conversations. It is isolated by the main database identity and receiving namespace/binding; reset clears the old database identity's records. A failed or unsettled store disables filtering. Names, counts, times and technical status are saved; message bodies and raw notification images are not stored.

Only an eligible direct MessagingStyle notification can be removed by a name rule. The listener rechecks the selected installation, current notification, catalogue/learned-name revision and whitelist immediately before cancellation. Android removal acknowledgment determines the displayed dismissal result. The measured connected app retains a business preview in its shared summary after its direct notification is dismissed. Android summary cancellation also cancels group children, so Business Gate preserves shared summaries and discloses this limitation. Foreground conversation cleanup can clear the remaining preview through the connected app. No complete preview-suppression claim is made.

The system-owned content token stays in memory for subsequent foreground inspection. A durable queue is not a promise that an opaque activity token survives process death. If an unverified conversation loses its route, it remains explicitly pending and asks the user to open that conversation; no phone number is guessed from its name. A subsequently posted notification can restore its route.

## Foreground cleanup

Opening Business Gate shows the pending cleanup count and status. With cleanup enabled during setup, up to five eligible pending conversations run in a visible batch of at most three minutes with progress and Stop. Each item rechecks the current receiver, actual business profile, exact number, current whitelist and exact-number exceptions. A name match alone never dispatches native Block. A changed or personal profile is skipped with an explicit result.

Whitelisting immediately stops future name-rule dismissals and queues native Unblock only for previously verified matching businesses. The app explains that the user must open it to finish unblocking. Stop cancels the batch and delayed continuations. A later explicit retry resumes pending work; interruption is never recorded as success.

## Acceptance and delivery

Required checks include exact-name and Unicode policy tests; catalogue provenance/schema/brand validation; actual Android cancellation with two business names and one personal sender; name whitelist and exact-number exceptions; local learning; persistent queue and lost-route recovery; Stop and policy-change races; real incoming business dismissal with the authorized personal notification preserved; visible native cleanup and Unblock; full lint/build/device gates; and verified signed public delivery. Physical qualification remains separate and unrun unless a qualified device becomes available.

## Starter catalogue research

No downloadable Indian displayed-sender dataset with clear reuse terms was found. The public directory at <https://www.wa-directory.com/> was examined; no usable export was available. General company databases do not establish the spelling of a connected-app sender. Instead, ten exact names were read from public profile headings and cross-checked against the businesses’ official published contact numbers on 14 September 2026. The offline [catalogue](../app/src/main/assets/indian-sender-names.json) records both sources for every public-profile entry. It includes banking, air travel, telecom and rail catering. These are factual public profile observations, not guaranteed notification titles or a comprehensive Indian directory.

The catalogue validator runs in the mandatory pure-test gate. The application performs no network lookup. Updates require a new APK. Names learned from measured profiles stay only in this installation and receiving scope.

## Measured limits

A name can be copied, a business can use another display name, and an unknown name remains visible. Exact-name filtering therefore has both false positives and incomplete coverage. It is separately consented and never creates native identity evidence. No native mutation is dispatched from a catalogue name alone.

The real shared-summary preview persisted after Android acknowledged direct business cancellation. This is an observed limitation, not an unrun test. Personal notifications must remain intact. A foreground cleanup is required to clear the business through its actual conversation.

Physical qualification remains unrun. Live results and artifact hashes are recorded separately from synthetic fixtures.

## Live acceptance result

The [22-assertion real incoming test](measurements/display-sender-live-api36.json) observed automatic direct business notification dismissal, preserved the authorized personal notification, launched no background activity during dismissal, and confirmed a fresh exact native Block after opening Business Gate. The durable item recorded the verified number and completed outcome. The shared-summary preview limitation remained observed and is explicitly excluded from complete suppression claims.

Cleanup starts from a focused Business Gate activity. Later items recheck the actual foreground, and Stop disables automatic retries. Measured screen reads clear stale accessibility cache entries and request a root without descendant prefetch. Post-action verification can wait up to twelve seconds within the existing finite session, with current authority checked throughout and no repeated mutation.

The [owned Android fixture](measurements/display-sender-fixture-api36.json) verifies two-name dismissal, personal and summary preservation, group exclusion, whitelist and exact-number choices, learned names, durable records, route loss, two-item scheduling, Stop, and erasure after reset while unbound. Native Block and Unblock were separately requalified against fresh profiles; their bundled route-source binding has been updated only after the 25-assertion live session passed.

## Supplied list · 15 September 2026

The owner supplied an additional list of exact sender names and requested Base64 encoding of that list in the repository. The [encoded rules](../app/src/main/assets/supplied-sender-rules.json) contain 55 unique names: one duplicate was removed and two entries conflicting with the branding rule were excluded. No plaintext copy, decoded fixture, or lookup table is committed. The original ten public-profile entries remain separately sourced.

These additional names are owner-provided matching preferences, not independently verified businesses, official accounts, or allegations about the named entities. The app decodes them locally and labels their source accurately. Whitelist, exact-number exceptions, group exclusions, current compatibility and fresh native identity checks retain precedence. Base64 is reversible encoding, not encryption or secrecy; runtime UI and locally received names can be readable.

The catalogue gate validates the decoded format and uniqueness; the mandatory brand gate checks the decoded payload in source and APK/AAB artifacts. The owned Android notification fixture uses a name loaded from the actual encoded asset without duplicating its plaintext in test source. Physical qualification remains unrun.
