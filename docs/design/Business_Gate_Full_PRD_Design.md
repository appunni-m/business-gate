# Brand-neutral source specification

Extracted from the supplied design. External product names and identifiers are removed. Implementation uses an independent blue identity. The full specification governs added review and manual-choice features; physical qualification is mandatory. Source document reference claims are not implementation evidence.

## Source page 1

9 September 2026  ·  Implementation specification
1
PRODUCT / DESIGN / ENGINEERING
Business
Gate
Choose the businesses.
Keep the people.
ANDROID ONLY  ·  US$1
A one-page, local-first connected messenger
automation app designed for under one
minute of monthly attention.
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
ENABLED BY YOU · 2
Family Clinic
+1 202 555 0101
Enabled · unblocked
Parcel Updates
+1 202 555 0102
Enabled · unblocked
NOT ENABLED · 2
Home Deals
+1 202 555 0103
Blocked in connected messenger
Loan Offers
+1 202 555 0104
Block pending
Review · 1 possible business
People · not auto-blocked
Apply 1 pending block
Runs visibly in connected messenger. You can stop.
Full PRD & interaction design
Version 2.0 / 9 September 2026
IMPLEMENTATION SPECIFICATION, NOT A WORKING APK
Includes visual states, deterministic rules, exact-number safety, database and build contracts, 96 acceptance
scenarios, and runnable reference checks. connected messenger integration and the monthly-attention target remain
explicit release gates.


## Source page 2

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
2
How to use this specification
Version 2.0 · 9 September 2026 · Android only · US$1 paid download
Product brief: Choose the connected messenger businesses you want. Let a local, narrowly scoped automation apply the
rest of your business-blocking rule. Keep personal conversations untouched; flag uncertain commercial senders
separately.
Primary experience target: No more than 60 seconds of required attention in a normal 30-day period after setup.
This is an unproven release criterion, not a current capability or an unconditional promise.
Document status: Implementation specification, not a working APK. Native interface, local rules and storage can
be built directly from this document. connected messenger-specific screen access, identity binding, read-state-safe
navigation and automation must pass the explicit physical-device validation gates before a paid release. All
measurements below are targets unless identified as reference-code test results.
Authority: This version supersedes the earlier Business Gate PRD and conversation proposals wherever they
conflict. MUST denotes a release requirement; SHOULD denotes an implementation preference that requires a
written justification to change. The working name has not undergone trademark clearance.
Reading map
Product and design: chapters 1–8. Android execution and storage: chapters 9–15. Acceptance and commercial
release: chapters 16–18. Primary sources: chapter 19. The runnable reference pack supplements the PDF; it does
not replace device validation.
Visuals are concept mockups. All names and telephone numbers in illustrations are fictional. The actual Android implementation must
honour the exact text, dimensions and state contracts, including large-font and dark-mode behaviour.


## Source page 3

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
3
Contents
4
1. Product contract and release boundaries
7
2. Scope, users and defaults
9
3. The less-than-one-minute product requirement
11
4. Account identity, policy and personal safety
14
5. New personal numbers and suspected businesses
18
6. Visual design: one quiet, searchable page
24
7. UI tokens, accessibility and copy contracts
28
8. Setup, consent and complete user journeys
30
9. Background behaviour and safe execution sessions
32
10. Android architecture and module contracts
35
11. connected messenger adapter implementation: measure, do not guess
37
12. Automation state machine, race conditions and recovery
40
13. Database, persistence and migration contract
45
14. Security, privacy and lifecycle
48
15. Build configuration and implementation sequence
52
16. Test strategy, edge-case catalogue and acceptance criteria
58
17. Paid release, policy, maintenance and launch decision
60
18. Reference implementation and delivery evidence
62
19. Primary sources and verification notes
Each chapter is bookmarked in the PDF. Source IDs such as [S1] link to the evidence register. Exact code files are supplied in the
companion pack.


## Source page 4

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
4
1. Product contract and release boundaries
1.1 The one job
BG-001. Maintain an exact-number list of businesses the user enables, and apply connected messenger's own Block control
to other positively identified business accounts when a supported, safe interface is available. Newly discovered
business numbers default to not enabled. An enabled brand's new number is not implicitly enabled.
BG-002. The product has one searchable, vertically scrollable management page. Enabled businesses are first.
Other discovered businesses are below. New or uncertain senders form a collapsed review area below the
business list. Personal records remain hidden unless searched for or expanded.
BG-003. Suspecting that a personal-number sender is commercial is not authority to block it. A separate, explicit
user decision can create a durable exact-number manual block. Neither a name match nor a sales-text classifier
can transfer that decision to another number.
BG-004. The app runs locally with no account, backend, advertising, telemetry SDK, cloud model or subscription.
Target zero third-party runtime libraries. The Android toolchain and developer test tools are not runtime
dependencies.
1.2 Priority order when requirements conflict
Priorit
y
Invariant
Consequence
1
Do not perform an unauthorised action or
affect the wrong person.
Uncertain identity, screen or consent means no click.
2
Honour an explicit enabled/kept number
immediately.
New block work is vetoed before storage finishes.
3
Report the actual state.
A pending or unverified action never appears as completed.
4
Minimise user attention and access to private
data.
Batch work; do not manufacture review tasks or harvest
message history.
5
Enforce the business default-deny rule.
Enforcement can wait when a higher-priority condition
fails.
This is default-deny for the policy of a confirmed business, not default-deny for every message or every stranger.
Unknown people remain reachable. When the product cannot safely perform its job, it says so rather than
compensating by blocking more broadly.
1.3 What is and is not achievable through this architecture
Android accessibility services can inspect exposed interface information and perform supported interface actions.
That does not give this app a private connected messenger message-delivery hook or access to connected messenger's internal
database. The proposed integration is UI automation, not a network firewall. [S1, S2, S8]
Desired outcome
Contract for this product
Block a confirmed, non-enabled
business number.
Attempt after discovery, with exact live identity and a supported blocking flow.


## Source page 5

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
5
Desired outcome
Contract for this product
Repeat when that business changes
number.
Apply the same default-deny rule independently to each newly confirmed
business number.
Block a business before its first
message.
Not promised. Discovery may begin with that first message.
Catch a salesperson using a new
personal number.
Optional sales hint or manual flag; no automatic block merely because the
number is new.
Know two numbers belong to the
same real company.
Not promised and not necessary for the official-business default-deny rule.
Perform invisible blocking while the
phone is locked.
Not promised. Work remains pending until an eligible visible session.
Preserve every existing personal chat's
unread state.
Automatic paths must avoid opening unread chats; otherwise the user must
explicitly authorise a scan with a read-state warning.
Remain effective forever.
Rules do not expire automatically; availability still depends on permissions, app
data, account context and compatible connected messenger UI.
No implementation can infer business ownership from a previously unseen personal number without evidence.
When the only observation is a new personal number, a relative and a salesperson are indistinguishable to this
product. The safe result is an optional review flag, not a stronger claim.
1.4 Release-stopping feasibility gates
Gate
Evidence required
On failure
G1: account type and
identity
The production service declaration can
obtain a connected messenger-controlled business
marker and bind an exact number in a
one-to-one profile.
Do not ship automatic business blocking for
that build.
G2: safe block and
verification
Exact-number binding survives the full
block dialog; no reporting/deletion side
effect; positive post-action UI
verification.
Disable the adapter; retain local preferences
only.
G3: recipient account
isolation
The receiving connected messenger account can
be established for each action session
and changes invalidate the session.
No mutation in ambiguous/multiple-account
contexts.
G4: background discovery
and safe navigation
Notification hints and/or supported
visible screens discover enough eligible
accounts without changing personal
read state silently.
Publish limited coverage only; no universal
claims.
G5: attention
A 30-day user test meets the attention
targets in section 3 without hiding
screen-wait time.
Do not market the one-minute proposition.
Rework or stop the release.
G6: distribution permission
Play accessibility declaration approved;
independent terms/privacy review
completed.
No paid public release through that route.


## Source page 6

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
6
Android can restrict sensitive views from non-accessibility-tool services. Test G1 with
isAccessibilityTool=false; falsely declaring the app an accessibility tool is not a fallback. [S6]
1.5 Explicit corrections to earlier proposals
A missing business badge is not proof that an account is personal. Use No business evidence seen, not a false
personal certification. A notification is a discovery hint, not a guaranteed business identifier. Swiping away the
activity does not imply guaranteed service survival. Lack of recent accessibility events does not prove that the
user is not touching the screen. Opening a chat may affect read receipts, so zero read-state effects cannot
coexist with arbitrary silent scanning. The reference architecture below resolves these issues by abstaining,
validating and asking once for any necessary visible scan.


## Source page 7

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
7
2. Scope, users and defaults
2.1 Supported product scope
Dimension
V1 decision
Receiving application
Official connected messenger Messenger, package qualified.target.package, verified signing lineage
captured from the supported official installation.
Platform
Android phones; minSdk 29, compileSdk 36, targetSdk 36 baseline. Claim
support only for tested OS/build combinations.
connected messenger versions
Exact build codes and UI variants listed in a bundled, tested compatibility manifest;
no unbounded version range.
UI language
English first. Other languages require their own fixtures and screen adapters;
ordinary personal messages in any language remain unblocked by default.
Account context
One receiving connected messenger account, one Android user/profile. A second account,
clone, work profile or ambiguous account switch disables mutation.
Device layout
Portrait/landscape, light/dark, standard and large font sizes. Unsupported connected messenger
layout changes pause actions, not the app's readable UI.
Money
Paid install through Google Play, US base price US$1 subject to accepted price
increments; review country prices before release.
Routine actions
Search, enable/disable a business, keep or manually block a suspected sender,
pause/resume, one-tap pending batch.
The API-level baseline is a reproducible choice. Google's published phone-app submission requirement beginning
31 August 2026 is API 36 or higher; recheck at submission. A target API declaration alone does not prove device
compatibility. [S11]
2.2 Non-goals
No replacement connected messenger client; no message composer, AI assistant, universal spam filter, reverse lookup, caller
ID, global company database, VPN, packet interception, unofficial connected messenger API, root access, ADB/Shizuku
requirement for customers, anti-spam reporting, group moderation, contact syncing, phone-number reputation
marketplace or remote control. No auto-delete, auto-archive, automatic read/unread manipulation or content
suppression. No multi-number brand grouping in V1.
The separate connected messenger Business receiving app (qualified.target.package), connected messenger Web/Desktop, cloned app
containers, locked chats, hidden chats, channels and communities are outside the mutation scope. The app may
display a reason that a context is unsupported; it must not attempt to work around its access controls.
2.3 Default settings, fixed in V1
Setting
Default
User-facing location
Global blocking rule
Off until explicit activation
Header status / Pause or Resume
Exact-number enablement
Off for newly confirmed businesses
Row switch


## Source page 8

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
8
Setting
Default
User-facing location
Personal and unknown
automatic blocking
Never
Not configurable
Notification-assisted
discovery
Optional, off until Android access
granted
Setup; later in inline Settings dialog
Sales hints from
notification text
Off; separate explicit opt-in
One setup checkbox, later Settings
Notifications from
Business Gate
Optional
Permission prompt only after requesting a
notice feature
Routine review digest
At most one silent digest per rolling 30
days, only if useful
Settings; off when own notification
permission is denied
Automatic foreground
takeovers
Never
Not configurable
Accessibility macro
gestures/coordinate taps
Disabled
Not configurable
Theme
Follow system
No theme picker needed
Cloud backup and analytics
Disabled / absent
Privacy explanation only
2.4 Primary user scenarios
A person wants delivery updates from two businesses but not promotions from everyone else. A user wants
existing family and colleague conversations left alone. A merchant keeps contacting the user from fresh business
numbers. A broker uses a normal account and therefore needs a separate, non-accusatory review flag. A user
loses a permission or upgrades connected messenger and must understand why new blocks are pending without
troubleshooting every week.
No persona authorises exceptions based on industry. A bank, clinic, school or courier marked as a business
remains subject to the user's exact-number choices. The setup warning explains the consequences; the app does
not secretly enable preferred categories.


## Source page 9

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
9
3. The less-than-one-minute product requirement
3.1 Define attention honestly
BG-010. Normal-month attention is the sum of required interaction time in Business Gate, required
reading/decision time, required visits to Android settings, and time during which a visible automation
monopolises connected messenger and the user must wait. Overlapping intervals count once. Time consumed by a
compatibility interruption is also reported; it cannot disappear into a separate metric.
One-time setup is measured separately. Normal-month reporting begins after activation and the initial choice of
wanted businesses. Report both the normal-use cohort and the entire cohort including support incidents. Do not
claim a universal maximum for every possible user's traffic volume, new-number volume or phone failure.
Proposed release measure
Target
Normal-month required attention, p90
At most 60 seconds per user per 30 days
Normal-month median
At most 30 seconds
Months requiring an app visit
At most one required review visit in the ordinary scenario
Required decisions per routine visit
At most five presented review items; remaining items optional and safely
untouched
Repeated requests for the same
dismissed sender
Zero unless the user requests another review or new authoritative business
evidence changes policy eligibility
Initial setup
p90 at most three minutes excluding an optional large historical scan; report
scan time separately
Permission/compatibility failures
One actionable explanation per incident; no repeated daily nag
These are design targets, not measured forecasts. The UI architecture alone cannot establish them.
3.2 Proposed 60-second budget
Monthly activity
Budget
Product design that supports it
Open and understand the
page
5 seconds
Stable layout; one status line; no dashboard.
Enable one wanted
business
5 seconds
Search plus one switch.
Resolve up to three useful
sales hints
15 seconds
Keep or Block; no content-reading task forced
by the app.
Start and wait for one
short batch
25 seconds
One explicit start; fixed macro; visible Stop.
Recovery reserve
10 seconds
Specific error copy and one next action.
The budget assumes a small number of decisions and a short eligible batch. It is not a capacity guarantee. When
queue volume or interface latency makes the budget impossible, the pilot fails G5 even if the app technically
blocks each number.


