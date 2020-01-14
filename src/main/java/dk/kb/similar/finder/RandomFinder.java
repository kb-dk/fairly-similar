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

import java.util.Random;

/**
 * Returns a random point. Used for optimal speed, extremely poor accurracy baseline.
 */
public class RandomFinder extends NearestFinder {
    public RandomFinder(MultiDimPoints multiDimPoints) {
        super(multiDimPoints);
    }

    @Override
    public Nearest findNearest(int basePoint) {
        Random random = new Random(basePoint);
        int candidate = random.nextInt(multiDimPoints.points);
        if (candidate == basePoint) {
            candidate = (candidate+1) % multiDimPoints.points;
        }
        return new Nearest(basePoint, basePoint, exactDistanceSquared(basePoint, candidate));
    }
}
