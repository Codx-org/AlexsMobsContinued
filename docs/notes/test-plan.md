# Test plan — clearing the client-verification backlog (`2.0.12` → `2.1.5`)

Ten releases in a row shipped with **no client session**. This is everything that is owed,
arranged so the whole backlog closes in **~10 minutes of headless work plus two client launches**
(a third, optional, mops up the last two items).

Order matters: do **step 0 first**. Four of the owed items turn out to need no GPU at all, and
doing them first shortens both client sessions.

⚠️ A dev client takes the user's GPU for the whole session, and a cold `runClient` is a
~20-minute launch. Do not start one to check a single thing.
⚠️ The user's own live server holds ports **25566** and **25588** — never reuse them.
⚠️ **`ydotool click` is unusable on this machine.** Anything reached only by clicking (the animal
dictionary's pages, the creative tabs, the advancement tabs) has to be navigated by hand — those
items are marked **[hands]** below.

---

## Step 0 — headless, no GPU (~10 min)

Two dev servers, driven over RCON. Per node, the first `runServer` writes
`versions/<node>/run/server.properties`; set `enable-rcon=true`, `rcon.password=amcdev`,
`rcon.port=25575` (25576 for the second node) and restart, then:

```bash
./gradlew ":1.21.1-neoforge:runServer"          # in one shell
python3 scripts/rcon.py 'say hello'             # in another
```

### H1 — #104a, mob inventories actually save · `1.21.1-neoforge`

`/data get entity` re-serializes the entity through `addAdditionalSaveData`, so this exercises the
exact code path that was dropping items. The node must be in the **1.20.6 → 1.21.5** band.

```
/summon alexsmobs:kangaroo 0 100 0 {Owner:[I;0,0,0,0],Items:[{Slot:0b,id:"minecraft:golden_helmet",count:1}]}
/data get entity @e[type=alexsmobs:kangaroo,limit=1] Items
```

- **PASS** — `[{Slot: 0b, id: "minecraft:golden_helmet", count: 1}]`
- **FAIL** (what `2.1.4` shipped) — `[{Slot: 0b}]`, the item gone

Same shape for the straddleboard, which is the other half of the fix:

```
/summon alexsmobs:straddleboard 0 100 0
/data get entity @e[type=alexsmobs:straddleboard,limit=1] BoardStack
```
`BoardStack` must name `alexsmobs:straddleboard`, not be an empty compound.

### H2 — #104b, conditional loot tables · `26.2-fabric` (any non-Forge node ≥1.21.2)

The maraca pool is `rolls: 1`, `count: 1..1` — guaranteed, so this is deterministic.

```
/summon alexsmobs:cockroach 0 100 0 {Maracas:1b,NoAI:1b}
/kill @e[type=alexsmobs:cockroach]
/data get entity @e[type=item,limit=1] Item
```

- **PASS** — an `alexsmobs:maraca` is among the drops (list them with
  `/data get entity @e[type=item]` if the first is a wing fragment)
- **FAIL** — only `alexsmobs:cockroach_wing_fragment` ever drops

⚠️ Classic **Forge ≥1.21.3 is a known, documented degrade here** — the override cannot exist on
that loader. Don't test it there and don't file it.

### H3 — #80, rider seat heights · either node

```
/summon alexsmobs:raccoon 0 100 0
/summon alexsmobs:blue_jay 0 100 0
/ride @e[type=alexsmobs:blue_jay,limit=1] mount @e[type=alexsmobs:raccoon,limit=1]
/data get entity @e[type=alexsmobs:blue_jay,limit=1] Pos[1]
/data get entity @e[type=alexsmobs:raccoon,limit=1] Pos[1]
```

- **PASS** — the jay sits **0.405** above the raccoon (`0.45 × 0.9`)
- **FAIL** — **0.9**, the raccoon's full height, i.e. floating off its back

The straddleboard's own seat is the same check with `+0.5` expected (`0.35` = the broken value).
The kangaroo's joey and the anteater's baby are visual and ride along in session A.

### H4 — already done, do not redo

**#85** (Fabric spawn biomes) and **#95** (anvil repair materials) were both closed headlessly in
earlier passes. **#93** (void worm summon) too.

---

## Session A — `26.2-fabric` (~20 min, closes 13 items)

```bash
./gradlew ":26.2-fabric:runClient"
```

Creative, superflat, `/gamerule doDaylightCycle false`, `/time set day`. This one node satisfies
every version floor in the backlog (≥1.21.9, ≥1.21.6, ≥1.21.5, ≥1.21.4, ≥1.21.2) **and** is
Fabric, so it is the single highest-value launch available.

| # | Do this | PASS looks like |
|---|---|---|
| **#107** / **#49** | Open the creative menu, click through to the Alex's Mobs tab, then open the advancement screen and let it sit. **[hands]** | The tab icon **still cycles** through mobs, all 59 advancement icons animate, and the screen does **not** stutter. This is the headline `2.1.5` fix and also the test for the long-open #49. |
| **#101** | `/give @s minecraft:shears`, then shear four mobs (see the summons below) | All four shear. **Watch the cockroach** — shearing it must **not** knock its maracas off. |
| **#105** | `/summon alexsmobs:laviathan ~ ~ ~5`, then right-click its **head and neck** with an empty hand, a saddle, and a lead | Every part responds. Before the fix only the tail hitbox did anything. |
| **#97** | Open the animal dictionary to the **underminer**, the **laviathan** and the **murmur** pages **[hands]** | Underminer holds its ghostly pickaxe; laviathan stands still (no shaking); murmur has its head. |
| **#21** | Same book, the **index** page **[hands]** | No hole where a mob should be. |
| **#90** | `/give @s alexsmobs:shattered_dimensional_carver`, carve a portal; then `/summon alexsmobs:farseer` and hit it | The portal is a **static/noise** effect, not a flat black circle. The farseer's eye (as it fires) and scars (as it is hurt) likewise. |
| **#92** / **#96** | Hold the shattered carver in the main hand **and the plain one in the off hand** | The shattered one drifts/animates and is held like a tool. ⚠️ The plain one lying flat is **correct** — that is why you hold both. |
| **#74** / **#75** / **#76** | Same book + `/give @s alexsmobs:mysterious_worm`; `/summon alexsmobs:enderiophage`, `alexsmobs:guster`, `alexsmobs:spectre` in the dark | Book mobs are **lit**, not black or blue. The worm is 3D and animated. All three mobs' glow layers show. |
| **#79** | Ride a straddleboard on land; then place one **from inside a lava pool** | It moves under you; the lava-placed one surfaces and you stop burning. Before the fix it froze, then flew off on release. |
| **#80** | Perch a blue jay on a raccoon; breed a kangaroo and an anteater | Jay on the back, joey in the pouch, baby on the back — none floating. |
| **#81** | Let a `alexsmobs:crimson_mosquito` latch onto you (or perch a tamed crow) | It drinks **and lets go**. Nothing this mod owns could ride a player at all on ≥1.21.2 before `2.0.14`. |
| **#84** | Place a capsid, put a **raw cod** in it | It converts. Dead on all Fabric nodes until `2.0.14`. |
| **#86** | Craft 8 `alexsmobs:mimicream` around a **damaged** pickaxe | The copy comes out **fully worn, with no Mending**. |
| **#82** | `/effect give @s alexsmobs:clinging 30 0` under an overhang | You are pushed **up** and the view flips. |
| **#83** | Feed a seal three fish on a beach; then hit one | It walks into the water, dives, digs ~100 ticks, comes back with treasure. An attacked seal flees **into** the water, not onto land. |
| **#22** | Perch a tamed bald eagle on the falconry glove and left-click | It launches. The whole falconry loop was silent on Fabric before `2.0.4`. |
| `2.0.1` | Attack a cachalot whale | No crash. (26.2 stopped assigning entity ids client-side; nobody has hit a whale here since.) |

Summons for the four shearable mobs — note each needs the state that makes it shearable:

```
/summon alexsmobs:bison ~ ~ ~3 {Sheared:0b}
/summon alexsmobs:alligator_snapping_turtle ~ ~ ~3 {MossLevel:3}
/summon alexsmobs:cockroach ~ ~ ~3 {Maracas:1b}
/summon alexsmobs:mungus ~ ~ ~3 {MushroomState:{Name:"minecraft:red_mushroom"},MushroomCount:3}
```

---

## Session B — `1.21.1-neoforge` (~8 min, closes 3 items)

```bash
./gradlew ":1.21.1-neoforge:runClient"
```

This node is the reporter's own, sits mid-band of the 1.20.6→1.21.3 alpha window **and** is
NeoForge ≥1.21 — the only combination that can see all three of these.

| # | Do this | PASS looks like |
|---|---|---|
| **#108** | `/give @s alexsmobs:straddleboard` and look at the **icon in the hotbar**, undyed | The grey panel is **visible**. It has been drawing at alpha 0 — invisible — since `2.0.0`, and in upstream. |
| *(new setting)* | `/aac config set straddleboardBaseColor 8421504` | The wood under the panel darkens. `/aac config set straddleboardPanelColor <int>` moves the panel the other way; `/aac config reset straddleboardBaseColor` puts it back. |
| **#106** | `/effect give @s alexsmobs:earthquake 20 0`, in **first person** | The camera shakes. On NeoForge ≥1.21 the shake was being discarded a frame later. |

If a ≥1.21.4 node is booted for any other reason, one extra check rides along: a **dyed**
straddleboard's icon should take the dye (`/give @s alexsmobs:straddleboard[minecraft:dyed_color=16711680]`
on ≥1.21.5, `[minecraft:dyed_color={rgb:16711680}]` on 1.21.4). That has never worked on those 30
nodes.

