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

import dk.kb.similar.MultiDimPoints;
import dk.kb.similar.Nearest;

/**
 * Nearly-dumb finder that calculates Euclid distance from base to all points, but skips calculations early for a given
 * candidate point if it exceeds a previously calculated shortest distance.
 */
public class EarlyNearestFinder extends NearestFinder {
    private int dimChecks = 0;

    public EarlyNearestFinder(MultiDimPoints multiDimPoints) {
        super(multiDimPoints);
    }

    @Override
    public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
        dimChecks = 0;
        Nearest nearest = super.findNearest(basePoint, startPoint, endPoint);
        return new Nearest(nearest.basePoint, nearest.point, nearest.distance,
                           "avg dimChecks=" + dimChecks / (endPoint - startPoint));
    }

    @Override
    protected double getDistance(double shortest, int basePoint, int point) {
        final int STEP = 100;
        double distance = 0;
        for (int dimMajor = 0; dimMajor < multiDimPoints.getDimensions(); dimMajor += STEP) {
            final int dimMax = Math.min(dimMajor + STEP, multiDimPoints.getDimensions());
            for (int dim = dimMajor; dim < dimMax; dim++) {
                final double diff = multiDimPoints.get(dim, basePoint) - multiDimPoints.get(dim, point);
                distance += (diff * diff);
            }
            if (distance > shortest) {
                //System.out.print("[" + dimMajor + "]");
                dimChecks += dimMajor;
                return distance;
            }
        }
        dimChecks += multiDimPoints.getDimensions();
        return distance;
    }
}
