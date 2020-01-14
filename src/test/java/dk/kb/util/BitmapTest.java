package dk.kb.util;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class BitmapTest {

    @Test
    public void testSpecificSetGets() {
        Bitmap b = new Bitmap(128);
        Arrays.stream(new int[]{0, 1, 63, 64, 127}).peek(b::set).forEach(
                bit -> assertTrue(b.get(bit), "The bit " + bit + " should be set"));
    }

    @Test
    public void testShiftCache() {
        Random r = new Random(87);
        Bitmap con = new Bitmap(256, true);
        for (int i = 0 ; i < 100 ; i++) {
            con.set(r.nextInt(con.size()));
        }
        Bitmap sans = con.makeCopy(false);

        Bitmap conDest = new Bitmap(con.size(), false);
        Bitmap sansDest = new Bitmap(sans.size(), false);

        con.shift(-62, conDest);
        sans.shift(-62, sansDest);
        assertTrue(conDest.equalBits(sansDest), "Specific shifting by -62 should give equal results");

        for (int i = -200 ; i <= 200 ; i++) {
            con.shift(i, conDest);
            sans.shift(i, sansDest);
            assertTrue(conDest.equalBits(sansDest), "Shifting by " + i + " should give equal results");
        }
    }

    @Test
    public void testNext() {
        Bitmap b = new Bitmap(128);
        Arrays.stream(new int[]{0, 1, 63, 64, 127}).forEach(b::set);
        final int[][] TESTS = new int[][] {
                {0, 0},
                {1, 1},
                {2, 63},
                {63, 63},
                {64, 64},
                {65, 127},
                {128, Integer.MAX_VALUE}
        };
        for (int[] test: TESTS) {
            assertEquals(test[1], b.thisOrNext(test[0]),"Next for " + test[0] + " should be as expected");
        }
    }

    @Test
    public void testRandomSetGet() {
        Bitmap b = new Bitmap(1000);

        Random r = new Random(87);
        for (int i = 0; i < 100 ; i++) {
            if (r.nextDouble() < 0.1) {
                b.set(i);
            }
        }

        r = new Random(87);
        for (int i = 0; i < 100 ; i++) {
            if (r.nextDouble() < 0.1) {
                assertTrue(b.get(i), "The bit at index " + i + " should be set but was not");
                b.set(i);
            } else {
                assertFalse(b.get(i), "The bit at index " + i + " should not be set but was");
            }
        }
    }

    @Test
    public void testSpecificShiftLect() {
        final int[] bits = new int[]{0, 1, 63, 64, 127};
        Bitmap b = new Bitmap(128);

        Arrays.stream(bits).forEach(b::set);

        b.shift(-1);

        final int[] expected = new int[]{0, 62, 63, 126};
        Arrays.stream(expected).forEach(
                bit -> assertTrue(b.get(bit), "The bit " + bit + " should be set"));
    }

    @Test
    public void testSpecificShiftRight() {
        final int[] bits = new int[]{0, 1, 63, 64, 127};
        Bitmap b = new Bitmap(128);

        Arrays.stream(bits).forEach(b::set);

        b.shift(1);

        final int[] expected = new int[]{1, 2, 64, 65};
        Arrays.stream(expected).forEach(
                bit -> assertTrue(b.get(bit),"The bit " + bit + " should be set"));
    }


    @Test
    public void testRandomShift() {
        final Random r = new Random(87);
        final int RUNS = 1000;

        for (int run = 0 ; run < RUNS ; run++) {
            final Bitmap b = new Bitmap(r.nextInt(4*Long.BYTES*8-1)+1);
            final int[] bits = new int[r.nextInt(b.size()+1)];
            for (int i = 0 ; i < bits.length ; i++) {
                bits[i] = r.nextInt(b.size());
            }

            Arrays.stream(bits).forEach(b::set);

            final int shift = (r.nextBoolean() ? -1 : 1) * r.nextInt(b.size());
            b.shift(shift);

            Arrays.stream(bits).map(bit -> bit+shift).filter(bit -> bit >= 0 && bit < b.size()).forEach(
                    bit -> assertTrue(b.get(bit), "The bit " + bit + " should be set"));
        }
    }

    public void testDump() {
        Bitmap b = new Bitmap(128);
        b.set(0);
        b.set(3);
        b.set(63);
        b.set(62);
        b.set(64);
        b.set(127);
        b.set(92);
        dump(b);
    }

    private void dump(Bitmap b) {
        for (long l: b.getBacking()) {
            String baseBin = String.format(Locale.ENGLISH, "%64s", Long.toBinaryString(l));
            System.out.println("0b" + baseBin.replace(' ', '0'));
        }
    }

}