---

## Session C — optional, `1.21.1-fabric` (~5 min)

Only two items live here, and both are low severity:

- **#23**, second half — the eleven ISTER items (shield of the deep, the End Pirate blocks, …) must
  have creative-menu icons on a **Fabric node below 1.21.4**. The ≥1.21.4 half is covered in
  session A.
- **#41** — a kangaroo wearing a chestplate should wear it on its **body**, not like a scarecrow.
  Any node ≥1.21.2 does; session A covers it if you remember to `/summon` a tamed one:
  `{Owner:[I;0,0,0,0],Items:[{Slot:1b,id:"minecraft:iron_chestplate",count:1}],ChestInvIndex:1}`.
  ⚠️ Never use vanilla's `equipment:{…}` tag on a kangaroo — it reads its own inventory, so that
  produces a **false negative**.

---

## Cannot be tested in this environment

Don't spend a session trying:

- **#16** — the underminer's `ClassCastException` needs **another mod** that adds a render layer to
  a foreign entity. Nothing here can produce one.
- **#40** — needs **Moonrise** installed. The test jar is at `/tmp/amc-moonrise/`; two sessions
  already failed to reproduce it, and it is blocked on the reporter's log anyway.
- **#98** — the MCA Reborn interaction fix needs **MCA Reborn on a Forge/NeoForge node**, and MCA
  publishes no build between 1.21.1 and 26.1.2. (**#99**, the crash that pairing caused, *is*
  closed — client-confirmed on `26.2-fabric` with MCA installed.)
- **#37** — needs **Create** installed.
- **#19**, **#51**, **#55**, **#64** — blocked on the reporter, not on hardware.

---

## Scoreboard

| Session | Cost | Items closed |
|---|---|---|
| Step 0 (headless) | ~10 min, no GPU | #104a, #104b, #80 |
| A · `26.2-fabric` | ~20 min | #107, #49, #101, #105, #97, #21, #90, #92, #96, #74, #75, #76, #79, #81, #84, #86, #82, #83, #22, `2.0.1` |
| B · `1.21.1-neoforge` | ~8 min | #108, the two new colour settings, #106 |
| C · `1.21.1-fabric` | ~5 min, optional | #23 (Fabric half), #41 |

After A and B, the only owed checks left are the four that are structurally impossible here.