## Source page 10

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
10
3.3 Eliminate unnecessary work
Confirmed business blocks do not create individual user prompts after activation. Successful actions are recorded
silently. Personal messages produce no notices and no recurring account-type checks that require a user tap.
Suspicions never force a decision; leaving them alone preserves messaging. A user-chosen Keep ends future
suspicion prompts for that exact number. There is no daily summary, streak, protection score, inbox-zero
demand, rate-us popup or subscription upsell.
Cap proactive sales-hint review at five items per rolling 30 days. This cap limits notifications and promoted cards,
not durable business policy or the number of confirmed-business actions. All unresolved optional flags remain
accessible in the collapsed review area; never auto-block overflow items to keep the queue small.
3.4 Measurement plan
Run a consented 30-day pilot with at least 30 activated participants spanning the supported device matrix. Use
local aggregate timers and an optional participant-exported, identifier-free summary; no production telemetry
server. Measure active attention with observed task studies for decision time and local durations for app/screen
sessions. Record p50/p90/p95, full distribution, traffic volume, skipped work and coverage. A pilot that only uses
scripted laboratory screens is insufficient for G5.
Every required foreground session counts, including a micro-batch started while connected messenger was open. Do not
assume the user is doing something else during a visible automation. Report lower-bound timing when human
reading time is unobserved. High-volume and incident cohorts are published internally as separate rows, not
excluded from the overall table.


## Source page 11

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
11
4. Account identity, policy and personal safety
4.1 Four independent concepts
Concept
Values
Meaning
Observed account type
UNKNOWN, BUSINESS_CONFIRMED,
REGULAR_PROFILE_OBSERVED,
AMBIGUOUS
What a supported current profile exposed.
Regular is an observation, not proof of the
owner's occupation.
User policy
DEFAULT, ALLOW, DENY_MANUAL
Exact-number choice. DEFAULT means apply
the business rule only when currently verified.
Review flag
NONE, NEW_SENDER,
POSSIBLE_COMMERCIAL,
TYPE_CHANGED
Informational; never action authority.
Observed block state
UNKNOWN, BLOCKED, UNBLOCKED
Last supported connected messenger UI observation,
with time and evidence.
Execution state is separate again: QUEUED, WAITING, RESOLVING, READY, APPLYING, VERIFYING, VERIFIED,
UNVERIFIED, FAILED, CANCELLED or PAUSED. These are not substitutes for user policy.
BG-020. No single isBlocked boolean may represent policy, classification, execution and outcome. The switch is
user policy. The subtitle is execution/observed reality.
4.2 Exact account key
Use (namespace_id, canonical_phone). The namespace identifies this installation's verified receiving connected messenger
account and Android user context. Never merge rows by display name, avatar, message, last digits, brand,
website or a phone number copied from message text.
For V1, accept a phone only from a tested connected messenger-controlled profile identity field or explicit user input
subsequently checked against that field. A notification phone-looking field is an untrusted hint until the adapter
validates its provenance. A user can pre-enable a complete number before discovery, but no mutation is allowed
until live binding succeeds.
Canonicalisation accepts one explicit leading + and ASCII digits, removes only known presentation spaces,
parentheses and hyphens, then checks the app's conservative 7-to-15-digit range with a nonzero first digit.
Reject extensions, multiple numbers, letters, bidirectional control characters, hidden text, ambiguous national
formats and inferred country codes. This is a strict product input rule, not a claim of complete global
telephone-number validation. Show “Enter the full number with country code” for rejected manual input.
Missing or hidden numbers, usernames without an exact supported identity mapping and non-phone identifiers
remain UNKNOWN/unsupported. A shared last four digits never establishes identity. The full canonical number
is visible on every mutating confirmation.
4.3 Default-deny without a brand database
Observation
Decision
Fresh confirmed business; DEFAULT;
exact safe binding
Block when a safe execution session exists.


## Source page 12

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
12
Observation
Decision
Same company name; new business
number; DEFAULT
Treat independently; block when verified.
Fresh confirmed business; ALLOW
Never auto-block.
UNKNOWN, AMBIGUOUS or regular
profile; DEFAULT
No automatic block.
Possible commercial personal number;
DEFAULT
Optional flag only.
Any exact number; DENY_MANUAL;
live identity matches
Apply the user's explicit block decision; do not relabel it as an official business.
A manually denied sender changes
number
New number requires its own business evidence or explicit decision.
Group, channel, community, broadcast
or self-chat
No mutation.
Unloaded policy, invalid namespace or
unknown screen
No mutation.
The app can repeatedly block new official business accounts without identifying common ownership. It cannot
promise automatic cross-number enforcement for normal accounts while also promising never to affect innocent
new people.
4.4 The personal cache
BG-021. Store a minimal record for safely bound, ordinary-profile observations so existing personal
conversations do not trigger repetitive inspection or notification work. Store the exact number, optional observed
name, observation time, adapter/version and any explicit user policy. Do not read the phone's address book or
copy message history.
Call the state REGULAR_PROFILE_OBSERVED. Customer copy is “No business badge seen” or “Not subject to
automatic blocking,” not “Verified human” or “Safe person.” Only a complete, recognised ordinary-profile variant
can set it. An inaccessible or incomplete screen is UNKNOWN.
A 30-day freshness interval reduces repeat inspection. Expiry means reconsider on a naturally visible profile; it
does not enqueue a block or force navigation. Revalidate earlier after an authoritative type change, exact-identity
conflict or incompatible adapter update. A name/photo change alone is not a reason to open a personal chat or
revoke the cache.
Keep creates ALLOW for that exact number with no automatic expiry. The cache itself never grants a business
exception. A formerly ordinary account that becomes a positively verified business follows the current rule unless
explicitly kept/enabled. Surface a TYPE_CHANGED explanation. This distinction prevents a stale cache from
bypassing the user's rule while preserving their deliberate exceptions.
4.5 Existing-message protection
No automatic action may send, reply, report, delete, archive, change read settings, open attachments, play media,
view a status or mark messages read/unread. Never traverse message bubbles to classify the owner's account
type. Use profile/header/control nodes only.
Automatic discovery must use a validated route that does not open unread chats. When only a read-affecting
route exists, leave the item pending. A user-started scan may proceed only after the specific warning described in


## Source page 13

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
13
section 8. connected messenger's read receipts are affected by reading messages; do not claim to invisibly undo that effect
by marking a chat unread. [S15]
Personal accounts are hidden from the default business list. Search may reveal them under “People - not
auto-blocked,” without a business switch. The optional expanded personal area shows a concise count and rows;
it is not a maintenance task.
4.6 External changes and irreversible consequences
ALLOW prevents future automatic blocks; it does not repeatedly undo blocks the user makes directly in
connected messenger. An explicit switch-ON or Unblock now action grants one unblock operation tied to the policy revision.
After that operation is consumed, a later external block is respected and shown as “Enabled here; blocked in
connected messenger.”
For DEFAULT confirmed businesses or DENY_MANUAL numbers, a manual unblock in connected messenger can be
reversed during a later eligible session while the rule remains on. Explain this on activation: enable the number
here or pause the rule to keep it unblocked.
Blocking affects messages and calls according to connected messenger's behaviour. Messages sent during a block are not
recovered merely by unblocking later. The app cannot restore missed delivery updates or authentication
messages. [S3]


## Source page 14

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
14
5. New personal numbers and suspected
businesses
5.1 Separate flag, separate authority
BG-030. New means “first seen by this installation,” not “not in your contacts,” “never messaged you before” or
“probably a business.” Existing conversations first encountered after installation can also be new to the app. The
label must state this when expanded.
A newly encountered ordinary/unknown sender belongs in a collapsed New senders area. It is not blocked,
muted or reported. If optional sales hints find sufficiently strong commercial wording, display Possible business in
amber. If a supported regular profile has been seen, the subtitle may add No business badge seen. Before profile
inspection, use Account type not checked instead of calling it a personal account.
Case
Visible treatment
Automatic consequence
New number, no readable
commercial evidence
New sender; collapsed, neutral styling
None
New number, strong
optional sales-text hint
Possible business; reason labels
None
Known/kept personal
number, sales wording
No promoted hint; manual flag remains
available
None
Confirmed official business
Move to business section; apply
exact-number rule
Eligible block unless enabled
User chooses Keep
Hide from review; record ALLOW
Future auto-blocks vetoed
User confirms Block this
number
Manual-denial row; Block pending, then
verified
Exact-number block only
Sender returns from a
fresh normal number
Independent new
sender/possible-business flag
No inherited block
The main list never labels a suspect “scam,” “fraud” or “verified business.” A forwarded offer from a friend and a
legitimate new vendor can trigger similar text patterns. The product is not a fraud detector.
5.2 Default-off optional sales hints
Without sales hints, the app uses only account-type evidence and user choices. This is a complete core product,
not a broken permission state. Sales hints add a local review aid for the user's personal-number edge case.
Enabling hints requires separate consent to briefly inspect new, exposed connected messenger notification text on the
device. Do not read historical chat bodies, attachments, images, voice notes, clipboard contents or other apps'
notifications. Missing, private, redacted or unsupported text produces no sales verdict. Android exposes
notification APIs but restricts some sensitive content; availability is not guaranteed. [S4, S7]
The hint engine is a fixed, transparent phrase classifier, not AI, and is not connected to the click executor. It may
set a review flag and reason bits. It cannot set BUSINESS_CONFIRMED, ALLOW, DENY_MANUAL or an action
job.


## Source page 15

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
15
5.3 Exact hint pipeline
1. Verify the notification package before inspecting extras. Ignore calls, group summaries, groups, channels and
 self messages when identified. An uncertain group/conversation classification does not yield a per-person
sales flag.
2. Establish a stable candidate hint using the supported notification adapter. Do not bind two unrelated numbers
 through matching display names. If exact identity is missing, retain only a short-lived unbound candidate; no
per-number user Block control is shown.
3. Require sales-hint consent and a sender not already kept/enabled or in a fresh ordinary-profile cache. The user
 can manually flag an existing sender despite these suppression rules.
4. Take only the latest newly exposed incoming text, at most 2,048 Unicode code points. Avoid parsing historical
 MessagingStyle entries repeatedly. Do not reconstruct missing or redacted content. [S17]
5. Convert to a bounded normalised token/phrase stream: Unicode NFKC, root-locale casefold approximation,
 whitespace collapse. Ignore display controls for classification only; never change the authoritative phone-key
parser.
6. Apply exclusions and the fixed feature groups below. Count each feature group at most once per message. Do
 not accumulate weak words across unrelated messages.
7. If the rule passes, set POSSIBLE_COMMERCIAL and store the feature bitmask, detector version and time.
 Otherwise retain at most a neutral NEW_SENDER observation.
8. Drop text references immediately after scoring. Do not persist message text, extracts, content hashes or full
 notifications. Java cannot guarantee erasure of every immutable string copy; the privacy claim is no retention
or transmission, not forensic memory zeroisation.
5.4 English feature groups for the first implementation
Phrase matching uses word/token boundaries, not substring matches within arbitrary words. All lists are bundled
in code, versioned and covered by tests. There is no runtime rule editor or downloaded script.
Feature group
Weight
Initial bounded phrase/token examples
SELLER_ID
3
“sales team”, “sales executive”, “property consultant”, “loan agent”, “authorised
dealer”, “authorized dealer”
OFFER
2
“special offer”, “limited offer”, “discount”, “pre approved loan”, “pre-approved
loan”, “new project launch”, “exclusive deal”
COMMERCIAL_CTA
2
“book now”, “buy now”, “schedule a demo”, “reply yes”, “call for details”, “contact
for price”, “book a site visit”
CATALOG
2
“price list”, “our catalogue”, “our catalog”, “available units”, “wholesale price”,
“bulk orders”
OPT_OUT
2
“reply stop”, “unsubscribe”, “opt out”
MONEY
1
Currency token adjacent to digits, such as INR, Rs, USD, dollar/rupee symbol; a
bare number is insufficient.
URL
1
A bounded http:// or https:// literal URL. Never fetch it.
URGENCY
1
“limited time”, “last few units”, “offer ends”, “today only”
Flag only when score is at least 5, at least 3 feature groups occur, one of SELLER_ID/OFFER/CATALOG occurs
and one of COMMERCIAL_CTA/OPT_OUT occurs. A URL, name, monetary amount or repeated message on its


## Source page 16

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
16
own never passes. Scores are rule weights, not probability estimates.
Exclusions: a message with an OTP/verification-code context and a code is not analysed for commercial hints; do
not preserve the code. Appointment/delivery/order-status-only messages without SELLER_ID, OFFER or
CATALOG are not promoted. Known forwarded-message indicators suppress promoted hints when the
notification adapter reliably exposes them; do not infer forwarding when the metadata is absent. The
English-only module first scans letters with Character.UnicodeScript.of(codePoint): any letter outside the
LATIN script makes the module abstain. Common punctuation, digits and symbols are allowed. This is a
conservative script gate, not language identification; Latin-script text still needs the complete English-phrase rule.
Mixed Malayalam/English text therefore abstains in V1. Unsupported text produces no keyword verdict, not a
translated guess.
5.5 Golden examples
Exposed text / situation
Expected output
“Hi, this is Maya. I got your number
from Arun.”
NEW_SENDER only; no sales flag.
“Your verification code is 123456. Do
not share it.”
No commercial hint; no code retained.
“Your parcel arrives today. Call the
driver on arrival.”
No commercial hint.
“Special offer: our catalogue is online.
Buy now.”
OFFER + CATALOG + CTA = 6; POSSIBLE_COMMERCIAL.
“Sales executive here. Book a site visit.
Limited time.”
SELLER_ID + CTA + URGENCY = 6; POSSIBLE_COMMERCIAL.
“Special offer, special offer, special
offer.”
One group only; no promoted hint.
“Can you lend me Rs 500? See
https://example.test”
MONEY + URL only; no promoted hint.
A kept friend sends the same
promotional wording.
Suppress promoted hint because the exact number is explicitly allowed.
New unknown sender uses
Malayalam-only sales text.
NEW_SENDER only; English detector abstains.
A business copies another business's
name/logo.
No cross-number trust or blocking transfer.
All examples are synthetic. Detector accuracy is unmeasured until the acceptance corpus is evaluated. Evasion by
wording changes, pictures, voice notes or unsupported languages is expected; missed hints are preferable to
treating a hint as proof.
5.6 User resolution and manual authority
The expanded review row shows the full number, “Not blocked,” up to two short reasons and two actions: Keep
and Block. A third text action Not now collapses/dismisses promotion for that number for 30 days. The
unresolved sender remains reachable.
Keep is one tap: commit ALLOW, clear promoted review, cancel queued blocking and show “Kept. This number
will not be auto-blocked.” A kept ordinary account appears only in search/personal expansion. If later confirmed


