# Brand-neutral source specification

Extracted from the supplied design. External product names and identifiers are removed. Implementation uses an independent blue identity. The full specification governs added review and manual-choice features; physical qualification is mandatory. Source document reference claims are not implementation evidence.

## Source page 1

PRODUCT / DESIGN / ENGINEERING
Business Gate
A quiet gate for business messages.
ANDROID ONLY
US$1 PAID DOWNLOAD
60 s / MONTH TARGET
One rule. One list.
Almost no
maintenance.
Enable only the business numbers you
need. Deny newly identified business
numbers by default. Leave personal and
uncertain accounts alone.
An implementation-ready specification with
visual states, data contracts, safety guards,
build steps and 97 acceptance scenarios.
9:41
LTE   100%
Business Gate
Pause
Rule on · No action needed
Search name or number
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
LO
Loan Offers
+1 202 555 0104
Blocked in connected messenger
BM
Bright Mart
+1 202 555 0107
Blocked · Different number
Personal accounts left alone: 436
VERSION 2.0  /  9 SEPTEMBER 2026
Specification and reference logic only. connected messenger integration, phone performance and store acceptance remain release gates.


## Source page 2

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
2
Reading map
The complete experience is deliberately small. The implementation is detailed because making changes inside
another application requires more safeguards than the screen suggests.
Founder / product: Read 01-04, 08-09, 24-26 and the release decision. Designer: Read 05-09 and the copy
catalog. Android engineer: Read 10-23 and the implementation annexes. QA / release: Read 04, 18, 23-27 and
the test catalog.
Authority order: safety invariants > explicit user choice > verified connected messenger state > automation convenience >
visual polish. The rule engine never treats a drawing, a cached status or a notification title as proof of a business
account.
Document boundary: This is an executable plan for a qualified Android engineer, including the procedure for
obtaining integration-specific facts. It cannot honestly substitute invented connected messenger identifiers for
measurements on a real phone. Every such measurement has an owner, output format and pass/fail gate.


## Source page 3

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
3
Contents
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 2
Reading map
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 4
01 / Product contract
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 5
02 / Final decisions and scope
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 6
03 / Feasibility and honest boundaries
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 8
04 / One-minute monthly effort budget
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 10
05 / Main-screen design
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 12
06 / Search, ordering and row behavior
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 14
07 / Visual system and accessibility
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 16
08 / Setup, empty and recovery designs
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 18
09 / Status, copy and recovery behavior
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 20
10 / Identity and account classification
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 21
11 / Personal-account safety cache
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 22
12 / Policy, block ownership and decision rules
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 23
13 / Discovery without disturbing conversations
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 25
14 / Background behavior and service lifecycle
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 26
15 / Deterministic automation state machine
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 28
16 / Verification, interruption and recovery
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 29
17 / Notification discovery without notification interference
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 30
18 / Data model, retention and transactions
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 31
19 / Privacy, security and threat model
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 33
20 / Native Android implementation architecture
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 35
21 / Build configuration and repository layout
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 36
22 / Manifest, resources and framework integration
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 37
23 / Adapter qualification and maintenance
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 38
24 / Quality, performance and measurement
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 39
25 / Pricing, store policy and legal review
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 40
26 / Execution backlog and ownership
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 41
27 / Release gates and operating playbook
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 43
Appendix A / Complete copy catalog
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 45
Appendix B / Baseline configuration examples
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 47
Appendix C / Engineering interfaces and action pseudocode
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 49
Appendix D / Acceptance tests and traceability
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 57
Appendix E / Evidence register and open integration inputs
 .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  . 59
Appendix F / Executable database and reference verification


## Source page 4

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
4
01 / Product contract
1.1 One purpose
Business Gate maintains an exact-number exception list and uses connected messenger's own interface to block newly
encountered, positively identified business accounts that the user has not enabled. It does not become a chat
client, inbox, spam classifier, contacts manager or general automation tool.
The user-facing rule: “Only the business numbers you enable are exempt from automatic blocking.” This is
narrower and more honest than promising that every other business is prevented from delivering its first message.
The implementation rule: A fresh, supported observation must bind a one-to-one business profile to an exact
canonical phone number in the currently verified connected messenger account. That number is blocked only when it has no
enabled exception and every action guard passes. Uncertain accounts are left alone.
The base product is a paid Google Play download with a US$1 price goal. No subscription, advertisements,
registration, backend, cloud sync, in-app purchases, analytics SDK or remote configuration is included.
1.2 Number rotation
A different number is a different account. The app does not need to prove that two numbers belong to the same
company to deny both. A confirmed business at A is denied; a confirmed business at B is independently denied; C
is treated the same way. A name, avatar, website, catalog or message never transfers trust.
An enabled company's new number also defaults to not enabled. The user must explicitly enable that exact
number. This trade-off is intentional: automatic company-level trust would defeat the user's protection against
changing numbers and impersonation.
Personal-account evasion is outside the classifier: a merchant using an ordinary personal account cannot be safely
identified as a business merely by its message or name. The app MUST leave it alone unless a current supported
connected messenger business indicator is observed.
1.3 The meaning of permanent
Enabled exceptions and the default-deny rule do not expire. Business Gate never unblocks a denied account on a
timer. Once a block is applied, it is a connected messenger action, not merely a local switch. Pausing or uninstalling Business
Gate MUST NOT run any unblock sequence.
“Permanent” does not guarantee continuous operation across revocation, force-stop, data loss, connected messenger
changes, account migration or inaccessible screens. Existing connected messenger block persistence across those events is a
separate test item, not a promised property. connected messenger documents per-account block/unblock controls. [S03]
1.4 Product acceptance in one sentence
After one-time setup, the user changes only the occasional exception; personal conversations remain untouched;
accessible unwanted business accounts are blocked without repeated approval; the app tells the truth when it
cannot complete an action.


## Source page 5

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
5
02 / Final decisions and scope
ID
Mandatory decision
Why it is fixed
R01
Android; ordinary connected messenger Messenger only
Do not split effort across platforms or clients.
R02
One receiving connected messenger login and one Android
user/profile in v1
Prevent cross-account policy application.
R03
One searchable management page
Enabled businesses first; other discovered
businesses below.
R04
Default-deny only for freshly confirmed businesses
Unknown and personal accounts are not blocking
candidates.
R05
Exceptions keyed by receiving-account namespace
plus exact number
Names and logos cannot grant permission.
R06
Personal-account safety cache
Reduce inspections; never make absence from this
cache a reason to block.
R07
No message-content processing
Classification comes from supported account
metadata, not text.
R08
No forced reading of existing conversations
Automatic discovery must use a qualified no-read
profile route.
R09
Separate desired choice, observation and job state
OFF is not proof of a completed block.
R10
Local, deterministic automation
No AI, arbitrary scripts or remote screen control.
R11
Minimal native stack
Java, framework views, SQLite; zero third-party
runtime libraries.
R12
Under-one-minute monthly effort target
Includes imposed waiting, troubleshooting and
permission repairs.
R13
Explicit one-shot authority for unblocking
A saved ON switch cannot reverse every future
manual block.
R14
Stop, pause and interruption safety
No protection feature may trap the user in an
automation sequence.
R15
Unknown layouts stop mutation
No loose string matching or coordinate fallback.
R16
No sale before feasibility and policy gates pass
A polished interface is not a working integration.


## Source page 6

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
6
Included
One-to-one business profiles, exact-number exceptions, current and cached personal observations, optional
notification hints, supported visible block/unblock sequences, local search, inline status, a one-time setup card, an
emergency Stop control and a small local diagnostic report.
Explicitly excluded
connected messenger Business as the receiving application; connected messenger Web/Desktop; iOS; cloned packages;
work/private/secondary Android profiles; multi-account switching; root, ADB or Shizuku in production; modified
clients; protocol reverse engineering; linked-device sessions; screen recording/OCR; VPN filtering; contact
uploads; cloud reputation; message classification; automatic reporting/deleting/archiving;
group/community/channel enforcement; notification suppression; auto-replies; exports of contact lists; scheduled
reviews; retention dashboards; widgets and shortcuts in v1.
A supported Android SDK range is not a guarantee that every OEM or connected messenger configuration works. The
compatibility matrix is authoritative.
03 / Feasibility and honest boundaries
3.1 Architecture that is actually available
Android provides background accessibility services that can inspect exposed UI and invoke supported actions. It
does not give this app direct access to connected messenger's private database. App sandboxing remains in force. [S01, S02]
The optional notification listener provides event hints. There is no trusted business-account flag assumed in its
data. Hidden, muted, suppressed, grouped or redacted notifications can be absent or incomplete. A notification is
not a complete inbox inventory. [S04, S05]
Actual changes depend on visible, accessible and correctly bound connected messenger controls. Background monitoring is
not hidden background blocking. Android restricts background activity launches and has exceptions; this product
deliberately refuses unsolicited foreground takeover, even where a platform exception might permit it. [S06]


## Source page 7

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
7
3.2 Requirements that must not be papered over
Desired property
Constraint
Product decision
Block before the first
message
No receiving pipeline integration
Never advertise this. First messages may arrive.
Block with phone locked
Required UI may be inaccessible
Keep work pending; never unlock or wake the
phone.
Discover every business
No guaranteed complete
enumeration
Report observed coverage; do not claim all
accounts were scanned.
Do not affect personal
messages
Opening a chat may change read
state
Qualify a direct profile route; do not auto-open
unread chats.
Almost no monthly attention
UI automation can consume visible
time
Measure every imposed second; hold release
when the target fails.
Work after every connected messenger
update
UI and protections can change
Signed app updates with measured adapters;
unknown builds stop.
Protect users while reading
sensitive metadata
Protected views may be unavailable
Respect the restriction; do not misdeclare
accessibility purpose.
3.3 Integration qualification gate G0
Before building the production action engine, the Android integration owner MUST record: target signing
certificate/lineage; version code; UI language; receiving-account identity route; business evidence; exact remote
number route; safe profile navigation; block confirmation; block verification; unblock confirmation; interruption
behavior; read-receipt effects; and typical visible duration.
Use the intended production declaration isAccessibilityTool=false. Android can restrict sensitive views from
services that are not genuine accessibility tools. Do not test with broader privileges and then assume production
will behave the same. [S07]
G0 passes only when a supported direct profile route can classify and bind an account without opening its
messages, safely block the correct test account, verify it, explicitly unblock it, and avoid touching personal/group
accounts. Missing any required field produces a failed qualification record, not a guessed selector.
A profile-only prototype that requires users to inspect each business manually does not satisfy the low-attention
paid product. It may be an engineering experiment, but MUST NOT be quietly presented as the completed release.


## Source page 8

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
8
04 / One-minute monthly effort budget
4.1 Definition, not a slogan
Define T30 as the union of time intervals during a rolling 30-day period in which the user must manage Business
Gate, repair its permissions/compatibility, respond to its notices, or wait because its automation occupies
connected messenger. Overlapping intervals are counted once. Normal connected messenger use unrelated to the app is excluded.
Required automation waiting is included even when no tap is needed.
The first setup session is measured separately. Reinstallation, recovery and repeat setup within later months are
not silently excluded from ongoing effort. Voluntary reading of Help is reported separately; required help and
remediation count.
Primary release target: p95 T30 <= 60 seconds across the target beta cohort after initial setup. Secondary target:
median routine app openings per month = 0; at least 90% of account-enforcement outcomes in the measured
cohort require no per-account decision. These are proposed validation criteria, not achieved results.
4.2 Attention allocation
Work
Design budget per ordinary month
Implementation consequence
Finding and enabling
occasional exceptions
24 seconds
Search + switch; no mandatory business detail
page.
Automation-imposed waiting
24 seconds
Short safe sessions; no repeated full-inbox
scans.
Rare repair or mismatch
resolution
12 seconds
One explanatory status and one relevant
action.
Routine reviews, reports,
success notices
0 seconds
Do not implement them.
Total target
60 seconds
Measure, do not infer from tap count.
The allocation is a design budget, not a limit on how many businesses the user is allowed to manage. High-volume
users and number-rotation attacks MUST be included in stress testing. Do not remove them from reporting to
improve the metric.
4.3 Quiet-by-default behavior
No “blocked successfully” notification; no badges that demand clearing; no monthly reminder; no unknown-sender
review queue; no expiration of enabled exceptions; no full-screen prompts after connected messenger opens; no automatic
support requests. Personal and unclassified accounts create no task for the user.
Only an actionable, persistent service problem may produce one low-importance notice. It updates in place, has no
sound/vibration, contains no business name/number on the lock screen, and does not re-alert for the same
unresolved cause. Permission denial is respected.


## Source page 9

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
9
4.4 Validation protocol
Run a consented 30-day beta with at least 30 participants across the intended devices. Include existing large
inboxes, many personal conversations, newly encountered businesses, number rotation, muted chats and
connected messenger updates. Record explicit management time and automation occupancy locally; collect sanitized
aggregate results only through a user-approved research process outside the production app.
Report p50/p90/p95/max T30, distribution of new-business counts, unresolved accounts, false blocks,
supported/unsupported time and time-to-block from first observation. A user doing nothing because the app
silently failed is not a success.
Stress arithmetic: 30 newly discovered businesses taking two seconds of unavoidable visible intervention each
already consume 60 seconds before any exception changes. This is an illustrative workload calculation, not
measured app performance. Caching alone cannot solve it. The qualified route must be faster, require less
imposed waiting, or the marketing claim must be narrowed before sale.


