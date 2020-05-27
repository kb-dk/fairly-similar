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
package dk.kb.fs;

import java.util.List;

/**
 * Aspects are the base structures making up a Document.
 */
public interface AspectReader<A extends AspectReader.Aspect> {

    /**
     * @return the ID/designation/type of the Aspect.
     */
    String getAspectID();

    /**
     * @param docID ID for a document, relative to the Segment it is in.
     * @param reuse Aspect for reuse. If null is provided, it is the responsibility of the AspectReader to create one.
     * @return the Aspect for the given docID.
     */
    A get(long docID, A reuse);

    /**
     * @return
     */
    long size();

    interface Aspect {
    }
}