## Source page 17

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
17
business, it appears in Enabled because the exact number was explicitly permitted.
Block opens one native dialog: “Block this number? connected messenger has not identified this account as a business. Block
[full number] because you chose it. Future messages and calls may be stopped. Another number will need its own
check.” Buttons: Cancel and Block number. Confirmation writes DENY_MANUAL and creates an exact-bound
job. The app never sets a counterfeit business-classification flag.
A manual denial persists without expiry for that exact number. A later explicit enable/Keep overrides it. If the
number is reassigned to someone else, the old exact-number rule still applies until the user changes it; do not
claim the app can detect reassignment from a name or security-code change. Explain this in row details.


## Source page 18

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
18
6. Visual design: one quiet, searchable page
6.1 Design intent
The page should feel like a small permissions list, not a security dashboard. No prominent blocked-total counter,
alarming red background, shield score, animated mascot or feed. Display names and exact-number differences
are more important than decorative avatars. Use simple local initials in small neutral circles; do not download
business photos.
The illustration pages are reference layouts, not screenshots of a built Android application. Numbers and business
names in every design are fictional. The PDF's typography may differ from Android's system font; the Android
specification below is authoritative.


## Source page 19

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
19
Design plate A · Everyday use and search
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
ENABLED BY YOU · 2
Family Clinic
+1 202 555 0101
Enabled · unblocked
Parcel Updates
+1 202 555 0102
Enabled · unblocked
NOT ENABLED · 2
Home Deals
+1 202 555 0103
Blocked in connected messenger
Loan Offers
+1 202 555 0104
Block pending
Review · 1 possible business
People · not auto-blocked
Apply 1 pending block
Runs visibly in connected messenger. You can stop.
Everyday view
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
maya
×
ENABLED BY YOU · 1 MATCH
Maya Dental Clinic
+1 202 555 0105
Enabled · unblocked
PEOPLE · 1 MATCH
Maya
+1 202 555 0106
Kept · never auto-blocked
Personal accounts have no business switch.
Search expands matching sections only. Clearing it restores
your previous view.
Search, including personal matches
Enabled rows come first. The switch stores preference; the subtitle reports actual connected messenger state. One list scrolls
below the pinned search field.
Concept interface · fictional data · dimensions specified in chapter 7


## Source page 20

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
20
6.2 Permanent screen structure
Vertical element
Behaviour
Android status bar
Respect system insets; never draw interactive app content under it.
56 dp toolbar
“Business Gate” at start; Pause/Resume text action; overflow icon at end.
One compact status line
Rule off/on, waiting, applying or specific issue. No “fully protected” claim.
Search field, minimum 48 dp
Pinned above the list; hint “Search name or number”; clear button only when
nonempty.
Single ListView
Contains section headers, account rows, expanded details and the personal
summary. No nested scrolling lists.
Conditional bottom action, minimum
48 dp
“Apply 2 pending” only for actionable queued work; otherwise absent. Insets
include the gesture/navigation area.
On a 390-by-844 dp reference viewport, use 16 dp side padding, 8 dp related-item spacing and 24 dp section
spacing. Account rows are at least 88 dp at default font scale, but grow with content and accessibility settings.
Never hard-code a row height that clips text.
6.3 Main content order
1. Enabled: official businesses with ALLOW; manually managed business-like rows with ALLOW; explicit
 pre-enabled numbers awaiting verification. Sort by display search key, then full number, then database ID.
2. Not enabled: confirmed businesses with DEFAULT/DENY_MANUAL and manually denied ordinary senders.
 Within this section, pending/errors precede completed rows; then newest relevant event descending, then
full number. Off does not mean already blocked.
3. Review - optional: collapsed by default unless opened from a review notification or search. Contains
 POSSIBLE_COMMERCIAL before neutral NEW_SENDER; newest flags first; no business switch.
4. People - not auto-blocked: collapsed summary; contains ordinary-profile cache and user-kept personal
 records. No routine action required.
The Enabled and Not enabled sections may each be empty. Do not fill them with recommended businesses. The
footer says “Only accounts found on this phone are listed,” not “All connected messenger businesses.”
6.4 Business row anatomy
Name uses 16 sp medium, at most two lines. A phone line uses 13 sp regular, full number when it fits; shorten
only in a collapsed visual display, never in search identity or confirmation. A 12 sp status line says “Enabled,”
“Block pending,” “Blocked - checked today,” “Could not verify,” or “Enabled here; blocked in connected messenger.” The
switch has a minimum 48-by-48 dp touch area and an accessible name including the full number.
The row body toggles expansion only. It never toggles the switch. Expanded detail is inline, at most one expanded
row at a time: exact number; evidence label; last checked time; current desired policy; last action; relevant
Retry/Unblock now action. Do not display raw class names, adapter hashes or internal error enums to ordinary
users.
Use subdued neutral styling for OFF. Use a green/teal ON track with text “Enabled.” Amber denotes optional
uncertainty or recoverable pending information, not danger. Red is reserved for explicit destructive confirmation
or an important failure label and must have an accompanying text/icon cue.


## Source page 21

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
21
Design plate B · Unknown personal-number sender
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
REVIEW · 1 POSSIBLE BUSINESS
Scrolled to review · business rows above
New sender
+1 202 555 0107
Possible business · not blocked
Account type not checked
Commercial offer · request to buy
Keep
Block
Not now
A new number could be a real person. Nothing is blocked
without your choice.
Quiet, optional review
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
REVIEW · 1 POSSIBLE BUSINESS
Scrolled to review · business rows above
New sender
+1 202 555 0107
Possible business · not blocked
Account type not checked
Commercial offer · request to buy
Keep
Block
Not now
A new number could be a real person. Nothing is blocked
without your choice.
Block this number?
connected messenger has not identified this account as
a business.
+1 202 555 0107
Only this number will be blocked. A different
number needs its own check.
Cancel
Block number
One explicit decision for this number
Amber is a review hint, not a verdict. Keep protects the exact number. Block requires a clear confirmation and creates
no permission for another number.
Concept interface · fictional data · dimensions specified in chapter 7


## Source page 22

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
22
6.5 Search specification
Search is local, case-insensitive and accent-normalised for display-name matching, or literal digits for phone
matching. Unicode search normalisation never changes account identity. Query limit is 128 code points; trim
leading/trailing whitespace; an empty query restores the normal collapsed view. Debounce by 150 ms, execute
off the main thread and drop stale query results using an incrementing request generation.
Search includes enabled, non-enabled, review and personal records. Preserve section ordering. A match in a
collapsed section expands matching results temporarily without overwriting its ordinary collapsed preference.
Match name substring or phone-digit substring; do not infer company aliases. Use bound SQL arguments and
escape LIKE wildcard characters. No network lookup, search suggestions based on contacts or message-content
search.
When there are no matches, show “No matching account on this phone” and the secondary action “Enable a
number” with explicit full-number entry. Do not claim the business does not exist. Back first closes the keyboard;
a second back closes inline details or exits naturally. Clearing search preserves the pre-search list anchor.
6.6 Interaction and motion
A switch change commits the desired policy even if connected messenger is unavailable. Show the new policy immediately
and a truthful pending subtitle. Reordering occurs after the transaction succeeds and the finger is lifted, with an
optional 120 ms fade/move; preserve the first visible stable row ID and pixel offset. Do not jump to the top or
clear search. Disable animation under reduced-motion/accessibility conditions.
ALLOW is an immediate main-thread veto before asynchronous persistence. On database failure, keep the
session paused and show “Could not save. No new blocks will run.” Never let a visual ON switch hide a failed
write while automation continues.
The OFF action for an already enabled business displays a brief first-use explanation, not a repeated modal,
because the setup disclosure already authorises this exact business-rule action. Manual denial of an
unconfirmed/ordinary sender always requires the explicit Block number dialog.
6.7 Empty, limited and setup designs
Before activation, the same page contains a setup card rather than navigating through a separate management
product. After setup, that card disappears. No businesses found means “Nothing to manage yet. Personal and
unknown accounts are not auto-blocked.” Permission or compatibility issues occupy one inline card with one next
action.
Do not use a disabled grey toggle as the only error indicator: saved choices must remain editable while actions
are paused. When connected messenger is missing, keep the product rule off, explain the supported receiving app and offer
the official store through an external intent after a user tap.


## Source page 23

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
23
Design plate C · Setup and safe failure
9:41
LTE  95%
Business Gate
•••
Setup not finished
Search name or full number
Choose, then turn on
Enable the businesses you want before the rule
starts blocking others.
1
Explain access
Nothing runs without your consent.
2
Choose exceptions
Search or add a full phone number.
3
Turn on the rule
Personal and unknown senders stay reachable.
Continue
One-time setup. No account. No subscription.
Optional sales hints are off by default.
Setup within the same page
9:41
LTE  95%
Business Gate
•••
Paused · compatibility check needed
Search name or full number
connected messenger changed
Automatic actions are paused. Your enabled list
is still saved.
No guessing. No clicks on unknown controls.
ENABLED BY YOU · 2
Family Clinic
+1 202 555 0101
Saved · changes paused
Parcel Updates
+1 202 555 0102
Saved · changes paused
The rule resumes only after compatibility is verified. Existing
connected messenger blocks are not undone.
Check compatibility
Safe failure, not silent failure
Permission screens and confirmations are supporting system dialogs. Routine work always returns to this one
management page.
Concept interface · fictional data · dimensions specified in chapter 7


## Source page 24

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
24
7. UI tokens, accessibility and copy contracts
7.1 Native design tokens
Token
Light
Dark
Usage
Background
#F7F8FA
#11161C
Page background
Surface
#FFFFFF
#1A222B
Search, inline card, dialog
Primary text
#17212B
#F1F5F9
Titles/names
Secondary text
#52606D
#B6C2CE
Number/status
Divider
#E1E7ED
#34414E
Non-text separators
Enabled accent
#116B55
#75D5B4
ON action and icon; white or
dark text chosen for contrast
Amber text
#7A4B00
#FFD48A
Possible business/review
Amber surface
#FFF3DA
#382A16
Optional flag chip/card
Error text
#A32525
#FFB4AB
Failure/destructive action
Focus outline
#255DB1
#AAC7FF
Keyboard/TalkBack-visible
focus
Typography is Android system sans-serif: toolbar 22 sp medium; section header 12 sp medium; row title 16 sp
medium; body 14 sp; number 13 sp; supporting status 12 sp. Body line spacing approximately 1.25. Corner radius
12 dp for search/cards, 8 dp for controls; no heavy shadow. Icons are hand-authored vector drawables, 24 dp
visual size, 48 dp target. Use layout-start/end rather than left/right.
Treat these colours as design inputs. Calculate actual contrast for each foreground/background pair and test both
themes; normal text target at least 4.5:1 and large text at least 3:1. Android recommends sufficiently large touch
targets and meaningful accessibility labels; the product's minimum target is 48 dp. [S14]
7.2 Accessibility and large screens
At 200% font scale, text wraps, rows grow and actions remain reachable by scrolling. No horizontal clipping of
numbers in expanded detail. At narrow widths, put secondary actions on a new line rather than shrinking text.
Landscape/tablet view keeps a centred single column with maximum width 600 dp; do not add a two-pane
dashboard. Apply system-bar and IME insets with platform WindowInsets APIs.
TalkBack reading order is section, name, number, status, switch or review actions, expanded detail. An OFF
switch announces “Not enabled; block pending” or “Not enabled; blocked,” not merely “off.” Mark section rows as
headings. Announce a completed user action once using a polite live region; do not announce every background
detection.
Switch controls use native checked state. Their content descriptions include disambiguating phone identity.
Recycled list views must reset every listener, expanded state and accessibility property before rebinding.
Keyboard focus and accessibility focus must not jump to a different sender after reordering.
Phone numbers always render left-to-right within bidirectional text. Display names remove non-printing/bidi
control characters for presentation and enforce a 120-code-point limit; keep complete authoritative number
identity separately. Never let a sender-provided name imitate the app's status line or create clickable embedded


## Source page 25

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
25
links.
7.3 State-to-copy table
State
Exact primary copy
Action
Setup incomplete
“Choose the businesses you want to
hear from.”
Set up
Rule off
“Rule off. No new blocks will run.”
Resume
Ready, no known queue
“Rule on. Checks run when connected messenger
is accessible.”
None
Queue present
“Rule on - {n} actions waiting.”
Apply {n} pending
Running
“Applying your choices in connected messenger.”
Stop
Accessibility disabled
“Blocking paused. Screen access is off.”
Open accessibility settings
Notification access absent
“New senders are found when
connected messenger is visible.”
Optional Enable discovery
Unsupported adapter
“Blocking paused for this connected messenger
version.”
Check compatibility
Identity unavailable
“This account's number could not be
verified.”
Check in connected messenger
Cached blocked result
“Blocked - last checked {relative date}.”
View details
Result not confirmed
“Action sent; result not verified.”
Retry check
Suspicion
“Possible business - not blocked.”
Keep / Block
Ordinary cached record
“Not subject to automatic blocking.”
Keep / Recheck, inline only
Manual denial
“Blocked by your choice” or “Your block
is pending.”
Enable switch
External block on allowed
row
“Enabled here; blocked in connected messenger.”
Unblock now
Relative times use local device time for display; include an absolute date/time in expanded detail. Do not use “just
now” forever after clock changes. Decision timeouts use monotonic elapsed time, not wall-clock time.
7.4 Overflow, supporting dialogs and notifications
Overflow contains only Enable a number, Settings & privacy, Compatibility & help, and Clear local data. These
open native dialogs/inline content over the same management page. No additional tab bar or persistent
navigation drawer.
Settings contains the optional sales-hint toggle, optional digest switch and shortcuts to Android permission
screens. Consent removal immediately stops text analysis and removes unreviewed sales-feature bits; explicit
user choices remain. A change that requires new data access must present a new disclosure first.
Own notification channel attention is low importance, silent and contains no sender number or message text on
the lock screen. Post only on an actual incident transition or the capped useful review occasion. Tap opens the
main page; it never starts a background connected messenger block. A separate session notice may be used during a


## Source page 26

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
26
user-started batch, but the visible Stop control cannot depend on notification permission.
The app's POST_NOTIFICATIONS permission controls its own notices; it is distinct from notification-listener
access to connected messenger notifications. Ask only when the relevant feature is requested. Denial must not disable the
core locally managed rule. [S9]


