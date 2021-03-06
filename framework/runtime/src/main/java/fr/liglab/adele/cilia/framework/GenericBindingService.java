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

package fr.liglab.adele.cilia.framework;

import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.model.Binding;

import java.util.Dictionary;
import java.util.Properties;


public class GenericBindingService extends AbstractBindingService implements
        CiliaBindingService {

    public Dictionary getProperties(Dictionary collectorProperties,
                                    Dictionary senderProperties, Binding b) throws CiliaException {

        Dictionary properties = new Properties();

        if (collectorProperties != null) {
            properties.put(CILIA_COLLECTOR_PROPERTIES, collectorProperties);
        }
        if (senderProperties != null) {
            properties.put(CILIA_SENDER_PROPERTIES, senderProperties);
        }
        return properties;
    }

}
