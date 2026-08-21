import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import java.io.File

/**
 * Expands the ONE predicated access-widener source into the exact file a given Fabric node needs.
 *
 * ## Why this exists
 *
 * A Forge access transformer that names an absent member is a silent no-op, so a single `.cfg` can
 * list both eras of a member that moved and every node just ignores the half that does not apply.
 * A Fabric access widener cannot: an entry naming an absent member is a **hard error**, and loom's
 * `validateAccessWidener` refuses to build. Two independent things then diverge across this tree's
 * Fabric nodes (1.20.1 → 26.2):
 *
 *  1. the header **namespace** — `official` on the unobfuscated 26.x nodes, whose loom-no-remap
 *     build has no mappings tree to remap a `named` widener with, and `named` below that, where
 *     classic loom remaps named → intermediary on the way into the jar and rejects `official`;
 *  2. individual **entries**, because members move: `Camera#move` changed descriptor at 1.20.6,
 *     `EnderDragon#reallyHurt` at 1.21.2, `AbstractArrow#setPierceLevel` changed package at
 *     1.21.11, `BlockBehaviour#drops` changed type twice, and a whole block of entries exists only
 *     because Forge and NeoForge ship a PATCHED jar — a set that itself grew over the versions.
 *
 * That is six-plus near-identical files if they are maintained by hand, each a place for the next
 * edit to be forgotten. The AW format is not preprocessed by Stonecutter (it is a resource, not
 * source), so the gating has to happen here instead.
 *
 * ## The source format
 *
 * `accesswidener/<mod>.accesswidener`, at the REPO ROOT and deliberately **not** under `src/`:
 * Stonecutter registers `.accesswidener` as a `#`-comment file type and preprocesses everything in
 * the source tree, so a template kept there dies at `stonecutterPrepare` with
 * `Extraneous input '{'` before this ever runs. (Which also means native Stonecutter gating is not
 * an option for the AW: its inactive arms come back as `#`-prefixed lines, and a widener's header
 * has to be the file's literal first line, so the one thing that must be gated cannot be.)
 *
 * The body is an ordinary widener with **no header line** (this function emits it) plus two
 * directives, both spelled with `#?` so the file stays a valid comment stream to anything that
 * does not understand them:
 *
 * ```
 * #? <expr>            gates the NEXT entry line
 * #?{ <expr>           opens a gated block
 * #?}                  closes it
 * ```
 *
 * `<expr>` is the same version-predicate grammar the `//?` source gates use — `>=1.21.2`, `<26`,
 * `(>=1.21 && <1.21.11)`, `!(...)` — evaluated atom-by-atom through Stonecutter itself so the
 * comparison semantics cannot drift from the rest of the tree. Blocks do not nest, matching the
 * house rule for `//?` blocks.
 *
 * Comments and blank lines are stripped from the output: the template is the documented copy, and
 * the shipped jar carries only the entries that node actually resolved. `scripts/aw_check.py`
 * understands the same directives, so the template can be pre-flighted against every cached
 * vanilla jar before a node is ever added.
 *
 * @param outputName the widener's filename, which must match `mod.fabric.access_widener` — it is
 *   what fabric.mod.json points loading clients at.
 */