## Source page 27

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
27
Design plate D · Light and dark themes
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
ENABLED BY YOU · 2
Family Clinic
+1 202 555 0101
Enabled · unblocked
Parcel Updates
+1 202 555 0102
Enabled · unblocked
NOT ENABLED · 2
Home Deals
+1 202 555 0103
Blocked in connected messenger
Loan Offers
+1 202 555 0104
Block pending
Review · 1 possible business
People · not auto-blocked
Apply 1 pending block
Runs visibly in connected messenger. You can stop.
Light
9:41
LTE  95%
Business Gate
•••
Rule on · 1 block waiting
Search name or full number
ENABLED BY YOU · 2
Family Clinic
+1 202 555 0101
Enabled · unblocked
Parcel Updates
+1 202 555 0102
Enabled · unblocked
NOT ENABLED · 2
Home Deals
+1 202 555 0103
Blocked in connected messenger
Loan Offers
+1 202 555 0104
Block pending
Review · 1 possible business
People · not auto-blocked
Apply 1 pending block
Runs visibly in connected messenger. You can stop.
Dark
Identical hierarchy and actions in both themes. No state relies on colour alone.
Concept interface · fictional data · dimensions specified in chapter 7


## Source page 28

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
28
8. Setup, consent and complete user journeys
8.1 First-run state machine
WELCOME -> ACCESS_DISCLOSURE -> ACCESS_SETTINGS -> CAPABILITY_CHECK -> OPTIONAL_DISCOVERY ->
REVIEW_ALLOWED -> ACTIVATE -> READY.
Persist completed steps and consent versions. Back/Not now never activates blocking. The service may be
enabled by Android while the rule remains off; these are distinct states. Resume setup after process death
without discarding existing choices or silently changing consent.
Before any initial mutations, the user must have a chance to enable wanted businesses. Offer pre-enabling exact
numbers and an optional discovery-only scan. Do not require enumeration of all connected messenger chats. The review
state says how many accessible profiles were checked and explicitly states that unvisited/hidden profiles were
not checked.
8.2 Accessibility disclosure: required text
“Business Gate uses Android Accessibility to inspect supported connected messenger account information and press Block
or Unblock according to your choices. This may stop messages and calls from those numbers. Screen access can
expose text shown in connected messenger. We do not save chat contents or send them to a server. Actions run only in
supported visible connected messenger screens and can be stopped. You choose when to start a scan that opens chats.”
Buttons: Not now and Agree and open settings. Store disclosure version, consent time and explicit user
response. The app must never click Android's permission-enable controls on the user's behalf. Google Play
requires disclosure/consent for this non-accessibility-tool use and separately reviews accessibility declarations.
[S5]
8.3 Notification discovery disclosure
“Notification access helps notice new connected messenger senders. Android may make notifications from other apps
accessible too; Business Gate ignores them. We do not save notification messages. Without this permission,
discovery is limited to supported connected messenger screens you open.”
Buttons: Skip and Agree and open settings. Package-filter before parsing. Without sales-hint consent, do not
inspect body text merely because notification access is available.
8.4 Optional sales-hint disclosure
Unchecked option: Flag possible sales messages from new senders.
Explanation: “Briefly checks new connected messenger notification text on this device for sales wording. Text is not saved or
uploaded. Hints can be wrong. They never block anyone automatically.” Buttons: Keep off and Enable hints.
Record independent consent and detector version. A native Settings switch turning on later must show this
disclosure again if the consent version changed.
8.5 Scan warning and activation warning
Read-affecting scan warning: “This scan may open selected connected messenger chats and mark messages as read. It will
not send, delete, archive or report anything. Only use it when you are comfortable with that. You can stop at any
time.” Buttons: Cancel and Start visible scan. The authorization is scoped to that session; it is not permission for
later hidden scans.
Activation warning: “Business accounts you have not enabled will be blocked when they can be checked safely.
This may include delivery updates, appointment reminders, support replies and authentication messages.


## Source page 29

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
29
Messages sent while blocked may not arrive after you enable the number again. Personal and uncertain accounts
are not automatically blocked.” Buttons: Review choices and Turn rule on. [S3]
A friend who uses a connected messenger Business account is still technically a business account for this rule. The review
instructions explicitly say to enable such wanted numbers. The app cannot infer family status without the user's
choice.
8.6 Routine flows
Journey
Exact sequence
New official business
Discovery hint -> live profile resolution -> business evidence -> current policy
-> queue -> eligible block -> verified subtitle.
New personal-number salesperson
Optional hint -> collapsed Possible business -> no action unless Keep or
explicit Block number -> exact-number policy.
Enable a blocked business
Set immediate veto -> persist ALLOW + revision -> cancel old block -> create
one unblock command -> execute/verify -> Enabled.
Disable a wanted business
Persist DEFAULT + revision -> queue business recheck -> block only if still
positively business and safe; otherwise no auto-block.
Reverse a manual personal block
Explicit Enable -> ALLOW + one unblock command -> verify exact number; no
automatic personal reclassification.
Pause
Immediate global veto, cancel uncommitted action chain, remove overlay;
retain connected messenger blocks and local choices.
Resume
Fresh consent/permission/namespace/adapter checks; rebuild jobs from
desired policy; do not replay saved clicks.
Add a wanted number before
discovery
Full-number input -> ALLOW locally -> “Enabled; not checked in connected messenger”
-> later exact profile binding.
Forgotten transactional sender
Search -> enable exact number -> explicit unblock; explain that missed
messages cannot be recovered by the app.
Reinstall or clear data
No restored active authority; setup again; existing connected messenger blocks are not
mass-unblocked.
8.7 Run a pending batch
Tap Apply pending, show a concise summary of confirmed automatic business blocks and separately authorised
manual/unblock actions, then open connected messenger through a user-initiated path. If this batch requires the
read-affecting route, show the scan warning; otherwise do not repeat activation consent. Validate the recipient
account at session start.
A visible non-obscuring overlay says “Applying your choices - 2 remaining” with a 48 dp Stop target. It must not
cover connected messenger's account number or confirmation controls. Maximum session is 25 seconds or five mutations,
whichever occurs first, unless the user explicitly starts another optional batch. Stop immediately on an unsafe
state. Returning from connected messenger shows completed, pending and unverified counts; it does not claim complete
inbox coverage.
The cap is a UX/engineering default, not a safe-rate limit published by connected messenger. Measure it in the pilot. A
workload that repeatedly requires more batches fails the one-minute promise; hiding remaining work is not a
solution.


## Source page 30

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
30
9. Background behaviour and safe execution
sessions
9.1 Two system-managed services, no hidden worker farm
The proposed app uses an enabled AccessibilityService for exposed UI observations/actions and an optional
NotificationListenerService for discovery. Neither is a promise that the process survives every OEM policy or
force-stop. Notification listener connection state must be handled explicitly; some device/profile cases limit
availability. [S1, S4]
Situation
Allowed work
Prohibited work
App activity closed;
permissions available
Receive eligible callbacks, maintain local
hints/queue
Claim guaranteed continuous availability
Phone locked/screen off
Bounded callback bookkeeping; no UI
mutation
Unlock, wake for scanning, defeat keyguard
Another app is foreground
Queue locally; optionally one useful
notice
Open connected messenger over it, click another app
connected messenger inbox visible
Inspect supported non-message
metadata
Assume idle means consent to navigate any
chat
Eligible business profile
already visible
Observe; execute only with a validated
current-profile session and prior rule
consent
Change unrelated screens or rely on cached
identity alone
User starts a pending batch
Navigate only the tested, authorised
route, within the batch budget
Send messages, bypass locked chats, continue
after cancellation
User force-stops app
Nothing until Android/user permits it
again
Restart loops or misleading active status
Permission/adapter lost
Retain choices; disarm actions
Continue with old nodes/coordinates
Android restricts background activity launches, with version-specific conditions and exceptions. The product
chooses not to exploit background-launch privileges to take over another task even where a particular device
would allow it. User-tapped foreground handoffs must still be tested against the current platform's PendingIntent
rules. [S10]
9.2 Session types and entry conditions
OBSERVE_ONLY: No changing actions. May classify a supported currently visible profile, refresh the ordinary
cache, create queue entries or update a known observed block state.
CURRENT_PROFILE: Prior activation authorises the fixed default-deny script on the current, exactly bound
profile. Available only after G1/G2/G3 and interruption testing pass. The profile must already be visible; the app
does not open another chat. A visible Stop overlay appears before the first action. If any precondition is
uncertain, remain observe-only. This capability is not presumed merely because the service can click.
USER_BATCH: A user taps Apply pending/Start scan. The exact allowed actions, scope, unread-side-effect
consent and session deadline are recorded. The app may navigate the tested route and verify each target
independently. No authority survives the session as saved click state.


## Source page 31

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
31
USER_UNBLOCK: A fresh explicit Enable/Unblock now command permits an exact-number unblock in a
foreground session, even when automatic blocking is paused. It does not resume other blocking work. A global
emergency disarm caused by corruption, identity or permission failure still stops this action.
9.3 Eligibility must be a complete predicate
Every mutation requires policy loaded; explicit consent version current; service connected; target package and
signing lineage supported; device unlocked and interactive; target window visible and expected; own overlay not
obscuring important controls; recipient account namespace verified; exact sender identity bound; one-to-one
profile; compatible build, locale and UI structure; expected job generation and policy revision; no immediate
ALLOW veto; no Stop/disarm; finite action deadline; and a supported next control.
Automatic business blocking additionally requires a fresh in-session connected messenger-controlled business marker. A
manual block instead requires a durable exact-number manual decision. Unblocking requires a fresh explicit
command; an ALLOW record alone is not enough.
The absence of recent UI events is not a reliable universal touch detector. Do not register input-intercepting
motion/touch-exploration flags to manufacture an idle signal. Accessibility motion observation can change input
delivery; the product does not use it as a passive keystroke/touch monitor. [S1]
9.4 Discovery route and read-state rules
Prefer account-info access directly from a tested inbox avatar/context route when it does not open the
conversation. The adapter's capability manifest must explicitly state whether the route can change read state.
Test unread counters and remote read receipts with consenting sender/receiver accounts before setting
readStatePreserving=true.
Without such a route, use naturally opened profiles or the explicitly authorised read-affecting batch. Do not
silently open a new personal chat simply to learn whether it is a business. Do not change connected messenger read-receipt
settings as a workaround. Opening the app via a supported user action is not permission to click every chat.
Initial discovery is limited: visible/reachable supported inbox profiles only. Archived chats are included only when
the user explicitly selects that scope and the adapter supports it. Locked/hidden chats are excluded. Report
inspected, skipped and unresolved counts separately. A scan stopping early is partial, not “complete.”
9.5 Bounded traversal
Track resolved exact numbers per scan session, not list positions. Inbox rows can move after notifications,
pinning or user input. Before opening each candidate, reacquire the current list and verify the candidate locator.
After profile resolution, deduplicate by canonical account key. Duplicate display names never merge.
Initial discovery budget: at most 100 resolved profiles or 120 seconds per explicit scan, whichever occurs first.
Pause/resume checkpoints are account IDs and scope, never UI nodes or coordinates. Three unchanged page
signatures after attempted scroll, a repeated candidate without progress or a missing expected screen ends the
scan with a partial-scope explanation. An unsolicited batch never starts just to finish a partial scan.


## Source page 32

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
32
10. Android architecture and module contracts
10.1 Dependency-minimal baseline
Use Java and Android framework classes. One application module is sufficient. No runtime framework for
navigation, reactive streams, image loading, analytics, SQL mapping, networking or payment is required. Native
ListView recycling and a small BaseAdapter support this interface without an AndroidX runtime dependency.
Module / file
Responsibility and boundary
GateApplication
Creates singletons; starts with actions disarmed; no background scan in
startup.
MainActivity / GateListAdapter
Renders immutable rows, preserves stable IDs, captures user commands;
cannot click connected messenger.
GateRepository / GateDbHelper
Owns SQLite transactions, schema, retention and immutable policy snapshots.
RuleEngine
Pure decision function; no Android dependencies, network, clock reads or side
effects.
SalesHintEngine
Pure bounded text-to-feature classifier; output cannot directly enqueue a
block.
GateAccessibilityService
Filters events, builds bounded structural observations, delegates action
execution.
GateNotificationListener
Package-first discovery; optional ephemeral text scoring; no notification
cancellation/reply.
AdapterRegistry /
MessagingAdapter
Tested exact-build screen recognition, identity binding, control selectors and
postconditions.
AutomationController
Finite state machine, session/deadline/cancellation ownership; one action at a
time.
SafetyGuard /
MutationDispatcher
Final immutable context and immediate-veto checks before any changing
action.
SessionOverlay
Visible progress and Stop; no generic system overlay privilege.
ConsentStore /
CompatibilityReport
Current grants, scope, adapter evidence and support status.


## Source page 33

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
33
ON-DEVICE BOUNDARY
One-page native UI
Choices, search, review and honest pending states
Policy + SQLite
Exact numbers; revisions; durable authorities
Automation controller
Bounded sessions; guards; verification
Notification hints
Optional, transient text; never block
authority
connected messenger UI adapter
Tested build, exact profile and native controls
Concept interface · fictional data · dimensions specified in chapter 7
The Android SQLite helper provides a database lifecycle without an ORM. Store policy in SQLite transactions, not
a scattered set of asynchronous preferences. [S12]
10.2 Thread ownership
The main thread owns UI rendering, accessibility callback scheduling, current session generation, immediate-veto
flags and actual performAction dispatch. Accessibility nodes are short-lived and reacquired after each transition;
never retain or persist them as durable work items.
A single serial executor owns database I/O, migrations, search preparation, retention and immutable snapshot
construction. Extract a small bounded primitive snapshot on the service callback thread; perform larger pure
computations off-thread. Do not pass live accessibility nodes into a delayed database job.
After a successful transaction, publish a complete immutable PolicySnapshot with monotonically increasing
revision. A main-thread map of immediate ALLOW/Stop vetoes protects the interval before persistence. Search
results have a separate generation so slow old searches cannot replace new results. Database migrations
complete before the service can arm.
10.3 Interfaces that developers must implement


