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

import java.util.Locale;

/**
 * Representation of the nearest point to a given origo.
 */
public class Nearest {
    public final int basePoint;
    public final int point;
    public final double distance;
    public final String note;

    public Nearest(int basePoint, int point, double distance) {
        this(basePoint, point, distance, null);
    }

    public Nearest(int basePoint, int point, double distance, String note) {
        this.basePoint = basePoint;
        this.point = point;
        this.distance = distance;
        this.note = note;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%d->%d dist=%.2f",
                             basePoint, point, distance) + (note == null ? "" : " (" + note + ")");
    }
}