fun Project.generateAccessWidener(
	outputName: String,
	sourceName: String = outputName,
): File {
	// Always the ROOT copy — the template lives outside the source tree precisely so Stonecutter
	// never projects or rewrites it, so there is exactly one to read.
	val source = rootProject.file("accesswidener/$sourceName")
	require(source.isFile) { "Access-widener source not found: $source" }

	val stonecutter = sc
	val mc = stonecutter.current.version
	// The 26.x line ships unobfuscated, so its loom-no-remap build has nothing to remap and
	// validates the header against the game's own (official) names; everything below is remapped
	// out of Mojmap, which loom calls "named".
	val namespace = if (evalVersionExpr(stonecutter, mc, ">=26")) "official" else "named"

	val out = StringBuilder()
	out.append("accessWidener\tv2\t$namespace\n")
	out.append("# GENERATED for MC $mc — do not edit.\n")
	out.append("# Source: accesswidener/$sourceName (expanded by build-logic/AccessWidener.kt).\n")

	var blockCondition: Boolean? = null
	var pendingCondition: Boolean? = null

	source.readLines().forEachIndexed { index, raw ->
		val lineNo = index + 1
		val line = raw.trim()
		val where = "$sourceName:$lineNo"

		when {
			line.isEmpty() -> return@forEachIndexed

			line.startsWith("#?}") -> {
				check(blockCondition != null) { "$where: '#?}' with no open '#?{'" }
				check(pendingCondition == null) { "$where: '#? <expr>' matched no entry line" }
				blockCondition = null
			}

			line.startsWith("#?{") -> {
				check(blockCondition == null) { "$where: '#?{' inside an open block — blocks do not nest" }
				check(pendingCondition == null) { "$where: '#? <expr>' matched no entry line" }
				blockCondition = evalVersionExpr(stonecutter, mc, line.removePrefix("#?{").trim(), where)
			}

			line.startsWith("#?") -> {
				check(pendingCondition == null) { "$where: '#? <expr>' matched no entry line" }
				pendingCondition = evalVersionExpr(stonecutter, mc, line.removePrefix("#?").trim(), where)
			}

			line.startsWith("#") -> return@forEachIndexed

			line.startsWith("accessWidener") ->
				error("$where: the source must NOT carry a header — the namespace is chosen per node")

			else -> {
				val gate = pendingCondition ?: blockCondition ?: true
				pendingCondition = null
				// Trailing "# …" is dropped rather than passed through: loom's own parser tolerates
				// it, but the shipped file is machine-written and the commentary belongs upstream.
				if (gate) out.append(line.substringBefore('#').trim()).append('\n')
			}
		}
	}
	check(blockCondition == null) { "$sourceName: unclosed '#?{'" }
	check(pendingCondition == null) { "$sourceName: trailing '#? <expr>' matched no entry line" }

	val target = layout.buildDirectory.get().asFile.resolve("generated/accessWidener/$outputName")
	val text = out.toString()
	// Rewriting an identical file would re-date it and make processResources / remapJar rerun on
	// every configuration, so only touch it when it actually changed.
	if (!target.isFile || target.readText() != text) {
		target.parentFile.mkdirs()
		target.writeText(text)
	}

	// Loom does not put the widener in the jar for us: `remapJar` REWRITES an entry that is
	// already there (named -> intermediary) and fails outright if it is missing, and fabric.mod.json
	// points every loading client at that path. So the generated dir has to be a resource root.
	extensions.getByType<JavaPluginExtension>().sourceSets.named("main") {
		resources.srcDir(target.parentFile)
	}
	return target
}

/**
 * Boolean structure evaluated here, version comparison delegated to Stonecutter.
 *
 * `StonecutterBuildExtension.eval` is only ever handed a single comparison anywhere else in this
 * build (`">=1.20.5"` and friends), and the AW template needs `&&` / `||` / `!` / parentheses. So
 * this walks the expression and calls `eval` once per atom: the comparison semantics — including
 * the fact that `26` outranks `1.21.11` — stay exactly the tree's, and only the boolean glue is
 * ours. Mirrored by `_eval` in scripts/aw_check.py.
 *
 * Grammar: `expr := or`, `or := and ('||' and)*`, `and := unary ('&&' unary)*`,
 * `unary := '!' unary | '(' expr ')' | atom`, where an atom is a Stonecutter version predicate.
 */
internal fun evalVersionExpr(
	stonecutter: StonecutterBuildExtension,
	mc: String,
	expr: String,
	where: String = "",
): Boolean {
	val prefix = if (where.isEmpty()) "" else "$where: "
	val tokens = Regex("""\s*(\(|\)|&&|\|\||!|[^()\s!&|]+)""")
		.findAll(expr).map { it.groupValues[1] }.toList()
	require(tokens.isNotEmpty()) { "${prefix}empty version predicate" }

	var pos = 0
	fun peek() = tokens.getOrNull(pos)

	fun parseOr(): Boolean {
		fun parseAnd(): Boolean {
			fun parseUnary(): Boolean {
				return when (val t = peek()) {
					null -> error("${prefix}unexpected end of predicate in '$expr'")
					"!" -> { pos++; !parseUnary() }
					"(" -> {
						pos++
						val inner = parseOr()
						require(peek() == ")") { "${prefix}unbalanced '(' in '$expr'" }
						pos++
						inner
					}
					")", "&&", "||" -> error("${prefix}unexpected '$t' in '$expr'")
					else -> {
						pos++
						runCatching { stonecutter.eval(mc, t) }
							.getOrElse { error("${prefix}not a version predicate: '$t' in '$expr'") }
					}
				}
			}
			// No short-circuit: every atom is validated even when the result is already decided,
			// so a typo in the unreached half of an expression still fails the build.
			var acc = parseUnary()
			while (peek() == "&&") { pos++; acc = parseUnary() && acc }
			return acc
		}
		var acc = parseAnd()
		while (peek() == "||") { pos++; acc = parseAnd() || acc }
		return acc
	}

	val result = parseOr()
	require(pos == tokens.size) { "${prefix}trailing '${tokens.drop(pos).joinToString(" ")}' in '$expr'" }
	return result
}