## Source page 34

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
34
JAVA
interface RuleEngine {
    Decision evaluate(Policy policy, Observation observation,
                      SessionContext context);
}
interface MessagingAdapter {
    boolean supports(BuildKey key);
    ScreenObservation inspect(NodeReader reader, SessionContext context);
    ActionPlan nextStep(ScreenObservation screen, ActionJob job);
    Verification verify(ScreenObservation screen, ActionJob job);
}
interface GateRepository {
    void applyUserCommand(UserCommand command, ResultCallback callback);
    PolicySnapshot currentSnapshot();
    void recordObservation(ScreenObservation observation);
    void recordActionResult(ActionResult result);
}
NodeReader exposes only whitelisted package/profile/control paths, bounded depth and read limits; it is not a
message scraper. ActionPlan is an enum-like supported node action plus expected postcondition, never arbitrary
code, a coordinate, URL to execute or an AI instruction. Decision includes reason codes and no UI handle.
Observation contains namespace, exact phone, observed type, business evidence kind, profile completeness,
block state, adapter ID, build/locale, observed monotonic time, window/session binding and ambiguity flags.
Policy contains desired choice, revision and any manual/unblock authority. SessionContext contains
connection/consent/foreground/unlock/identity readiness, deadline, generation, immediate veto and safe-entry
type.
10.4 Repository layout
TEXT
app/src/main/
  AndroidManifest.xml
  java/com/example/businessgate/
    GateApplication.java
    ui/{MainActivity,GateListAdapter,UiRow,SetupController}.java
    data/{GateDbHelper,GateRepository,PolicySnapshot}.java
    policy/{RuleEngine,SalesHintEngine,Decision}.java
    service/{GateAccessibilityService,GateNotificationListener}.java
    automation/{AutomationController,MessagingAdapter,AdapterRegistry}.java
    automation/{ActionJob,ScreenObservation,SessionContext}.java
    safety/{SafetyGuard,MutationDispatcher,IdentityResolver}.java
    support/{ConsentStore,CompatibilityReport,SessionOverlay}.java
  res/layout/{activity_main,row_account,row_section,row_review}.xml
  res/layout/{row_person,row_expanded,session_overlay}.xml
  res/xml/{gate_accessibility,data_extraction_rules,backup_rules}.xml
  res/values/{strings,colors,dimens,styles}.xml
  res/values-night/{colors,styles}.xml
  res/drawable/   # vector icons and state-list backgrounds
  assets/adapters/compatibility.json
app/src/test/    # pure Java tests, no phone required
app/src/androidTest/ # optional test-only Android harness
com.example.businessgate is a development placeholder. Choose and freeze the owned release application ID
before creating the paid listing. Do not imply ownership of a real domain that has not been registered.


## Source page 35

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
35
11. connected messenger adapter implementation: measure,
do not guess
11.1 Why the adapter is a release dependency
connected messenger screen resource IDs, profile layouts and block dialogs are not a stable public automation contract
established by this PRD. The exact selectors must come from legal, consenting test-device observations of the
installed supported build. Do not invent IDs, use private reflection into connected messenger or ship a generic text-clicking
fallback.
The completed adapter manifest is the required integration deliverable. A missing row is an unsupported
capability, not an invitation for the developer to make the script optimistic.
11.2 Capability record to fill from physical-device evidence
Field
Required measurement
adapter_id
Unique semantic ID, e.g. adapter_en_build_<actualcode>_r1.
package_name,
long_version_code, version_name
Read from the official target installation.
signing_cert_sha256[]
Verified signing lineage from the supported official install; do not pin an
invented hash.
os_api_range, locale,
layout_variant
Exactly the combinations tested.
recipient_account_route
Tested way to bind the receiving connected messenger account before a session.
one_to_one_profile_signature
Required class/ID/ancestor structure and exclusions.
phone_selector
Authoritative identity-field path; format and ambiguity handling.
business_marker_selector
connected messenger-controlled type indicator in a non-message structural location.
ordinary_profile_signature
Complete ordinary profile variant; absent marker alone is insufficient.
block_entry, block_dialog,
confirm_selector
Unique supported node paths and expected transition bindings.
blocked_postcondition,
unblocked_postcondition
Fresh exact-account UI evidence, not a click return value.
report_delete_controls
Expected absence or explicitly unchecked/unused states; abort on unknown
combined action.
navigation_route,
readStatePreserving
Verified entry/return route and unread/read-receipt tests.
notification_binding_contract
Exact hint fields validated against real profiles; missing identity remains
unresolved.
fixtures, evidence_hash,
reviewer, tested_at
Reproducible fixtures, screen recordings and approval record.


## Source page 36

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
36
Until these fields are measured, the bundled production registry is empty and automation is unavailable. The
interface and rule tests may still run in a fake adapter harness clearly labelled development-only.
11.3 Capture protocol
Use at least two consenting test phones/accounts: a receiver and independent senders. Include an official
business account, a regular account, two accounts with identical names, a newly changed number, an
already-blocked sender, and a group. Give them synthetic names and no unrelated personal chat data.
Record OS API/build, OEM, locale, font/display scale, navigation mode, connected messenger version and signing certificate.
With the real production accessibility declaration, inspect selected nodes on inbox/profile/block/unblock
screens. A developer-only recorder may export a redacted structural fixture and video with consent; never
compile that recorder into release.
Capture states with unread messages and check both recipient unread counters and sender read receipts
before/after each proposed route. Repeat with notification grouping, hidden previews, missing phone number,
blocked state, slow network, background app changes and keyboard visibility. Re-run after connected messenger
UI/server-side variants change even when version code stays constant.
11.4 Selector safety contract
Match structure and provenance, not a global text search. A supported selector includes expected package,
screen class/signature, stable resource ID when exposed, bounded ancestor path, control role, visible/enabled
state and expected localised label. Sender-controlled names and profile descriptions are never control labels or
authority.
A selector must resolve to exactly one eligible target. Zero or multiple matches aborts that step. If IDs are absent,
a structural/localised-label route needs separate fixture coverage and must still exclude message/description
subtrees. No fuzzy coordinate clicking, OCR fallback, screen-image AI or shell input tap in production.
Maintain a continuous binding chain from exact-number profile to name-only confirmation dialog. A generic
confirmation dialog that could have come from another chat is not safe merely because its displayed name
matches. Every intervening window/event must match the expected transition; otherwise invalidate the chain
and re-resolve.
11.5 Adapter changes and safe degradation
Bundle only tested deterministic adapters in signed application updates. No downloaded code, remote selector
scripts or server kill-switch dependency. Unknown build/signature/layout switches mutation off. A known build
with an unknown screen may still allow observation of unrelated supported profiles, but must never advance a
changing action through the unknown screen.
Record a circuit-breaker error after one identity-binding or unexpected-dialog anomaly. Disable that adapter for
the session immediately and until an explicit compatibility check passes. A signed app update may introduce a
corrected adapter; perform shadow/observe-only checks before enabling it. Maintain a release support table
rather than “supports all connected messenger versions.”


## Source page 37

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
37
12. Automation state machine, race conditions and
recovery
12.1 Action flow
TEXT
QUEUED -> WAIT_SAFE_SESSION -> RESOLVE_NAMESPACE -> RESOLVE_TARGET
       -> VERIFY_LIVE_PROFILE -> EVALUATE_CURRENT_POLICY
       -> PREPARE_CONTROL -> VALIDATE_DIALOG -> DISPATCH_CONFIRM
       -> VERIFY_RESULT -> COMMIT_OBSERVATION -> COMPLETE
Any unexpected identity, window, policy, permission or Stop change:
       -> CANCEL / PAUSE / UNVERIFIED, never speculative continuation.
Block/unblock actions use different job kinds and authority checks. Already-blocked/unblocked live states are
idempotent completion only when the same target and expected result are verified. A cached blocked state may
prevent needless discovery work, but cannot establish current live UI success after a contradictory notification or
external change.
12.2 Per-state execution contract
State
Work
Exit condition / abort
WAIT_SAFE_SESSION
No clicks; retain desired work
All session preconditions true; deadline not
expired.
RESOLVE_NAMESPACE
Bind receiving account
Ambiguous account, switch menu or changed
identity aborts.
RESOLVE_TARGET
Navigate only an authorised adapter
route
Exact authoritative sender number obtained,
not guessed.
VERIFY_LIVE_PROFILE
Confirm
one-to-one/type/number/blocked
controls
Fresh structurally valid profile snapshot.
EVALUATE_CURRENT_PO
LICY
Read current immutable revision and
immediate veto
Decision is eligible, not stale.
PREPARE_CONTROL
Reacquire unique block/unblock entry
Package/window/session binding unchanged.
VALIDATE_DIALOG
Inspect target-bound prompt and
side-effect controls
Correct exact-bound action, no report/delete
ambiguity.
DISPATCH_CONFIRM
Final guard, one supported node action
Immediately move to verification, regardless
of boolean return.
VERIFY_RESULT
Observe supported postcondition on
same target
Verified desired state, timeout or ambiguity.
COMMIT_OBSERVATION
Transactionally record result if
generation current
Never overwrite newer user choice; preserve
forensic reason.
COMPLETE
Remove overlay/return only by
recognised safe route
No blind Back taps after user navigation.


## Source page 38

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
38
A successful performAction return does not prove connected messenger persisted a block. Verification is a separate product
requirement. connected messenger's exposed block/unblock flow is the operation being automated, not a public third-party
blocking API. [S2, S3]
12.3 Numerical limits: initial defaults
Limit
Value
Required response on breach
Accessibility-event
debounce
250 ms
Coalesce duplicate observation requests;
never defer Stop.
Stable expected-state
window
400 ms
Useful filter only, not proof that the user is
idle.
Structural node budget
250 nodes, depth 14
Abstain if needed structure exceeds budget;
do not scan message subtrees.
Captured text from a
profile/control node
160 code points; phone field 64
Truncate display only; reject truncated
identity.
Transition timeout
2.5 seconds
Stop advancing; reacquire/verify from known
state.
Mutation attempt timeout
8 seconds
Record UNVERIFIED or WAITING, no blind
repeated confirmation.
Explicit batch
25 seconds or 5 mutations
End safely; remaining work stays pending.
Retries after a transient
failure
Up to 2 additional attempts, only in
later eligible opportunities
Then FAILED with one explicit recovery
action.
Retry backoff
Earliest 30 seconds, then 5 minutes
Event-driven eligibility; no exact alarm/wake
to retry.
Consecutive unexpected
structure/binding error
1
Disarm adapter/session; compatibility check
required.
Timing values are engineering defaults requiring measurement, not validated connected messenger characteristics. Do not
shorten them to conceal slow or unreliable integration. Never use fixed sleeps followed by coordinates as the
action engine.
12.4 Toggle-versus-click race
The UI command handler first writes an in-memory immediate ALLOW veto on the main thread, increments the
session policy generation and cancels that target's pending dispatcher work. It then schedules the durable SQLite
transaction. The dispatcher checks vetoes and current revision immediately before handing an action to Android.
There remains an unavoidable boundary once a click has already been dispatched to another app. A later user
action cannot recall it. If Enable arrives after that point, verify the result, then execute the explicitly authorised
unblock as compensation when safe. Report the transient block and do not imply missed messages are
recoverable. Tests must place the toggle on both sides of this boundary.
A database failure after an ALLOW veto disarms automation globally until a successful save/reload; it must not
silently drop the veto and continue blocking. On process death, only durably committed policy survives. Do not
claim persistence for a transaction that never committed.


## Source page 39

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
39
12.5 User interference and return navigation
Stop invalidates the session generation immediately and removes any queued future click. Unexpected window
changes, package changes, keyboard/composition UI, calls, pickers, overlays, account switches, visible navigation
or unexpected profile identity abort the macro. onInterrupt is not a universal signal for every user touch.
Do not intercept all touches or disable the user's controls. The Stop overlay must be available without an
unrelated overlay permission. If a safe non-obscuring position is unavailable, stop rather than covering a
confirmation. On cancellation, do not press Back to “clean up” a screen that the user may already have changed.
Recovery starts with observation and fresh target resolution. Never persist a next-button position,
AccessibilityNodeInfo, delayed callback or assumed screen as a restart checkpoint.
12.6 Error taxonomy and copy
Internal reason
User outcome
Retry policy
POLICY_NOT_READY /
CONSENT_MISSING
Setup needed; no action
After valid load/consent only
LOCKED / BACKGROUND /
USER_INTERRUPTED
Waiting for a safe connected messenger session
On next eligible opportunity
IDENTITY_MISSING /
IDENTITY_CONFLICT
Number could not be verified
Explicit inspect; conflict disarms session
UNSUPPORTED_BUILD /
SCREEN_CHANGED
Blocking paused for this version/screen
Supported adapter/check required
REPORT_OR_DELETE_RISK
This blocking screen is not supported
Never auto-retry the same unsafe dialog
TRANSITION_TIMEOUT
Action could not finish
Verify first; bounded later retry
RESULT_UNVERIFIED
Action sent; result not verified
Reinspect only before any repeated mutation
DB_FULL / DB_CORRUPT /
SAVE_FAILED
Choices could not be saved; blocking
paused
Repair/save; no silent destructive reset
ACCOUNT_CHANGED
Set up the current connected messenger account
Explicit rebind; old namespace not reused
PERMISSION_REVOKED
Screen access is off
User controls Android settings
Notifications/overlay language must describe the next useful action, not expose an exception stack trace. No
repeated toast per event. Successful blocks remain quiet.


## Source page 40

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
40
13. Database, persistence and migration contract
13.1 Durable and ephemeral data
Persist exact-number user choices, minimal account observations, explicit command authority, queued work,
consent versions and bounded outcomes. Keep unbound notification hints in memory only: maximum 256, at
most 24 hours, oldest evicted first. Do not invent a phone row from an ambiguous title.
Record
Retention
ALLOW and DENY_MANUAL choices
Until explicit user change/clear-data; no scheduled expiry.
Discovered official businesses and
their last outcome
Retain locally; no rule expiry.
Ordinary DEFAULT cache records
Freshness 30 days; remove after 180 days unseen; target cache cap 10,000
using least-recently-seen eviction.
Unresolved optional flags
Clear promotion after 7 days; keep explicit user choices; suppress repeat
promotion according to review rules.
Local action events
At most 5,000 records or 30 days, whichever retains less.
Aggregate UX/coverage counters
At most 62 days, local only; no account IDs or message data.
Consent record
Current consent plus bounded previous versions needed to explain active
authority.
Live UI nodes, text and notification
objects
Only for the current callback/step; never durable storage.
Cache eviction never deletes explicit user choices or authorises blocking. A hard storage limit must pause new
work and show a storage issue, not silently drop allowlist entries. Account counts shown in the UI reflect retained
records, not every person ever contacted.
13.2 Schema version 2
The following is executable SQLite DDL. Enum CHECK constraints are intentional. Android code enables foreign
keys and WAL through the helper configuration, and uses bound arguments for all values. Store wall-clock UTC
milliseconds for display/audit; use elapsed monotonic time only within live sessions.