## Source page 10

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
10
05 / Main-screen design
9:41
LTE   100%
Business Gate
Pause
Rule on · No action needed
Search name or number
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
LO
Loan Offers
+1 202 555 0104
Blocked in connected messenger
BM
Bright Mart
+1 202 555 0107
Blocked · Different number
Personal accounts left alone: 436
9:41
LTE   100%
Business Gate
Pause
Rule on · No action needed
Search name or number
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
LO
Loan Offers
+1 202 555 0104
Blocked in connected messenger
BM
Bright Mart
+1 202 555 0107
Blocked · Different number
Personal accounts left alone: 436
Everyday view
System dark appearance
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.
5.1 Information architecture
One Activity contains a fixed top app bar, one concise status area, a fixed search field and one virtualized list.
Section headers are list rows. There are no bottom tabs, drawer, second inbox, floating add button or nested scroll
containers.
The normal page order is Enabled by you, Not enabled, then a collapsed Personal accounts left alone summary.
Unclassified account details appear only when a user searches for an exact known label/number or expands the
small local-status explanation; they do not become a routine approval list.


## Source page 11

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
11
Names and numbers in visual examples are synthetic. The design is not a screenshot of an implemented app or a
claimed connected messenger integration.
5.2 App bar and status
Title: “Business Gate”. Right action: “Pause” when rule is active, “Resume” when paused. An overflow icon opens a
native dialog containing How it works, Permissions, Compatibility, Privacy and Reset local data. There is no
separate settings Activity.
The first status line describes readiness, not total protection. Normal copy: “Rule on”. Secondary copy: “New
business numbers are checked when accessible.” A waiting state says “2 blocks pending”, not “2 businesses
blocked”. Do not use “Fully protected”, “100% safe” or an unconditional shield badge.
Pause and Resume are verbs, not ambiguous global toggle icons. Pause persists across process restarts and app
updates. Resume does not unblock anyone.
5.3 Business row anatomy
Each business row has: initials tile; display label; exact-number context; execution subtitle; one native switch. Use
initials, not profile photos or brand artwork. The normal number is fully readable when it fits; on narrow displays
show an ellipsis in the middle and reveal the full value by expanding the row. Never truncate the distinguishing last
digits.
A tap outside the switch expands the same row. Expanded content: full number; classification evidence time; last
observed connected messenger block state; reason for pending/failure; and a context-specific action such as Apply now or
Unblock now. Only one row expands at a time.
The switch changes policy immediately after a durable transaction; its position does not claim that connected messenger has
completed the change. A small pending subtitle is sufficient. The whole page must remain usable while actions are
queued.


## Source page 12

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
12
06 / Search, ordering and row behavior
9:41
LTE   100%
Business Gate
Pause
Rule on · No action needed
har
ENABLED BY YOU · 1
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
NOT ENABLED · 1
HC
Harbor Care Offers
+1 202 555 0105
Blocked in connected messenger
PERSONAL · LEFT ALONE · 1
HE
Harriet Example
+1 202 555 0106
Personal observed · No action
Search is local. Personal results never have a blocking
switch.
9:41
LTE   100%
Business Gate
Pause
Rule on · No action needed
Search name or number
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
This choice applies only to this number. A new
number needs its own exception.
Last verified · illustrative example
LO
Loan Offers
+1 202 555 0104
Blocked in connected messenger
Personal accounts left alone: 436
Search includes safe personal results
Details expand inside the same list
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.
6.1 Deterministic ordering
Enabled businesses always appear before not-enabled businesses, including in search. Within Enabled, sort by
normalized name, then canonical number, then stable database ID. Within Not enabled, order pending/unverified
discrepancies first, then freshly discovered businesses, then observed blocked businesses; within each subgroup
use the same name/number/ID order.


## Source page 13

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
13
Do not reorder the whole list whenever a timestamp changes. A focused row stays anchored until its interaction
ends. When a toggle moves a row between sections, retain the search query and nearest visible stable-ID anchor.
Announce the move for assistive technology; never jump to the top without a user action.
Each header count reflects the filtered business rows under it. With a query, show “2 matching” where useful; do
not combine global and filtered counts without labels. Omit empty sections during search. With no query, an
empty Enabled section retains one short hint: “Enable a business to keep it.”
6.2 Search specification
Search placeholder: “Search business or number”. Match display/search names and normalized digits. Use Unicode
normalization, case folding through Locale.ROOT, and whitespace normalization for labels. Identity comparison
never uses label normalization.
A digit query matches the canonical number after stripping permitted visual separators from the query. Name
queries use substring matching. SQL %, _ and the escape character are treated literally, using bound parameters
and an explicit escape clause. No fuzzy matching, transliteration dependency or remote results.
Debounce 150 ms. Execute reads off the main thread. Assign each query a monotonically increasing sequence;
discard results from superseded queries. Blank input restores the normal view. Clear button has a 48 dp touch
target. Back dismisses the keyboard before leaving the screen.
6.3 Personal and unknown search results
Personal matches are shown after business sections under “Personal - left alone”. They have no business switch
and no Block action. Subtitle: “Not subject to automatic blocking”. Unknown matches say “Not classified - left
alone”. A non-business observation is not an enabled exception.
No query or personal-summary tap causes connected messenger navigation. A deliberate “Check account” action may start a
supported visible inspection but not change the account's state without fresh business evidence and an active
rule. Search history is not persisted.
6.4 Switch and feedback details
OFF to ON: save the exception, invalidate any old block work, issue a one-shot unblock authorization and show
ON. When a currently observed block needs removal, show “Unblock pending”. Never revert the switch because
UI automation failed.
ON to OFF: remove the exception and queue reevaluation. If the current type is personal/unknown/stale, no
automatic block is authorized. If business evidence is fresh and safe, run the rule. A four-second inline Undo
affordance may appear in the status area without moving controls; Undo is a new policy revision, not a rollback of
external history.
Native switch ripple/haptic feedback is sufficient. Avoid dialogs for each ordinary toggle. A one-time setup
explanation establishes that ON authorizes one unblock attempt if necessary. A later external block requires an
explicit Unblock now action.


## Source page 14

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
14
07 / Visual system and accessibility
7.1 Layout tokens
Token
Value / behavior
Reference canvas
390 x 844 dp; system bars are illustrative, never hard-coded.
Horizontal page gutter
16 dp; 24 dp above 600 dp width.
Content maximum width
600 dp centered on large screens; still one column.
App bar
Minimum 56 dp; allow text expansion.
Search field
Minimum 48 dp high, 12 dp radius, 12 dp horizontal padding.
Row
Minimum 88 dp, wrap-content height; 12 dp vertical padding.
Initials tile
36 x 36 dp, 10 dp radius; decorative to screen readers.
Row text / switch gap
At least 12 dp; text column uses remaining width.
Switch interaction
At least 48 x 48 dp focus/touch region.
Section heading
13 sp semibold; 24 dp top and 8 dp bottom spacing.
Expanded details
12 dp top gap, no horizontal scrolling, wrap long labels.
Motion
120-180 ms fade/move, disabled with reduced-motion settings.
Dividers
1 dp low-contrast line; no heavy card borders around every row.
7.2 Typography and color
Use Android system sans-serif; do not bundle fonts. App title 22 sp medium; row name 16 sp medium; number 13
sp regular; status/body 14 sp; expanded detail 13 sp. Font scaling applies to all text, including buttons. No
essential text below 12 sp.
Light palette: background #F7F8FA, surface #FFFFFF, primary text #17212B, secondary text #536170, accent
#2459D3, outline #D9DFE7, warning text #855100 on #FFF1D6, error text #A2262C on #FBE9EA.
Dark palette: background #11161D, surface #19212B, primary text #F2F5F8, secondary text #B9C3CF, accent
#A8C4FF, outline #3B4655, warning text #FFD28A on #3B2C17, error text #FFB3B8 on #44242A.
Follow the system day/night theme with native resources; no theme selector. The app intentionally does not
imitate connected messenger's branding. Use words/icons as well as color for all states. Validate text contrast and disabled
controls rather than assuming the palette alone guarantees accessibility.
The 48 dp touch-target baseline follows Android accessibility guidance; the palette and other dimensions are
product design choices. [S18]


## Source page 15

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
15
7.3 Assistive technology and responsive behavior
Minimum interaction targets follow Android's 48 dp accessibility guidance. [S18] TalkBack order: title,
Pause/Resume, overflow, status action, search, section header, row label, row switch. Switch label: “Enable Harbor
Clinic, number ending 0110”; checked state comes from the native control, not repeated prose.
A personal row announces “Personal account, not subject to automatic blocking”. Expanded details expose a
heading and a clear collapse action. Use polite announcements for completed user-initiated changes; background
discovery must not read out each newly blocked business.
At 200% font scale, names wrap to two or more lines, rows grow vertically and switches remain visible. In
landscape, tablet, display zoom and split-screen, layout remains one column. Automation is suspended in
unqualified multi-window contexts even though the management UI still works.
Support hardware keyboard traversal, Enter/Space activation, RTL container layout and LTR-isolated phone
numbers. Strip display-control characters from untrusted labels before showing them; do not alter canonical
identity to make a name look correct.


## Source page 16

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
16
08 / Setup, empty and recovery designs
9:41
LTE   100%
Business Gate
Pause
Setup needed
Search name or number
Choose your businesses.
Leave the rest to the rule.
Business numbers you do not enable will be
blocked when connected messenger can be safely
inspected.
1
Connect connected messenger
Review screen-access permission
2
Choose exceptions
Keep the businesses you need
3
Start the rule
Personal accounts are left alone
Connect connected messenger
No account. No subscription. No message storage.
9:41
LTE   100%
Business Gate
Pause
Choose exceptions · Rule not started
Search name or number
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Will stay enabled
PD
Parcel Desk
+1 202 555 0102
Will stay enabled
Start default-deny?
Other confirmed business numbers will be blocked.
This can stop deliveries, appointments,
authentication messages and support replies.
Messages sent while blocked may not be recovered.
Start the rule
One-time connection
Choose exceptions, then activate
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.
8.1 One-time setup flow
The same screen starts with a setup card replacing the normal status. Nothing is automatically armed when the
accessibility service is first enabled. The card uses a compact progress indicator: Connect, Choose, Start. These are
setup stages, not permanent tabs.
Connect: verify the supported connected messenger package and qualified environment; show a separate prominent
accessibility disclosure; user accepts, then Android settings opens. Returning without granting permission leaves
the app in setup. The app cannot grant this access itself.


## Source page 17

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
17
Choose: offer a visible discovery-only session using the qualified no-read profile route. Display actual coverage,
including skipped locked/archived/unsupported accounts. Let the user enable businesses in the same list. No
block occurs before activation. An explicit Skip review action is permitted only after showing the full default-deny
consequence.
Start: show the activation statement and a single “Start blocking other businesses” button. Commit consent and
rule authority transactionally. Existing enabled choices persist. Queue eligible discovered businesses, then return
to the quiet page. Do not automatically jump to connected messenger merely because activation completed.
Optional notification access is offered after the essential setup in a small dismissible card with its own disclosure.
Declining never prevents supported profile-based blocking. Optional own-notification permission is requested
only when explaining rare service-problem notices.
8.2 Setup copy
Accessibility disclosure: “Business Gate uses Accessibility to inspect connected messenger account details and press Block or
Unblock according to your choices. Android gives this service broad screen access. The app processes supported
connected messenger account details locally, does not save message text, and does not send your data to a server. Changes
run visibly when connected messenger is accessible. You can stop them at any time.” Buttons: “Agree and open settings” and
“Not now”.
Activation statement: “Businesses you have not enabled will be blocked when they can be identified safely. This
includes delivery, appointment, support and authentication messages. The first message may arrive. Personal and
unclassified accounts are left alone. An enabled number may be unblocked once when you turn it on.” Do not
pre-enable banks, clinics or delivery companies.
connected messenger's documented unblock behavior does not provide a recovery mechanism for messages sent during a
block. Explain this as “Unblocking does not recover missed messages,” and recheck the help guidance at release.
[S03]
8.3 No artificial setup promise
Proposed usability target: essential consent and choosing up to five exceptions within three minutes of user effort,
excluding a large user-requested initial discovery run but reporting that run's waiting time separately. This is a
first-run target, not the monthly metric. An extensive existing inbox may require more time; show coverage rather
than pretending the setup is exhaustive.


## Source page 18

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
18
09 / Status, copy and recovery behavior
9:41
LTE   100%
Business Gate
Pause
Rule on · 1 action waiting
Search name or number
A choice is waiting
Actual blocking needs a safe connected messenger screen.
Open connected messenger and apply
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
Personal accounts left alone: 436
9:41
LTE   100%
Business Gate
Pause
Paused · connected messenger needs support
Search name or number
connected messenger changed
Your choices are saved. Existing blocks stay.
Automation waits for a supported app update.
ENABLED BY YOU · 2
HC
Harbor Clinic
+1 202 555 0101
Enabled · Unblocked
PD
Parcel Desk
+1 202 555 0102
Enabled · Unblocked
NOT ENABLED · 3
BM
Bright Mart
+1 202 555 0103
Blocked in connected messenger
Personal accounts left alone: 436
Pending is not the same as blocked
Compatibility failure preserves choices
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.
Priority
State
Exact primary copy
Action / effect
1
Safety circuit open
“Automation stopped safely”
Show cause; no repeated retry
button for unsupported
layouts.


