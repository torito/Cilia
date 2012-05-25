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

package fr.liglab.adele.cilia.runtime.application;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.cilia.Adapter;
import fr.liglab.adele.cilia.ApplicationSpecification;
import fr.liglab.adele.cilia.Chain;
import fr.liglab.adele.cilia.ChainCallback;
import fr.liglab.adele.cilia.CiliaContext;
import fr.liglab.adele.cilia.Mediator;
import fr.liglab.adele.cilia.MediatorComponent;
import fr.liglab.adele.cilia.Node;
import fr.liglab.adele.cilia.NodeCallback;
import fr.liglab.adele.cilia.event.CiliaEvent;
import fr.liglab.adele.cilia.event.CiliaFrameworkEvent;
import fr.liglab.adele.cilia.event.CiliaFrameworkListener;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalStateException;
import fr.liglab.adele.cilia.exceptions.CiliaInvalidSyntaxException;
import fr.liglab.adele.cilia.model.PatternType;
import fr.liglab.adele.cilia.runtime.AbstractTopology;
import fr.liglab.adele.cilia.runtime.ConstRuntime;
import fr.liglab.adele.cilia.runtime.WorkQueue;
import fr.liglab.adele.cilia.util.UnModifiableDictionary;

/**
 * Application implementation
 * 
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ApplicationSpecificationImpl extends AbstractTopology implements
		ApplicationSpecification, CiliaEvent, CiliaFrameworkEvent {

	private final Logger logger = LoggerFactory.getLogger(ConstRuntime.LOG_NAME);

	private CiliaContext ciliaContext;
	private CiliaFrameworkListener listenerFramework;
	private ApplicationListenerSupport listenerSupport;
	/* Reference injected by ipojo */
	private WorkQueue workQueue;

	public ApplicationSpecificationImpl(BundleContext bc) {
		listenerSupport = new ApplicationListenerSupport(bc);
	}

	public void start() {
		logger.info("ModelS@RunTime 'Application specification' - started");
		super.setContext(ciliaContext);

		listenerSupport.start(workQueue);
		listenerFramework.register(this, ALL_EVENTS);
	}

	public void stop() {
		logger.info("ModelS@RunTime 'Application specificartion' - stopped");
		listenerSupport.stop();
		listenerFramework.unregister(this);
	}

	/**
	 * Type = pattern matching
	 * 
	 * @throws CiliaIllegalParameterException
	 */
	protected Node[] getEndpoints(String ldapFilter, PatternType type)
			throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {

		Adapter adapter;
		Set adapterResult = new HashSet();
		Filter filter = ConstRuntime.createFilter(ldapFilter);

		Dictionary dico = new Hashtable();
		String chainId[] = getChains();
		try {
			ciliaContext.getMutex().readLock().acquire();
			try {
				for (int i = 0; i < chainId.length; i++) {
					/* retreive all adapters per all chain */
					dico.put(ConstRuntime.CHAIN_ID, chainId[i]);
					/* Iterate over all adapters per chain */
					Iterator it = ciliaContext.getChain(chainId[i]).getAdapters()
							.iterator();
					while (it.hasNext()) {
						adapter = (Adapter) it.next();
						dico.put(ConstRuntime.NODE_ID, adapter.getId());
						if (filter.match(dico)) {
							/* verify the pattern */
							PatternType pattern = adapter.getPattern();
							if ((pattern.equals(type)
									|| (pattern.equals(PatternType.UNASSIGNED)) || (pattern
									.equals(PatternType.IN_OUT)))) {
								adapterResult.add(adapter);
							}
						}
					}
				}
				return (Node[]) adapterResult.toArray(new Node[adapterResult.size()]);
			} finally {
				ciliaContext.getMutex().readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException();
		}
	}

	/* callback event fired by the cilia framework */
	public void event(String chainId, String mediatorId, int evtNumber) {
		if ((evtNumber & EVENT_CHAIN_ADDED) == EVENT_CHAIN_ADDED) {
			listenerSupport.fireEventChain(0, chainId);
		}

		if ((evtNumber & EVENT_CHAIN_REMOVED) == EVENT_CHAIN_REMOVED) {
			listenerSupport.fireEventChain(1, chainId);
		}

		if ((evtNumber & EVENT_CHAIN_STARTED) == EVENT_CHAIN_STARTED) {
			listenerSupport.fireEventChain(2, chainId);
		}

		if ((evtNumber & EVENT_CHAIN_STOPPED) == EVENT_CHAIN_STOPPED) {
			listenerSupport.fireEventChain(3, chainId);
		}

		if ((evtNumber & EVENT_MEDIATOR_ADDED) == EVENT_MEDIATOR_ADDED) {
			try {
				listenerSupport.fireEventNode(ApplicationListenerSupport.EVT_ARRIVAL,
						getModel(chainId, mediatorId));
			} catch (CiliaIllegalStateException e) {
			}
		}

		if ((evtNumber & EVENT_MEDIATOR_REMOVED) == EVENT_MEDIATOR_REMOVED) {
			try {
				listenerSupport.fireEventNode(ApplicationListenerSupport.EVT_DEPARTURE,
						getModel(chainId, mediatorId));
			} catch (CiliaIllegalStateException e) {
			}
		}

		if ((evtNumber & EVENT_ADAPTER_ADDED) == EVENT_ADAPTER_ADDED) {
			try {
				listenerSupport.fireEventNode(ApplicationListenerSupport.EVT_ARRIVAL,
						getModel(chainId, mediatorId));
			} catch (CiliaIllegalStateException e) {
			}
		}

		if ((evtNumber & EVENT_ADAPTER_REMOVED) == EVENT_ADAPTER_REMOVED) {
			try {
				listenerSupport.fireEventNode(ApplicationListenerSupport.EVT_DEPARTURE,
						getModel(chainId, mediatorId));
			} catch (CiliaIllegalStateException e) {
			}
		}

		if ((evtNumber & EVENT_MEDIATOR_PROPERTIES_UPDATED) == EVENT_MEDIATOR_PROPERTIES_UPDATED) {
			try {
				listenerSupport.fireEventNode(ApplicationListenerSupport.EVT_MODIFIED,
						getModel(chainId, mediatorId));
			} catch (CiliaIllegalStateException e) {
			}
		}
	}

	/*
	 * retrieve properties ( Read only access ) (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.specification.Application#properties
	 * (fr.liglab.adele.cilia.knowledge.core.Node)
	 */
	public Dictionary properties(Node node) throws CiliaIllegalStateException,
			CiliaIllegalParameterException {
		MediatorComponent mc = getModel(node);
		return new UnModifiableDictionary(mc.getProperties());
	}

	public Node[] findNodeByFilter(String ldapFilter) throws CiliaInvalidSyntaxException,
			CiliaIllegalParameterException {

		Filter filter = ConstRuntime.createFilter(ldapFilter);
		MediatorComponent component;
		Dictionary dico = new Hashtable();
		Set componentSet = new HashSet();

		String chainId[] = getChains();
		try {
			ciliaContext.getMutex().readLock().acquire();
			try {
				for (int i = 0; i < chainId.length; i++) {

					/* retreive all adapters per chain */
					dico.put(ConstRuntime.CHAIN_ID, chainId[i]);
					/* Iterate over all adapters */
					Iterator it = ciliaContext.getChain(chainId[i]).getAdapters()
							.iterator();
					while (it.hasNext()) {
						component = (MediatorComponent) it.next();
						dico.put(ConstRuntime.NODE_ID, component.getId());
						if (filter.match(dico)) {
							componentSet.add(component);
						}
					}
					/* Iterate over all mediators */
					it = ciliaContext.getChain(chainId[i]).getMediators().iterator();
					while (it.hasNext()) {
						component = (MediatorComponent) it.next();

						dico.put(ConstRuntime.NODE_ID, component.getId());
						if (filter.match(dico)) {
							componentSet.add(component);
						}
					}
				}
				return (Node[]) componentSet.toArray(new Node[componentSet.size()]);
			} finally {
				ciliaContext.getMutex().readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException();
		}
	}

	public Node[] connectedTo(Node node) throws CiliaIllegalStateException {
		Chain chain;
		Mediator mediator;
		Adapter adapter;
		Node[] nodes;

		if (node == null)
			return new Node[0];
		try {
			ciliaContext.getMutex().readLock().acquire();
			try {
				/* retreive the chain hosting the mediator/component */
				chain = ciliaContext.getChain(node.chainId());
				if (chain != null) {
					/* checks if the node is an adapter */
					adapter = chain.getAdapter(node.nodeId());
					if (adapter != null) {
						nodes = getNextNodes(adapter.getOutBindings(), node);
					} else {
						/* Mediators */
						mediator = chain.getMediator(node.nodeId());
						if (mediator == null) {
							nodes = new Node[0];
						} else {
							nodes = getNextNodes(mediator.getOutBindings(), node);
						}
					}
				} else
					/* chain no found */
					nodes = new Node[0];
				return nodes;
			} finally {
				ciliaContext.getMutex().readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	public void addListener(String ldapFilter, NodeCallback listener)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {
		listenerSupport.addListener(ldapFilter, listener);
	}

	public void removeListener(NodeCallback listener)
			throws CiliaIllegalParameterException {
		listenerSupport.removeListener(listener);
	}

	public void addListener(String ldapFilter, ChainCallback listener)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {
		listenerSupport.addListener(ldapFilter, listener);
	}

	public void removeListener(ChainCallback listener)
			throws CiliaIllegalParameterException {
		listenerSupport.removeListener(listener);
	}

}