## Source page 41

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
41
SQL
PRAGMA foreign_keys = ON;
CREATE TABLE app_state (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  install_id TEXT NOT NULL,
  mode TEXT NOT NULL DEFAULT 'OFF'
    CHECK (mode IN ('OFF','ON','PAUSED')),
  policy_revision INTEGER NOT NULL DEFAULT 0,
  emergency_disarmed INTEGER NOT NULL DEFAULT 1
    CHECK (emergency_disarmed IN (0,1)),
  schema_version INTEGER NOT NULL DEFAULT 2
);
CREATE TABLE namespace (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  receiver_phone TEXT NOT NULL,
  profile_install_key TEXT NOT NULL,
  created_ms INTEGER NOT NULL,
  verified_ms INTEGER,
  active INTEGER NOT NULL DEFAULT 0 CHECK (active IN (0,1)),
  UNIQUE(receiver_phone, profile_install_key)
);
CREATE UNIQUE INDEX only_one_active_namespace
  ON namespace(active) WHERE active = 1;
CREATE TABLE account (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  namespace_id INTEGER NOT NULL REFERENCES namespace(id),
  phone TEXT NOT NULL,
  name TEXT,
SQL · CONTINUED
  search_name TEXT NOT NULL DEFAULT '',
  search_digits TEXT NOT NULL,
  observed_type TEXT NOT NULL DEFAULT 'UNKNOWN'
    CHECK (observed_type IN ('UNKNOWN','BUSINESS_CONFIRMED',
      'REGULAR_PROFILE_OBSERVED','AMBIGUOUS')),
  choice TEXT NOT NULL DEFAULT 'DEFAULT'
    CHECK (choice IN ('DEFAULT','ALLOW','DENY_MANUAL')),
  choice_revision INTEGER NOT NULL DEFAULT 0,
  managed_kind TEXT NOT NULL DEFAULT 'UNRESOLVED'
    CHECK (managed_kind IN ('OFFICIAL','USER_FLAGGED',
      'PREENABLED','PERSONAL','UNRESOLVED')),
  block_observed TEXT NOT NULL DEFAULT 'UNKNOWN'
    CHECK (block_observed IN ('UNKNOWN','BLOCKED','UNBLOCKED')),
  type_evidence TEXT,
  type_verified_ms INTEGER,
  type_fresh_until_ms INTEGER,
  block_verified_ms INTEGER,
  first_seen_ms INTEGER NOT NULL,
  last_seen_ms INTEGER NOT NULL,
  adapter_id TEXT,
  observed_build TEXT,
  observed_locale TEXT,
  observation_generation INTEGER NOT NULL DEFAULT 0,
  UNIQUE(namespace_id, phone)
);
CREATE INDEX account_policy
  ON account(namespace_id, choice, observed_type);
CREATE INDEX account_search ON account(namespace_id, search_name);
CREATE TABLE authority (


## Source page 42

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
42
SQL · CONTINUED
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id INTEGER NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK (kind IN ('MANUAL_BLOCK','UNBLOCK')),
  policy_revision INTEGER NOT NULL,
  command_id TEXT NOT NULL UNIQUE,
  created_ms INTEGER NOT NULL,
  expires_ms INTEGER,
  consumed_ms INTEGER,
  revoked_ms INTEGER
);
CREATE TABLE action_job (
  account_id INTEGER PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK (kind IN ('INSPECT','BLOCK','UNBLOCK')),
  state TEXT NOT NULL CHECK (state IN ('QUEUED','WAITING','RESOLVING',
    'READY','APPLYING','VERIFYING','VERIFIED','UNVERIFIED','FAILED',
    'CANCELLED','PAUSED')),
  generation INTEGER NOT NULL,
  expected_policy_revision INTEGER NOT NULL,
  authority_id INTEGER REFERENCES authority(id),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  next_eligible_ms INTEGER,
  created_ms INTEGER NOT NULL,
  updated_ms INTEGER NOT NULL,
  last_error TEXT
);
CREATE TABLE review_flag (
  account_id INTEGER PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK (kind IN ('NEW_SENDER',
SQL · CONTINUED
    'POSSIBLE_COMMERCIAL','TYPE_CHANGED')),
  reason_mask INTEGER NOT NULL DEFAULT 0,
  detector_version INTEGER,
  created_ms INTEGER NOT NULL,
  expires_ms INTEGER NOT NULL,
  snoozed_until_ms INTEGER,
  last_promoted_ms INTEGER,
  CHECK(reason_mask >= 0 AND reason_mask <= 255)
);
CREATE TABLE consent (
  kind TEXT PRIMARY KEY CHECK (kind IN ('ACCESSIBILITY',
    'DISCOVERY','SALES_HINTS','RULE_ACTIVATION')),
  version INTEGER NOT NULL,
  granted INTEGER NOT NULL CHECK (granted IN (0,1)),
  changed_ms INTEGER NOT NULL
);
CREATE TABLE action_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id INTEGER REFERENCES account(id) ON DELETE SET NULL,
  command_id TEXT,
  kind TEXT NOT NULL,
  outcome TEXT NOT NULL,
  reason_code TEXT NOT NULL,
  event_ms INTEGER NOT NULL,
  adapter_id TEXT,
  generation INTEGER,
  policy_revision INTEGER
);


## Source page 43

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
43
SQL · CONTINUED
CREATE INDEX event_time ON action_event(event_ms);
CREATE TABLE daily_metrics (
  day_utc TEXT PRIMARY KEY,
  app_attention_ms INTEGER NOT NULL DEFAULT 0,
  foreground_automation_ms INTEGER NOT NULL DEFAULT 0,
  decision_count INTEGER NOT NULL DEFAULT 0,
  blocked_verified_count INTEGER NOT NULL DEFAULT 0,
  discovered_business_count INTEGER NOT NULL DEFAULT 0,
  pending_count INTEGER NOT NULL DEFAULT 0,
  interruption_count INTEGER NOT NULL DEFAULT 0
);
PRAGMA user_version = 2;
managed_kind determines presentation only and must never substitute for live observed type or authority.
Validate phone syntax in the repository before INSERT/UPDATE. The unique key and all foreign keys are
checked in tests. No raw JSON payload column is allowed to become a route for storing full notifications or
message text.
13.3 Critical transactions
Enable/Keep: set immediate veto on main thread; begin transaction; validate namespace/account; increment
global and row policy revision; write ALLOW; revoke old authorities; cancel block work; clear review; create a
single unblock authority only when the user explicitly requested it; upsert UNBLOCK job; append reason-coded
event; commit; publish snapshot. A Keep of an unblocked/new sender need not enqueue any unblock job.
Manual Block: capture a fresh user confirmation command ID; begin transaction; set
DENY_MANUAL/USER_FLAGGED and revision; create durable MANUAL_BLOCK authority for that exact
revision; upsert BLOCK job; append event; commit. The authority is revoked when the user changes policy. It has
no automatic expiry while that explicit deny policy remains unchanged.
Automatic block queue: only after a supported fresh business observation; verify DEFAULT or appropriate deny
state and current rule mode; upsert one current job; never create manual authority. Repeated identical
observations do not multiply jobs.
Record result: confirm matching account key, action generation and expected revision. Record the observed
external result separately from current desired policy. A stale completion may add an audit event but must not
mark a newer opposite job complete or overwrite ALLOW. Consume a one-shot unblock authority only after
matching completion/reconciliation.
Unblock commands expire seven days after creation unless consumed/revoked sooner. The ALLOW choice does
not expire. An expired pending command shows “Enabled; tap to finish unblocking.” This avoids an old unused
instruction undoing a much later external block. All authority expiry is checked again at dispatch.
13.4 Migrations and recovery
Use ordered, transactional migrations, with fixtures for each shipped version. From the earlier conceptual
schema, map known business rows to account records, preserve exact-number choices and namespace, but set
observed states to UNKNOWN and cancel in-flight work. Map any earlier PERSONAL_VERIFIED value to
REGULAR_PROFILE_OBSERVED with expired freshness, not an immortal exemption. If identity provenance is
missing, migrate it to an unresolved review record and do not mutate from it.
No destructive fallback on migration failure. Pause, keep a private copy of the existing database and offer a local
recovery explanation. Never export that copy automatically. Startup detects unfinished APPLYING/VERIFYING
jobs and changes them to UNVERIFIED/inspection-required; it does not replay confirmation clicks.
Use app-private credential-encrypted storage, not direct-boot device-protected storage for the policy database.
Create a random installation marker in getNoBackupFilesDir; if it is missing/mismatched relative to the database,


## Source page 44

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
44
disarm and require namespace/consent review. This is a restore safeguard, not proof against every OEM transfer
behaviour. Android backup settings have OEM/device-transfer nuances, so test exclusions explicitly. [S8, S16]


## Source page 45

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
45
14. Security, privacy and lifecycle
14.1 Permission inventory
Permission/capability
Decision and rationale
Accessibility binding
Required for supported UI automation; explicit Android user enablement.
Notification-listener binding
Optional discovery; package-first filtering.
POST_NOTIFICATIONS
Optional, only the app's own notices on applicable Android versions.
Package visibility
Narrow <queries> entry for qualified.target.package; no QUERY_ALL_PACKAGES.
INTERNET
Not declared. No backend, ads, remote recipes or analytics.
READ_CONTACTS / SMS / phone /
call log
Not declared. No address-book scan or telephony blocking.
Camera / microphone / location /
external storage
Not declared. No media classification or file scraping.
SYSTEM_ALERT_WINDOW / device
admin / root
Not declared. Use only a scoped accessibility session overlay.
Gesture injection / touch exploration /
screenshots
Not requested for this utility. No coordinate/OCR fallback.
Wake lock / exact alarm / battery
exemption
Not requested in V1; event-driven behaviour only.
Service binding permissions are not permissions the app can grant itself. Android settings remain user-controlled.
Do not instruct users to disable Play Protect or other security checks to make the integration work.
14.2 Threat model
Threat
Mandatory control
Business name says “Block” or
“Business account.”
Never treat sender-controlled text as an account-type/control marker.
Duplicate names, cloned branding or
reused avatar
Full-number identity and namespace; no brand trust transfer.
Notification spoofs a phone number or
conversation title
Hint-only until profile provenance/binding succeeds.
Group notification points to one
participant
Require one-to-one profile; group contexts never mutate.
Malicious content tries to instruct the
app
No LLM or arbitrary instruction interpreter; static data-only parser.
Another app exposes similar
UI/package text
Actual package/window/installed-signature checks, not visual appearance.
UI changes after node discovery
Fresh selector and binding checks at every changing action.


## Source page 46

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
46
Threat
Mandatory control
User taps Enable during action
Immediate veto, revision checks and post-dispatch compensation handling.
Database or intent injection
Bound SQL; internal components not exported; no implicit mutation intents.
Notification or diagnostic exfiltration
No network permission; do not serialise raw objects; diagnostics exclude
numbers/content.
Local data extraction via backup
Exclude app data from backup/transfer; restore marker disarms authority.
Accessibility permissions abuse
Narrow package processing, visible operation, Stop, false tool declaration
prohibited.
No Internet permission reduces the app's own network surface but does not alone prove privacy: exported
components, external intents, logs, backups, crash tools and included SDKs also require audit. Support links open
only after a user action and contain no phone or message data.
14.3 Storage and privacy-policy wording
The product stores numbers, optional observed names, choices, limited classification metadata and operation
results in its private database. It does not claim SQLCipher-style application-level encryption. Platform
sandbox/storage protections apply; rooted/compromised devices and unlocked-device access are outside any
absolute confidentiality guarantee. [S8]
The privacy policy must say exactly what Accessibility can expose, what is retained, what optional sales hints
process transiently, that nothing is uploaded by the app, that local data is removed through clear-data/uninstall,
and that connected messenger and Google Play have separate data/payment practices. Do not say “never reads messages”
when the optional hint feature reads notification text.
Google Play's Data safety answers must match the release binary and actual flows, including diagnostic sharing
initiated by a user. On-device processing and the absence of a backend do not waive the need to review the
disclosure definitions. [S19]
14.4 Lifecycle behaviours
Event
Required behaviour
Cold start / service connected before
UI
Disarmed until repository, consent, namespace and compatibility checks finish.
Activity destroyed/rotated
Preserve search/anchor/expanded row; do not restart a batch from UI
recreation.
Service disconnected or killed
Invalidate session; no assertion that queued jobs completed.
Force-stop
Respect it; no watchdog or auto-restart workaround.
Reboot
No UI automation until unlocked and freshly validated; no saved-screen
continuation.
connected messenger updates while process alive
Invalidate all screen bindings; reload exact adapter compatibility.
App updates
Migrate transactionally, preserve preferences, invalidate in-flight generation.
connected messenger changes receiving account
Disarm; establish new namespace; never copy exceptions silently.
User changes sender phone/name
New phone = new identity; name alone does not alter exact-number authority.


## Source page 47

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
47
Event
Required behaviour
Backup/device transfer/reinstall
Disarm on missing installation marker; explicit setup required.
Local data cleared
Global rule off, consent reset, cache/choices removed; no connected messenger unblock
loop.
User uninstalls
App cannot execute future work; it must not attempt to remove prior
connected messenger blocks beforehand.
A private compatibility issue may be shown on the main page even when notification permission is denied. The
product must never display a cached green state without checking current permission and service readiness
when its UI resumes.
14.5 Support data
A Copy diagnostics action may produce only app version, Android API, OEM/model, connected messenger version, adapter
ID, consent booleans, coarse counts and reason codes. It must exclude names, phone numbers, sender IDs,
account namespace numbers, notification text and accessibility trees. Show the exact text before sharing.
User-controlled sharing through another app is explicit and does not justify automatic uploads.
Never request personal chat dumps to diagnose a selector problem. Reproduce it using developer-owned test
accounts. The UI may direct a user to a public compatibility page via the system browser; a static web page is not
an app backend and may have its own hosting logs, which the privacy notice should distinguish.


