package dk.kb.util;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Specialized Bitmap (the specialized part is the whole-bitmap shift).
 */
public class Bitmap {
    public static final boolean DEFAULT_ENABLE_SHIFT_CACHE = false;

    private final long[] backing;
    private final int size;
    private final Map<Integer, Bitmap> shiftCache;

    private int cardinality = -1;

    public Bitmap(int size) {
        this(size, DEFAULT_ENABLE_SHIFT_CACHE);
    }
    public Bitmap(int size, boolean enableShiftCache) {
        this.size = size;
        backing = new long[(size >>> 6) + ((size & 63) == 0 ? 0 : 1)];
        shiftCache = enableShiftCache ? new HashMap<>() : null;
    }
    public void set(int index) {
        backing[index >>> 6] |= 1L << (63-(index & 63));
        invalidate();
    }
    public boolean get(int index) {
        return (backing[index >>> 6] & (1L << (63-(index & 63)))) != 0;
    }

    // Integer.MAX_VALUE means no more bits
    public int thisOrNext(int index) {
        if ((index & 0x63) != 0) { // Current word check
            final int nextAligned = ((index >>> 6) + 1) << 6;
            while (index < nextAligned) {
                if (get(index++)) {
                    return index - 1;
                }
            }
        }
        // Skip empty words
        int word = index >>> 6;
        while (word < backing.length && backing[word] == 0) {
            word++;
            index += 64;
        }
        // Look in-word until hit or EOD
        while (index < size && !get(index)) {
            index++;
        }

        return index < size ? index : Integer.MAX_VALUE;
    }

    public long[] getBacking() {
        return backing;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public int cardinality() {
        if (cardinality != -1) {
            return cardinality;
        }
        cardinality = 0;
        for (int i = 0 ; i <backing.length ; i++) {
            cardinality += Long.bitCount(backing[i]);
        }
        return cardinality;
    }

    // Imprecise cardinality counter. Counts at least up to limit (if possible)
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public int cardinalityStopAt(int limit) {
        if (cardinality != -1) {
            return cardinality;
        }
        int cardinality = 0;
        for (int i = 0 ; i <backing.length ; i++) {
            cardinality += Long.bitCount(backing[i]);
            if (cardinality >= limit) {
                return cardinality;
            }
        }
        this.cardinality = cardinality;
        return cardinality;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public boolean isCardinalityAtLeast(int atLeast) {
        return cardinalityStopAt(atLeast) >= atLeast;
    }

    public static Bitmap or(Bitmap map1, Bitmap map2, Bitmap reuse) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        for (int i = 0 ; i < map1.backing.length ; i++) {
            reuse.backing[i] = map1.backing[i] | map2.backing[i];
        }
        reuse.invalidate();
        return reuse;
    }

    public static Bitmap and(Bitmap map1, Bitmap map2, Bitmap reuse, boolean updateCardinality) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        reuse.invalidate();
        if (updateCardinality) {
            reuse.cardinality = 0;
            for (int i = 0; i < map1.backing.length; i++) {
                reuse.cardinality += Long.bitCount(reuse.backing[i] = map1.backing[i] & map2.backing[i]);
            }
        } else {
            for (int i = 0; i < map1.backing.length; i++) {
                reuse.backing[i] = map1.backing[i] & map2.backing[i];
            }
        }
        return reuse;
    }

    public static Bitmap xor(Bitmap map1, Bitmap map2, Bitmap reuse) {
        if (reuse == null) {
            reuse = new Bitmap(map1.size);
        }
        for (int i = 0 ; i < map1.backing.length ; i++) {
            reuse.backing[i] = map1.backing[i] ^ map2.backing[i];
        }
        reuse.invalidate();
        return reuse;
    }

    public void invert() {
        for (int i = 0 ; i < backing.length ; i++) {
            backing[i] = ~backing[i];
        }
        invalidate();
    }

