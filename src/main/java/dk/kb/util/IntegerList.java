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
package dk.kb.util;

import java.util.Arrays;

/**
 *
 */
public class IntegerList {

    private int[] values = new int[10];
    private int size = 0;

    public int get(int index) {
        return values[index];
    }

    public void set(int index, int value) {
        values[index] = value;
    }

    public void add(int value) {
        if (size > values.length) {
            int[] oldValues = values;
            values = new int[values.length*2];
            System.arraycopy(oldValues, 0, values, 0, oldValues.length);
        }
        values[size++] = value;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        Arrays.fill(values, 0);
    }

}
