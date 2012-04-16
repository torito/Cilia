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

package fr.liglab.adele.cilia.knowledge.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import fr.liglab.adele.cilia.knowledge.Constants;
import fr.liglab.adele.cilia.knowledge.Node;

/**
 * Privates constants
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 *
 */
public final class Knowledge {
	/**
	 * by default the cache service is running
	 */
	public static final boolean CACHED_AUTORUN = true ;
	/*
	 * logger name
	 */
	public static final String LOG_NAME = "cilia.knowledge";
	/*
	 * Framework period in ms
	 */
	public static final int FRAMEWORK_PERIOD = 1000;
	/*
	 * #items stored per state variables
	 */
	public static final int DEFAULT_QUEUE_SIZE = 10;
	/*
	 * 
	 */
	public static final String DEFAULT_CONDITION = null;

	/**
	 * concat 2 arrays
	 * 
	 * @param first
	 * @param second
	 * @return first+second
	 */
	public static final Node[] concat(Node[] first, Node[] second) {
		Node[] result = (Node[]) Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static final Set ldapKeys;
	static {
		Set set = new HashSet();
		set.add(Constants.UUID);
		set.add(Constants.CHAIN_ID);
		set.add(Constants.NODE_ID);
		ldapKeys = Collections.unmodifiableSet(set);
	}

	public synchronized static final Filter createFilter(String filter)
			throws InvalidSyntaxException {
		if (filter == null)
			throw new InvalidSyntaxException("filter is null !", null);
		boolean found = false;
		/* at least one keyword is required */
		Iterator it = ldapKeys.iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (filter.contains(key)) {
				found = true;
				break;
			}
		}
		if (found == false)
			throw new InvalidSyntaxException("missing ldap filter keyword "
					+ ldapKeys.toString() + "!", filter);
		return FrameworkUtil.createFilter(filter);
	}
}