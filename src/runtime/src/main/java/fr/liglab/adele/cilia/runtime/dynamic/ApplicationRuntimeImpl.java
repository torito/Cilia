/* Copyright Adele Team LIG
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.cilia.Adapter;
import fr.liglab.adele.cilia.Chain;
import fr.liglab.adele.cilia.CiliaContext;
import fr.liglab.adele.cilia.Mediator;
import fr.liglab.adele.cilia.Node;
import fr.liglab.adele.cilia.NodeCallback;
import fr.liglab.adele.cilia.dynamic.ApplicationRuntime;
import fr.liglab.adele.cilia.dynamic.MeasureCallback;
import fr.liglab.adele.cilia.dynamic.RawData;
import fr.liglab.adele.cilia.dynamic.SetUp;
import fr.liglab.adele.cilia.dynamic.Thresholds;
import fr.liglab.adele.cilia.dynamic.ThresholdsCallback;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalStateException;
import fr.liglab.adele.cilia.exceptions.CiliaInvalidSyntaxException;
import fr.liglab.adele.cilia.model.ChainRuntime;
import fr.liglab.adele.cilia.model.PatternType;
import fr.liglab.adele.cilia.runtime.AbstractTopology;
import fr.liglab.adele.cilia.runtime.ConstRuntime;
import fr.liglab.adele.cilia.runtime.WorkQueue;
import fr.liglab.adele.cilia.util.concurrent.ReadWriteLock;
import fr.liglab.adele.cilia.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * React on [arrival & departure] components <br>
 * hold state variables published by cilia framework (monitoring handler)
 * 
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ApplicationRuntimeImpl extends AbstractTopology implements
		ApplicationRuntime, NodeCallback {

	private final Logger logger = LoggerFactory.getLogger(ConstRuntime.LOG_NAME);

	/* injected by ipojo , registry access */
	private RuntimeRegistry registry;
	/* Cilia components discovery (adapters, mediators) */
	private NodeDiscoveryImpl discovery;
	/* Cilia application model */
	private CiliaContext ciliaContext;
	/* holds values fired by mediators/adapters */
	private MonitorHandlerListener chainRt;
	private ReadWriteLock mutex;
	private NodeListenerSupport nodeListenerSupport;
	/* Reference injected by ipojo */
	private WorkQueue workQueue;

	public ApplicationRuntimeImpl(BundleContext bc) {
		discovery = new NodeDiscoveryImpl(bc, this);
		mutex = new WriterPreferenceReadWriteLock();
		nodeListenerSupport = new NodeListenerSupport(bc);
		chainRt = new MonitorHandlerListener(bc, nodeListenerSupport);
	}

	/*
	 * Start the service
	 */
	public void start() {
		setContext(ciliaContext);
		nodeListenerSupport.start(workQueue);
		discovery.setRegistry(registry);
		chainRt.setRegistry(registry);
		/* Start listening state variables */
		chainRt.start();
		/* Start runtime discovery */
		discovery.start();
		logger.info("ModelS@RunTime 'Application Runtime' - started");
	}

	/*
	 * Stop the service
	 */
	public void stop() {
		nodeListenerSupport.stop();
		chainRt.stop();
		discovery.stop();
		logger.info("ModelS@RunTime 'Application Runtime' - stopped");
	}

	private void addNode(String uuid) {
		try {
			mutex.writeLock().acquire();
			try {
				RegistryItem item = registry.findByUuid(uuid);
				/* Store in the registry the specification reference */
				item.setSpecificationReference(getModel(item));
				chainRt.addNode(uuid);
			} catch (Exception e) {
				logger.error("Internal error, cannot retrieve mediatorComponent reference");
			} finally {
				mutex.writeLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	private void removeNode(String uuid) {
		try {
			mutex.writeLock().acquire();
			try {
				chainRt.removeNode(uuid);
				registry.unregister(uuid);
			} finally {
				mutex.writeLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	public void onArrival(Node node) {
		addNode(node.uuid());
	}

	public void onDeparture(Node node) {
		removeNode(node.uuid());
	}

	public void onModified(Node node) {
	}

	/*
	 * Return a proxy for configuring the node (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.dynproperties.DynamicProperties#
	 * nodeSetup(java.lang.String)
	 */
	public SetUp[] nodeSetup(String ldapFilter) throws CiliaInvalidSyntaxException,
			CiliaIllegalParameterException {
		Node[] item = registry.findByFilter(ldapFilter);
		Set set = new HashSet();
		try {
			mutex.readLock().acquire();
			try {
				String uuid;
				for (int i = 0; i < item.length; i++) {
					uuid = (String) item[i].uuid();
					try {
						registry.lock_uuid(uuid);
						SetUp proxy = chainRt.proxySetUp(uuid);
						if (proxy != null)
							set.add(proxy);
					} finally {
						registry.unlock_uuid(uuid);
					}
				}
				return (SetUp[]) set.toArray(new SetUp[set.size()]);
			} finally {
				mutex.readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	/*
	 * return a proxy for gathering raw data sent by the node at runtime
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.dynproperties.DynamicProperties#
	 * nodeMonitoring(java.lang.String)
	 */
	public RawData[] nodeRawData(String ldapFilter) throws CiliaInvalidSyntaxException,
			CiliaIllegalParameterException {
		Node[] item = registry.findByFilter(ldapFilter);
		Set set = new HashSet();
		try {
			mutex.readLock().acquire();
			try {
				String uuid;
				for (int i = 0; i < item.length; i++) {
					uuid = (String) item[i].uuid();
					try {
						registry.lock_uuid(uuid);
						RawData proxy = chainRt.proxyRawData(uuid);
						if (proxy != null)
							set.add(proxy);
					} finally {
						registry.unlock_uuid(uuid);
					}
				}
				return (RawData[]) set.toArray(new RawData[set.size()]);
			} finally {
				mutex.readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	public Thresholds[] nodeMonitoring(String ldapFilter)
			throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
		Node[] item = registry.findByFilter(ldapFilter);
		Set set = new HashSet();
		try {
			mutex.readLock().acquire();
			try {
				String uuid;
				for (int i = 0; i < item.length; i++) {
					uuid = (String) item[i].uuid();
					try {
						registry.lock_uuid(uuid);
						Thresholds proxy = chainRt.proxyMonitoring(uuid);
						if (proxy != null)
							set.add(proxy);
					} finally {
						registry.unlock_uuid(uuid);
					}
				}
				return (Thresholds[]) set.toArray(new Thresholds[set.size()]);
			} finally {
				mutex.readLock().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	/* retreives adapters in or out */
	protected Node[] getEndpoints(String ldapFilter, PatternType type)
			throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
		Chain chain;
		Adapter adapter;
		Set adapterSet = new HashSet();
		Node[] item = registry.findByFilter(ldapFilter);
		for (int i = 0; i < item.length; i++) {
			chain = ciliaContext.getChain(item[i].chainId());
			if (chain != null) {
				adapter = chain.getAdapter(item[i].nodeId());
				if (adapter != null) {
					PatternType pattern = adapter.getPattern();
					/* Checks the pattern if possible ! */
					if ((pattern.equals(type)) || (pattern.equals(PatternType.IN_OUT))
							|| (pattern.equals((PatternType.UNASSIGNED)))) {
						adapterSet.add(item[i]);
					}
				}
			}
		}
		return (Node[]) adapterSet.toArray(new Node[adapterSet.size()]);
	}

	/*
	 * Return the list of nodes connected at runtime (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.dynproperties.DynamicProperties#
	 * connectedTo(fr.liglab.adele.cilia.knowledge.core.Node)
	 */
	public Node[] connectedTo(Node node) throws CiliaIllegalStateException {
		Chain chain;
		Mediator mediator;
		Adapter adapter;
		Node[] nodes;

		if (node == null)
			return new Node[0];

		if (registry.findByUuid(node.uuid()) == null)
			throw new CiliaIllegalStateException("node disappears");
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
	}

	/*
	 * return a proxy to the node Setup (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#nodeSetup
	 * (fr.liglab.adele.cilia.knowledge.core.Node)
	 */
	public SetUp nodeSetup(Node node) throws CiliaIllegalStateException,
			CiliaIllegalParameterException {
		if (node == null)
			throw new CiliaIllegalParameterException("node is null !");
		/* uuid is locked for unregister during the call */
		try {
			registry.lock_uuid(node.uuid());
			return chainRt.proxySetUp(node.uuid());
		} finally {
			registry.unlock_uuid(node.uuid());
		}
	}

	/*
	 * return a proxy to the node Raw Data (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#nodeRawData
	 * (fr.liglab.adele.cilia.knowledge.core.Node)
	 */
	public RawData nodeRawData(Node node) throws CiliaIllegalStateException,
			CiliaIllegalParameterException {
		if (node == null)
			throw new CiliaIllegalParameterException("node is null !");
		/* uuid is locked for unregister during the call */
		try {
			registry.lock_uuid(node.uuid());
			return chainRt.proxyRawData(node.uuid());
		} finally {
			registry.unlock_uuid(node.uuid());
		}
	}

	/*
	 * return a proxy to tthe node monitoring ( thresholds ) (non-Javadoc)
	 * 
	 * @see fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#
	 * nodeMonitoring(fr.liglab.adele.cilia.knowledge.core.Node)
	 */
	public Thresholds nodeMonitoring(Node node) throws CiliaIllegalStateException,
			CiliaIllegalParameterException {
		if (node == null)
			throw new CiliaIllegalParameterException("node is null !");
		/* uuid is locked for unregister during the call */
		try {
			registry.lock_uuid(node.uuid());
			return chainRt.proxyMonitoring(node.uuid());
		} finally {
			registry.unlock_uuid(node.uuid());
		}
	}

	public Node[] findNodeByFilter(String ldapFilter) throws CiliaInvalidSyntaxException,
			CiliaIllegalParameterException {
		RegistryItem[] nodes = registry.findByFilter(ldapFilter);
		Node[] result = new Node[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			result[i] = nodes[i].specificationReference();
		}
		return result;
	}

	public Node findNodeByUUID(String uuid) throws CiliaIllegalParameterException {
		if (uuid == null)
			throw new CiliaIllegalParameterException("uuid is null !");
		return registry.findByUuid(uuid).specificationReference();
	}

	public int getChainState(String chainId) throws CiliaIllegalParameterException,
			CiliaIllegalStateException {
		if (chainId == null)
			throw new CiliaIllegalParameterException("chain id is null");
		ChainRuntime chain = ciliaContext.getChainRuntime(chainId);
		if (chain == null)
			throw new CiliaIllegalStateException("'" + chainId + "' not found");
		return chain.getState();
	}

	public Date lastCommand(String chainId) throws CiliaIllegalParameterException,
			CiliaIllegalStateException {
		if (chainId == null)
			throw new CiliaIllegalParameterException("chain id is null");
		ChainRuntime chain = ciliaContext.getChainRuntime(chainId);
		if (chain == null)
			throw new CiliaIllegalStateException("'" + chainId + "' not found");
		return chain.lastCommand();
	}

	public void addListener(String ldapfilter, NodeCallback listener)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {
		nodeListenerSupport.addListener(ldapfilter, listener);
	}

	public void removeListener(NodeCallback listener)
			throws CiliaIllegalParameterException {
		nodeListenerSupport.removeListener(listener);
	}

	public void addListener(String ldapfilter, ThresholdsCallback listener)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {
		nodeListenerSupport.addListener(ldapfilter, listener);
	}

	public void addListener(String ldapfilter, MeasureCallback listener)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {
		nodeListenerSupport.addListener(ldapfilter, listener);
	}

	public void removeListener(ThresholdsCallback listener)
			throws CiliaIllegalParameterException {
		nodeListenerSupport.removeListener(listener);
	}

	public void removeListener(MeasureCallback listener)
			throws CiliaIllegalParameterException {
		nodeListenerSupport.removeListener(listener);
	}

}