## Source page 48

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
48
15. Build configuration and implementation
sequence
15.1 Pinned toolchain
Use Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17, Android SDK platform 36 and Build Tools 35.0.0 as a
Java-only reproducibility baseline. The official compatibility page supports this pairing; this is not a claim that it is
the newest available toolchain. [S13]
Pin versions, commit the Gradle wrapper and its published distribution SHA-256, and avoid dynamic dependency
versions. Generate/verify the wrapper against the official Gradle distribution rather than copying an unverified
wrapper JAR. Recheck tool security updates and submission requirements before release. A deliberate future
upgrade must run the full compatibility suite.
15.2 Gradle and project setup
GROOVY
// settings.gradle
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = 'BusinessGate'
include ':app'
// root build.gradle
plugins { id 'com.android.application' version '8.13.2' apply false }
// app/build.gradle
plugins { id 'com.android.application' }
android {
    namespace 'com.example.businessgate'
    compileSdk 36
    buildToolsVersion '35.0.0'
    defaultConfig {
        applicationId 'com.example.businessgate'
        minSdk 29
        targetSdk 36
        versionCode 1
        versionName '0.1.0'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    buildTypes {
GROOVY · CONTINUED
        debug { applicationIdSuffix '.debug' }
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile(
                'proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
dependencies { /* No third-party release runtime libraries. */ }


## Source page 49

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
49
Do not add Kotlin/Compose or an AndroidX UI template inadvertently. A plain Java Activity and framework XML
layout are sufficient. Inspect releaseRuntimeClasspath and the packaged DEX/manifest to verify the absence of
bundled SDKs; an empty dependencies block alone is not a complete audit.
15.3 Manifest baseline
XML
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <queries><package android:name="qualified.target.package" /></queries>
  <application
      android:name=".GateApplication"
      android:label="@string/app_name"
      android:theme="@style/GateTheme"
      android:allowBackup="false"
      android:fullBackupContent="@xml/backup_rules"
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:usesCleartextTraffic="false">
    <activity android:name=".ui.MainActivity"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    <service android:name=".service.GateAccessibilityService"
        android:exported="true"
        android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
      <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
      </intent-filter>
      <meta-data android:name="android.accessibilityservice"
          android:resource="@xml/gate_accessibility" />
    </service>
    <service android:name=".service.GateNotificationListener"
        android:exported="true"
        android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
XML · CONTINUED
      <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
      </intent-filter>
    </service>
  </application>
</manifest>
Only the launcher and protected system-bound services are exported. The Activity ignores untrusted extras
asking for a mutation. No exported receiver/service accepts “block this number” from another app. Own
notification PendingIntents are immutable, explicit and carry only an internal navigation hint; fresh user authority
is checked after opening the app.
15.4 Service and backup resources


## Source page 50

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
50
XML
<!-- res/xml/gate_accessibility.xml -->
<accessibility-service
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:description="@string/accessibility_description"
  android:packageNames="qualified.target.package"
  android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewScrol
    led|typeViewClicked|typeWindowsChanged"
  android:accessibilityFeedbackType="feedbackGeneric"
  android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
  android:notificationTimeout="250"
  android:canRetrieveWindowContent="true"
  android:canPerformGestures="false"
  android:isAccessibilityTool="false" />
Use other-window metadata only to detect unsafe focus/IME/overlay conditions, not to inspect other apps' text.
Package filtering does not replace a fresh window/package check before each action. The base
AccessibilityService contract and configuration guide are the implementation references. [S1, S2]
XML
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
  <exclude domain="root" path="." />
  <exclude domain="database" path="." />
  <exclude domain="sharedpref" path="." />
  <exclude domain="file" path="." />
  <exclude domain="external" path="." />
</full-backup-content>
<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
  <cloud-backup>
    <exclude domain="root" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="file" path="." />
    <exclude domain="external" path="." />
  </cloud-backup>
  <device-transfer>
    <exclude domain="root" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="file" path="." />
    <exclude domain="external" path="." />
  </device-transfer>
</data-extraction-rules>
No sensitive data is placed in device-protected storage. Test actual cloud/device transfer on supported OS/OEM
versions; future transport formats require review rather than assuming these declarations cover them forever.
The installation marker remains a defence if restored data is unexpectedly present. [S16]
15.5 Build and audit commands
SH
# Android SDK tools on the developer machine; never required on a customer phone.
sdkmanager 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
./gradlew --version
./gradlew clean :app:lintDebug :app:assembleDebug
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
./gradlew :app:assembleRelease :app:bundleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
Configure release signing through an uncommitted local/CI secret configuration and Play App Signing; never
embed passwords or keystores in the repository. The signing setup, application ID, Play identity verification,


## Source page 51

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
51
actual support contact and privacy-policy URL must be filled with the publisher's real values. These are
owner-supplied credentials, not product design choices to invent.
Inspect the release APK/AAB with Android tooling: permissions, components, network permission absence,
native-library absence, dex library inventory, debug flags and string resources. Install the release-signed variant
on real devices; debug success is not sufficient. The APK's business adapter registry must fail closed if no
measured supported build is installed.
15.6 Work packages and definition of done
Package
Owner role
Implementation deliverables
Exit evidence
W0: integration
proof
Android lead + QA
Production-declaration probe;
exact adapter measurement
record; safe
block/unblock/video.
G1-G4 pass on at least one
complete supported
combination.
W1: core policy
Android developer
Pure Java rule engine,
canonicalisation, hint engine,
exhaustive tests.
No automatic
personal/unknown/suspect
block; explicit-authority cases
correct.
W2: local storage
Android developer
Schema/helper, repository
transactions, migration/restart
logic.
Constraint, idempotence, crash
and restore tests.
W3: one-page UI
Android developer +
designer
Native layouts, tokens,
sections/search, dialogs and all
states.
200% text, TalkBack, large list
and interaction tests.
W4: execution
Android lead
Adapter, controller, live
guards, overlay, verification
and circuit breaker.
Full transition/race/side-effect
suite.
W5:
discovery/privacy
Android developer +
security reviewer
Optional listener/hints,
bounded retention, disclosure
and privacy audit.
No text persistence/network/
extraneous access; hint tests.
W6: realistic pilot
Product + QA
30-day measured use,
attention/coverage report,
compatibility incidence.
G5; no hidden manual burden
or wrong-person block.
W7: paid release
Publisher + policy/legal
reviewer
Listing, price, consent/video
declaration, policy review,
support/refund process.
G6 and signed release
checklist.
W1-W3 can proceed while W0 is investigated. Do not invest in a public sales claim or treat W4 as routine glue
before W0 succeeds. Every work package has evidence; a checkbox asserting “done” without a tested build is
insufficient.


## Source page 52

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
52
16. Test strategy, edge-case catalogue and
acceptance criteria
16.1 Test layers and evidence
Use four layers. Pure Java tests cover policy, normalisation and sales features without Android. SQLite tests
execute the real schema, constraints, migrations and transaction outcomes. A developer-owned fake UI app tests
controller transitions and interruptions without contacting real businesses. Physical-phone tests validate actual
connected messenger semantics, access restrictions, identity, unread effects and block outcomes.
A fake adapter is available only in a debug/test build and accepts only a separate development fixture package. It
must not be compiled as a release fallback for arbitrary apps. Real connected messenger tests use consenting controlled
senders, not mass blocking or unsolicited messages to real businesses.
For each test record app version, adapter/build/locale, device/API, initial account policy, starting screen, action
sequence, expected state, actual state, side effects, timing and fixture/video reference. Do not substitute a
successful click event for verified outcome. Any wrong-person block, report, deletion or message send is a
release-stopping incident.
16.2 Device and configuration matrix
Minimum coverage: one reference Pixel-class device on API 36; one Samsung device with its supported current
Android version; one supported OEM with aggressive process management; one older API 29/30 device; and a
newer OS combination before advertising support there. “Current” device/connected messenger builds are recorded during
testing, not assumed in this PRD.
Cross the supported targets with light/dark theme; 100%/150%/200% font scale; default/large display scale;
gesture/three-button navigation; screen lock; app switch; rotation; notification previews on/off; optional
permissions denied; slow/no network; and TalkBack. Each supported connected messenger language/layout needs distinct
profile/dialog fixtures. Cover ordinary users, business accounts, same-name senders, groups, self-chat,
archived/locked contexts and manual external blocks.
Low-RAM Android Q and work-profile notification listener limitations are explicit cases; the listener may not be
available there. A limited discovery mode may still run, but its attention/coverage claims must be measured
separately. [S4]
16.3 Account, policy and identity tests
ID
Setup / trigger
Required result
T001
Confirmed business, DEFAULT, fresh exact
safe profile
BLOCK intent, verified only after postcondition.
T002
Same case but ALLOW
No automatic block at any phase.
T003
UNKNOWN sender with no badge/profile
completeness
No automatic block.
T004
Complete regular profile, DEFAULT
Cache observation; no block or prompted review.
T005
Ambiguous/incomplete profile
UNKNOWN/AMBIGUOUS; no click.
T006
Same name as enabled company, different
number
New independent identity; no permission inheritance.


## Source page 53

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
53
ID
Setup / trigger
Required result
T007
Two accounts share the same last four digits
No merge; full numbers distinguish.
T008
Sender name contains “Business account”
No official type classification from name.
T009
Message/profile description contains “Block”
No control click from sender text.
T010
Phone-looking text appears in message
Not accepted as account identity.
T011
Profile lacks number or exposes username
only
Unsupported identity; no block.
T012
National-format number with no country code
No inferred canonical key; request full identity.
T013
Bidi/hidden controls inside number field
Reject binding.
T014
Same name, different group and individual
Only verified one-to-one profile eligible.
T015
Stale personal cache expires
No block/forced chat opening; natural recheck only.
T016
Kept personal number converts to business
ALLOW remains; show under Enabled when confirmed.
T017
DEFAULT cached regular profile becomes
confirmed business
Fresh official evidence enables normal default-deny; show
type change.
T018
Manual-denied regular sender appears again
Reblock exact number only with current manual authority.
T019
Manual-denied sender changes normal
number
Independent review only; no inherited denial.
T020
Enabled number manually blocked in
connected messenger later
Do not auto-unblock from ALLOW alone.
T021
Denied official business manually unblocked
externally
Reapply fixed rule on next eligible observation.
T022
Reassigned number matches a durable rule
Exact rule remains; no unverifiable owner inference.
T023
Incoming account is a friend using Business
app
Rule applies unless enabled; setup copy covers this.
T024
Group/channel/community/self-chat/locked
context
Ignore mutation, even if a business name appears.
16.4 Sales hints and new-person tests
ID
Setup / trigger
Required result
T025
Sales hints off, readable message notification
Body not analysed; core discovery still works.
T026
New personal sender says hello only
Neutral collapsed NEW_SENDER, no notice/block.
T027
Three required feature groups meet threshold
POSSIBLE_COMMERCIAL reason bits only.
T028
One keyword repeated 20 times
One feature group; no inflated score.
T029
URL and price without sales/CTA combination
No promoted flag.


## Source page 54

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
54
ID
Setup / trigger
Required result
T030
OTP context with digits and a link
No sales flag; no code retained.
T031
Delivery/appointment update without sales
evidence
No promoted flag.
T032
Supported forwarded indicator on promotional
text
Suppress promoted hint.
T033
Known kept friend sends promotional wording
No promoted hint; explicit ALLOW remains.
T034
Notification is a group summary
No per-person flag or binding.
T035
Notification hides/redacts text
No guess/reconstruction.
T036
Unsupported-language commercial wording
Detector abstains; no translation dependency.
T037
Notification has duplicate display names
Separate unresolved candidates; no wrong-number flag.
T038
User taps Keep
ALLOW, cleared promotion, no repeated prompt.
T039
User taps Block then Cancel
No policy change or job.
T040
User confirms Block number
DENY_MANUAL with exact authority; type is not falsified.
T041
User ignores all flags
All uncertain personal accounts remain reachable.
T042
100 suspect senders arrive
Bounded queue/promotion; no automatic overflow
blocking.
T043
User revokes sales-hint consent
Stop parsing; clear unreviewed feature metadata; retain
explicit choices.
T044
Same notification is reposted repeatedly
No duplicate job or repeated review notice.
16.5 Automation, interruption and race tests
ID
Setup / trigger
Required result
T045
Phone locked or display off
No click, unlock or wake scan.
T046
Another app foreground
Queue only; no connected messenger takeover.
T047
Unsupported connected messenger build/signature
Registry rejects mutations.
T048
Same build, new server-side UI variant
Structural checks fail safely.
T049
Selector resolves to zero or two controls
No guessed click.
T050
Combined Block-and-report screen
Abort unless a separately tested no-report route exists.
T051
Report/delete checkbox unexpectedly
checked
No confirmation; stop unsafe flow.
T052
Name-only dialog without uninterrupted exact
binding
Abort.
T053
User changes chat while profile is resolving
Generation invalidated; wrong chat untouched.


## Source page 55

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
55
ID
Setup / trigger
Required result
T054
User opens keyboard, call or picker
Stop changing actions.
T055
Stop pressed before final dispatcher
No new confirm action.
T056
Enable before dispatcher linearisation
Immediate veto prevents block.
T057
Enable after an already dispatched block
Verify and compensate via explicit unblock; truthful
transient status.
T058
Policy save fails after Enable tap
Global disarm; no new blocks.
T059
Process dies after block click before
verification
Restart at inspect/UNVERIFIED, no blind duplicate click.
T060
performAction returns true but UI does not
change
Do not show Blocked.
T061
Result screen belongs to another number
No success; identity incident.
T062
Live target already blocked
Verify/idempotent completion, no repeat click.
T063
Live target already unblocked for an unblock
command
Verify/idempotent completion, consume matched
command.
T064
User navigates after Stop
No cleanup Back taps over their new screen.
T065
Session exceeds budget
End safely, leave queue truthful.
T066
Receiver changes connected messenger account
mid-session
Abort and disarm namespace.
T067
Muted sender has no available notification
No discovery guarantee; passive/explicit routes only.
T068
Unread personal chat in an automatic scan
path
Do not open through read-affecting route.
T069
User authorises read-affecting scan
Scope/side effects disclosed; no send/delete/report.
T070
Inbox rows reorder during scanning
Reacquire candidate; dedupe by verified exact key.
T071
Slow network or delayed block UI
Bounded wait; verify before retry; no guessed success.
T072
Unblock authority expires after seven days
ALLOW persists; no late automatic unblock.
16.6 Storage, privacy, lifecycle and UI tests
ID
Setup / trigger
Required result
T073
Duplicate account insert
Unique namespace/phone key prevents duplicate durable
row.
T074
Namespace A and B contain same phone
Choices remain isolated; only one active namespace.
T075
Invalid enum/foreign key
SQLite rejects it.
T076
Toggle and stale completion interleave
Newer choice/job not overwritten.


## Source page 56

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
56
ID
Setup / trigger
Required result
T077
Disk full or migration error
Disarm; no destructive reset or discarded allowlist.
T078
Reboot/force-stop/service disconnect
No invisible resurrection or saved-screen continuation.
T079
Device restore with mismatched installation
marker
Setup/namespace review before authority.
T080
Clear local data
Rule off, local data removed; no connected messenger mass-unblock.
T081
Notification from another app
Package check returns before body/extras analysis.
T082
Inspect release files/logs after sales tests
No message text, OTP, screenshot or raw tree retained.
T083
Inspect release manifest/dependencies
No Internet/contact/storage/gesture capability or
unwanted SDK.
T084
Own notification permission denied
Core UI/Stop work; no repeated permission prompt.
T085
Search across Enabled, Not enabled, Review,
People
Correct section order and exact-number result identity.
T086
Toggle while filtered/scrolled
Search/anchor retained; correct row moves without wrong
listener binding.
T087
10,000 synthetic records
Search target met; scrolling remains usable.
T088
200% font scale, landscape, small viewport
No clipped text/actions; one scrollable column.
T089
TalkBack on, row recycled/moved
Correct name/number/state and stable focus behaviour.
T090
Dark theme and contrast checks
Text/control contrast meets targets; colour not sole cue.
T091
Repeated optional flags or compatibility
events
Digest/incident caps; no nag loop.
T092
App closes during setup
No accidental rule activation; resume correctly.
T093
Release/debug differences
Fixture recorder and fake adapter absent from release.
T094
External intent attempts “block number”
command
No untrusted mutation authority.
T095
Clock/time-zone change
No expired in-session authority resurrected; timestamps
readable.
T096
Pilot normal and high-volume months
Attention, queue coverage and screen waiting reported
honestly.
16.7 Quantitative engineering targets
Area
Proposed target / measurement
Safety
Zero wrong-number, enabled-number or unauthorised personal blocks in the
complete matrix; any incident stops release. This is not a statistical proof of
zero future failures.


## Source page 57

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
57
Area
Proposed target / measurement
Eligible execution
At least 99% verified intended results across 1,000 controlled eligible
attempts; classify all failures and report overall coverage separately.
Discovery coverage
At least 90% of official business senders actually received in the supported
pilot are discovered in a normal-use month; verify with consented ground
truth, not notifications alone.
End-to-end latency
Report first-message-to-verification separately from
eligible-session-to-verification; locked time cannot be hidden.
Search
p95 at most 100 ms for a completed query after debounce, 10,000 synthetic
records on the declared reference phone.
UI
No long database work in main-thread traces; smooth list scrolling;
accessibility traversal bounded and measured.
Storage
Under 20 MiB for 10,000 account records and capped history as a test target,
not a hard quota that deletes choices.
Binary
Aim for download APK under 5 MiB and installed app under 10 MiB, excluding
OS storage accounting variation.
Idle activity
Zero app-owned recurring polling alarms, wake locks or background scanning
when no relevant events occur.
Resource regression
Compare CPU, memory and battery traces against baseline on the same
workload; no sustained busy loop or runaway event processing.
A 250-node ceiling is a maximum, not a requirement to traverse all nodes in one frame. Measure callback latency,
extract only required paths and abort complex unsupported trees. Any engineering performance target that
conflicts with correct identity or verification yields to safety.
16.8 Mandatory traceability
BG-001/G1-G2 maps to T001-T024 and T045-T072. BG-002 maps to T085-T090. BG-003/BG-030 maps to
T025-T044. BG-004 maps to T081-T084/T093-T094. BG-010/G5 maps to T091/T096 and the measured pilot.
BG-020/BG-021 maps to T015-T022/T073-T080. G3 maps to T066/T074/T079. G4 maps to
T034-T037/T045-T046/T067-T070. G6 maps to the launch checklist in section 17.
The test suite is never considered complete solely because every row passed once on one phone. Add a
regression fixture for each incident and each newly supported connected messenger variant.


## Source page 58

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
58
17. Paid release, policy, maintenance and launch
decision
17.1 Pricing and distribution
Offer a paid download, not a free shell with an in-app unlock. No billing SDK or account system is required for the
paid-install business model. Configure US$1 as the US base price where the console accepts it and review
local-currency pricing/taxes in Play Console. Do not hard-code an assumed rupee conversion or claim every
country pays exactly the same amount.
Google's pricing guidance distinguishes paid/free status and notes that an app previously offered free cannot
later become paid under the same package. Use appropriate test tracks for testing and settle the public paid/free
status before launch. Refunds and purchase restoration follow the actual store/account flow and applicable
requirements; do not invent a custom server licence mechanism. [S18]
The app charges for this utility, not for access to connected messenger, its data or guaranteed future connected messenger
compatibility. App-name, trademark, terms and commercial-use review are mandatory owner tasks. No affiliation
with Meta or connected messenger should be implied.
17.2 Accessibility and platform approval
Google Play distinguishes narrow deterministic rule-based automation from prohibited autonomous
planning/decision/execution uses of Accessibility. This design is the former: fixed human-defined rules,
exact-number choices and measured screen scripts. Approval is not guaranteed. Declare
isAccessibilityTool=false, explain the data access prominently, demonstrate consent and actual behaviour,
and submit the required declaration/video. [S5]
connected messenger terms are a separate contract; Play approval is not connected messenger authorisation. Review the exact
proposed commercial automation and data handling against the applicable regional terms with qualified advice
before selling it. Do not state that all UI automation is automatically allowed, or that this PRD establishes a
ban-free operation. [S20]
Do not switch to an unofficial connected messenger API, modified client, root requirement or falsely declared accessibility
tool if approval/access fails. A narrowly described assisted utility might require a different product promise and a
fresh review; it is not a silent equivalent to the proposed default-deny product.
17.3 Listing copy boundaries
Acceptable draft: “Choose which connected messenger businesses you want. Business Gate applies your choices using
visible Android automation when supported connected messenger screens are accessible. Personal and uncertain accounts
are not automatically blocked.”
Required limitations appear before purchase: Android only; supported connected messenger versions/language/account
context; first messages can arrive; the phone must be unlocked for changing actions; optional hints are not proof;
block/unblock may affect transactional communications; no automatic guarantee against new personal-number
salespeople.
Do not publish “blocks every business instantly,” “never miss an important message,” “100% safe,” “works
forever,” “runs invisibly while locked,” “catches every new number” or “one minute per month” before the actual
applicable evidence exists. A later one-minute claim must describe the tested conditions and not imply a universal
maximum.


## Source page 59

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
59
17.4 Maintenance and support budget
There are no per-message server costs in this design, but connected messenger compatibility is an ongoing engineering cost.
A US$1 one-time price must be tested against support/refund/update effort. Track maintenance hours,
supported-build longevity, user-reported incidents and refund reasons without collecting private chat data.
Unknown updates pause actions rather than keeping an old script running. The app cannot obtain a remote fix
because it has no network service; corrected adapters arrive with signed app updates. Keep a public
compatibility/support page and clear in-app status. Do not promise a remote kill switch that the binary cannot
receive.
Routine users should not need to troubleshoot every connected messenger update. If updates create repeated
permission/compatibility work that breaks the attention target, this architecture fails the product requirement
even if refunds keep complaints low. Narrow supported configurations or redesign before broad release; do not
conceal the burden behind a permanently green badge.
17.5 Launch checklist
Item
Required evidence
Integration
Completed actual adapter manifest; G1-G4 records, physical-phone videos and
fixture hashes.
Personal safety
All unauthorised/identity tests pass; read-state tests prove the claimed
automatic route.
User effort
Completed 30-day attention/coverage report; normal and incident cohorts
visible; G5 passes for any advertised claim.
Privacy
Manifest/dependency/log/storage audit; truthful disclosures and privacy
policy; no production recorder.
Release quality
Release-signed binary tested, no fake adapter, versioned migrations,
crash/retry behaviour verified.
Store
Real publisher details, paid pricing, Data safety answers, accessibility
declaration and approval.
Legal/policy
Applicable connected messenger terms and branding review completed; no implied
affiliation.
Support
Working support destination, refund handling, compatibility page and incident
response owner.
Evidence wording
Store screenshots show real supported app states; no mockups presented as
real operation.
17.6 Go/no-go rule
Go only when the claimed support configuration can safely identify and block, preserve explicit exceptions,
disclose uncertain personal-number senders, meet the measured effort goal, and pass distribution review.
No-go when account identity/type is inaccessible, business dialogs cannot be safely isolated, personal unread
state is silently changed, normal phone use must repeatedly be interrupted, wrong-account actions occur, or
policy approval is not obtained. These are explicit gates, not implementation TODOs to ignore.
The PRD does not promise that all gates will pass. It provides the exact experiments, failure behaviour,
user-facing limits and implementation requirements needed to find out before customers are charged.


## Source page 60

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
60
18. Reference implementation and delivery
evidence
18.1 What the companion reference code covers
The companion pack contains a dependency-free Java policy evaluator, conservative phone canonicaliser,
bounded sales-hint classifier, executable SQLite schema, synthetic tests and the editable source of this PRD. It is
a reference core, not an Android APK and not a tested connected messenger adapter. Android service/Activity wiring must
implement the contracts in sections 10-15.
The policy evaluator distinguishes automatic business blocks, explicit manual blocks, explicit unblocks, waits and
no-ops. Its tests check that suspicion never becomes automatic authority and that current identity/session/policy
conditions are required. The hint engine's synthetic tests demonstrate the specified rules, not real-world
classification accuracy.
The schema reference is executed against SQLite and tested for duplicate identities, cross-namespace separation,
invalid enums, invalid foreign keys and the single-active-namespace constraint. Actual Android migrations,
encrypted-device storage behaviour, OEM lifecycle and connected messenger UI are not validated by those tests.
18.2 Reproduce the core tests
SH
cd reference
javac -d out GateCore.java GateCoreTest.java
java -cp out GateCoreTest
python3 schema_test.py
Executed reference checks on 9 September 2026: 140 Java assertions passed; 23 SQLite checks passed. These
are synthetic local-core/storage tests, not execution of the 96 Android/product acceptance scenarios.
No third-party Java or Python test package is required for these reference checks. The PDF is sufficient to
understand the requirements without running this companion pack. Test results reported in the delivery summary
refer only to the commands actually executed, not the entire acceptance catalogue.
18.3 Open integration evidence register
Unknown at document
delivery
Required owner action
Safe state until resolved
Exact current connected messenger
locators and signing
lineage
Capture the actual official supported
installation; complete adapter record.
No release adapter / no mutation.
Business marker
accessibility with false tool
declaration
Run production-declaration G1 probe
on supported phones.
Observe-only/unsupported.
Read-state-preserving
route
Compare unread/read receipts before
and after controlled scans.
No automatic unread-chat opening.
Safe current-profile
automation under user
interference
Run interruption/race matrix with real
UI.
Only explicitly started validated sessions.


## Source page 61

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
61
Unknown at document
delivery
Required owner action
Safe state until resolved
Real-world sales-hint
quality
Evaluate
consented/synthetic-expanded corpus,
grouped false positives and misses.
Optional flag-only, never block.
One-minute monthly effort
Full 30-day measured pilot including
screen waiting.
Unproven target; no marketing claim.
Store/terms approval
Publisher and qualified policy/legal
review.
No paid public launch.
The register is deliberately explicit. Neither this document nor its design images should be interpreted as
evidence that a connected messenger production integration already works.


## Source page 62

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
62
19. Primary sources and verification notes
All source pages below were checked on 9 September 2026. They establish platform/API/policy facts, not the
correctness of an unbuilt integration. Product thresholds, UI copy, algorithms and engineering trade-offs are
original specifications in this document. Recheck policy and target-platform requirements at submission. No
source provides a verified Business Gate connected messenger screen adapter.
ID
Source and what it supports
S1
Android Developers, AccessibilityService API reference. Service lifecycle, exposed interface actions and
motion-event caveats.
https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
S2
Android Developers, Create an accessibility service. Service configuration and UI interaction mechanism.
https://developer.android.com/guide/topics/ui/accessibility/service
S3
connected messenger Help Center, How to block and report someone. Native blocking/unblocking and
communication consequences. [external service reference removed]
S4
Android Developers, NotificationListenerService API reference. Notification callbacks, connection lifecycle
and device/profile limitations.
https://developer.android.com/reference/android/service/notification/NotificationListenerService
S5
Google Play Console Help, Use of the AccessibilityService API. Deterministic automation, truthful tool
declaration, disclosure and review.
https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
S6
Android Developers Blog, Enhancing Android security: Stop malware from snooping on your app data.
Sensitive accessibility views and non-tool access restrictions. https://developer.android.com/blog/posts/e
nhancing-android-security-stop-malware-from-snooping-on-your-app-data
S7
Android Developers, Android 15 behaviour changes: all apps. Security/privacy changes including sensitive
notification handling. https://developer.android.com/about/versions/15/behavior-changes-all
S8
Android Developers, Data and file storage overview. App-private storage boundaries.
https://developer.android.com/training/data-storage
S9
Android Developers, Notification runtime permission. Own notifications versus other capabilities.
https://developer.android.com/develop/ui/compose/notifications/notification-permission
S10
Android Developers, Activity security / background activity launches. Background restrictions and
foreground PendingIntent handoffs.
https://developer.android.com/guide/components/activities/secure-bal
S11
Google Play Console Help, Target API level requirements. API 36 requirement beginning 31 August 2026.
https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
S12
Android Developers, SQLiteOpenHelper API reference. Framework database creation/upgrades.
https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper
S13
Android Developers, Android Gradle Plugin 8.13 release notes. AGP 8.13.2, Gradle 8.13, JDK 17 and
build-tool compatibility. https://developer.android.com/build/releases/agp-8-13-0-release-notes
S14
Android Developers, Make apps more accessible. Interaction targets, labels and readable UI principles.
https://developer.android.com/guide/topics/ui/accessibility/apps
S15
connected messenger Help Center, How to check read receipts. Read-state and read-receipt behaviour.
[external service reference removed]


## Source page 63

BUSINESS GATE
PRD / DESIGN / ENGINEERING     v2.0
9 September 2026  ·  Implementation specification
63
ID
Source and what it supports
S16
Android Developers, Back up user data with Auto Backup. Backup/transfer exclusions and OEM
limitations. https://developer.android.com/identity/data/autobackup
S17
Android Developers, Notification.MessagingStyle API reference. Notification message representation; not
an official connected messenger identity contract.
https://developer.android.com/reference/android/app/Notification.MessagingStyle
S18
Google Play Console Help, Set up your app's prices. Paid/free setup and local pricing management.
https://support.google.com/googleplay/android-developer/answer/6334373?hl=en
S19
Google Play Console Help, Provide information for the Data safety section. Reporting definitions and
disclosure obligations.
https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
S20
connected messenger Terms of Service. Separate service/brand/automated-use contractual considerations;
applicable regional version must be reviewed. [external service reference removed]
End of specification. The smallest useful first build is the exact-number policy core and one validated
profile-to-block-to-verify flow. Expand only after its safety, maintenance and attention costs are visible.