## Source page 19

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
19
Priority
State
Exact primary copy
Action / effect
2
Namespace mismatch
“connected messenger account changed”
Reconnect and review the new
namespace; old choices
retained inactive.
3
Accessibility absent
“Accessibility is off”
Open settings only after a tap.
4
Unsupported
build/language
“connected messenger compatibility check
needed”
Explain; offer app-store
update, not blind retry.
5
Explicitly paused
“Paused”
Resume; existing blocks
remain unchanged.
6
Applying explicit batch
“Applying your choices”
Show remaining count and
Stop.
7
Pending work
“2 blocks pending”
Apply now; detail explains
safe-context requirement.
8
Enabled/externally
blocked mismatch
“1 enabled business is still blocked”
Expand that row; explicit
Unblock now.
9
Ready
“Rule on”
No action required; no “fully
protected” badge.
10
No businesses observed
“No businesses found yet”
Explain discovery without
asking for routine scanning.
The highest-priority unresolved condition drives the single status card. Additional details expand underneath;
never stack a column of warning banners. Search and policy changes remain available while automation is paused
or unsupported.
Row subtitles are fixed vocabulary: “Enabled”; “Unblock pending”; “Enabled here; blocked in connected messenger”; “Block
pending”; “Blocked - checked today”; “Block state not verified”; “Account changed - waiting for inspection”;
“Personal - left alone”; “Not classified - left alone”. A timestamped observed block is not a server-delivery
guarantee.
Mismatch recovery: a user who explicitly enabled an already blocked number sees ON immediately and one Apply
now action. No per-number confirmation is needed inside Business Gate because the explicit toggle grants the
fixed action; connected messenger's own confirmation is handled through a qualified sequence. A manual block observed
later is not overridden by that exhausted authority.
Pause: immediately invalidate the session generation and prevent the next action. Persist pause before reporting
success. An already executed click cannot be retroactively canceled; verify later without a blind undo. Stop during
a visible session also pauses the rule until Resume, so the app does not immediately restart after the user stops it.
Reset: native confirmation: “Delete this app's local choices and history? This stops future automation. It does not
unblock anyone in connected messenger.” Default cancel. On confirm, disarm first, then clear local policy/caches, remove
overlays and return to setup.


## Source page 20

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
20
10 / Identity and account classification
10.1 Canonical identity
The primary key is (namespace_id, phone_e164). A namespace binds the receiving connected messenger login, target
package signing identity, Android profile context and local installation authority. A random local UUID is only a
database key; it is not proof of the active connected messenger login.
Require a qualified way to establish the receiver's identity during activation and every new mutation session. If
account switching cannot be detected or the active login cannot be bound sufficiently, that build is not qualified
for unattended navigation. Do not transfer exceptions between namespaces without explicit future product work;
v1 has no transfer feature.
A remote phone number must come from the supported account-identity field, not a business address, website,
message, contact card or notification body. Parse an explicitly international form only. Permit ASCII digits, leading
plus and the tested presentation separators; remove those separators, reject other characters, require the first
digit to be nonzero and a maximum of 15 digits. This is conservative syntax validation, not proof of ownership or
numbering-plan validity. Do not infer a country from the SIM, language or location.
Signing information is obtained through the platform package APIs and qualified against the accepted signing
history. [S17]
Names are capped at 160 Unicode code points for display storage. Canonical phone identity is never truncated.
Bidi-control characters and line-break/control characters are removed from display labels, while original messages
are never stored. Search normalization is separate from canonical equality.
10.2 Classification evidence
Classification
Evidence required
Action permission
BUSINESS_CONFIRMED
Supported connected messenger-controlled
business indicator in a qualified
structural location, exact identity
and namespace binding
May enter the rule engine; still requires fresh
guards.
PERSONAL_OBSERVED
Qualified ordinary-profile structure
with complete required fields and
no conflicting business evidence
Skip routine inspection temporarily; never
block from this state.
UNKNOWN
No reliable current classification or
no exact identity
Leave alone.
AMBIGUOUS
Conflicting evidence,
partial/protected tree or multiple
plausible identities
Leave alone; invalidate old action work.
STALE
An earlier observation is no longer
reusable
Reobserve opportunistically; not a block
reason.
NON_DIRECT
Group, community, channel,
broadcast list, system account or
unsupported context
Ignore for enforcement.


## Source page 21

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
21
“Personal observed” is deliberately not a claim that connected messenger exposes an authoritative personal-account API.
Absence of a business badge alone is insufficient. If the adapter cannot establish its qualified ordinary-profile
structure, use UNKNOWN. The app should prefer a missed business over an incorrect personal block.
10.3 Forbidden evidence
No keywords, sender names, catalog prose, verified-looking emoji, message snippets, business hours, logos,
Person.isBot, phone-shaped substrings, address-book membership or notification channel names are sufficient
to classify an account for mutation. No AI confidence score can replace explicit structural and identity checks.
11 / Personal-account safety cache
11.1 Purpose
The cache avoids repeatedly inspecting familiar ordinary conversations. It is an optimization and a safeguard, not a
separate allowlist and not a prerequisite for receiving personal messages. A personal number absent from the
cache is still left alone unless current supported business evidence is obtained.
Store only the exact canonical number, namespace, optional visible label, observation class, adapter/build
signature and timestamps. No photos, profile-image fingerprints, message text, contacts permission or chat history
is needed.
11.2 Lifetime and invalidation
Default personal-observation reuse window: 30 days, measured from a valid profile observation. On ordinary
notifications from an already mapped personal account, refresh last_seen but not verified_at or the 30-day
classification window. A notification does not reverify type.
Expiration moves the observation to STALE without queueing a block or forcing a profile visit. Reinspect only
when naturally visible or through a qualified safe discovery route. Profile display-name changes invalidate
label/search caches but do not independently prove a type change. No photograph watching is implemented.
New current business evidence for the same number overrides the personal cache only after it is independently
validated and persisted. A business-to-personal transition cancels future automatic business blocks; it does not
automatically undo a block already in connected messenger. Existing block status remains separately visible when relevant.
Invalidate reusable evidence on unsupported connected messenger updates, adapter revision changes, namespace changes,
contradictory identity or an uncertain restored installation. Never map these events directly to
BUSINESS_CONFIRMED.
11.3 Privacy and presentation
Personal records idle for 90 days may be deleted if they have no enabled business exception, pending explicit
action or retained business block history. Deleting a cache record never changes connected messenger. The default business
list only shows a footer such as “Personal accounts left alone: 436”. This is a local observation count, not a
complete count of the user's contacts.
Tapping the footer expands a small explanation, not another contacts-management screen. Name/number
searches may expose matching personal rows without switches. There are no manual “block personal” or “mark all
businesses” functions in this product.


## Source page 22

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
22
11.4 Critical transition tests
Unknown to personal: no mutation. Fresh personal to stale: no mutation. Stale personal to unknown: no mutation.
Personal to freshly confirmed business: reevaluate exact exception, then require every block guard. Same personal
display name on another number: separate record. Enabled former business becomes personal: keep the exception
preference but do not expose a personal blocking control.
12 / Policy, block ownership and decision rules
12.1 Four independent state dimensions
Classification describes the latest account evidence. Desired policy records whether the user explicitly enabled
this exact number. Observed connected messenger state is UNKNOWN, BLOCKED or UNBLOCKED with a timestamp.
Execution state describes work that has or has not completed. Store these separately.
Default-deny is derived: a confirmed business with no enabled exception is denied. It is not implemented as “every
unknown account has blocked=true”. A stale cached BUSINESS record can keep a pending intent but cannot
authorize a current click.
12.2 Decision precedence
Order
Condition
Required decision
1
Policy not loaded, no activation, paused or consent
invalid
No mutation.
2
Target signing identity, namespace, profile or
adapter unsupported
Abort/pause; no action.
3
Personal, unknown, stale, ambiguous or non-direct
current observation
Leave alone.
4
Current exact number does not match the job
target
Abort and invalidate binding.
5
Exact number enabled by latest committed policy
Never automatically block.
6
Fresh same-account observation already shows
blocked
Record observation; complete any duplicate block
job.
7
Safe UI lease unavailable
Keep pending, without claiming completion.
8
All guards pass, current confirmed business denied
Execute the qualified block sequence.
12.3 Unblocking is separate
A deliberate OFF-to-ON change or an explicit Unblock now action issues a durable one-shot authorization scoped
to namespace, exact number, policy revision and a random nonce. It survives ordinary process restart while the
action is pending, but is canceled by another toggle, Stop/Pause, namespace change, evidence of later user
intervention or a safety fault.


## Source page 23

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
23
Consume the authorization when a fresh verified observation shows the requested unblocked state or when the
user cancels. A stored enabled preference without a live authorization NEVER undoes a subsequently observed
manual block. Unknown-type explicit unblock actions may proceed only for an already known business record
with exact identity, verified blocked state and a qualified unblock route; never use this to create a general
personal-unblock utility. Fresh PERSONAL_OBSERVED, AMBIGUOUS, STALE and NON_DIRECT observations
never authorize an app-side unblock. A former business now observed as personal is managed directly in
connected messenger.
If Pause cancels an unfinished unblock, keep the desired ON state and show “Enabled here; blocked in connected messenger”
until a new explicit action. No timeout silently flips the business OFF or unblocks someone later.
12.4 Existing manual blocks and race conditions
A block observed at discovery is marked EXTERNAL_OR_UNKNOWN, not credited as a Business Gate action. An
enabled account externally blocked later stays blocked unless the user explicitly resolves it. A denied business
manually unblocked in connected messenger will be blocked again on fresh safe observation while the rule remains active;
explain this once at activation.
The local database and connected messenger UI cannot form one atomic transaction. Recheck revisions immediately before
the final action and again before publishing its result. If enabling races with an already executed block, preserve
ON, record the observed mismatch and reconcile only with the still-valid explicit unblock authority. Do not claim
this race is impossible.
13 / Discovery without disturbing
conversations
13.1 Discovery sources
Source A is a naturally visible supported profile; source B is a qualified direct-profile route from a stable inbox row;
source C is an optional notification hint that can later be resolved through A or B. None provides guaranteed total
coverage.
Onboarding may offer a user-started discovery-only pass. The pass visits accessible one-to-one profiles through
the qualified no-read route; it does not auto-open unread chats, scroll through messages, archive anything or
operate inside locked/private chat areas. Rows known to be groups or personal-cache hits are skipped when
mapping is exact and still reusable.
A business name in an inbox row is only a navigation hint. After entering the profile, the exact number MUST be
resolved again before any policy evaluation. Duplicate names and moving rows are expected, not exceptional.
13.2 No-read qualification
For each candidate route, create a fresh unread message from a consenting personal test sender. Record the
receiver's unread status and sender-side read state. Navigate using the proposed route, then verify that no
message opened, unread flag changed, sender receipt changed, reply was sent, notification was dismissed or chat
position was modified by a side-effect action.
Repeat on business accounts, groups, archived rows and every claimed layout variant. A route that changes read
state is disabled for automatic discovery. The app MUST NOT try to restore unread flags, disable read receipts,
simulate invisibility or disguise this effect.
A user can always manually open a chat themselves; their own normal connected messenger action is outside this guarantee.
Business Gate must not initiate that side effect under the label of protection.


## Source page 24

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
24
13.3 Traversal algorithm
At the start of an explicit pass, establish namespace and adapter, build a bounded set of visible row hints, and
prioritize exact mapped pending businesses before unclassified candidate rows. For each row, reacquire it,
traverse the qualified direct-profile path, bind the exact account, inspect minimal metadata, persist, and return
only through a known route.
Use exact resolved IDs to deduplicate. Never use screen coordinates, list index or a name as durable identity. Stop
on repeated visible-row fingerprints without progress, scroll action failure, user interruption, 100 inspected
profiles, or a 90-second explicit pass budget. These are proposed protective limits; show “Inspected 37 accessible
profiles; 8 skipped” rather than a fake percentage of the entire inbox.
An explicit pass is resumable from fresh rows and visited identities, never from saved nodes or screen positions.
No automatic daily/full-inbox rescan is implemented.
13.4 Opportunistic enforcement
After global consent, automatic work may start only from a qualified stable inbox/profile context with no editable
focus, call/picker/selection state, unexpected overlay or recent detected interaction. A 1.5-second stable
observation window is a minimum heuristic, not proof that the user is idle. The adapter qualification must
demonstrate that false starts abort safely.
Only one account is handled per automatic lease, with a six-second hard session budget and at least 30 seconds
before another automatic lease in the same foreground visit. Exceeding the budget stops and preserves pending
work. These limits are defaults to validate against the monthly attention target; do not silently increase them in
production to conceal poor performance.


