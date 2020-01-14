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

import dk.kb.similar.Length;
import dk.kb.similar.MultiDimPoints;
import dk.kb.similar.Nearest;

/**
 * Pre-calculates the length of all vectors (sum of squared values). When searching for nearest neighbours,
 * the points with lengths nearest to the length of the base point are considered first.
 */
public class LengthNearestFinder extends NearestFinder {
    private final int MAX_EXTRA_CHECKS = multiDimPoints.points / 10; // 10% overall
    private final Length[] lengths;

    public LengthNearestFinder(MultiDimPoints multiDimPoints) {
        super(multiDimPoints);
        lengths = multiDimPoints.getLengths(); // Calculate up front
    }

    @Override
    public Nearest findNearest(int basePoint) {
        double basePointLength = -1;
        int backIndex = -1;
        int forwardIndex = multiDimPoints.points;
        for (int i = 0; i < lengths.length; i++) {
            if (basePoint == lengths[i].getPointIndex()) {
                backIndex = i - 1;
                forwardIndex = i + 1;
                basePointLength = lengths[i].getLength();
                break;
            }
        }

        int bestPointIndex = -1;
        double shortestDistanceSqr = Double.MAX_VALUE;

        int nonMatchesSinceLastReset = 0;
        int checks = 0;
        while ((backIndex >= 0 || forwardIndex < lengths.length) && nonMatchesSinceLastReset < MAX_EXTRA_CHECKS) {
            checks++;
            if (backIndex >= 0) {
                nonMatchesSinceLastReset++;
                Length current = lengths[backIndex];
//                    double maxDist = current.length + basePointLength;
                double minDist = current.length - basePointLength;
                double minDistAbs = Math.abs(minDist);
                if (minDistAbs < shortestDistanceSqr) {
                    double exactDistanceSquared = exactDistanceSquared(current.pointIndex, basePoint);
//                        System.out.println(String.format("Backward: min=%.2f, exact=%.2f, max=%.2f, index=%d",
//                                                         minDistAbs, exactDistanceSquared, maxDist, backIndex));
//                        System.out.println("Back: checking for shorter min " + minDistSqr + ", exact " + exactDistanceSquared + " and shortestExact " + shortestDistanceSqr + " with index " + current.pointIndex);
                    if (exactDistanceSquared < shortestDistanceSqr) {
//                            System.out.println("Back: New shortest " + exactDistanceSquared + " from " + shortestDistanceSqr + " with index " + current.pointIndex);
                        shortestDistanceSqr = exactDistanceSquared;
                        bestPointIndex = current.pointIndex;
                        nonMatchesSinceLastReset = 0;
                    }
                    backIndex--;
                } else {
                    backIndex = -1;
                }
            }
            if (forwardIndex < lengths.length) {
                nonMatchesSinceLastReset++;
                Length current = lengths[forwardIndex];
                double minDist = current.length - basePointLength;
//                    double maxDist = current.length + basePointLength;
                double minDistAbs = Math.abs(minDist);
                if (minDistAbs < shortestDistanceSqr) {
                    double exactDistanceSquared = exactDistanceSquared(current.pointIndex, basePoint);
//                        System.out.println(String.format("Forward:  min=%.2f, exact=%.2f, max=%.2f, index=%d",
//                                                         minDistAbs, exactDistanceSquared, maxDist, forwardIndex));
                    if (exactDistanceSquared < shortestDistanceSqr) {
                        shortestDistanceSqr = exactDistanceSquared;
                        bestPointIndex = current.pointIndex;
                        nonMatchesSinceLastReset = 0;
                    }
                    forwardIndex++;
                } else {
                    forwardIndex = lengths.length;
                }
            }
        }
//            System.out.println("checks=" + checks);
        return new Nearest(basePoint, bestPointIndex, shortestDistanceSqr, "checked=" + checks);
    }

    @Override
    public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    protected double getDistance(double shortest, int basePoint, int point) {
        return atMostDistanceSquared(shortest, basePoint, point);
    }

}
