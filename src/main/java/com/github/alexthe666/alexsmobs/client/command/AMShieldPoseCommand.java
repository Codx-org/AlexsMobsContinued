package com.github.alexthe666.alexsmobs.client.command;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * {@code /shieldpose} — tune the shield of the deep's held pose from inside a running client.
 *
 * <p><b>Why this exists.</b> A held-item pose is nine numbers per display context and none of them
 * can be reasoned about from the JSON: the shield of the deep is gripped handle-toward-the-camera,
 * so a rotation reads on screen as a translation and vice versa. Bug #33 burned four blind client
 * launches at ~25 minutes each converging on nothing. The fix is to stop guessing — edit the pose
 * with the shield in your hand and watch it move.
 *
 * <p><b>How it updates without a rebuild.</b> The command rewrites the {@code display} block of the
 * shield's model files — the source copy under {@code src/} <i>and</i> every node's already-built
 * {@code versions/&lt;node&gt;/build/resources/main} copy, which is the one a dev client actually
 * reads — then calls {@link Minecraft#reloadResourcePacks()}, the same thing F3+T does. So the edit
 * both shows up immediately and survives into the repo, with no Gradle run in between.
 *
 * <p><b>It is a development tool and disables itself outside a checkout.</b> {@link #available()}
 * looks for the repo root by walking up from the game directory, and the registration sites skip
 * the command when there is none. A player's install has no {@code stonecutter.properties.toml}
 * above its {@code .minecraft}, so a shipped jar never shows this command at all — it neither
 * appears in tab-completion nor writes to anything.
 *
 * <p><b>The sibling tool.</b> {@code scripts/shieldpose.py} does the same edits from a shell and
 * shares this class's file list, context map and on-disk format exactly (tab indent, expanded
 * arrays, whole numbers written without a decimal point), so the two can be used interchangeably
 * and neither reformats the other's output. The script additionally prints a projected on-screen
 * bounding box, which needs no running client; this command additionally needs no alt-tab.
 *
 * <p><b>The source type is a type parameter</b> for the same reason
 * {@code docs/notes/client-settings.md} records for the old {@code /aac}: Forge and NeoForge hand
 * out a {@code CommandDispatcher<CommandSourceStack>} from {@code RegisterClientCommandsEvent}
 * while Fabric API's {@code ClientCommandRegistrationCallback} hands out one over
 * {@code FabricClientCommandSource}. Leaving {@code S} open lets the tree below compile unchanged
 * on all 49 nodes with each caller supplying only how to say something back. Everything here is
 * plain {@code com.mojang.brigadier} — no {@code Commands.literal}, no {@code ClientCommandManager}
 * (removed in fabric-command-api-v2 3.x, which the 26.x nodes pin).
 */
public final class AMShieldPoseCommand {

    private AMShieldPoseCommand() {
    }

    /**
     * Pose name to the model files carrying it: the {@code >=1.21.4} rebuilt model first (it is the
     * one read for current values), then the {@code <1.21.4} original, which still owns the display
     * block on those nodes. #33's fix deliberately diverges from upstream in <b>both</b>, so an edit
     * that touched only one would fix half the tree.
     */
    private static final Map<String, String[]> MODELS = new LinkedHashMap<>();

    /**
     * Short context name to the {@code display} keys it writes.
     *
     * <p>Only {@code first} writes two keys, and it is the only context that <b>solves</b> rather
     * than copies: the four first-person entries it fills (two hands times two model files) each
     * sit behind a different vanilla pre-transform, so none of them need equal what was typed. See
     * {@link #solve}. {@code firstright}/{@code firstleft} write one key raw, the escape hatch for
     * a deliberately asymmetric pose. In third person upstream wrote genuinely different numbers
     * per hand, so those stay separate contexts and are tuned by eye.
     */
    private static final Map<String, String[]> CONTEXTS = new LinkedHashMap<>();

    /**
     * Vanilla's <b>own</b> first-person arm transform, per model file: {@code {rx, ry, rz}} in
     * degrees then {@code {x, y, z}} in blocks, both for the <b>right</b> hand. Absent means none.
     *
     * <p>From <b>1.21.4</b>, {@code ItemInHandRenderer}'s {@code case BLOCK} wraps any item that is
     * not a {@code ShieldItem} in a hardcoded pose before the {@code display} block gets a say:
     *
     * <pre>    translate(invert * -0.14142136, 0.08, 0.14142136);
     *    Rx(-102.25); Ry(invert * 13.365); Rz(invert * 78.05);</pre>
     *
     * Below 1.21.4 that branch is a bare {@code applyItemArmTransform} — no rotation at all. The
     * boundary was measured, not recalled: every 1.20.1 → 26.2 client jar was disassembled and
     * 1.21.3 has no such constants while 1.21.4 does. {@link com.github.alexthe666.alexsmobs.item.ItemShieldOfTheDeep}
     * extends {@code Item}, never {@code ShieldItem}, on every loader, so it always takes the
     * branch — and {@code invert} makes the off-hand a <b>mirror</b> of the main hand, which is
     * exactly what turned the spikes to face the player when blocking left-handed.
     *
     * <p>This is why one set of numbers cannot simply be copied into all four first-person entries:
     * two hands times two eras is four different pre-transforms. {@link #solve} inverts each.
     *
     * <p>The {@code applyItemArmTransform} offset that precedes this in every era
     * ({@code translate(invert * 0.56, -0.52, -0.72)}) is deliberately absent: it is the same for
     * both files and its own {@code invert} is what puts the off-hand on the other side of the
     * screen, which is wanted, so it cancels out of the solve. {@code scripts/shieldpose.py} keeps
     * it because its on-screen projection needs an absolute position.
     */
    private static final Map<String, double[][]> ARM = new LinkedHashMap<>();

    /**
     * Index into {@link #MODELS}'s value of the file <i>this</i> client renders — the frame a typed
     * pose is authored in. The other file is solved to match, so tuning on any node fixes both.
     */
    //? if >=1.21.4 {
    /*private static final int PRIMARY = 0;
    *///?} else {
    private static final int PRIMARY = 1;
    //?}

    /**
     * Model file to the file whose frame a typed pose is authored in — its pose's
     * {@link #PRIMARY}. {@link #solve} needs it to know what a typed number meant before it can say
     * what the other era's number should be.
     */
    private static final Map<String, String> PRIMARY_OF = new LinkedHashMap<>();

    static {
        ARM.put("shield_of_the_deep_3d_blocking.json",
                new double[][]{{-102.25, 13.365, 78.05}, {-0.14142136, 0.08, 0.14142136}});

        MODELS.put("normal", new String[]{"shield_of_the_deep_3d.json", "shield_of_the_deep.json"});
        MODELS.put("blocking", new String[]{"shield_of_the_deep_3d_blocking.json", "shield_of_the_deep_blocking.json"});
        for (String[] files : MODELS.values()) {
            for (String file : files) {
                PRIMARY_OF.put(file, files[PRIMARY]);
            }
        }

        CONTEXTS.put("first", new String[]{"firstperson_righthand", "firstperson_lefthand"});
        CONTEXTS.put("firstright", new String[]{"firstperson_righthand"});
        CONTEXTS.put("firstleft", new String[]{"firstperson_lefthand"});
        CONTEXTS.put("third", new String[]{"thirdperson_righthand"});
        CONTEXTS.put("thirdleft", new String[]{"thirdperson_lefthand"});
        CONTEXTS.put("gui", new String[]{"gui"});
        CONTEXTS.put("ground", new String[]{"ground"});
        CONTEXTS.put("fixed", new String[]{"fixed"});
        CONTEXTS.put("head", new String[]{"head"});
    }

    private static final String MODEL_DIR = "assets/alexsmobs/models/item";

    /** Sticky working pose and context, so a repeated nudge is one short line plus up-arrow. */
    private static String pose = "blocking";
    private static String context = "first";

    /** Cached because {@link #available()} is asked once per command registration. */
    private static Path repoRoot;
    private static boolean repoRootResolved;

    // ------------------------------------------------------------------ the tree

    /**
     * Registers {@code /shieldpose} into a client dispatcher.
     *
     * @param feedback how to show a message to whoever ran the command, on this loader.
     */
    public static <S> void register(CommandDispatcher<S> dispatcher, BiConsumer<S, Component> feedback) {
        LiteralArgumentBuilder<S> root = LiteralArgumentBuilder.<S>literal("shieldpose")
                .executes(c -> show(c.getSource(), feedback));

        root.then(LiteralArgumentBuilder.<S>literal("show")
                .executes(c -> show(c.getSource(), feedback)));

        root.then(LiteralArgumentBuilder.<S>literal("reload")
                .executes(c -> {
                    Minecraft.getInstance().reloadResourcePacks();
                    say(c.getSource(), feedback, "reloading resources");
                    return 1;
                }));

        // `/shieldpose pose blocking` and `/shieldpose ctx third` set the sticky target, so the
        // edit commands stay short. Both are literals rather than a string argument to get
        // tab-completion of the valid names for free.
        LiteralArgumentBuilder<S> poseNode = LiteralArgumentBuilder.literal("pose");
        for (String name : MODELS.keySet()) {
            poseNode.then(LiteralArgumentBuilder.<S>literal(name).executes(c -> {
                pose = name;
                return show(c.getSource(), feedback);
            }));
        }
        root.then(poseNode);

        LiteralArgumentBuilder<S> ctxNode = LiteralArgumentBuilder.literal("ctx");
        for (String name : CONTEXTS.keySet()) {
            ctxNode.then(LiteralArgumentBuilder.<S>literal(name).executes(c -> {
                context = name;
                return show(c.getSource(), feedback);
            }));
        }
        root.then(ctxNode);

        // `set` takes the value, `nudge` adds to it. The two subtrees are identical in shape and
        // differ only in the flag handed to apply(), so one helper builds both.
        root.then(verbs("set", false, feedback));
        root.then(verbs("nudge", true, feedback));

        dispatcher.register(root);
    }

    private static <S> LiteralArgumentBuilder<S> verbs(String name, boolean relative, BiConsumer<S, Component> feedback) {
        return LiteralArgumentBuilder.<S>literal(name)
                .then(vec3("rot", relative, feedback))
                .then(vec3("trans", relative, feedback))
                .then(scalar("scale", relative, feedback));
    }

    private static <S> LiteralArgumentBuilder<S> vec3(String key, boolean relative, BiConsumer<S, Component> feedback) {
        return LiteralArgumentBuilder.<S>literal(key)
                .then(RequiredArgumentBuilder.<S, Float>argument("x", FloatArgumentType.floatArg())
                        .then(RequiredArgumentBuilder.<S, Float>argument("y", FloatArgumentType.floatArg())
                                .then(RequiredArgumentBuilder.<S, Float>argument("z", FloatArgumentType.floatArg())
                                        .executes(c -> apply(c, feedback, key, relative,
                                                new float[]{f(c, "x"), f(c, "y"), f(c, "z")})))));
    }

    private static <S> LiteralArgumentBuilder<S> scalar(String key, boolean relative, BiConsumer<S, Component> feedback) {
        return LiteralArgumentBuilder.<S>literal(key)
                .then(RequiredArgumentBuilder.<S, Float>argument("v", FloatArgumentType.floatArg())
                        .executes(c -> {
                            float v = f(c, "v");
                            return apply(c, feedback, key, relative, new float[]{v, v, v});
                        }));
    }

    private static <S> float f(CommandContext<S> c, String name) {
        return FloatArgumentType.getFloat(c, name);
    }

    // ------------------------------------------------------------------ the work

    private static <S> int apply(CommandContext<S> c, BiConsumer<S, Component> feedback,
                                 String key, boolean relative, float[] values) {
        S source = c.getSource();
        Path root = repoRoot();
        if (root == null) {
            say(source, feedback, "no checkout found above the game directory — nothing to edit");
            return 0;
        }
        String field = fieldName(key);
        String[] keys = CONTEXTS.get(context);

        // The whole entry travels, not just the edited field: solving a hand needs the rotation and
        // the translation together, and an untouched field has to reach the other era's file too.
        JsonObject entry = current(root, pose, keys[0]);
        float[] target = values;
        if (relative) {
            float[] currentValue = get(entry, field);
            target = new float[3];
            for (int i = 0; i < 3; i++) {
                target[i] = currentValue[i] + values[i];
            }
        }
        entry.add(field, array(target));
        entry = ordered(entry);

        int written;
        try {
            written = write(root, pose, keys, entry);
        } catch (IOException e) {
            say(source, feedback, "write failed: " + e);
            return 0;
        }
        if (written == 0) {
            say(source, feedback, "no model file carried a display block — nothing written");
            return 0;
        }

        // Rebaking the model is the whole point of the command; without it the JSON on disk and
        // the shield in your hand disagree until the next manual F3+T.
        Minecraft.getInstance().reloadResourcePacks();
        say(source, feedback, pose + " [" + context + "] " + field + " = " + fmt(target, field)
                + "  (" + written + " file" + (written == 1 ? "" : "s") + ", reloading)");
        return 1;
    }

    private static <S> int show(S source, BiConsumer<S, Component> feedback) {
        Path root = repoRoot();
        if (root == null) {
            say(source, feedback, "no checkout found above the game directory — this is a dev-only tool");
            return 0;
        }
        say(source, feedback, "editing " + pose + " [" + context + "]  (/shieldpose pose … | ctx … to switch)");
        String[] keys = CONTEXTS.get(context);
        for (String p : MODELS.keySet()) {
            JsonObject entry = current(root, p, keys[0]);
            String line = p + ": rot " + fmt(get(entry, "rotation"), "rotation")
                    + "  trans " + fmt(get(entry, "translation"), "translation")
                    + "  scale " + fmt(get(entry, "scale"), "scale");
            say(source, feedback, "  " + line);
        }
        return 1;
    }

    /** Short verb to the {@code display} entry key it edits. */
    private static String fieldName(String key) {
        switch (key) {
            case "rot":
                return "rotation";
            case "trans":
                return "translation";
            default:
                return "scale";
        }
    }

    /**
     * One {@code display} entry of the pose's {@link #PRIMARY} model — the file <i>this</i> client
     * renders, and therefore the frame a typed pose is authored in. Reading one file consistently
     * is what makes a nudge idempotent; the other era's file is never read, only solved into.
     *
     * <p>A missing entry answers an empty object rather than a filled-in default, so a pose that
     * never wrote a {@code scale} does not gain one just by having its rotation nudged.
     */
    private static JsonObject current(Path root, String poseName, String displayKey) {
        JsonObject model = readJson(root.resolve("src/main/resources").resolve(MODEL_DIR)
                .resolve(MODELS.get(poseName)[PRIMARY]));
        if (model != null && model.has("display")) {
            JsonElement entry = model.getAsJsonObject("display").get(displayKey);
            if (entry != null && entry.isJsonObject()) {
                return entry.getAsJsonObject().deepCopy();
            }
        }
        return new JsonObject();
    }

    /**
     * The three keys in the order the checked-in models carry them, dropping anything else. Both
     * tools write this order, which is what keeps one from reformatting the other's output.
     */
    private static JsonObject ordered(JsonObject entry) {
        JsonObject out = new JsonObject();
        for (String key : new String[]{"rotation", "translation", "scale"}) {
            if (entry.has(key)) {
                out.add(key, entry.get(key));
            }
        }
        return out;
    }

    /**
     * One field of an entry as a triple, defaulting the way the JSON does: an absent {@code scale}
     * is {@code 1}, everything else {@code 0}, and a scalar {@code scale} widens.
     */
    private static float[] get(JsonObject entry, String field) {
        float[] fallback = field.equals("scale") ? new float[]{1, 1, 1} : new float[]{0, 0, 0};
        JsonElement value = entry.get(field);
        if (value == null) {
            return fallback;
        }
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            float[] out = fallback.clone();
            for (int i = 0; i < Math.min(3, array.size()); i++) {
                out[i] = array.get(i).getAsFloat();
            }
            return out;
        }
        float v = value.getAsFloat();
        return new float[]{v, v, v};
    }

    /**
     * Writes an entry into every copy of the pose's models, returning how many files changed.
     *
     * <p>"Every copy" is the source tree plus each node's built resources. Writing the built copies
     * is what makes the change visible to the client already running; writing the source is what
     * makes it survive the next Gradle run. Nodes that were never built are simply absent and
     * skipped, and a model whose {@code display} block the {@code >=1.21.4} migration emptied is
     * skipped too rather than being given one back.
     *
     * <p>Two display keys means {@code ctx first}, the one context that {@linkplain #solve solves}:
     * each file gets its own pair of hands worked out from the authored pose. Any other context is
     * written verbatim into each key it names.
     */
    private static int write(Path root, String poseName, String[] displayKeys, JsonObject entry) throws IOException {
        boolean paired = displayKeys.length == 2;
        int written = 0;
        for (String file : MODELS.get(poseName)) {
            JsonObject[] solved = paired ? solve(entry, file) : null;
            for (Path path : targets(root, file)) {
                JsonObject model = readJson(path);
                if (model == null || !model.has("display")) {
                    continue;
                }
                JsonObject display = model.getAsJsonObject("display");
                if (paired) {
                    display.add(displayKeys[0], solved[0].deepCopy());
                    display.add(displayKeys[1], solved[1].deepCopy());
                } else {
                    for (String key : displayKeys) {
                        display.add(key, entry.deepCopy());
                    }
                }
                writeJson(path, model);
                written++;
            }
        }
        return written;
    }

    // ------------------------------------------------------------------ the solve

    /**
     * The {@code {right, left}} first-person entries for one model file, from a pose authored
     * against {@link #PRIMARY}'s right hand.
     *
     * <p>The authored numbers are first turned into a <b>world pose</b> — the orientation and
     * offset the shield actually ends up with, independent of hand and era — and then each entry is
     * solved back out of it through that file's and that hand's own pre-transform:
     *
     * <pre>    world orientation  W = A * Q    =&gt;  Q = A^T * W
     *    world offset       P = a + A * T  =&gt;  T = A^T * (P - a)</pre>
     *
     * <p>The off-hand needs no separate translation. Its arm transform is the right hand's
     * conjugated by the x-mirror, so the JSON translation that lands the mirrored position is the
     * <i>same</i> one and {@code ItemTransform.apply}'s own negation of {@code translation.x} does
     * the mirroring. Its rotation is not free that way — we want the orientation identical, not
     * mirrored — so it is solved separately and then pre-negated in y and z to cancel that same
     * negation. Writing one rotation into both hands is exactly what turned the spikes to face the
     * player: near ±90° a mirror and a rotation are furthest apart.
     */
    private static JsonObject[] solve(JsonObject entry, String file) {
        double[] rot = dbl(get(entry, "rotation"));
        double[] trans = dbl(get(entry, "translation"));

        String primary = PRIMARY_OF.get(file);
        double[][] authored = armRotation(primary, true);
        double[] authoredOffset = armOffset(primary, true);
        double[][] world = mul(authored, rotXYZ(rot));
        double[] scaled = {trans[0] / 16.0, trans[1] / 16.0, trans[2] / 16.0};
        double[] turned = mulVec(authored, scaled);
        double[] position = {authoredOffset[0] + turned[0], authoredOffset[1] + turned[1],
                authoredOffset[2] + turned[2]};

        double[][] right = armRotation(file, true);
        double[] rightOffset = armOffset(file, true);
        double[][] left = armRotation(file, false);
        double[] rotRight = eulerXYZ(mul(transpose(right), world));
        double[] rotLeft = eulerXYZ(mul(transpose(left), world));
        rotLeft = new double[]{rotLeft[0], -rotLeft[1], -rotLeft[2]};
        double[] delta = {position[0] - rightOffset[0], position[1] - rightOffset[1],
                position[2] - rightOffset[2]};
        double[] transRight = mulVec(transpose(right), delta);
        for (int i = 0; i < 3; i++) {
            transRight[i] *= 16.0;
        }

        return new JsonObject[]{solved(entry, rotRight, transRight), solved(entry, rotLeft, transRight)};
    }

    /** One solved entry, keeping the authored {@code scale} verbatim and only when there was one. */
    private static JsonObject solved(JsonObject entry, double[] rotation, double[] translation) {
        JsonObject out = new JsonObject();
        out.add("rotation", array(rotation));
        out.add("translation", array(translation));
        if (entry.has("scale")) {
            out.add("scale", entry.get("scale").deepCopy());
        }
        return out;
    }

    /** Vanilla's arm rotation for one model file and one hand — identity where {@link #ARM} is silent. */
    private static double[][] armRotation(String file, boolean rightHand) {
        double[][] arm = ARM.get(file);
        if (arm == null) {
            return rotXYZ(new double[]{0, 0, 0});
        }
        int invert = rightHand ? 1 : -1;
        return mul(mul(rotationX(arm[0][0]), rotationY(invert * arm[0][1])), rotationZ(invert * arm[0][2]));
    }

    /** Vanilla's arm offset for one model file and one hand, in blocks. */
    private static double[] armOffset(String file, boolean rightHand) {
        double[][] arm = ARM.get(file);
        if (arm == null) {
            return new double[]{0, 0, 0};
        }
        return new double[]{(rightHand ? 1 : -1) * arm[1][0], arm[1][1], arm[1][2]};
    }

    /**
     * The rotation a {@code display} block's {@code rotation} array produces: {@code Rx*Ry*Rz}.
     *
     * <p>{@code ItemTransform.apply} rotates by JOML's {@code Quaternionf.rotationXYZ}, whose
     * multiplication order was confirmed by running JOML rather than by recall.
     */
    private static double[][] rotXYZ(double[] r) {
        return mul(mul(rotationX(r[0]), rotationY(r[1])), rotationZ(r[2]));
    }

    /** Inverse of {@link #rotXYZ}. The gimbal branch never fires on any pose worth writing. */
    private static double[] eulerXYZ(double[][] m) {
        double b = Math.asin(Math.max(-1, Math.min(1, m[0][2])));
        double a;
        double c;
        if (Math.abs(m[0][2]) < 0.999999) {
            a = Math.atan2(-m[1][2], m[2][2]);
            c = Math.atan2(-m[0][1], m[0][0]);
        } else {
            a = Math.atan2(m[2][1], m[1][1]);
            c = 0;
        }
        return new double[]{Math.toDegrees(a), Math.toDegrees(b), Math.toDegrees(c)};
    }

    private static double[][] rotationX(double deg) {
        double a = Math.toRadians(deg);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{1, 0, 0}, {0, c, -s}, {0, s, c}};
    }

    private static double[][] rotationY(double deg) {
        double a = Math.toRadians(deg);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{c, 0, s}, {0, 1, 0}, {-s, 0, c}};
    }

    private static double[][] rotationZ(double deg) {
        double a = Math.toRadians(deg);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{c, -s, 0}, {s, c, 0}, {0, 0, 1}};
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] out = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return out;
    }

    private static double[][] transpose(double[][] m) {
        double[][] out = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = m[j][i];
            }
        }
        return out;
    }

    private static double[] mulVec(double[][] m, double[] v) {
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            out[i] = m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2];
        }
        return out;
    }

    private static double[] dbl(float[] v) {
        return new double[]{v[0], v[1], v[2]};
    }

    /** The source copy of one model, then every node's built copy of it. */
    private static List<Path> targets(Path root, String file) {
        List<Path> out = new ArrayList<>();
        out.add(root.resolve("src/main/resources").resolve(MODEL_DIR).resolve(file));
        Path versions = root.resolve("versions");
        if (Files.isDirectory(versions)) {
            try (DirectoryStream<Path> nodes = Files.newDirectoryStream(versions)) {
                for (Path node : nodes) {
                    out.add(node.resolve("build/resources/main").resolve(MODEL_DIR).resolve(file));
                }
            } catch (IOException ignored) {
                // A node mid-build can hand back a partial listing; the ones already collected are
                // still worth writing, and the source copy above is never in that set.
            }
        }
        out.removeIf(p -> !Files.isRegularFile(p));
        return out;
    }

    // ------------------------------------------------------------------ json i/o

    private static JsonObject readJson(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Writes tab-indented JSON with expanded arrays — byte-for-byte what Python's
     * {@code json.dump(indent="\t")} produces, which is what {@code scripts/shieldpose.py} has
     * always written and what the checked-in models are formatted as. Matching it exactly is what
     * keeps the two tools from reformatting each other's output into the diff.
     *
     * <p>Untouched numbers survive verbatim because {@link JsonParser} keeps them as lazily-parsed
     * text; only the values written by {@link #array} are re-rendered, and those follow Python's
     * rule of dropping the decimal point when the value is whole.
     */
    private static void writeJson(Path path, JsonObject model) throws IOException {
        try (Writer out = Files.newBufferedWriter(path)) {
            JsonWriter json = new JsonWriter(out);
            json.setIndent("\t");
            new Gson().toJson(model, json);
            // Flush before the trailing newline: JsonWriter buffers, so writing straight to `out`
            // while it still holds content would put the newline in the middle of the file.
            json.flush();
            out.write("\n");
        }
    }

    private static JsonArray array(float[] value) {
        JsonArray out = new JsonArray();
        for (float v : value) {
            out.add(new JsonPrimitive(v == Math.rint(v) ? (Number) (int) v : (Number) v));
        }
        return out;
    }

    /**
     * A solved triple, rounded to four decimals the way {@code scripts/shieldpose.py} rounds — a
     * solve lands on values like {@code -157.34579999} that would otherwise reach the diff in full.
     */
    private static JsonArray array(double[] value) {
        JsonArray out = new JsonArray();
        for (double v : value) {
            double r = Math.rint(v * 10000.0) / 10000.0;
            out.add(new JsonPrimitive(r == Math.rint(r) ? (Number) (int) r : (Number) r));
        }
        return out;
    }

    private static String fmt(float[] value, String field) {
        if (field.equals("scale")) {
            return trim(value[0]);
        }
        return trim(value[0]) + "," + trim(value[1]) + "," + trim(value[2]);
    }

    private static String trim(float v) {
        return v == Math.rint(v)
                ? Integer.toString((int) v)
                : String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "");
    }

    // ------------------------------------------------------------------ dev-checkout detection

    /**
     * Whether this client is running out of a checkout of this repo, and therefore whether the
     * command has anything to edit. Registration sites use it to keep {@code /shieldpose} out of a
     * shipped jar's command list entirely.
     */
    public static boolean available() {
        return repoRoot() != null;
    }

    /**
     * The repo root, found by walking up from the game directory looking for the marker files.
     *
     * <p>A dev run's game directory is {@code versions/<node>/run}, so this normally climbs three
     * levels — but walking rather than counting keeps it correct for the active node, whose run
     * directory sits elsewhere, and for anyone who has pointed the run config somewhere else. Both
     * markers are required: {@code stonecutter.properties.toml} alone could plausibly be a sibling
     * checkout, and the pair is unique to this repo's root.
     */
    private static Path repoRoot() {
        if (!repoRootResolved) {
            repoRootResolved = true;
            Path dir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath();
            while (dir != null) {
                if (Files.isRegularFile(dir.resolve("stonecutter.properties.toml"))
                        && Files.isRegularFile(dir.resolve("settings.gradle.kts"))) {
                    repoRoot = dir;
                    break;
                }
                dir = dir.getParent();
            }
        }
        return repoRoot;
    }

    private static <S> void say(S source, BiConsumer<S, Component> feedback, String message) {
        feedback.accept(source, Component.literal("[shieldpose] " + message));
    }
}