## Source page 25

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
25
14 / Background behavior and service lifecycle
Environment
Monitoring
Actual block/unblock
Business Gate screen closed;
services enabled
May receive relevant system events
Only through a qualified accessible connected messenger
session.
connected messenger active on
supported safe context
Minimal metadata observation
Eligible single-account work can run visibly.
User composing, calling,
scrolling or selecting
Cancel current sequence
No new mutation.
Another app foreground
Notification hints only, when
available
Never take over the screen.
Phone locked or screen off
Hints may arrive
No unlock, wake lock or navigation.
Accessibility revoked
No accessibility work
Stop. Optional listener may still report
degraded state.
User force-stops app
No restart promise
Do not fight the force-stop.
Process killed by system
System may later reconnect
services
Load authority and reobserve; no click replay.
Unsupported update / locale
Read-only readiness checks
Pause until a qualified adapter exists.
App uninstalled / data
cleared
Future work stops
No mass unblock.
Accessibility and notification listeners are system-bound services, not an excuse to add a permanent foreground
keep-alive service. The implementation has no polling job, boot auto-launch, alarm, battery exemption request,
hidden activity, full-screen intent or watchdog. Android service lifecycle and notification-listener connection rules
must be respected. [S02, S04]
On service creation, authority is UNLOADED and no action is possible. Load SQLite on the serial executor,
validate installation/consent, publish the immutable policy snapshot, then permit read-only observations.
Mutation still requires a new validated session binding.
On onInterrupt, disconnect, destruction, target package change or unexpected foreground transition: increment
generation, clear scheduled step callbacks, remove overlay, release node references and invalidate UI leases.
Queued policy intent persists; uncertain current action state becomes REINSPECT.
Notification listener recovery starts only after onListenerConnected. Read current active notification metadata
once to reconstruct bounded in-memory hints. requestRebind may be used at a legitimate reconnect point, not in
an endless loop. Revocation is respected. [S04]


## Source page 26

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
26
15 / Deterministic automation state machine
Observe
Minimal fresh profile evidence only.
Bind
Qualified package, receiver and exact number.
Classify
Fresh business indicator is required.
Read latest choice
An enabled exact number is never auto-blocked.
Acquire safe lease
No interruption; revisions and UI still match.
Block, then verify
Use supported controls; record the observed result.
Unknown / mismatch
Leave alone. Never guess.
Personal / stale
No automatic block.
Enabled
Preserve the exception.
Unsafe / unavailable
Wait. Do not claim success.
Uncertain result
Reinspect; never blind-retry.
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.


## Source page 27

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
27
15.1 State transitions
State
Entry work
Exit condition / next state
DISARMED
No authority or explicit pause
Valid activation and policy load ->
OBSERVING.
OBSERVING
Minimal event extraction; no
navigation
Candidate or pending job -> WAIT_SAFE.
WAIT_SAFE
No clicks; await qualified lease
Lease acquired -> BIND; interruption stays
pending.
BIND
Verify receiver, target
package/build and remote identity
Exact account and route -> INSPECT.
INSPECT
Read current profile class and block
state
Personal/unknown -> DONE_NO_ACTION;
business -> POLICY.
POLICY
Read latest exception and job
revision
Allowed/already satisfied -> DONE; otherwise
-> OPEN_CONTROL.
OPEN_CONTROL
Invoke only the qualified
block/unblock control
Recognized confirmation ->
CONFIRM_GUARD.
CONFIRM_GUARD
Recheck identity chain, consent,
revision and report/delete choices
All true -> APPLY; any doubt -> ABORT.
APPLY
Journal intent first; invoke one
supported action
Always -> VERIFY, even if action return is
uncertain.
VERIFY
Reacquire exact account and block
indicator
Verified state -> COMMIT; timeout ->
REINSPECT.
COMMIT
Persist observation and consume
authority when appropriate
Optional qualified return -> OBSERVING.
ABORT / REINSPECT
Drop nodes and lease; keep honest
pending/error state
New safe session only; no continuation from
stale screen.
15.2 Guards before every action
Check policy loaded; consent current; not paused; current epoch/revision matches; target package and signing
identity accepted; exact connected messenger build/language supported; active receiver bound; remote number unchanged;
current window belongs to the qualified flow; nodes visible/enabled/actionable; no editable focus; no
call/picker/multi-window/foreign overlay; no user interruption; lease not expired; and expected step outcome
structurally present.
Confirmation may display only a name. It is allowed only when a qualified uninterrupted transition establishes a
unique binding to the previously verified exact number. Reused names alone are never sufficient. In ambiguous
simultaneous-window situations, abort.


## Source page 28

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
28
15.3 Block only, not report
Never select Report, delete, clear, archive, send, call, add contact or change privacy settings. Every supported
confirmation layout explicitly identifies all additional choices. If report/delete is preselected and there is no
separately qualified way to leave it off, abort. Unchecking an optional report box may be included only in a tested
fixed recipe; do not broadly interact with arbitrary checkboxes.
Do not include a fallback that looks for the word “Block” anywhere on screen. Message text and
business-controlled profile text are untrusted input. The parser only recognizes documented roles and structural
locations in its own versioned adapter.
16 / Verification, interruption and recovery
16.1 What counts as success
performAction returning true only means that the action request was accepted at the UI layer. Success requires a
fresh same-account observation showing the qualified blocked/unblocked indicator. Store the observation
timestamp, adapter and session. The resulting claim is “connected messenger's UI showed blocked”, not independently
verified server enforcement. [S02]
An offline or sync-pending device may expose local state before external behavior settles. The app has no private
server acknowledgment channel and does not send a test message to verify blocking. QA tests actual delivery
separately using consenting accounts.
16.2 Timeouts and bounded retries
Coalesce relevant events for 250 ms. Bound a screen extraction to 128 relevant nodes, depth 8 and a measured 4
ms main-thread extraction budget. Where a required path exceeds that limit, reject the adapter or revise the
budget through performance review; never truncate identity silently.
Default step timeout: four seconds; automatic session total: six seconds; explicit user-started apply session: up to
60 seconds; initial discovery pass: up to 90 seconds. Proposed budgets may be tuned before release only with
safety and attention tests.
At most two fresh inspection attempts per account per explicit session. No blind repeat of final confirmation.
Three structural failures in one foreground session open a circuit breaker for that adapter/environment. A
build/layout mismatch stops immediately without waiting for three failures. Duplicate “already blocked”
observations are no-ops.
16.3 User control
During every mutating session, display a small native accessibility overlay reading “Business Gate - applying 1
choice” and a Stop button with a 48 dp target. It MUST NOT conceal the target control or imitate a
connected messenger/system prompt. No business name is necessary. Use TYPE_ACCESSIBILITY_OVERLAY, not general
draw-over-other-apps permission. [S19]
Outside the Stop chip, touches pass through normally. Any detected unexpected click/scroll/text-entry/window
transition invalidates the generation. A quiet event period is not a guaranteed global no-touch signal; pre-action
revalidation remains mandatory. The service does not request keylogging, touch-exploration or general
gesture-injection capability.
Do not navigate Back automatically after an interruption: that may undo the user's new navigation. A return step
is allowed only while the original lease and exact expected screen remain intact. If uncertain, remove the overlay
and leave the app where it is.


## Source page 29

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
29
16.4 Crash recovery
Persist ACTION_INTENT before the external effect. After process death at any stage, mark uncertain work
REINSPECT and read the present account state. If already satisfied, finish without another click. If unsatisfied,
require the current rule/explicit authority and a fresh session. Never persist or replay AccessibilityNodeInfo,
coordinates, PendingIntent parcels or screen positions.
17 / Notification discovery without notification
interference
The notification listener is optional and never the blocking authority. It can shorten discovery latency only when
reliable conversation hints exist on the qualified build. It does not guarantee identification, first-notification
prevention or business classification. Android can redact sensitive notification content. [S04, S05]
Check StatusBarNotification.getPackageName() before reading extras. Ignore every package except the
qualified target. Ignore group summaries, system/background notices and non-message contexts when they can
be identified safely. Do not read message bodies, titles as identity, text lines, OTPs, media or MessagingStyle
message text.
An opaque shortcut/conversation key may be held in memory as a hint. It is not assumed to contain a phone
number or to remain stable forever. Associate it with an account only after a qualified profile resolution;
namespace and target version are part of the mapping. A previously resolved personal mapping lets repeated
ordinary messages be ignored cheaply.
Unresolved hints: maximum 256, 24-hour elapsed-time lifetime, memory only. Deduplicate by opaque key. If full,
discard oldest unresolved hints and increment a nonidentifying diagnostic counter. No overflow warning is shown
to the user. Reconstruct only active notifications after process restart; lost hints are a coverage limitation, not
permission to inspect all chats.
The app MUST NOT call notification cancel, snooze, mark-shown, reply or channel-modification operations for
connected messenger. This protects personal notifications and avoids disguising muting as blocking. No notification content
is sent, copied to a clipboard or written to local logs.
Own notices use one low-importance service_status channel with a stable notification ID. Request
POST_NOTIFICATIONS only for these notices on applicable Android versions. Denial changes only notices, not
the business rule. Notices contain generic state, and a direct Activity PendingIntent opens the main screen; no
service/receiver launch trampoline.


## Source page 30

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
30
18 / Data model, retention and transactions
18.1 Authoritative schema
Appendix F reproduces the executable schema; the identical file is supplied as reference/schema.sql. It defines
namespace, account, action_job, action_event, app_meta and attention_daily. Foreign keys are enabled on
every connection. Use integer booleans with CHECK constraints, explicit text enums, unique namespace/phone
pairs and one current job per account.
Entity
Required contents
namespace
Local ID; target package; receiver identity fingerprint; installation binding;
rule/paused flags; global revision; consent version/time; qualification ID.
account
Canonical number; optional label/search key; classification; evidence adapter/build;
personal reuse expiry; enabled flag; per-account revision; observed block state/time;
block origin; timestamps.
action_job
Account ID; BLOCK or UNBLOCK; state; expected global/account revisions; session
generation; one-shot nonce if needed; attempts; reason; timestamps.
action_event
Local ID; nullable account ID; controlled event type/outcome/reason; timestamp;
adapter ID. Never free-form screen text.
app_meta
Schema/installation metadata and last compatible app version. No secrets in
exported preferences.
attention_daily
Local day, management milliseconds, automation-occupancy milliseconds and
occurrence counts; no account identifier.
Personal, unknown and business records share the account table to avoid contradictory classifications for the
same number. Unresolved hints with no canonical number never enter that table as invented identities.
18.2 Atomic policy writes
Toggle transaction: validate input and namespace; increment the account revision; set enabled; cancel/replace
stale work; create an explicit nonce for a needed one-shot unblock; append a controlled user-policy event;
commit. Publish a new immutable policy snapshot only after commit. If disk write fails, keep the old switch and
explain “Could not save your choice”; do not mutate connected messenger.
Pause/activation/namespace transactions increment global revision and invalidate all session leases. Global Stop
also cancels outstanding unblocking authority. Resume arms the rule only after readiness checks, and never
creates unblock authority.
Result transaction: validate account/job generations and revisions; update only observation fields;
complete/retain job as appropriate; consume a used authorization; append a bounded event. An old result may be
retained as historical evidence but cannot overwrite a new enabled preference.


## Source page 31

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
31
18.3 Retention
Enabled exceptions: until explicit removal/reset. Known business deny/block records: no automatic expiry.
Personal records: prune after 90 idle days when unreferenced. Resolved unknown accounts without
policy/history: prune after 30 idle days. Action events: retain at most 5,000 or 30 days, whichever removes more.
Daily effort aggregates: 35 days. Unresolved notification hints: memory-only cap/lifetime specified in section 17.
If total durable accounts reach 50,000, preserve policy and business history, stop adding optional personal cache
records and display only a diagnostic limit when the app is opened. Do not silently delete enabled exceptions. If a
new business action cannot be durably recorded within storage limits, do not perform it; show a storage-limited
state when opened. This is a protective implementation ceiling, not a pricing tier.
18.4 Storage and migration
Use app-private credential-protected SQLite with WAL and one serial writer. Platform sandboxing and device
storage protections are the baseline; do not claim the database is independently end-to-end encrypted. No
external storage or network permission. SQLiteOpenHelper provides framework lifecycle/migration support. [S01,
S10]
Each schema upgrade is explicit and transactionally tested from all shipped versions. No destructive fallback. A
failed migration disarms automation, preserves the original database, and offers a controlled recovery message.
Do not erase the allowlist to make startup succeed.
The old document's schema was a reference, not a shipped application. New installations start with this schema. If
an actual predecessor binary exists, its exact schema must be supplied before writing a migration; do not invent a
production migration from a chat example.
19 / Privacy, security and threat model
19.1 Data boundary
All enforcement decisions are local. The app has no internet permission, server, login, telemetry or ad SDK. It
stores only the minimum account identifiers and policy/evidence metadata required for this function. Broad
permission capability must be explained even though the parser deliberately minimizes what it reads.
No production storage of message bodies, notification previews, profile photos, raw accessibility trees, screen
images, OTPs, contacts databases, authentication tokens, session cookies, browsing history, precise location or
passwords. Debug traces use developer-controlled synthetic accounts and are excluded from release artifacts.
Disable backup with both legacy and current exclusion rules, including device-to-device transfer.
allowBackup=false alone may not disable every OEM transfer path. A no-backup installation marker and
qualification reset detect unexpected restoration where possible. On mismatch, preserve data inertly and require
reconnect/consent; never reactivate automatically. [S11]


