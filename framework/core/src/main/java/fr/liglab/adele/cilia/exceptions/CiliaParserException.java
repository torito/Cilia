/*
 * Copyright Adele Team LIG
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.liglab.adele.cilia.exceptions;

/**
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project Team</a>
 */
public class CiliaParserException extends CiliaException {


    /**
     *
     */
    private static final long serialVersionUID = -298187608836425525L;

    /**
     *
     */
    public CiliaParserException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public CiliaParserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public CiliaParserException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public CiliaParserException(Throwable cause) {
        super(cause);
    }

}
