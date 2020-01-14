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

import dk.kb.similar.MultiDimPoints;

import java.io.*;
import java.nio.file.Paths;

/**
 * converts a file with floating point numbers represented as text to float-bits.
 */
public class ConvertTextToFloat {

    public static void main(String[] args) throws IOException {
        convert("/home/te/projects/ponder-this/pixplot_vectors_270707.txt.gz", " ", Integer.MAX_VALUE,
                "/home/te/projects/ponder-this/pixplot_vectors_270707.bin");
    }

    private static void convert(String in, String splitRegexp, int maxLines, String out) throws IOException {
        System.out.println("Converting " + in + " to " + out);
        int totalLines = 0;
        try (BufferedReader input = MultiDimPoints.getReader(Paths.get(in));
             OutputStream os = new FileOutputStream(out);
             OutputStream bos = new BufferedOutputStream(os);
             DataOutputStream dos = new DataOutputStream(bos)
        ) {
            String line;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                totalLines++;
                for (String floatStr : line.split(splitRegexp)) {
                    dos.writeFloat(Float.parseFloat(floatStr));
                }
                if (totalLines == maxLines) {
                    break;
                }
                if (totalLines % 1000 == 0) {
                    System.out.println(totalLines);
                }
            }
        }
        System.out.println("Finished converting " + totalLines + " lines");
    }
}