## Source page 32

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
32
19.2 Threats and mitigations
Threat
Required defense
Business uses trusted brand
name on a new number
Exact number only; no name-based inheritance.
Message contains fake UI
instructions
Do not traverse message subtrees; whitelist structural roles.
Lookalike app uses target
package outside official
distribution
Check qualified signing lineage, not just package text.
Same name appears in multiple
chats
Resolve current exact identity before every mutation.
Screen changes between
detection and confirmation
Fresh node lookup, generation invalidation, short binding lease.
Another app sends crafted
intents to enable businesses
No exported mutation endpoints; launcher extras never grant authority.
Local database restored on
another device/login
Installation/namespace mismatch disarms automation.
Malicious or buggy adapter
update
Bundled, reviewed recipes only; no remote scripts or dynamic code.
Full phone compromise or root
Out of scope; do not claim protection from a privileged device attacker.
User needs to stop/remove app
Always respect Stop, force-stop, revocation and uninstall.
19.3 Component restrictions
Only the launcher Activity is ordinarily exported. System-bound accessibility and notification services are
exported only with their respective binding permissions. All internal receivers/providers are absent or
nonexported. No arbitrary URL deep link imports policy. No shell command execution, reflection into connected messenger
internals, local HTTP server or debug socket ships.
Use immutable PendingIntents created by this app. Clear sensitive overlays on lock and foreground loss. No
clipboard use except a deliberate Copy diagnostic action that contains no contact names/numbers or message
text. A human-readable local privacy explanation is available offline in the overflow dialog; the public
privacy-policy page is opened in the system browser only after a tap.


## Source page 33

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
33
20 / Native Android implementation
architecture
Single native Activity
Search, exact-number switches, inline status and Pause. No network or chat UI.
Policy + SQLite repository
Durable choices, revisions, observations, jobs and
grants.
Read-only observations
Accessibility profile evidence; optional opaque
notification hints.
Deterministic controller + guard engine
One active job. Fresh identity. Typed actions. Explicit Stop. No arbitrary scripts.
Qualified connected messenger adapter
Visible block/unblock flow, safe no-read navigation and fresh result verification.
connected messenger UI is the external dependency. Reference rule tests do not qualify that integration.
Design reference, not a connected messenger screenshot. All identities and counts are illustrative. Layout dimensions and behavior are specified below.


## Source page 34

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
34
20.1 Chosen stack
Responsibility
Native implementation
Application screen
Activity, LinearLayout, EditText, ListView, BaseAdapter, Switch, native
dialogs.
State and policy
Immutable Java objects; AtomicReference<PolicySnapshot>; pure
deterministic evaluator.
Persistent data
SQLiteOpenHelper, SQLiteDatabase, bound query parameters, one serial
ExecutorService.
Event scheduling
Main Handler, bounded delayed guards, monotonic SystemClock.
connected messenger integration
AccessibilityService, short-lived nodes and explicit adapter contracts.
Optional discovery
NotificationListenerService; opaque hints held in memory.
Theme / resources
Android XML drawables, native day/night resources, system font.
Payment
Paid store listing; no Billing library inside the app.
Serialization
Android-provided org.json for bundled recipe metadata; no external parser.
No Compose, AndroidX, Flutter, React Native, Room, Hilt, Dagger, WorkManager, Kotlin runtime, RxJava,
networking library, image loader or external automation SDK is required. Test-only tooling may be used when it
does not enter the runtime dependency graph; every addition must justify its maintenance cost.
20.2 Module boundaries
ui renders immutable rows and emits user intents; it never clicks connected messenger. data owns transactions and
publishes policy. policy knows no Android classes. automation owns the state machine and session lease.
adapter recognizes one qualified environment. service receives framework callbacks. safety checks the
environment and revocation. diagnostics exposes only sanitized counters and reason codes.
Prefer constructor injection with explicit factories over a dependency-injection framework. One application-level
repository owns the executor; Activities attach/detach listeners without retaining view references. No service
stores an Activity context.
20.3 Threading
Callbacks extract only bounded, necessary structural metadata on the service/main thread and convert it to an
immutable snapshot. Never pass live accessibility nodes to a worker and use them after the window changes.
Workers perform SQLite operations, search and pure policy evaluation. Main-thread action steps reacquire fresh
nodes and recheck the current immutable policy immediately before acting.
Every asynchronous result carries global revision, account revision, observation generation and query/session ID.
Drop results that no longer match. All exceptions produce a controlled state rather than falling through to a
permissive action.


## Source page 35

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
35
21 / Build configuration and repository layout
21.1 Reproducible baseline
Use Java source with JDK 17, Android Gradle Plugin 9.1.1, Gradle 9.3.1 and SDK Build Tools 36.0.0 as an explicit
compatible baseline, not a claim that these are the newest tools. [S08] Set minSdk=29, compileSdk=36,
targetSdk=36. The checked Play requirement for new phone-app submissions from 31 August 2026 is API 36 or
higher; recheck before submission. [S09]
No production dependencies block is necessary. Inspect AGP's automatically added artifacts and prevent Kotlin
stdlib packaging in a Java-only project. An empty explicit dependency block alone is not proof of zero runtime
libraries. Pin plugin/wrapper versions, verify wrapper distribution checksums, and reject dynamic versions.
Choose the final owned application ID before creating the paid listing. com.example.businessgate is used only in
sample code. Product name, signing identity, privacy-policy URL, developer contact and merchant details are
release-owned inputs, not fabricated values.
21.2 Repository tree
app/src/main/
  AndroidManifest.xml
  java/com/example/businessgate/
    GateApplication.java
    ui/MainActivity.java
    ui/GateListAdapter.java
    ui/UiRow.java
    ui/DisclosureController.java
    data/GateDbHelper.java
    data/GateRepository.java
    model/AccountRecord.java
    model/PolicySnapshot.java
    policy/RuleEngine.java
    service/GateAccessibilityService.java
    service/GateNotificationListener.java
    automation/AutomationController.java
    automation/SessionLease.java
    automation/ActionJob.java
    automation/ScreenSnapshot.java
    adapter/MessagingAdapter.java
    adapter/AdapterRegistry.java
    safety/SafetyGuard.java
    safety/NamespaceBinding.java
    diagnostics/LocalDiagnostics.java
  res/layout/activity_main.xml
  res/layout/row_business.xml
  res/layout/row_personal.xml
  res/layout/row_section.xml
  res/layout/row_details.xml
  res/layout/automation_stop_chip.xml
  res/xml/gate_accessibility.xml
  res/xml/backup_rules.xml
  res/xml/data_extraction_rules.xml
  res/values/strings.xml
  res/values/colors.xml
  res/values/styles.xml
  res/values-night/colors.xml
  assets/adapters/qualification-index.json


## Source page 36

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
36
21.3 First build sequence
Install the baseline JDK/SDK; create a Java Android application without template libraries; pin the Gradle
wrapper/plugin; add native layouts and the manifest; implement the pure policy and schema; run reference tests;
implement the read-only qualification probe; then add only qualified UI actions. Use ./gradlew clean
:app:assembleDebug :app:lint for the local build and lint pass. Use ./gradlew :app:dependencies
--configuration releaseRuntimeClasspath to audit dependencies. Build the signed release App Bundle only
after the release gates pass.
The supplied reference policy test runner compiles with ordinary javac and uses no external libraries. It is not a
compiled Android application and does not validate service or connected messenger behavior.
22 / Manifest, resources and framework
integration
22.1 Manifest contract
The launcher Activity is exported for launch only; ignore extras that request policy mutations.
GateAccessibilityService is exported with BIND_ACCESSIBILITY_SERVICE and an accessibility metadata
resource. GateNotificationListener is exported with BIND_NOTIFICATION_LISTENER_SERVICE. These are system
binding permissions, not user-grantable runtime permissions.
Declare a narrow package visibility query for qualified.target.package. Do not request QUERY_ALL_PACKAGES. Optional
POST_NOTIFICATIONS is the only ordinary runtime permission in the baseline. INTERNET, READ_CONTACTS,
SMS, phone, storage, camera, microphone, location, SYSTEM_ALERT_WINDOW, device admin, exact alarms,
wake locks and battery-optimization exemptions are absent.
The complete baseline manifest and XML examples are in the implementation annex. They require actual
class/resource implementation; declaring them does not create the detector.
22.2 Accessibility settings
Use packageNames="qualified.target.package", canRetrieveWindowContent=true, isAccessibilityTool=false, generic
feedback and the narrowly needed event types. Include view IDs and interactive-window metadata only where
needed to reject IME/foreign-window states. Package filtering is reinforced by checking every active target
window. It is not a permission scope that guarantees the service can only see one business profile.
Never call event.getText() merely to decide whether the user is typing. Text-change events can invalidate a
lease without reading their contents. Do not request screen capture, gestures, touch exploration, key filtering or
interactive coordinate injection.
22.3 Platform details that affect the simple screen
Use framework WindowInsets for status/navigation/IME padding, not fixed system-bar heights. The target-SDK
edge-to-edge behavior must be tested; search and bottom rows must remain visible above the keyboard. Support
day/night resource recreation while preserving query/scroll state in memory or saved instance state, never a
whole row adapter in a Bundle.
Stable IDs must derive from database IDs and typed section/header IDs. Detach switch listeners during row
binding, set the checked state, then reattach; otherwise recycled rows can emit accidental user toggles. Submit
immutable row snapshots and ignore clicks whose account revision no longer matches the rendered row until
reloaded.


## Source page 37

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
37
23 / Adapter qualification and maintenance
23.1 Required adapter contract
interface MessagingAdapter {
    boolean supports(TargetBuild build, UiLocale locale);
    ScreenKind classify(ScreenSnapshot snapshot);
    AccountEvidence inspectAccount(ScreenSnapshot snapshot);
    NamespaceEvidence inspectReceiver(ScreenSnapshot snapshot);
    NavigationPlan directProfileRoute(VerifiedInboxRow row);
    ActionPlan nextBlockStep(VerifiedContext context);
    ActionPlan nextUnblockStep(VerifiedContext context);
    BlockObservation inspectBlockState(ScreenSnapshot snapshot);
}
ActionPlan is an internal typed action from a finite enum, not JavaScript, arbitrary code or a downloaded macro.
Each plan contains the required screen kind, role locator, expected next screen, binding requirements, side-effect
exclusions and timeout. Only fixed reviewed implementations exist.
23.2 Qualification output
For every qualified environment, create a signed-off record with: QA ID; package/signing lineage; connected messenger
version code; observed UI locale; Android build/OEM; target/receiver identity evidence path; business and
ordinary-profile schema signatures; direct-profile navigation steps; confirmation roles; report/delete exclusion;
verification roles; window-transition constraints; fresh/blocked/unblocked fixtures; failure cases; no-read
evidence; timings; reviewer and date.
Resource IDs, roles and labels are acquired by the integration engineer on developer-controlled test accounts. No
identifier is invented in this document. The companion qualification JSON starts with qualified=false and an
empty environment list. Production activation remains disabled until a real record has been completed and
accepted.
23.3 Compatibility policy
Initial language: English connected messenger UI; app labels also English. Device language may differ from connected messenger
language. Support is by qualified build, language, structure and device family, not merely one literal English label.
Unknown builds and changed signatures stop mutations immediately.
Adapters ship in normal signed application updates. There is no remote executable recipe, automatic LLM repair,
covert fallback or downgrade instruction. An installed older version can safely refuse a newer connected messenger layout.
Readiness errors appear once; they do not ask the user to keep manually trying every day.
The maintenance owner monitors public release channels, validates candidate builds on test phones, updates
fixtures and ships the normal app update. This is a human release process, not a promised background service
operated by the PRD author. No backend is required by the installed application.


## Source page 38

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
38
24 / Quality, performance and measurement
24.1 Required targets
Metric
Proposed release threshold
False block of personal, unknown
or enabled account
Zero observed in all release tests; any occurrence is release-blocking.
Wrong report, delete, send, call
or archive action
Zero; any occurrence is a critical safety defect.
Successful eligible block/unblock
sequences
At least 99%; separately publish unsupported and interrupted cases.
Honest UI result labels
100% of “Blocked” results have matching recorded UI evidence.
Search with 10,000 local records
p95 under 100 ms query-to-result after debounce, reference device stated.
Main page first usable render
p95 under 500 ms warm / 1,000 ms cold, after process start.
Ordinary toggle persistence
p95 under 150 ms; no optimistic external mutation before commit.
Automatic visible enforcement
occupancy
Target p95 <= 2 seconds; six-second hard session cap.
Monthly effort
p95 <= 60 seconds including imposed waiting and repairs.
Idle resource work
No periodic polls, wakes, alarms or permanent foreground service.
Additional battery drain
Target <= 0.5 percentage point per 24h against matched baseline; report noise and
repeats.
Release package
Target download <= 5 MB; no bundled fonts/images/native libraries.
Local resource limits
256 in-memory hints; bounded trees; 5,000/30-day events; 50,000-account ceiling.
A zero-observed-failure test suite does not prove a zero statistical risk. Do not market these thresholds as a
mathematical safety guarantee. User interruption is separately counted and must not become a convenient way to
hide bad completion rates.
24.2 Device matrix
Use physical devices representing an AOSP-like/Pixel device, Samsung and Xiaomi/another intended OEM.
Qualify each claimed Android/connected messenger combination, including Android 10 baseline through the chosen
supported current releases. Emulators validate layouts and lifecycle logic, not the final connected messenger accessibility
integration.
Test small screens, 200% font size, 60/120 Hz, day/night, navigation modes, keyboard open, rotation,
multi-window, battery saver, Doze, permission revocation, low storage, process death, app restore, connected messenger
update and locale change. A device/OS that cannot satisfy the safe route must be explicitly unsupported.


