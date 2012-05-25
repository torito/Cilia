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

package fr.liglab.adele.cilia.runtime.dynamic;

import java.util.Dictionary;

import fr.liglab.adele.cilia.MediatorComponent;
import fr.liglab.adele.cilia.Node;
import fr.liglab.adele.cilia.framework.monitor.statevariable.ComponentStateVarService;

/**
 * Objects stored in the registry
 * 
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 * 
 */
@SuppressWarnings("rawtypes")
public interface RegistryItem extends Node {

	/**
	 * 
	 * @param key
	 *            String key
	 * @param value
	 *            Value to set , must not be null
	 */
	void setProperty(String key, Object value);

	/**
	 * 
	 * @param key
	 *            String property to retreive
	 * @return value
	 */
	Object getProperty(String key);

	/**
	 * 
	 * @return all properties
	 */
	Dictionary getProperties();

	/**
	 * 
	 * @return object reference to monitor mediator handler or monitor adapter
	 *         handler
	 */
	ComponentStateVarService runtimeReference();

	void setRuntimeReference(ComponentStateVarService o);

	/**
	 * 
	 * @return reference to objects storing data received at runtime
	 */
	DynamicNode dataRuntimeReference();

	void setDataRuntimeReference(DynamicNode o);

	/**
	 * @return Specification model
	 */
	MediatorComponent specificationReference();

	void setSpecificationReference(MediatorComponent mc);

}