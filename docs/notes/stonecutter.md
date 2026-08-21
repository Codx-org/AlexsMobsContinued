# Stonecutter — the preprocessor rules and their traps

> Read before editing `stonecutter.gradle.kts` or any `//?` gate. Every trap here has shipped a bug.
>
> Part of the Alex's Mobs Continued porting notes.

### The two tools for a signature change, and when to use which

This wave settled the division of labour, which is worth keeping:

- **Stonecutter `replacements`** rewrite an *override declaration* and its matching `super.` call.
  An override cannot be delegated to a helper — its signature has to change in place — and there
  are ~120 of them, far too many for per-file conditionals. The rules key off text that only ever
  appears in a signature (the parameter's **type**, e.g. `(DamageSource `) so they cannot hit a
  call site, and the `super.` rules key off `super.`, which only appears inside the override.
- **`misc/AMCompat` static helpers** cover *call sites*, which usually sit in AI goals or item code
  where the new parameter (a `ServerLevel`) is not in scope at all. Where the return type itself
  differs by era the helper is declared **twice**, inside one `//? if >=1.21.2 { … } else { … }`
  block, rather than branching inside a single body.

⚠️ **A rule keyed on the CALL form never rewrites the DECLARATION, and that silence is total.** The
mirror image of the split above: a rename rule written as `replace(".oldName()", ".newName()")` uses
the leading dot precisely so it cannot hit a declaration — which is right when the mod only *calls*
the method, and a silent disaster when the mod also *overrides* it. `EntityStraddleboard`'s upstream
`isControlledByLocalInstance()` was left behind that way on 27 nodes, compiling clean under a name
nothing calls, and neither the compiler, the mixin verifiers nor `verify_overrides.py` (blind to
renames by construction) can see it. **Before adding a `.method()` rule, grep the tree for the
declaration form as well as the call form** — and if the new name is `final`, as it was here, the
answer is not a rename at all but a gated override of whatever feeds it. See
[`bug-reports.md`](bug-reports.md) #79 and [`api-eras.md`](api-eras.md).

**The whole file was swept for this on 2026-08-12** — all **11** `replace(".old()", ".new()")` rules,
cross-checked against every declaration of the old name in `src/main/java`. Three hits, only one of
them a fault:

| old name | declaration found in | verdict |
|---|---|---|
| `isControlledByLocalInstance` | `EntityStraddleboard` | **the bug** — fixed, #79 |
| `getNewTarget` | `fabric/forge/.../LivingChangeTargetEvent` (a Fabric shim) | safe — the rule is gated NeoForge-only, so on Fabric nodes neither side is renamed. The shim's javadoc says `{@link #getNewTarget()}`, with a `#`, which the rule cannot match either |
| `entityGlintDirect` | `AMRenderTypes` (gated `>=1.21.5`) | harmless — callers become `AMRenderTypes.entityGlint()`, which resolves through static inheritance from `RenderType` (or the `>=1.21.11` delegate), leaving the gated declaration merely **unused** rather than wrong |

Re-run that sweep whenever a `.method()` rule is added: extract the rules with a regex over
`stonecutter.gradle.kts` and grep `src/main/java` for `(public|protected|private).*\bold\s*\(\s*\)\s*\{`.

⚠️ **The second argument to `string(id, …)` is `reversible`, and it must be `true`.** Passing
`false` does not mean "one-way" — it makes Stonecutter apply the rule **backwards** (`to`→`from`).
That silently turned every `InteractionResult.SUCCESS` in the tree into
`InteractionResultHolder<ItemStack>.SUCCESS` and produced ~500 "illegal start of type" errors.
Every rule in `stonecutter.gradle.kts` therefore passes `true`. The consequence to respect is that
a reversible rule's *reverse* must also be harmless, which is why the active node must stay
`1.20.1-forge`: reversing `InteractionResultHolder<ItemStack>`→`InteractionResult` on root `src/`
would corrupt it.

### ⚠️ Stonecutter has NO operator precedence — parenthesize every mixed `&&`/`||` condition

The single most dangerous thing in this file, found 2026-07-30. **`&&` does not bind tighter than
`||`** — Stonecutter evaluates a condition strictly **left-to-right with equal precedence**, so

| written | parses as | intended |
|---|---|---|
| `A && B \|\| C` | `(A && B) \|\| C` | ✅ same by luck |
| `A \|\| B && C` | `(A \|\| B) && C` | ❌ |
| `A && B \|\| C && D` | `((A && B) \|\| C) && D` | ❌ |

The first row is why a dozen sites in this tree worked for four milestones and the rule went
unnoticed. **Parentheses are supported and are now mandatory house style** for any condition mixing
the two operators. Audit with:

```bash
grep -rn "//?.*\(if\|elif\)" --include="*.java" src/main/java | grep "&&" | grep "||" | grep -v "("
```

Two live faults came from this, and they are worth contrasting because only one of them was
findable by compiling:

- `//? if neoforge || forge && <26.2` on the four shearable entities parsed as
  `(neoforge || forge) && <26.2`, which is **false on a NeoForge 26.2 node** — so *both* arms of a
  class declaration were commented out and the file had no top-level type. That is a **compile
  error** (`compact source file should not have package declaration`), caught immediately.
- `//? if neoforge && >=1.20.5 || forge && >=26` on `SpawnBiomeData.conventionTag` parsed as
  `((neoforge && >=1.20.5) || forge) && >=26`, i.e. **it required MC ≥ 26 on both loaders**. Nothing
  structural was missing, so it compiled clean — see the Milestone 13 correction in
  [`porting-log.md`](porting-log.md) for what it cost.

#### ⚠️ A `//?` gate with no `{` covers the NEXT LINE ONLY — and a comment does not extend it

Sibling of the precedence trap: also silent, also only findable at runtime. `//? if <expr>` without a
brace comments exactly **one** line; `//? if <expr> {` … `//?}` comments the block. The failure is
writing the first and meaning the second, because the two statements read as one thought:

```java
//? if !fabric
MinecraftForge.EVENT_BUS.register(new ClientEvents());
initRainbowBuffers();      // ← NOT gated. Runs on Fabric.
```

This shipped an NPE on `1.20.1-fabric` (see [`fabric.md`](fabric.md)). It survives every static
check because the un-gated line is **valid Java on every loader** — a gate that leaks a
`net.minecraftforge.*` reference onto Fabric fails the compile instantly, since this tree has no
shim; a gate that leaks a *vanilla* call compiles clean and fails only when something runs it.

Two habits that would have caught it:

- **Prefer the block form the moment a gate has, or could grow, a second line.** `//? if X {` … `//?}`
  around a single statement costs one line and cannot be extended by accident later.
- **Never let a comment sit between a single-line gate and the line it gates.** A reader (and the
  next editor) will assume the gate reaches past it; Stonecutter counts the comment as the gated
  line. Put explanatory prose *above* the `//?`, never below it.

Audit for the shape — a single-line gate whose next line is not obviously self-contained:

```bash
grep -rn --include="*.java" -E "^[[:space:]]*//\? if " src/main/java | grep -v "{[[:space:]]*$"
```

#### ⚠️ A comment belongs INSIDE the commented arm's `/* … */`, never between `//?}` and the `/*`

Third member of the same family. In a multi-arm gate the false arms are written already-commented,
and the `/*` that opens one is **part of the arm**. Prose written between the arm marker and that
`/*` is outside the block, so when the arm is selected on some node the prose is left behind as bare
text where statements are expected:

```java
*///?} else {
// why this arm exists          ← ✗ outside the block; becomes code on the nodes that select it
/*doTheThing();
*///?}

*///?} else {
/*// why this arm exists        ← ✓ inside; the `/*` comes first, then the `//`
doTheThing();
*///?}
```

Cost this the first time: the `>=26` arm of `FabricClientEvents#registerFarseerStatic`, which failed
`26.1.2-fabric` and `26.2-fabric` with `unclosed character literal` / `illegal character: '—'`
pointing at an English sentence. The compiler diagnostic is unmistakable once seen — an error whose
caret lands on prose means a comment escaped its block — but it does not name the gate.

**It has now cost a second wave** — the `>=26` arm of `FabricNameTagMixin` (Wave 3b-6), same two
nodes, this time as a wall of `illegal start of type` / `';' expected`. Two things make this the
most repeatable mistake in the file: the ✗ and ✓ forms differ by **four characters** and read
identically at a glance, and writing prose above the thing it describes is a correct habit
everywhere else in the codebase — including two sections up, where the single-line-gate rule says
to put prose *above* the `//?`. The two rules genuinely point opposite ways, so check which shape
you are in. When an arm is long enough to want a heading comment, write the arm first, compile the
node that selects it, and only then add the prose.

#### ⚠️ A reversible `regex` rule bans its own output token from root `src/` — including in comments

`regex("!mc26-guigraphics", true) { replace("\\bGuiGraphics\\b", "GuiGraphicsExtractor",
"\\bGuiGraphicsExtractor\\b", "GuiGraphics") }` works only because root `src/` contains the token
`GuiGraphics` and **never** `GuiGraphicsExtractor` — the second pair is the reverse direction, used
to project *back*. Write the output token anywhere in root `src/` and it gets rewritten to the input
token on every node, including the ones you meant it for.

This is not a code-only hazard: it applies to prose, javadoc and identifier fragments alike, and a
comment that silently says the opposite of what you wrote is worse than no comment. When an arm
needs to *talk about* the rewritten name, describe it ("the 26-era name of `GuiGraphics`") instead
of spelling it. Same reasoning for every other `reversible = true` rule in the file.

#### ⚠️ Stonecutter `replacements` rules DO NOT CHAIN

The single most important thing learned this wave, and it shapes the whole `>=26` group.

A later rule sees the **original** text at any offset an earlier rule has already claimed — a rule
spanning a claimed offset simply never matches. With two live MC-version rule groups (`>=1.21.11`
and `>=26`) touching the same tokens, that means a `>=26` node **cannot** rely on the 1.21.11 rules
having run first. Two consequences, both encoded in the file:

1. **Every `>=26` rule is keyed on the 1.20.1 spelling and emits the final 26 spelling**, doing both
   hops itself.
2. `stonecutter.gradle.kts` carries a **stand-down flag** — `val mc26 = eval(current.version, ">=26")`
   — and five `!mc2111-*` rules are wrapped in `if (!mc26)` so they cannot half-rewrite a region the
   `>=26` rule needs whole.

> ⚠️ **The render-type swap is the dangerous case.** 26.1 did not just move the `RenderType`
> factories to `RenderTypes` — it **swapped two of them**. What 1.21.11 calls `entityCutout` (culled)
> is now `entityCutoutCull`, and what it calls `entityCutoutNoCull` is now plain `entityCutout`. The
> names survive in both eras and **mean the opposite thing**, so a one-rule "rename" silently flips
> the backface culling of every mob in the mod, with no compile error and nothing a gate can see.
> Verify after any edit there by grepping the **generated** sources, not the rules.

> ⚠️ **A rule whose search text is a SUBSTRING of its own replacement cannot be shadowed by another
> rule** — so do not plan on declaring a narrower rule earlier to pre-empt it. Wave 1 of the Fabric
> work needed a third destination for `Player.PERSISTED_NBT_TAG`: the existing `!mc205-persistednbt`
> rewrote it to `ServerPlayer.PERSISTED_NBT_TAG` on `>=1.20.5`, and Fabric has neither spelling. A
> `!fab-persistednbt` rule declared *before* it in the Fabric block still lost the offset, and any
> arm spelling the Forge name would have been rewritten in turn (`Player.PERSISTED_NBT_TAG` matches
> inside `ServerPlayer.PERSISTED_NBT_TAG`). **The fix is to stop using a rule**: both were deleted
> and the name became a three-arm `AMCompat.PERSISTED_NBT_TAG` constant that *binds* the platform
> field per era. A gated constant in a compat class is the general escape whenever a name needs more
> destinations than a rule can express.

Other traps this wave re-confirmed or added:

- **Stonecutter blocks are siblings and NEVER nest.** Hit four times here; the remedy is always the
  same — extract the inner-gated body into its own top-level method (`ClientProxy.onRegisterGuiLayers`,
  `RenderUnderminer.renderBreaking`, `BlockTransmutationTable.explodeOnDestroy`) or split one block
  into several siblings (`AlexsMobs`' mod-bus listener block, so the layer-definitions listener could
  take its own gate). A **whole-file** gate is subject to the same rule: `fabric/forge/event/village/
  VillagerTradesEvent` is one block with three arms — two complete copies of the class plus an empty
  `>=26` arm — because the class was already gated `<26` and then needed a second split at 1.21.5.
  Duplicating the body is the correct answer there, not an inner block.
- The else-if spelling is **`elif`**, and compound conditions work — but see the precedence trap
  below: **`&&` does NOT bind tighter than `||`**, so every mixed condition must be parenthesized.
- ⚠️ **`!mc121-vtx-color` rewrites EVERY `.color(` in the tree** to `.setColor(`. Where a 26 API
  genuinely wants `.color(`, dodge it with whitespace after the dot (`source. color(state)`).
- ⚠️ **Group references (`$1`) in `regex()` are broken** — they splice from the original offset into a
  shifted buffer. Write literal replacements.
- **Fish does not word-split a variable**, so `./gradlew $TASKS --continue` is read as one project
  name. Write the task list to a file and run
  `bash -c './gradlew $(cat nodes.txt | tr "\n" " ") --continue'`.
  ⚠️ **This applies to the gate SCRIPTS too, and there it fails quietly.** `scripts/clientgate.sh
  $NODES` hands all 32 names to `for node in "$@"` as **one** argument, so the gate runs a single
  bogus node, reports `TIMEOUT`/`rc=1`, and looks like a real failure while having tested nothing.
  Same for `bootgate.sh`. Wrap the whole thing: `bash -c 'scripts/clientgate.sh $(ls versions | …)'`,
  and sanity-check the arg count (`echo $nodes | wc -w`) before trusting a red verdict.
- **Loom's `…-deobf-…-sources.jar` for the 26 Forge node is an EMPTY ZIP.** The usable oracles are the
  extracted trees: `/tmp/mc2612/` (26.1.2 vanilla `net/minecraft` + NeoForge
  `net/neoforged`, as `.java`) and `/tmp/fg2612src/` (Forge 26 `net/minecraftforge/**`,
  649 files).
- ⚠️ **Kotlin block comments NEST.** In `build-logic`, writing a path like `data/…/recipe/*.json` inside
  a KDoc opens a nested `/*` and swallows the rest of the file. Escape or reword.