## Source page 39

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
39
24.3 Local diagnostics
Reason-code counters: attempted, verified, skipped-personal, skipped-unknown, identity-mismatch,
unsupported-layout, interrupted, verification-timeout and service-disconnected. Do not maintain a leaderboard or
weekly report. Diagnostics are available only on demand in an overflow dialog.
The sanitized copyable diagnostic string contains app/adapter version, OS API, OEM model, target
version/language, readiness flags and reason counts. Exclude phone numbers, names, opaque conversation IDs,
receiver fingerprints, message text and raw trees. Copy requires an explicit user action.
25 / Pricing, store policy and legal review
25.1 Paid download
The commercial model is a US$1 base-price goal on Google Play with appropriate local prices configured in Play
Console. This is not a promise to charge for every reinstall. No billing SDK, in-app paywall, subscription, license
server or piracy-detection network check is part of v1. Store pricing and availability are configured outside the
app. Once publicly offered free, an app cannot simply be changed to paid under the same package; establish the
paid listing correctly. [S12]
Developer setup must include final application ID, account identity verification, merchant/payment setup as
applicable, taxes and regional availability, support contact, privacy-policy page, signing and upload-key
management. These are release-owner tasks. Do not substitute invented business or financial details.
25.2 Accessibility and data disclosures
Google Play's checked guidance permits narrow deterministic automation while prohibiting autonomous
planning/decision flows of the described kind. This app uses a fixed human-authorized rule, not an agent that
invents goals. Approval is still required and is not guaranteed. It must not claim to be a disability accessibility tool.
[S13]
Provide the accessibility declaration, separate prominent in-app disclosure, affirmative consent, store explanation
and review video. The video shows acceptance and refusal, setup, one denied business, an enabled business left
alone, a personal account left alone, visible Stop, pending/unsupported states and the optional listener disclosure.
Data safety answers follow the actual binary. Pure on-device processing has different disclosure treatment from
transmission, but this does not remove the need to explain broad access in the app. Audit manual
diagnostics/browser handoffs and store-side data before finalizing answers. [S14]
For applicable personal developer accounts created after 13 November 2023, the checked production-access
process includes at least 12 closed testers continuously opted in for 14 days. The product's own 30-day attention
beta is a separate, stronger research requirement. [S15]
25.3 connected messenger terms and naming
connected messenger's terms include restrictions on unauthorized automated use, collection and commercial exploitation.
Selling this companion requires review of the specific integration and marketing; Play approval alone is not
connected messenger authorization. The document does not conclude that the commercial integration is permitted or that
account restrictions are impossible. [S16]
Do not use connected messenger/Meta logos, claim partnership or name the app as an official product. “Business Gate” is a
working name pending availability/trademark checks. No account credentials, QR linking or unofficial protocol
libraries are introduced as a workaround.


## Source page 40

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
40
25.4 Listing draft and claims to avoid
Draft: “Keep your chosen connected messenger business numbers in one simple list. Business Gate uses Accessibility to apply
your block choices to detected business accounts on supported Android and connected messenger versions. Personal and
unclassified accounts are left alone. New accounts must be identified before blocking; first messages may arrive.
Changes may run visibly when connected messenger is accessible. Local operation. No ads, subscription or cloud account.
Not affiliated with connected messenger or Meta.”
Do not publish “blocks every business forever”, “prevents the first message”, “works invisibly while locked”, “all
contacts scanned”, “never needs updates” or “under one minute per month” before the relevant evidence exists.
Refund/support policies must respect store rules and applicable consumer law; do not invent a universal policy in
the UI.
26 / Execution backlog and ownership
Each work package produces a reviewable artifact. Do not start with a broad automation framework or a polished
dashboard. The first risky dependency is the safe connected messenger integration.
Work
package
Owner role
Deliverable
Exit gate
B01
Product
contract
Product
Approved R01-R16 and attention
definition
No contradictory promise of
invisible universal blocking.
B02
Platform qu
alification
Android integration + QA
Completed environment JSON,
fixtures, no-read video, timings
G0 passes with production
permissions.
B03 Pure
policy
Android/core
Evaluator, transition tests,
revision/nonce model
All safety decisions pass
without Android.
B04
Storage
Android/data
SQLite schema, repository, recovery
and migration tests
Durable choices; failure
disarms; no destructive reset.
B05
One-page
UI
Android/UI + design
Native layouts for all visual states
Search, grouping, toggle races,
large text and TalkBack pass.
B06 Service
observation
s
Android/platform
Read-only service + optional hint
cache
No message reads/logs;
personal traffic unchanged.
B07 Action
engine
Android integration
Typed state machine, Stop overlay,
journal, block/unblock routes
Identity and side-effect tests
pass on physical phones.
B08
Lifecycle
and safety
Android + QA
Revoke/kill/restore/update/circuit-br
eaker handling
No stale authority or replayed
clicks.
B09 Quiet
usability
Product + QA
30-day attention report and coverage
report
p95 <= 60 seconds without
silent failure.


## Source page 41

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
41
Work
package
Owner role
Deliverable
Exit gate
B10 Privac
y/security
Reviewer
Manifest, dependency, log, backup
and component audit
No forbidden
data/permissions/endpoints.
B11
Store/legal
Release owner
Declarations, video, listing, pricing,
review record
Actual approval and
integration review; no inferred
authorization.
B12 Releas
e/maintena
nce
Release owner + QA
Signed AAB, rollout/rollback plan,
qualified build list
Every P0 test passes; support
path works.
Definition of done per feature
Code implemented; behavior demonstrated on the declared environment; unit/fixture/device tests applicable to
the feature pass; UI text matches the catalog; error and cancellation behavior implemented; no forbidden side
effects; privacy/dependency impact reviewed; requirement ID and test IDs linked; documentation updated.
Implementation sequence
B01 and B02 first. B03-B05 can proceed against synthetic snapshots once the feasibility contract is clear. B06
precedes B07. B08 and security tests run continuously, not as final polish. B09 uses the integrated real-device
build. B10-B12 are release gates, not paperwork to complete after customer purchases.
27 / Release gates and operating playbook
Required evidence before charging
G0: real-phone business/identity/no-read/block/unblock qualification. G1: all pure safety tests and schema
constraints. G2: single-page UI and accessibility verification. G3: physical-device mutation/recovery suite with no
critical side effects. G4: privacy and dependency audit. G5: target-cohort coverage and monthly effort evidence.
G6: store declarations, actual approval and terms review. G7: signed rollout and maintenance ownership.
The companion test catalog contains concrete acceptance cases. Any personal/allowed wrong block,
report/delete/send, permission bypass, namespace confusion or false “blocked” success is P0 and prevents
release. A feature's visual implementation does not downgrade its failed gate.
Release procedure
Freeze the qualified environment matrix and fixture hashes. Build from a clean tagged revision. Run pure tests,
schema tests, lint, release dependency audit and instrumentation/device suite. Inspect merged manifest,
APK/AAB contents, signing configuration, backup exclusions, exported components and logs. Verify that fixture
recorders and debug action endpoints are absent. Check privacy/listing text against the binary, not a planned
architecture.
Use internal/closed distribution first, then a staged production rollout supported by the store. Monitor
store-provided crash/review signals and consented reports; no new telemetry SDK is silently added. If a safety
failure is found, halt distribution, publish a fixed signed version that disarms the affected adapter, and inform
affected users through the available release/support channels. There is no remote kill switch in an offline app;
local circuit breakers reduce but do not eliminate the exposure until updates arrive.


## Source page 42

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
42
Compatibility incident
Reproduce using a test account; compare the exact failed screen to the qualified fixture; decide whether it is a
new build, server-side layout variant, locale, OEM or account-context change; add a failing test before adjusting
the adapter. Never broaden selectors merely to make a demonstration work. Re-run the entire mutation safety
suite on the affected flow, plus monthly-attention regression scenarios.
Product stop conditions
Do not release the paid blocking promise if the required fields are protected, receiver binding is unreliable, a safe
no-read route is unavailable, user interruptions can misdirect clicks, the attention budget fails in ordinary target
use, or platform review rejects the use. Continue engineering with explicit experiments rather than shipping a
different product disguised as this one.


## Source page 43

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
43
Appendix A / Complete copy catalog
Surface / key
Exact English text
APP_TITLE
Business Gate
SEARCH_HINT
Search business or number
ENABLED_HEADER
Enabled by you
DENIED_HEADER
Not enabled
PERSONAL_HEADER
Personal - left alone
UNKNOWN_HEADER
Not classified - left alone
RULE_READY
Rule on
RULE_READY_DETAIL
New business numbers are checked when accessible.
RULE_PAUSED
Paused
RULE_PAUSED_DETAIL
Existing connected messenger blocks have not changed.
PAUSE / RESUME
Pause / Resume
APPLY_NOW
Apply now
STOP
Stop
EMPTY
No businesses found yet
EMPTY_DETAIL
Businesses appear here after a supported account check.
NO_MATCH
No matching accounts
NO_MATCH_DETAIL
Try a business name or the full phone number.
PERSONAL_DETAIL
Not subject to automatic blocking
BLOCK_PENDING
Block pending
UNBLOCK_PENDING
Unblock pending
OBSERVED_BLOCKED
Blocked - checked {relative_date}
UNVERIFIED
Block state not verified
EXTERNAL_BLOCK
Enabled here; blocked in connected messenger


## Source page 44

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
44
Surface / key
Exact English text
UNBLOCK_ACTION
Unblock now
PERMISSION_OFF
Accessibility is off
PERMISSION_ACTION
Open settings
VERSION_UNSUPPORTED
connected messenger compatibility check needed
UNSUPPORTED_DETAIL
This connected messenger layout is not qualified. No changes will be made.
SAFE_STOP
Automation stopped safely
ACCOUNT_CHANGED
connected messenger account changed
SAVE_FAILED
Could not save your choice. Nothing was changed.
APPLY_NOTICE
Applying your choices
OVERLAY
Business Gate - applying {count} choice(s)
REVIEW_COVERAGE
Inspected {count} accessible profiles; {skipped} skipped.
NEW_NUMBER_DETAIL
A new number needs its own permission.
START_BUTTON
Start blocking other businesses
NOTIFICATION_OFFER
Notice new senders sooner
NOTIFICATION_DETAIL
Optional notification access provides discovery hints. It does not block notifications
or guarantee detection.
NOTIFICATION_DECLINE
Not now
SERVICE_NOTICE
Business Gate needs attention
SERVICE_NOTICE_DETAIL
Automatic blocking is paused. Open the app for details.
RESET_TITLE
Delete local choices and history?
RESET_DETAIL
This stops future automation. It does not unblock anyone in connected messenger.
RESET_CONFIRM
Delete local data
CANCEL
Cancel
Pluralization uses Android plural resources, not concatenated English. Dates use locale-aware formatting. Do not
display raw internal error codes as primary copy; keep them in expanded diagnostics. Accessibility and activation
disclosure prose is specified in section 08 and must not be shortened until it loses the described consequences.


## Source page 45

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
45
Appendix B / Baseline configuration examples
B.1 App module
plugins { id 'com.android.application' }
android {
    namespace 'com.example.businessgate'
    compileSdk 36
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
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile(
                'proguard-android-optimize.txt'),
                'proguard-rules.pro'
        }
    }
}
dependencies { /* No runtime libraries. */ }
Root plugin declaration pins com.android.application to 9.1.1 with apply false. Repositories are Google and
Maven Central for build tooling only; no runtime dependency is introduced by this statement. Gradle wrapper is
pinned to 9.3.1. Store secrets and signing passwords outside version control. Do not include private keys in any
handoff archive.


## Source page 46

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
46
B.2 Manifest
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <queries><package android:name="qualified.target.package" /></queries>
    <application
        android:name=".GateApplication"
        android:label="@string/app_name"
        android:theme="@style/AppTheme"
        android:allowBackup="false"
        android:fullBackupContent="@xml/backup_rules"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:supportsRtl="true">
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
            android:label="@string/app_name"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
The exported services are protected by system binding permissions. Do not expose additional binder methods that
allow policy changes. POST_NOTIFICATIONS is optional at runtime; notification listener access is a different
user-granted setting.
B.3 Accessibility metadata
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_description"
    android:packageNames="qualified.target.package"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeWindowsCha
    nged|typeViewScrolled|typeViewClicked|typeViewTextChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:notificationTimeout="250"
    android:canRetrieveWindowContent="true"
    android:isAccessibilityTool="false" />
The long XML event-type line is one attribute. Validate resource compilation on the baseline. The service must
ignore text payloads, inspect only necessary target metadata, and reject nonqualified windows regardless of the
event package filter.


## Source page 47

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
47
B.4 Backup resources
Legacy backup_rules.xml excludes root, file, database and sharedpref domains with path=".". Modern
data_extraction_rules.xml contains the same exclusions in both cloud-backup and device-transfer. Include
device-protected variants if any device-protected storage is later added; the baseline does not use it. Place the
independent installation marker in getNoBackupFilesDir() and check it before loading active authority. Test real
OEM transfer behavior. [S11]
<data-extraction-rules>
  <cloud-backup>
    <exclude domain="root" path="." />
    <exclude domain="file" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
  </cloud-backup>
  <device-transfer>
    <exclude domain="root" path="." />
    <exclude domain="file" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
  </device-transfer>
