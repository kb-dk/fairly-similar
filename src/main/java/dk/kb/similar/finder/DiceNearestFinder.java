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
package dk.kb.similar.finder;

import dk.kb.util.Bitmap;
import dk.kb.util.BoundedPriorityQueue;
import dk.kb.similar.MultiDimPoints;
import dk.kb.similar.Nearest;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Inspired by https://sujitpal.blogspot.com/2019/10/efficient-reverse-image-search-on.html
 *
 * Divides each dimension randomly and counts the number of times the dimensional values for a given point
 * are below the divide. If it is more than half, the point is marked as satisfying the divide.
 * Calculates X divides and keeps track of the points belonging to each divide.
 * When matching, the divides for the base image are iterated and the images with the highest number of
 * matching divides are taken as candidates for proper distance comparison.
 *
 * Unsolved problem: Too many all- _and_ too many none-matches.
 */
public class DiceNearestFinder extends NearestFinder {
    final Divide[] divides;
    final Bitmap[] pointDivides;

    public static final int CANDIDATES = 500;
    public static final int DEFAULT_DIVIDE_COUNT = 10;

    public DiceNearestFinder(MultiDimPoints multiDimPoints) {
        this(multiDimPoints, DEFAULT_DIVIDE_COUNT);
    }
    public DiceNearestFinder(MultiDimPoints multiDimPoints, int divideCount) {
        super(multiDimPoints);

        // Find all min & max
        double[] minDimVals = new double[multiDimPoints.dimensions];
        double[] maxDimVals = new double[multiDimPoints.dimensions];
        Arrays.fill(minDimVals, Double.MAX_VALUE);
        Arrays.fill(maxDimVals, Double.MIN_VALUE);
        for (int point = 0; point < multiDimPoints.points ; point++) {
            for (int dim = 0 ; dim < multiDimPoints.dimensions ; dim++) {
                double value = multiDimPoints.get(dim, point);
                if (value < minDimVals[dim]) {
                    minDimVals[dim] = value;
                }
                if (value > maxDimVals[dim]) {
                    maxDimVals[dim] = value;
                }
            }
        }

        // Generate dividers
        divides = new Divide[divideCount];
        for (int i = 0; i < divides.length ; i++) {
            divides[i] = new Divide(i, multiDimPoints, minDimVals, maxDimVals);
        }

        // Calculate divide members
        pointDivides = new Bitmap[multiDimPoints.points];
        int passesNone = 0;
        int firstPassNone = -1;
        int passesAll = 0;
        int firstPassAll = -1;
        for (int point = 0; point < multiDimPoints.points ; point++) {
            Bitmap aboveDivides = new Bitmap(divides.length);
            pointDivides[point] = aboveDivides;
            int matches = 0;
            for (int divideI = 0 ; divideI < divides.length ; divideI++) {
                if (divides[divideI].isAboveDivide(point)) {
                    aboveDivides.set(divideI);
                    matches++;
                }
            }
            if (matches == divides.length) {
                passesAll++;
                if (firstPassAll ==-1) {
                    firstPassAll = point;
                }
            }
            if (matches == 0) {
                passesNone++;
                if (firstPassNone ==-1) {
                    firstPassNone = point;
                }
            }
            // TODO: Seems like some candidates passes all possible divides?
            //System.out.println("point: " + point + " Bitmap[0]: " + Long.toBinaryString(aboveDivides.getBacking()[0]));
        }
        if (passesAll > 0) {
            System.out.println("Dice-preprocess: " + passesAll + " points passes all divides. First to pass all was point " + firstPassAll);
        }
        if (passesNone > 0) {
            System.out.println("Dice-preprocess: " + passesNone + " points passes no divides. First to pass none was point " + firstPassNone);
        }
        System.out.println();
    }

    @Override
    public Nearest findNearest(int basePoint) {
        final Bitmap baseDivides = pointDivides[basePoint];
        PriorityQueue<DivideCandidate> topCandidates = new BoundedPriorityQueue<>(CANDIDATES);
        for (int point = 0; point < multiDimPoints.points ; point++) {
            if (point == basePoint) {
                continue;
            }
            topCandidates.add(new DivideCandidate(point, (int) baseDivides.countIntersectingBits(pointDivides[point])));
        }

        double bestDist = Double.MAX_VALUE;
        int bestPoint = -1;
        while (!topCandidates.isEmpty()) {
            DivideCandidate candidate = topCandidates.poll();
            double dist = this.atMostDistanceSquared(bestDist, basePoint, candidate.point);
            if (dist < bestDist) {
                bestDist = dist;
                bestPoint = candidate.point;
            }
        }
        return new Nearest(basePoint, bestPoint, bestDist);
    }

    public static class DivideCandidate implements Comparable<DivideCandidate> {
        final int point;
        final int divideMatches;

        public DivideCandidate(int point, int divideMatches) {
            this.point = point;
            this.divideMatches = divideMatches;
        }

        @Override
        public int compareTo(DivideCandidate other) {
            return Integer.compare(divideMatches, other.divideMatches);
        }
    }

    private static class Divide {
        final int seed;
        final MultiDimPoints mdp;
        final double[] minDimVals;
        final double[] maxDimVals;
        final int aboveLimit;

        public Divide(int seed, MultiDimPoints mdp, double[] minDimVals, double[] maxDimVals) {
            this.seed = seed;
            this.mdp = mdp;
            this.minDimVals = minDimVals;
            this.maxDimVals = maxDimVals;

            int minAbove = Integer.MAX_VALUE;
            int maxAbove = Integer.MIN_VALUE;
            for (int point = 0 ; point < mdp.points ; point++) {
                int above = countAbove(point);
                if (above < minAbove) {
                    minAbove = above;
                }
                if (above > maxAbove) {
                    maxAbove = above;
                }
            }
            aboveLimit = (int) ((minAbove + maxAbove) * 0.5);
        }

        public boolean isAboveDivide(int pointIndex) {
            int above = countAbove(pointIndex);
            //System.out.println("point " + pointIndex + ", seed " + seed + ", above " + above + ", aboveLimit " + aboveLimit);
            return above >= aboveLimit;
        }

        private int countAbove(int pointIndex) {
            Random random = new Random(seed);
            int above = 0;
            for (int dim = 0 ; dim < mdp.dimensions ; dim++) {
                final double divide = random.nextDouble() * (maxDimVals[dim]-minDimVals[dim]) + minDimVals[dim];
                if (mdp.get(dim, pointIndex) > divide) {
                    above++;
                }
            }
            return above;
        }
    }
}
