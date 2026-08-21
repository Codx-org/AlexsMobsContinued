package com.github.alexthe666.alexsmobs.citadel.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

/**
 * Utilities for interacting with PacketBuffer.
 *
 * @author cpw
 * @since 1.0.0
 */
public class PacketBufferUtils {
    /**
     * The number of bytes to write the supplied int using the 7 bit varint encoding.
     *
     * @param toCount The number to analyse
     * @return The number of bytes it will take to write it (maximum of 5)
     */
    public static int varIntByteCount(int toCount) {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : ((toCount & 0xFFFFC000) == 0 ? 2 : ((toCount & 0xFFE00000) == 0 ? 3 : ((toCount & 0xF0000000) == 0 ? 4 : 5)));
    }

    /**
     * Read a varint from the supplied buffer.
     *
     * @param buf     The buffer to read from
     * @param maxSize The maximum length of bytes to read
     * @return The integer
     */
    public static int readVarInt(ByteBuf buf, int maxSize) {
        Validate.isTrue(maxSize < 6 && maxSize > 0, "Varint length is between 1 and 5, not %d", maxSize);
        int i = 0;
        int j = 0;
        byte b0;

        do {
            b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;

            if (j > maxSize) {
                throw new RuntimeException("VarInt too big");
            }
        }
        while ((b0 & 128) == 128);

        return i;
    }

    /**
     * An extended length short. Used by custom payload packets to extend size.
     *
     * @param buf
     * @return
     */
    public static int readVarShort(ByteBuf buf) {
        int low = buf.readUnsignedShort();
        int high = 0;
        if ((low & 0x8000) != 0) {
            low = low & 0x7FFF;
            high = buf.readUnsignedByte();
        }
        return ((high & 0xFF) << 15) | low;
    }

    public static void writeVarShort(ByteBuf buf, int toWrite) {
        int low = toWrite & 0x7FFF;
        int high = (toWrite & 0x7F8000) >> 15;
        if (high != 0) {
            low = low | 0x8000;
        }
        buf.writeShort(low);
        if (high != 0) {
            buf.writeByte(high);
        }
    }

    /**
     * Write an integer to the buffer using variable length encoding. The maxSize constrains
     * how many bytes (and therefore the maximum number) that will be written.
     *
     * @param to      The buffer to write to
     * @param toWrite The integer to write
     * @param maxSize The maximum number of bytes to use
     */
    public static void writeVarInt(ByteBuf to, int toWrite, int maxSize) {
        Validate.isTrue(varIntByteCount(toWrite) <= maxSize, "Integer is too big for %d bytes", maxSize);
        while ((toWrite & -128) != 0) {
            to.writeByte(toWrite & 127 | 128);
            toWrite >>>= 7;
        }

        to.writeByte(toWrite);
    }

    /**
     * Read a UTF8 string from the byte buffer.
     * It is encoded as <varint length>[<UTF8 char bytes>]
     *
     * @param from The buffer to read from
     * @return The string
     */
    public static String readUTF8String(ByteBuf from) {
        int len = readVarInt(from, 2);
        String str = from.toString(from.readerIndex(), len, StandardCharsets.UTF_8);
        from.readerIndex(from.readerIndex() + len);
        return str;
    }

    /**
     * Write a String with UTF8 byte encoding to the buffer.
     * It is encoded as <varint length>[<UTF8 char bytes>]
     *
     * @param to     the buffer to write to
     * @param string The string to write
     */
    public static void writeUTF8String(ByteBuf to, String string) {
        byte[] utf8Bytes = string.getBytes(StandardCharsets.UTF_8);
        Validate.isTrue(varIntByteCount(utf8Bytes.length) < 3, "The string is too long for this encoding.");
        writeVarInt(to, utf8Bytes.length, 2);
        to.writeBytes(utf8Bytes);
    }

    /**
     * Write an {@link ItemStack} using minecraft compatible encoding.
     *
     * @param to    The buffer to write to
     * @param stack The itemstack to write
     */
    public static void writeItemStack(ByteBuf to, ItemStack stack) {
        com.github.alexthe666.alexsmobs.misc.AMCompat.writeItem(wrap(to), stack);
    }

    /**
     * Read an {@link ItemStack} from the byte buffer provided. It uses the minecraft encoding.
     *
     * @param from The buffer to read from
     * @return The itemstack read
     */
    public static ItemStack readItemStack(ByteBuf from) {
        try {
            return com.github.alexthe666.alexsmobs.misc.AMCompat.readItem(wrap(from));
        } catch (Exception e) {
            // Unpossible?
            throw new RuntimeException(e);
        }
    }

    /**
     * Adopt a buffer without changing what it is (#72).
     *
     * <p>Citadel's helpers are typed on the raw {@link ByteBuf}, and both of them used to
     * {@code new FriendlyByteBuf(to)} on the way in. That is lossless below 1.20.5 and destructive
     * from 1.20.5 up: a stack's components need the registries, so {@code ItemStack}'s stream codec
     * runs over a {@code RegistryFriendlyByteBuf} and {@link
     * com.github.alexthe666.alexsmobs.misc.AMCompat#writeItem} casts to one — but the connection
     * hands the message its registry-carrying buffer, and re-wrapping it produced a *plain*
     * {@code FriendlyByteBuf} around the same bytes. The cast then threw
     * {@code ClassCastException} inside the packet encoder, which vanilla treats as a fatal
     * connection error: the player is disconnected mid-interaction.
     *
     * <p>Only two messages route stacks through here — {@code MessageUpdateCapsid} and
     * {@code MessageUpdateTransmutablesToDisplay} — so the symptom was "putting an item into a
     * capsid kicks you" and the same for the transmutation table's display sync, on all three
     * loaders on every node >=1.20.5. The kangaroo messages call {@code AMCompat.writeItem}
     * straight on the message's own buffer, which is why report #24's fix left this untouched.
     */
    private static FriendlyByteBuf wrap(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }

    /**
     * Write an {@link CompoundTag} to the byte buffer. It uses the minecraft encoding.
     *
     * @param to  The buffer to write to
     * @param tag The tag to write
     */
    public static void writeTag(ByteBuf to, CompoundTag tag) {
        // NBT needs no registries, so this pair never hit #72 — routed through wrap anyway so the
        // "never re-wrap a buffer you were handed" rule holds for the whole class.
        wrap(to).writeNbt(tag);
    }

    /**
     * Read an {@link CompoundTag} from the byte buffer. It uses the minecraft encoding.
     *
     * @param from The buffer to read from
     * @return The read tag
     */
    @Nullable
    public static CompoundTag readTag(ByteBuf from) {
        try {
            return wrap(from).readNbt();
        } catch (Exception e) {
            // Unpossible?
            throw new RuntimeException(e);
        }
    }
}