</data-extraction-rules>
Appendix C / Engineering interfaces and action
pseudocode
C.1 Immutable evidence
AccountEvidence contains namespace, canonical remote number, classification, observed block state, target
build/adapter, structural evidence token, wall-clock observation time, monotonic session time and generation. It
never contains chat text or live UI nodes. A boolean business=true with no identity and freshness evidence is
insufficient.
PolicySnapshot contains global revision, activation/pause/consent state, namespace and an immutable map of
per-account enabled state/revision. SessionLease binds one qualified foreground flow to that snapshot, target
identity and generation with a deadline.


## Source page 48

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
48
C.2 Guarded action
onCandidate(evidence):
    require minimal, immutable, supported evidence
    persist observation without changing user preference
    decision = evaluate(latestPolicy, freshEvidence)
    if decision is not eligible: stop or retain pending
    journal job intent with revisions and exact identity
    request a qualified safe session
beforeEachStep(job):
    reacquire current target window and supported role
    verify session, receiver, target number and foreground
    verify latest global and per-account revisions
    verify allowed state / explicit unblock authorization
    verify no report/delete/send/call side effect
    if any check fails: abort; never click a fallback
    perform the single typed action
    wait for the expected observed transition, not a sleep
afterFinalAction(job):
    reacquire profile and exact identity
    read qualified current block indicator
    persist observed result, not an assumed server state
    if revisions changed: reconcile without overwriting choice
    consume one-shot authority only on confirmed satisfaction
C.3 Authority and clocks
Policy writes and action grants use durable revisions. UI freshness uses monotonic time within the current
process/session; a device clock change cannot extend a UI lease. Persisted timestamps are useful for display and
retention only, not for resuming an old action. After process restart, all prior UI leases expire regardless of their
wall-clock timestamp.
C.4 Error vocabulary
Controlled reasons: POLICY_UNLOADED, RULE_PAUSED, CONSENT_MISSING, TARGET_UNSUPPORTED,
SIGNATURE_MISMATCH, NAMESPACE_MISMATCH, IDENTITY_MISSING, IDENTITY_CHANGED,
TYPE_UNKNOWN, TYPE_PERSONAL, TYPE_NON_DIRECT, TYPE_NOT_ELIGIBLE_FOR_UNBLOCK,
STALE_EVIDENCE, ENABLED, ALREADY_SATISFIED, UI_UNSAFE, USER_INTERRUPTED, LEASE_EXPIRED,
CONFIRMATION_AMBIGUOUS, SIDE_EFFECT_RISK, ACTION_UNCERTAIN, VERIFY_TIMEOUT,
STORAGE_FAILURE CIRCUIT_OPEN, INVALID_INPUT, POLICY_CHANGED, NOT_CURRENT_BUSINESS,
NO_UNBLOCK_AUTHORITY, BLOCK_STATE_UNKNOWN, EXPLICIT_AUTHORITY and
CONFIRMED_BUSINESS_DENIED. The final four action-decision reasons are internal typed values, not
user-facing error copy. Do not store arbitrary exception strings that can contain screen data.


## Source page 49

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
49
Appendix D / Acceptance tests and traceability
Every case uses consenting developer-controlled accounts. “No mutation” means no
block/unblock/report/delete/archive/send/call/notification operation. Test current UI state and persisted state,
not just a returned boolean.
D.1 Identity and classification
ID /
requirement
Scenario
Required result
T001 / R04
R05
New confirmed business, exact number,
all guards valid
Queue and verify Block; OFF remains the desired policy.
T002 / R05
Same denied brand sends from a
second business number
Treat separately and deny; no brand-recognition service
required.
T003 / R05
Same name/logo as an enabled account,
different number
Do not inherit the enabled exception.
T004 / R04
Personal account with a bank-like
display name
No block; display name is not classification evidence.
T005 / R07
Chat message contains Business
account or Block
Ignore message subtree; no classification or click from
those words.
T006 / R04
Business marker missing from a
partial/protected profile
UNKNOWN or AMBIGUOUS; no mutation.
T007 / R05
Phone missing, local-format-only or
replaced by a username
No guessed country code or name identity; no mutation.
T008 / R05
Phone-shaped number appears in
business description
Do not use it as the profile identity.
T009 / R04
Group, channel, community or system
thread has business members
NON_DIRECT; no member, group or notification action.
T010 / R05
Two visible profiles have duplicate
names
Bind exact number before action; position/name cannot
select identity.
T011 / R02
Receiving connected messenger account changes
mid-session
Invalidate namespace and lease; no cross-account policy
application.
T012 / R01
R02
Cloned/work-profile/connected messenger
Business receiving package
Unsupported; do not attach the ordinary-account
allowlist.
T013 / R15
Package name matches but signing
lineage does not
Disarm; show compatibility issue without fallback.
T014 / R05
Phone has bidi controls, invalid
characters or over 15 digits
Reject identity; sanitize display only, never silently
normalize ambiguity.


## Source page 50

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
50
D.2 Personal-cache protection
ID /
requirement
Scenario
Required result
T015 / R06
Known fresh personal sender sends
repeated messages
Skip routine profile inspection; notification and chat
unchanged.
T016 / R06
New personal sender not in cache
No block because the cache lacks the number.
T017 / R06
Personal observation reaches 30 days
Mark STALE; no forced visit and no queued block.
T018 / R06
Repeated personal notifications over 30
days
last_seen may advance; verified_at and reusable
classification do not.
T019 / R04
R06
Fresh business evidence contradicts old
personal cache
Validate and persist independently; then apply exact
current exception.
T020 / R06
Business converts to ordinary personal
profile
Cancel future automatic blocks; do not automatically
undo an existing block.
T021 / R06
Personal label changes, but account
type is not observed
Update/invalidate label cache only; do not infer a
business conversion.
T022 / R06
Idle personal row qualifies for 90-day
pruning
Delete local cache only; leave connected messenger unchanged.
T023 / R05
R06
Enabled former business is now
personal
Keep exact exception; show no personal blocking switch.
T024 / R06
R15
Adapter update or reinstall invalidates
personal evidence
UNKNOWN/STALE, not BUSINESS; no automatic deny
from invalidation.


## Source page 51

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
51
D.3 Policy, blocking and explicit unblocking
ID /
requirement
Scenario
Required result
T025 / R05
R09
Enable an unblocked known business
Commit ON first; cancel blocks; no unnecessary unblock
click.
T026 / R13
Enable a currently blocked known
business
Create one-shot authority; show unblocking pending until
verified.
T027 / R13
App restarts with pending explicit
unblock
Keep valid grant, but reacquire all UI/identity evidence
from scratch.
T028 / R13
R14
Stop/Pause occurs before unblock
completes
Cancel grant and generation; retain ON and display
mismatch.
T029 / R13
Saved ON account is later manually
blocked in connected messenger
No automatic unblock; require explicit Unblock now.
T030 / R04
Denied business is manually unblocked
while rule active
Reblock only after fresh safe business observation; do not
undo unrelated changes.
T031 / R09
Switch OFF while connected messenger
unavailable
Save preference; show Block pending, not Blocked.
T032 / R09
Storage write fails on toggle
Keep previous committed switch; no external action.
T033 / R09
R13
OFF-to-ON races with final block
confirmation
Preserve ON; record actual effect; reconcile only with
valid grant.
T034 / R09
R13
ON-to-OFF races with pending unblock
Invalidate old grant/job; no stale result overwrites the
latest choice.
T035 / R13
Previously known business has
unknown current type but exact
blocked identity
Only explicit valid known-business unblock route may
act; no general unblock tool.
T036 / R09
Already blocked business is
encountered again
Record fresh matching observation; do not repeat block.
T037 / R13
Replayed nonce or obsolete revision
after success
Reject; no duplicate or future perpetual unblock
authority.
T038 / R09
Offline UI shows blocked
Label as connected messenger UI observation; test actual sender
behavior after reconnection separately.


## Source page 52

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
52
D.4 No-read discovery and interruption
ID /
requirement
Scenario
Required result
T039 / R08
First discovery sees unread personal
and business chats
Use qualified direct profile route; preserve read/unread
and receipts.
T040 / R08
R16
Only available profile route opens an
unread chat
Do not run it automatically; fail low-attention integration
gate.
T041 / R08
User manually opens an unread chat
User navigation is not reversed; app performs no
additional read operation.
T042 / R08
Locked, hidden or archived chats not
safely enumerated
Skip and record coverage honestly; no unlock/archive
bypass.
T043 / R08
R14
User starts typing or opens attachment
picker/call
Abort automation; do not press Back after losing the
lease.
T044 / R14
User taps outside Stop chip during
mutation
Pass touch through; invalidate unexpected transition and
reacquire before any click.
T045 / R14
User presses Stop
Persist pause, revoke grants and leases, remove overlay;
no next action.
T046 / R14
Phone locks or screen turns off
No wake/unlock; discard active lease and retain safe
pending intent.
T047 / R14
Another app or system dialog takes
focus
No takeover or click-through; pause current sequence.
T048 / R08
Inbox reorders after arrival or
pinned-row changes
Resolve current row identity anew; never trust old
coordinates/index.
T049 / R08
Discovery repeats same items or
reaches its limit
Stop with checkpoint; report actual inspected scope, not
all chats.
T050 / R14
Flow interrupted immediately before
planned Back action
Do not navigate Back; preserve the user's new context.
T051 / R14
R15
Overlay would cover the required
confirmation control
Reposition only through qualified layout; otherwise abort.


## Source page 53

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
53
D.5 Automation, lifecycle and adversarial UI
ID /
requirement
Scenario
Required result
T052 / R15
Confirmation dialog offers Report or
Delete
Verify safe unchecked state; never report/delete; abort if
separation uncertain.
T053 / R15
Unexpected button with text Block
appears elsewhere
Do not match by loose text; require supported structural
role.
T054 / R15
connected messenger build, language or
protected-view structure changes
Stop mutation immediately, independent of retry counter.
T055 / R14
performAction returns true but result
never appears
Do not mark Blocked; reobserve and show
uncertain/pending result.
T056 / R14
Process dies before/after final
confirmation
Journal survives; re-inspect before retry; never replay the
last click.
T057 / R15
A stale AccessibilityNodeInfo points to
another window
Never reuse; reacquire and bind fresh node.
T058 / R14
Six-second automatic lease expires
Stop safely; no extension by wall-clock changes or
repeated sleeps.
T059 / R14
R15
Three structural failures in same
environment
Open circuit; suppress endless retry loops and repeated
user prompts.
T060 / R10
Notification/window event storm
Coalesce and bound work; one active mutation; no
battery-draining loop.
T061 / R14
Service reconnects before policy has
loaded
Disarmed until consent, storage, namespace and adapter
checks pass.
T062 / R14
Permission revoked, force-stop or
uninstall
No watchdog; future automation stops; no mass unblock.
T063 / R15
Monotonic lease valid but device wall
clock changes
Action deadline does not extend; display timestamps may
differ only.
T064 / R10
Malicious intent/deep link requests a
block or enable
Reject; exported Activity accepts navigation only, not
mutation commands.


## Source page 54

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
54
D.6 Notifications, privacy and storage
ID /
requirement
Scenario
Required result
T065 / R07
Notification from any package other
than connected messenger
Reject before reading extras; no content/log/storage.
T066 / R07
Redacted or empty connected messenger
notification
No invented sender/type; bounded opaque hint only
when available.
T067 / R07
Group summary or duplicate
notification key
Ignore/deduplicate; no business verdict or repeated job.
T068 / R07
Personal connected messenger notification
remains visible
No cancel/snooze/reply/mark-shown/channel change.
T069 / R07
Hint queue exceeds 256 or 24-hour age
Drop bounded unresolved hints; no personal detail in
diagnostic.
T070 / R07
Notification access or
POST_NOTIFICATIONS denied
Core safe accessibility behavior remains; no repeated
permission nag.
T071 / R11
Release dependency and permission
audit
No third-party runtime library, INTERNET, contacts,
camera, SMS, root or remote scripts.
T072 / R07
Inspect production logs/database/crash
diagnostics
No message bodies, OTPs, photos, screenshots or raw
trees.
T073 / R09
Restore/transfer or missing installation
marker
No active automation until fresh installation/account
review.
T074 / R09
Schema migration fails or database is
corrupt/full
Disarm and preserve existing data; never
destructive-fallback.
T075 / R05
Duplicate namespace/phone write or
orphan foreign key
Reject invalid write; no silent policy replacement.
T076 / R06
R09
50,000 durable account threshold
Preserve exceptions/history; stop optional cache growth
and fail safely if new authority cannot be saved.
T077 / R07
Privacy or support link opened
Use external browser; no implicit account/diagnostic
upload.


