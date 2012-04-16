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

package fr.liglab.adele.cilia.knowledge;

import org.osgi.framework.InvalidSyntaxException;

/**
 * Registry access , 
 * retreives the runtime object 
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 * 
 */
public interface Registry {
	/**
	 * Return an array of entries matching the filter <br>
	 * keywords = {uuid, chain, node} <br>
	 * example
	 * (findByFilter("&((application.id=chain1)(component.id=adapt*))");
	 * 
	 * @param ldapFilter
	 *            , LDAP filter
	 * @return entries matching the filter or an array size 0  if not item founded
	 */
	Node[] findByFilter(String ldapFilter) throws InvalidSyntaxException;

	/**
	 * Fast access
	 * 
	 * @param uuid
	 * @return object stored in the registry, or null if not found
	 */
	Node findByUuid(String uuid);
}