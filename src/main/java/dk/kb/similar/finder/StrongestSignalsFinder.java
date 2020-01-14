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

import dk.kb.util.BoundedPriorityQueue;
import dk.kb.similar.MultiDimPoints;
import dk.kb.similar.Nearest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extract the most significant signals (largest numbers in the vector) from each point and uses that
 * for lookups for candidates.
 */
// TODO: Significant it not necessarily the highest number. Could also be the one
public class StrongestSignalsFinder extends NearestFinder {
    // Bit of a dark magic to get a nice signalCount/matchFudge combo with nice speed.
    // Setting count=1 and fudge=0 guarantees perfect results, but is slow (speed 100%)
    // Setting count=10 and fudge=2 has very slight mismatch, but is a bit faster (speed 200%)
    // Setting count=30 and fudge=2 has slight mismatch, but is faster (speed 500%)
    // Setting count=100 and fudge=1 has slightly higher mismatch, but is way faster (speed 1500%)

    static final int DEFAULT_SIGNAL_COUNT = 30; // < 10 = slow, > 30 = imprecise
    static final int DEFAULT_MATCH_FUDGE = 2; // > 0 = higher precision at the cost of speed

    final int signalCount;
    final int matchFudge;
    final int[][] signals; // dim, points

    public StrongestSignalsFinder(MultiDimPoints multiDimPoints) {
        this(multiDimPoints, DEFAULT_SIGNAL_COUNT, DEFAULT_MATCH_FUDGE);
    }
    public StrongestSignalsFinder(MultiDimPoints multiDimPoints, int signalCount, int matchFudge) {
        super(multiDimPoints);
        this.signalCount = signalCount;
        this.matchFudge = matchFudge;

        signals = new int[multiDimPoints.dimensions][];
        List<List<Integer>> signalsList = new ArrayList<>(); // dim, point
        for (int signal = 0 ; signal < multiDimPoints.dimensions ; signal++) {
            signalsList.add(new ArrayList<>());
        }
        for (int point = 0 ; point < multiDimPoints.points ; point++) {
            for (int dim: getTopDimensions(point)) {
                signalsList.get(dim).add(point);
            }
        }
        for (int dim = 0 ; dim < multiDimPoints.dimensions ; dim++) {
            List<Integer> points = signalsList.get(dim);
            signals[dim] = new int[points.size()];
            for (int i = 0 ; i < points.size() ; i++) {
                signals[dim][i] = points.get(i);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int[] getTopDimensions(int point) {
        BoundedPriorityQueue<SignalPair> pq = new BoundedPriorityQueue<>(signalCount);
        for (int dim = 0 ; dim < multiDimPoints.dimensions ; dim++) {
            pq.add(new SignalPair(dim, multiDimPoints.get(dim, point)));
        }

        int[] topDim = new int[signalCount];
        for (int i = 0 ; i < signalCount ; i++) {
            topDim[i] = pq.poll().dimension;
        }
        Arrays.sort(topDim);
        return topDim;
    }

    @Override
    public Nearest findNearest(int basePoint) {
        int[] topDims = getTopDimensions(basePoint);
        int[] pointCloud = new int[multiDimPoints.points];
        for (int topDim: topDims) {
            for (int point : signals[topDim]) {
                pointCloud[point]++;
            }
        }

        int bestPoint = -1;
        int bestCount = -1;
        double bestDist = Double.MAX_VALUE;
        long cloudPoints = 0;
        for (int point = 0 ; point < pointCloud.length ; point++) {
            if (point == basePoint || pointCloud[point] < bestCount - matchFudge) {
                continue;
            }
            cloudPoints++;
            double dist = atMostDistanceSquared(bestDist, basePoint, point);
            if (dist > bestDist) {
                continue;
            }
            bestDist = dist;
            bestPoint = point;
            bestCount = pointCloud[point];
        }
        return new Nearest(basePoint, bestPoint, bestDist, "cloudPoints=" + cloudPoints);
    }

    static class SignalPair implements Comparable<SignalPair> {
        final int dimension;
        final double strength;

        public SignalPair(int dimension, double strength) {
            this.dimension = dimension;
            this.strength = strength;
        }

        @Override
        public int compareTo(SignalPair o) {
            return Double.compare(strength, o.strength);
        }
    }
}
