# Qualifying a connected-app integration

The production registry is empty and connected-app actions remain unavailable. The receiver account is reported installed in the test emulator. A consenting test sender and physical-device evidence are still missing. No real block/unblock result is recorded.

## Acquire measurements before enabling a contract

Use dedicated consenting receiver/sender accounts with synthetic test content and no unrelated conversations. A spare phone and second test number, or a trusted consenting test participant, can provide the sender. A Business Gate fixture can test engineering behavior but cannot establish real sender delivery, block state, or read receipts.

Record the selected official installation's runtime identity fingerprint, exact long version code, current signing certificate and reviewed history, Android API, device family, device language, actual connected-app UI language, display/font configuration, orientation, and navigation mode. Never put the external product name or literal package identifier in project files. Identity fingerprints verify a package selected at runtime; they must not reconstruct or disguise an embedded identifier.

Measure these boundaries:

1. Authoritative receiving-account identity, independent of the sender's full number, with no policy transfer across receivers, profiles, clones, or installations.
2. Full one-to-one profile structure, exact number, explicit business/regular signatures, and fresh blocked/unblocked state. A missing business badge alone does not prove a regular account.
3. Bounded root-to-field paths, ancestor structure, resource suffixes, classes, expected labels, unique controls, and report/delete/other side-effect exclusions, including disabled and checked controls.
4. Entry, confirmation, and positive result screens with an uninterrupted identity chain. The current parser requires sender and receiver identity on each supported screen. A name-only dialog requires separately implemented and proven binding; it remains unsupported.
5. Navigation's unread/read-receipt effects. Automatic routes must preserve them. A read-affecting visible scan requires explicit scoped consent and separate implementation. No route may inspect message subtrees or use coordinates/gestures.
6. Real Stop, app switch, lock, IME/system windows, overlay placement, user interaction, action-event ordering, slow UI, policy changes, process death, and uncertain results.
7. Independently checked sender delivery and receiver state. A click return, local job, or screenshot of Business Gate's own subtitle is insufficient proof.

Keep original measurements outside the public project when they contain restricted branding or private data. Reviewed neutral fixtures must preserve the measured structural facts and identify their provenance; they cannot invent a qualification result. Production code has no recorder or runtime adapter-import endpoint.

## Bundled contract and evidence

`AdapterRegistry` validates only bundled assets. A row includes `id`, `packageSha256`, `versionCode`, `signingSha256`, `signingHistorySha256`, `api`, `locale`, `environment`, qualification/reviewer fields, `evidenceSha256`, and three screen recipes. The environment records manufacturer, model, device locale, font-scale percentage, density, and orientation. Connected UI language is additionally checked through the measured controls; device language alone does not establish it.

Each `PROFILE`, `BLOCK_DIALOG`, and `UNBLOCK_DIALOG` recipe specifies a signature, phone, receiver, business/regular and blocked/unblocked fields, block/unblock controls, and bounded nonempty forbidden-control paths. Each path specifies bounded child indices, resource suffix, class, expected text, and every ancestor's resource suffix, class, and child count. Unknown or missing structure is rejected.

The row's evidence is `adapters/evidence/<id>.json`. Its hash must match the row. It identifies the reviewer, capture date, physical/read-receipt/side-effect findings, acceptance results, and actual neutral fixture/report files with matching hashes. Its `contractSha256` covers the canonical row excluding `evidenceSha256`. Canonical JSON uses sorted object keys, ordered arrays, UTF-8, and no insignificant whitespace.

Both `scripts/check_registry.py` and the APK audit validate the evidence. Current promotion checks require the full case inventory to pass; synthetic runs alone do not supply physical evidence. Measurements or trial builds used to produce missing evidence must remain explicitly experimental. Never fill in physical flags, passing cases, or reviewer approval to get a build through the gate.

## Work still required

The connection and current-profile session plumbing now exists, along with namespace, cancellation, durable acknowledgement, and guard fixes. It is not a completed integration. Own-action event attribution is still conservative; navigation, bounded multi-profile batches, visible scanning, qualified notification candidate/text extraction, and complete external-transition recovery/compensation still need measured implementation and testing.

Use the [completion plan](implementation-plan.md) and [acceptance ledger](acceptance-ledger.json) to track these independently. The full and companion acceptance numbers are distinct. Any wrong-person/allowed-number/unauthorized personal block, false success, report/delete/send side effect, receiver confusion, or permission bypass stops promotion.

## Distribution and claims

The existing GitHub pipeline already signs and publishes installable development APKs. The publisher signing secret is configured; test probes use their own key. Qualification does not require another publisher credential.

The 30-day attention and discovery targets require a consented pilot including waiting, compatibility repair, unsupported cases, and high-volume users. Current local timing is a lower bound. A paid store launch is a separate owner milestone covering license, support/privacy publication, current platform and terms review, declarations, pricing, and actual approval. No commercial approval or successful live blocking is implied by this document.
