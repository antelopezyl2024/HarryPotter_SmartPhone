# DLC: Sirius Black Must Live — Scene Design

An alternate-timeline retelling of the Department of Mysteries arc from *Order of the Phoenix*. Five decisions stand between Sirius Black and the Veil. Get four right, and he walks free.

## Mechanics

- Five fixed-order scenes; one decision per scene.
- Every scene is `mode: "EITHER"` — the player may answer with a button choice OR with free text. The Android UI exposes both paths; the player picks one per scene.
- **Binary scoring**: +1 if the player's answer is correct (in whichever mode they used), +0 otherwise.
- Total score is computed after all 5 scenes. Two endings:
  - `SUCCESS` — total ≥ 4
  - `FAIL` — total ≤ 3 (3 maps to `FAIL`)
- Free-text inputs are classified by the LLM into one of the scene's predefined categories. Each category carries an `is_correct` flag; the score follows that flag.

## Scenes

### S1 — The Vision
> Mid-exam, Harry collapses. A vision: Sirius being tortured at the Department of Mysteries. He wakes alone in the infirmary. The vision could be real — or a trap planted in his mind.

**Choice (A/B/C):**
- A. Bolt for the Forbidden Forest. — ❌
- B. Use the two-way mirror Sirius gave you. — ✅
- C. Find Snape and pass the agreed signal to the Order. — ✅

**Free-text** prompt: *"What is your plan?"*
- ✅ `SAFE_VERIFICATION` — verify the vision via mirror / Snape / Dumbledore / another in-school agent
- ✅ `CAUTIOUS_INVESTIGATE` — gather more info, consult others first
- ❌ `IMPULSIVE_RUSH` — leave Hogwarts immediately to reach the Ministry
- ❌ `INVALID` — off-topic, refusal, or nonsense

### S2 — The Reinforcement
> After verifying the danger, Harry prepares to leave. Hermione, Ron, Neville, Luna, and Ginny stand with him. The Order may not arrive in time — unless you leave a clear trail.

**Choice (A/B):**
- A. Take only the DA — speed matters more than backup. — ❌
- B. Leave a clear signal for the Order before leaving. — ✅

**Free-text** prompt: *"How will you make sure reinforcements arrive in time?"*
- ✅ `ORDER_SIGNAL` — alert the Order via a specific channel (Snape, Patronus, McGonagall, DA coin, written message)
- ❌ `DA_ONLY` — go with only the DA; explicitly skip the Order
- ❌ `WAIT_AND_HOPE` — vague hope without a specified mechanism
- ❌ `INVALID`

### S3 — The Prophecy
> You stand in the Hall of Prophecies. A blue-glowing orb on the shelf bears your name. The Death Eaters haven't reached you yet — but their footsteps echo down the corridor.

**Choice (A/B):**
- A. Lift the orb to study it. — ❌
- B. Ignore it. Find the exit and get everyone out. — ✅

**Free-text** prompt: *"What do you do — and what do you tell the others?"*
- ✅ `DISSUADE_AND_FLEE` — refuse the orb and lead the group toward an exit
- ❌ `TAKE_AND_FLEE` — take the orb, even if planning to flee with it after
- ❌ `DELAY_AND_INSPECT` — stay at the shelf to study, read, or analyze
- ❌ `INVALID`

### S4 — The Brink of Death
> Combat erupts in the Death Chamber. Sirius is duelling Bellatrix in front of the Veil — laughing, taunting her. He hasn't noticed how close the archway looms behind him. You have one breath to act.

**Choice (A/B/C/D):**
- A. Cast `Impedimenta` on Bellatrix. — ✅
- B. Cast `Protego` on Sirius and push him forward. — ✅
- C. Tackle Sirius bodily away from the Veil. — ✅
- D. Cast `Stupefy` past Sirius at Bellatrix. — ❌

**Free-text** prompt: *"Shout your action — fast."*
- ✅ `PROTECT_PULL` — physically move Sirius away from the Veil
- ✅ `PROTECT_SPELL` — defensive spell on Sirius, or non-lethal spell on Bellatrix that doesn't endanger Sirius
- ❌ `OFFENSIVE_ONLY` — attack without protecting Sirius's position
- ❌ `INVALID`

### S5 — The Trial of Fate
> The fight is over. Sirius lives, exhausted. Fudge bursts in with Aurors — he sees you, the DA, an unconscious Death Eater on the floor. He still believes Sirius is a fugitive.

**Choice (A/B):**
- A. Slip into the chaos and escape with Sirius. — ❌
- B. Force a public reckoning: revive a Death Eater and have him confess in front of Fudge. — ✅

**Free-text** prompt: *"What do you say to Fudge — and to Sirius?"*
- ✅ `PUBLIC_PROOF` — force a public confession / display the truth in front of witnesses
- ❌ `QUICK_ESCAPE` — urge Sirius to flee while he can
- ❌ `HESITATE` — defer the decision, stall
- ❌ `INVALID`

## Files in this folder

- `manifest.json` — DLC metadata + scene order.
- `scenes/S{1..5}_*.json` — individual scene definitions consumed by the backend's content loader.
- `endings.json` — the two endings keyed by `score_range`.
- `lore.json` — Sirius-arc knowledge for Hermione's Notes (RAG corpus). Currently 10 sample entries; expand toward ~30–50 for the demo.