## Source page 55

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
55
D.7 One-page UI and low-attention experience
ID /
requirement
Scenario
Required result
T078 / R03
Empty installation with no permissions
Single setup card, clear inactive state; no fake discovered
directory.
T079 / R03
Search matches enabled and denied
businesses
Enabled section stays first; query persists through toggle.
T080 / R03
R06
Search matches a personal record
Show after business matches, without switch or block
action.
T081 / R03
Rapid toggles while ListView recycles
rows
Detach listeners on bind; stable IDs; only intended exact
account changes.
T082 / R03
Toggle moves row between sections
Preserve visible anchor and query; no jump to top or
accidental second action.
T083 / R03
Names include apostrophe, %, _,
Unicode or bidi controls
Bound escaped search; safe label rendering; exact
number unchanged.
T084 / R03
No search matches or no businesses
discovered
Explain locally; no network lookup, forced scan or upsell.
T085 / R03
200% font, TalkBack, 320 dp width,
dark theme
No clipped decisions; 48 dp targets; state announced
without color alone.
T086 / R03
Keyboard, rotation, insets and process
recreation
Restore query/focus/anchor safely; no replay of switch
events or permission consent.
T087 / R12
Quiet month with no exception changes
or faults
No routine app visits, success notifications, digests or
review chores.
T088 / R12
New-business-heavy user exceeds
attention budget
Record all work and waiting; fail target, do not exclude
unsupported cases.
T089 / R12
Permission/compatibility repair after
initial setup
Count imposed time in T30; no special exclusion for
inconvenient failures.
T090 / R12
Management and automation intervals
overlap
Count interval union, not sum; preserve separate
diagnostic components.
T091 / R12
Thirty-day real-user pilot
Report p50/p95 effort and all-case coverage, false blocks,
faults and unsupported users.


## Source page 56

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
56
D.8 Release and commercial completeness
ID /
requirement
Scenario
Required result
T092 / R16
Only reference evaluator tests have
passed
Do not claim Android automation is qualified or sell a
working blocker.
T093 / R16
Store declaration differs from release
behavior
Block release until disclosure, consent, video and
behavior agree.
T094 / R16
Platform/legal approval is absent or
adverse
Do not sell or hide the limitation behind notification
muting.
T095 / R11
R16
Paid listing and update/reinstall testing
No in-app billing flow or per-reinstall fee; verify actual
Play account pricing.
T096 / R12
R16
Compatible connected messenger versions update
frequently
Include real repair burden in pilot and maintenance
model.
T097 / R16
Stop-ship safety incident after rollout
Halt rollout, test a corrected signed update; no claim of a
remote kill switch.


## Source page 57

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
57
Appendix E / Evidence register and open
integration inputs
All references were checked on 9 September 2026. Sources establish platform behavior, not the correctness of
Business Gate. The source register and engineering requirements are separate: measurements, thresholds, design
tokens and policy choices are proposed by this specification.
[S01] Android application sandbox. Application data isolation; why a normal app cannot read connected messenger's private
database.
https://source.android.com/docs/security/app-sandbox
[S02] Create an accessibility service / AccessibilityService. System-bound accessibility, exposed node actions and
event-based observation. The required connected messenger fields still need measurement.
https://developer.android.com/guide/topics/ui/accessibility/service
Companion API reference: AccessibilityService.
[S03] connected messenger: block/unblock a business; blocking and reporting. Business-level controls; related blocking FAQ
confirms that messages sent during a block are not recovered by later unblocking.
[external service reference removed]
Related FAQ: How to block and report someone.
[S04] NotificationListenerService API. Notification callbacks and current notifications; not a connected messenger
account-type or blocking API.
https://developer.android.com/reference/android/service/notification/NotificationListenerService
[S05] Android 15: behavior changes for all apps. Sensitive-notification restrictions and redaction; no
completeness assumption.
https://developer.android.com/about/versions/15/behavior-changes-all
[S06] Restrictions on starting activities from the background. Background-launch limits and exceptions. Business
Gate chooses no takeover even where an exception might apply.
https://developer.android.com/guide/components/activities/secure-bal
[S07] Enhancing Android security: sensitive accessibility data. Protected views and the need for truthful
isAccessibilityTool declarations.
https://developer.android.com/blog/posts/enhancing-android-security-stop-malware-from-snooping-on-your-ap
p-data
[S08] Android Gradle Plugin 9.1.1 release notes. Pinned toolchain baseline: Gradle 9.3.1, JDK 17 and Build Tools
36.0.0.
https://developer.android.com/build/releases/agp-9-1-0-release-notes
[S09] Google Play target API requirements. Published submission target requirements; recheck before release.
https://developer.android.com/google/play/requirements/target-sdk
[S10] SQLiteOpenHelper API. Framework database creation and migration without a third-party ORM.
https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper


## Source page 58

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
58
[S11] Back up user data with Auto Backup. Legacy and modern backup rules; device-to-device transfer behavior
needs separate review/testing.
https://developer.android.com/identity/data/autobackup
[S12] Play Console: set up app prices. Paid listings and local prices; a previously free package cannot later become
paid.
https://support.google.com/googleplay/android-developer/answer/6334373?hl=en
[S13] Play policy: AccessibilityService API. Narrow deterministic human-defined automation, disclosure and
consent; no assurance of approval.
https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
[S14] Play Console: Data safety form. Evaluate actual collection/sharing and SDK behavior; local processing is not
a blanket exemption from all policy duties.
https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
[S15] Testing requirements for new personal developer accounts. Applicable production-access testing
requirements; check the actual developer account.
https://support.google.com/googleplay/android-developer/answer/14151465?hl=en
[S16] connected messenger Terms of Service. Separate platform/commercial review; this PRD is not legal authorization.
[external service reference removed]
[S17] SigningInfo API. Package signing information; qualification must account for valid signing history rather than
an invented hash.
https://developer.android.com/reference/android/content/pm/SigningInfo
[S18] Make apps accessible: Views. Accessible labels and recommended minimum 48 dp interactive target;
app-specific layouts must still be tested.
https://developer.android.com/guide/topics/ui/accessibility/views/apps-views
[S19] WindowManager.LayoutParams: TYPE_ACCESSIBILITY_OVERLAY. Accessibility overlay window type
used for the visible Stop control.
https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_
OVERLAY
Inputs that must be acquired, not guessed
The integration owner supplies measured connected messenger locators, current signed package identity, receiver binding
and no-read route. QA supplies physical-device timings, test evidence and coverage. The release owner supplies
final app identity, pricing, privacy URL, contact, signing setup and actual approval. These inputs are explicitly gated
in the qualification template; leaving them absent disables activation.
Final release decision
Build the smallest reliable experience, not the smallest-looking demo. The app succeeds when ordinary
conversations need no attention, business exceptions take one search and one switch, and the implementation
never confuses a user's preference with an action it has not actually verified.
The nonnegotiable rule remains: only a currently, positively identified business at an exact non-enabled number
may be automatically blocked. Every uncertain case is left alone.


## Source page 59

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
59
Appendix F / Executable database and
reference verification
F.1 New-install schema
The following schema matches the companion SQL. Application code enforces controlled reason strings, label
length, monotonic session freshness and explicit grants; SQL constraints do not replace those guards. A saved
nonce is authorization metadata, never proof that the visible connected messenger target is correct. Keep foreign keys
enabled, use bound arguments and a serial writer.
-- Business Gate v2.0 / new-install reference schema, SQLite.
-- Enable foreign keys on every connection. WAL is selected by GateDbHelper.
-- Timestamps ending _ms are epoch milliseconds unless explicitly noted.
PRAGMA foreign_keys = ON;
CREATE TABLE namespace (
  id INTEGER PRIMARY KEY,
  target_package TEXT NOT NULL CHECK(target_package='qualified.target.package'),
  receiver_binding TEXT NOT NULL,
  signing_lineage TEXT NOT NULL,
  android_profile_binding TEXT NOT NULL,
  installation_binding TEXT NOT NULL,
  qualification_id TEXT,
  rule_enabled INTEGER NOT NULL DEFAULT 0 CHECK(rule_enabled IN (0,1)),
  paused INTEGER NOT NULL DEFAULT 1 CHECK(paused IN (0,1)),
  global_revision INTEGER NOT NULL DEFAULT 0 CHECK(global_revision>=0),
  consent_version TEXT,
  consent_at_ms INTEGER,
  created_at_ms INTEGER NOT NULL
);
CREATE TABLE account (
  id INTEGER PRIMARY KEY,
  namespace_id INTEGER NOT NULL REFERENCES namespace(id),
  phone_e164 TEXT NOT NULL CHECK(
    length(phone_e164) BETWEEN 3 AND 16 AND
    substr(phone_e164,1,1)='+' AND
    substr(phone_e164,2,1) BETWEEN '1' AND '9' AND
    substr(phone_e164,2) NOT GLOB '*[^0-9]*'),
  display_name TEXT,
  search_key TEXT NOT NULL DEFAULT '',
  classification TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK(classification IN
    ('UNKNOWN','PERSONAL_OBSERVED','BUSINESS_CONFIRMED',
     'AMBIGUOUS','STALE','NON_DIRECT')),
  ever_business INTEGER NOT NULL DEFAULT 0 CHECK(ever_business IN (0,1)),
  enabled INTEGER NOT NULL DEFAULT 0 CHECK(enabled IN (0,1)),
  policy_revision INTEGER NOT NULL DEFAULT 0 CHECK(policy_revision>=0),
  observed_block_state TEXT NOT NULL DEFAULT 'UNKNOWN'
    CHECK(observed_block_state IN ('UNKNOWN','BLOCKED','UNBLOCKED')),
  block_origin TEXT NOT NULL DEFAULT 'EXTERNAL_OR_UNKNOWN'
    CHECK(block_origin IN ('BUSINESS_GATE','EXTERNAL_OR_UNKNOWN')),
  evidence_adapter TEXT,
  evidence_target_build TEXT,
  verified_at_ms INTEGER,
  personal_reuse_until_ms INTEGER,
  block_verified_at_ms INTEGER,
  first_seen_at_ms INTEGER NOT NULL,
  last_seen_at_ms INTEGER NOT NULL,
  UNIQUE(namespace_id,phone_e164),
  CHECK(enabled=0 OR ever_business=1)
);
CREATE INDEX account_display ON account(namespace_id,enabled,search_key,id);
CREATE INDEX account_retention ON account(classification,last_seen_at_ms);
CREATE TABLE action_job (
  account_id INTEGER PRIMARY KEY REFERENCES account(id),
  requested_action TEXT NOT NULL CHECK(requested_action IN ('BLOCK','UNBLOCK')),
  state TEXT NOT NULL CHECK(state IN
    ('PENDING','WAIT_SAFE','INSPECTING','ACTION_INTENT','VERIFYING',
     'REINSPECT','DONE','CANCELED','FAILED')),


## Source page 60

BUSINESS GATE
PRODUCT REQUIREMENTS + DESIGN  /  v2.0
9 September 2026  ·  Implementation specification
60
  expected_global_revision INTEGER NOT NULL,
  expected_account_revision INTEGER NOT NULL,
  generation INTEGER NOT NULL DEFAULT 0,
  authorization_nonce TEXT,
  attempts INTEGER NOT NULL DEFAULT 0 CHECK(attempts>=0),
  last_reason TEXT,
  created_at_ms INTEGER NOT NULL,
  updated_at_ms INTEGER NOT NULL,
  CHECK(requested_action='UNBLOCK' OR authorization_nonce IS NULL),
  CHECK(requested_action<>'UNBLOCK' OR state IN ('DONE','CANCELED','FAILED')
    OR authorization_nonce IS NOT NULL)
);
CREATE TABLE action_event (
  id INTEGER PRIMARY KEY,
  account_id INTEGER REFERENCES account(id) ON DELETE SET NULL,
  event_type TEXT NOT NULL CHECK(event_type IN
    ('POLICY_CHANGED','OBSERVED','ACTION_STARTED','ACTION_FINISHED',
     'ACTION_ABORTED','RULE_CHANGED','COMPATIBILITY_CHANGED')),
  outcome TEXT NOT NULL CHECK(outcome IN
    ('NONE','SUCCESS','PENDING','ABORTED','FAILED')),
  reason TEXT NOT NULL,
  at_ms INTEGER NOT NULL,
  adapter_id TEXT
);
CREATE INDEX event_retention ON action_event(at_ms,id);
CREATE TABLE app_meta (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
CREATE TABLE attention_daily (
  day_utc TEXT PRIMARY KEY,
  management_ms INTEGER NOT NULL DEFAULT 0 CHECK(management_ms>=0),
  occupancy_ms INTEGER NOT NULL DEFAULT 0 CHECK(occupancy_ms>=0),
  union_observed_ms INTEGER NOT NULL DEFAULT 0 CHECK(union_observed_ms>=0),
  management_opens INTEGER NOT NULL DEFAULT 0 CHECK(management_opens>=0),
  forced_repairs INTEGER NOT NULL DEFAULT 0 CHECK(forced_repairs>=0),
  blocked_successes INTEGER NOT NULL DEFAULT 0 CHECK(blocked_successes>=0),
  CHECK(union_observed_ms<=management_ms+occupancy_ms)
);
INSERT INTO app_meta(key,value) VALUES('schema_version','1');
PRAGMA user_version=1;
F.2 Local verification actually performed
The companion pure-Java evaluator passed 8,244 assertions in this document's build environment. These include
all 4,096 combinations of twelve Boolean context guards for block and unblock decisions, plus classification,
exception and grant cases. The SQLite schema executed successfully and passed 15 basic constraint checks.
These results verify reference logic and selected storage constraints only.
No Android app was compiled, no physical connected messenger flow was tested, and no accessibility/notification service or
Play submission was validated. Every real-device acceptance case remains a required test, not a reported pass.
Run the reference with JDK 17 or newer:
cd reference
sh run-tests.sh
The output is a pure rule test result. The internal Context flags must be derived from fresh, qualified Android
observations in the future application; callers must never fill them with optimistic constants.
All design identities and counts are illustrative; no real message data is included. The reference does not replace
release qualification.
