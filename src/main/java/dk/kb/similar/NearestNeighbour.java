/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.similar;

import dk.kb.similar.finder.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

/**
 * Testing performance of brute force nearest neighbour on high-dimensional vector spaces.
 *
 * Extend {@link #createFinders(MultiDimPoints)} with new candidates.
 */
public class NearestNeighbour {
    public static final int POINTS_DEFAULT = 20000;
    public static final int RUNS_DEFAULT = 10;
    public static final int DIMENSIONS_DEFAULT = 2048;
    private static final String SAMPLE_DEFAULT = "pixplot_vectors_270707.bin";

    private final int dimensions;
    private final int points;
    private DISTRIBUTION distribution;
    private final String datafile;

    enum DISTRIBUTION {random, linear, exponential, logarithmic, onlyTen, thack2, load}

    private List<NearestFinder> createFinders(MultiDimPoints multiDimPoints) {
        List<NearestFinder> finders = new ArrayList<>();
//        finders.add(new DumbNearestFinder(multiDimPoints)); // Guaranteed correct
        finders.add(new EarlyNearestFinder(multiDimPoints)); // Guaranteed correct

        finders.add(new StrongestSignalsFinder(multiDimPoints));
        finders.add(new DiceNearestFinder(multiDimPoints));
//        finders.add(new LengthNearestFinder(multiDimPoints));
        finders.add(new RandomFinder(multiDimPoints));
        return finders;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("No arguments provided. Using default values. Usage:");
            System.out.println(String.format(Locale.ENGLISH, 
                    "NearestNeighbour <points (default %d)> <runs (default %d)> " +
                    "<dimensions (default %d)> <datafile (default %s)>",
                    POINTS_DEFAULT, RUNS_DEFAULT, DIMENSIONS_DEFAULT, SAMPLE_DEFAULT));
        }

        final int points = args.length >= 1 ? Integer.parseInt(args[0]) : POINTS_DEFAULT;
        final int runs = args.length >= 2 ? Integer.parseInt(args[1]) : RUNS_DEFAULT;
        final int dimensions = args.length >= 3 ? Integer.parseInt(args[2]) : DIMENSIONS_DEFAULT;
        final String datafile = args.length >= 4 ? args[3] : SAMPLE_DEFAULT;

        NearestNeighbour nn = new NearestNeighbour(dimensions, points, DISTRIBUTION.load, datafile);
        nn.measureEarlyTermination(runs);
    }

    public NearestNeighbour(int points) {
        this(DIMENSIONS_DEFAULT, points, DISTRIBUTION.load, SAMPLE_DEFAULT);
    }
    public NearestNeighbour(int dimensions, int points, DISTRIBUTION distribution, String datafile) {
        System.out.println(String.format(Locale.ENGLISH, "Points=%d, dimensions=%d, distribution=%s, datafile=%s",
                                         points, dimensions, distribution, datafile));
        this.dimensions = dimensions;
        this.points = points;
        this.distribution = distribution;
        this.datafile = datafile;
    }

    public void measureEarlyTermination(int runs) throws IOException {
        MultiDimPoints multiDimPoints;
        if (distribution == DISTRIBUTION.load) {
            URL datafileURL = Thread.currentThread().getContextClassLoader().getResource(datafile);
            if (datafileURL == null) {
                throw new FileNotFoundException("Unable to locate '" + datafile + "' using class loader");
            }
            multiDimPoints = MultiDimPoints.load(Paths.get(datafileURL.getFile()), points, dimensions);
        } else {
            multiDimPoints = new MultiDimPoints(dimensions, points);
            multiDimPoints.fill(distribution, true);
        }
        List<NearestFinder> finders = createFinders(multiDimPoints);
        StringBuilder summary = new StringBuilder(1000);

        final int[] nearestPoints = new int[runs];
        final double[] nearestDist = new double[runs];
        Arrays.fill(nearestPoints, -1);
        for (NearestFinder finder: finders) {
            Random random = new Random(87);
            long totalNS = 0;
            double totalDist = 0.0;
            for (int run = 0; run < runs; run++) {
                long ns = -System.nanoTime();
                int basePoint = random.nextInt(multiDimPoints.getPoints());
                Nearest nearest = finder.findNearest(basePoint);
                ns += System.nanoTime();
                long pointsPerSec = (long)(multiDimPoints.points/(ns/1000000000.0));
                System.out.print(String.format(
                        Locale.ENGLISH, "%s: %s in %dms (%,d points/s)",
                        finder.getClass().getSimpleName(), nearest, (ns / 1000000), pointsPerSec));
                totalNS += ns;
                totalDist += nearest.distance;
                if (nearestPoints[run] == -1) {
                    nearestPoints[run] = nearest.point;
                    nearestDist[run] = nearest.distance;
                } else if (nearestPoints[run] != nearest.point) {
                    System.out.print(String.format(
                            Locale.ENGLISH, " (not a perfect match, which was %d->%d dist=%.2f)",
                            basePoint, nearestPoints[run], nearestDist[run]));
                }
                System.out.println();
            }
            long totalPointsPerSec = (long)(multiDimPoints.points*runs/(totalNS/1000000000.0));
            String stats = String.format(
                    Locale.ENGLISH,"%dms (%,d points/sec, totalDist=%.1f)",
                    totalNS / 1000000, totalPointsPerSec, totalDist);
            System.out.println("Total: " + stats + "\n");
            summary.append(finder.getClass().getSimpleName()).append(": ").append(stats).append("\n");
        }

        System.out.println("\nFinished processing. Final summary:\n" + summary);
    }

    private double slowManhattan(MultiDimPoints array, int point1, int point2) {
        double distance = 0;
        for (int x = 0; x < array.getPoints() ; x++) {
            distance +=  Math.abs(array.get(x, point1) - array.get(x, point2));
        }
        return distance;
    }

}
