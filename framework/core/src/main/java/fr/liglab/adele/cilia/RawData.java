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

package fr.liglab.adele.cilia;

import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalStateException;
import fr.liglab.adele.cilia.exceptions.CiliaRuntimeException;

import java.util.Map;

/**
 * Management of dynamic data values
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */
public interface RawData extends Node {

    /**
     * validity of a mediator
     *
     * @return true if the mediator is valid
     */
    boolean isValid() throws CiliaIllegalStateException;

    /**
     * @return list of state variables enabled
     */
    String[] getAllEnabledVariable() throws CiliaIllegalStateException;

    /**
     * @param variableId
     * @return true if state enable , false disable
     * @throws CiliaIllegalStateException
     * @throws CiliaIllegalParameterException
     */
    boolean getStateVariableState(String variableId) throws CiliaIllegalStateException,
            CiliaIllegalParameterException;

    /**
     * @param variableId
     * @return list of measures stored
     * @throws CiliaRuntimeException
     */
    Measure[] measures(String variableId) throws CiliaIllegalParameterException,
            CiliaIllegalStateException;

    /**
     * checks if the measure is out of bounds
     *
     * @return ThresholdsCallback.NONE ,
     * ThresholdsCallback.VERY_LOW,ThresholdsCallback.LOW
     * ThresholdsCallback.HIGH, ThresholdsCallback.VERY_HIGH
     */
    int viability(String variableId, Measure measure)
            throws CiliaIllegalParameterException, CiliaIllegalStateException;

    Map toMap();

}