    public void shift(int offset) {
        shift(offset, this);
    }
    /**
     * Shift the full bitmap by the specified offset.
     * @param offset negative offsets shifts left, positive shifts right.
     */
    public void shift(int offset, Bitmap destination) {
        if (offset == 0) {
            if (this == destination) {
                return;
            }
            System.arraycopy(backing, 0, destination.backing, 0, backing.length);
            destination.invalidate();
        }
        if (offset < 0) {
            offset = -offset;
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift left
                System.arraycopy(backing, lOffset, destination.backing, 0, backing.length-lOffset);
                Arrays.fill(destination.backing, backing.length - lOffset, backing.length, 0L);
            } else { // boundary crossing shift left
                if (shiftCache != null) { // Cache mode
                    Bitmap bShifted = shiftCache.get(-bOffset);
                    if (bShifted == null) {
                        bShifted = this.makeCopy(false);
                        bShifted.shift(-bOffset);
                        shiftCache.put(-bOffset, bShifted);
                    }
                    bShifted.shift(-(lOffset << 6), destination);
                } else {
                    for (int i = 0; i < backing.length; i++) {
                        final long source1 = i + lOffset >= backing.length ? 0L : backing[i + lOffset];
                        final long source2 = i + lOffset + 1 >= backing.length ? 0L : backing[i + lOffset + 1];
                        destination.backing[i] = source1 << bOffset | source2 >>> (64 - bOffset);
                    }
                }
            }
        } else {
            final int lOffset = offset >>> 6;
            final int bOffset = offset & 63;
            if ((bOffset) == 0) { // Whole block shift right
                System.arraycopy(backing, 0, destination.backing, lOffset, backing.length-lOffset);
                Arrays.fill(destination.backing, 0, lOffset, 0L);
            } else {
                if (shiftCache != null) { // Cache mode
                    Bitmap bShifted = shiftCache.get(bOffset);
                    if (bShifted == null) {
                        bShifted = this.makeCopy(false);
                        bShifted.shift(bOffset);
                        shiftCache.put(bOffset, bShifted);
                    }
                    bShifted.shift(lOffset << 6, destination);
                } else {
                    for (int i = backing.length - 1; i >= 0; i--) {
                        final long source1 = i - lOffset < 0 ? 0L : backing[i - lOffset];
                        final long source2 = i - lOffset - 1 < 0 ? 0L : backing[i - lOffset - 1];
                        destination.backing[i] = source1 >> bOffset | source2 << (64 - bOffset);
                    }
                }
            }
        }
    }

    private void invalidate() {
        cardinality = -1;
        if (shiftCache != null) {
            shiftCache.clear();
        }
    }

    public boolean equalBits(Bitmap other) {
        if (size != other.size) {
            return false;
        }
        for (int i = 0 ; i < backing.length ; i++) {
            if (backing[i] != other.backing[i]) {
                return false;
            }
        }
        return true;
    }

    public int[] getIntegers() {
        final int[] result = new int[cardinality()];
        int pos = 0;
        for (int i = 0 ; i < size ; i++) {
            if (get(i)) {
                result[pos++] = i;
            }
        }
        return result;
    }

    public Bitmap makeCopy() {
        return makeCopy(DEFAULT_ENABLE_SHIFT_CACHE);
    }
    public Bitmap makeCopy(boolean enableShiftcache) {
        Bitmap b = new Bitmap(size, enableShiftcache);
        copy(b);
        return b;
    }
    public void copy(Bitmap destination) {
        System.arraycopy(backing, 0, destination.backing, 0, backing.length);
        destination.cardinality = cardinality;
    }

    public long countIntersectingBits(Bitmap other) {
        long intersecting = 0;
        final int max = Math.min(backing.length, other.backing.length);
        for (int i = 0 ; i < max ; i++) {
            intersecting += Long.bitCount(backing[i] & other.backing[i]);
        }
        return intersecting;
    }
}
