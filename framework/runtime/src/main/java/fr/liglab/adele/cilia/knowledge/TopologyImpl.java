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

import fr.liglab.adele.cilia.Node;
import fr.liglab.adele.cilia.Topology;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalStateException;
import fr.liglab.adele.cilia.exceptions.CiliaInvalidSyntaxException;
import fr.liglab.adele.cilia.model.*;
import fr.liglab.adele.cilia.model.impl.PatternType;
import fr.liglab.adele.cilia.runtime.ConstRuntime;
import fr.liglab.adele.cilia.util.Const;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * topological access
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TopologyImpl implements Topology {
    private final Logger logger = LoggerFactory.getLogger(Const.LOGGER_RUNTIME);

    protected CiliaContainer ciliaContainer;

    public TopologyImpl(CiliaContainer cc) {
        ciliaContainer = cc;
    }

    /**
     * Retreives all nodes matching the filter
     *
     * @param ldapFilter , keywords = uuid, chain, node
     * @param proxy      if true return a proxy weak reference ( recommanded )
     * @return array of node matching the filter, array size 0 if no node
     * matching the filter
     * @throws CiliaInvalidSyntaxException , ldap syntax error
     */
    public Node[] findNodeByFilter(String ldapFilter, boolean proxy)
            throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
        Filter filter = ConstRuntime.createFilter(ldapFilter);
        MediatorComponent component;
        Dictionary dico = new Hashtable();
        Set componentSet = new HashSet();

        String chainId[] = getChainId();
        try {


            for (int i = 0; i < chainId.length; i++) {

					/* retreive all adapters per chain */
                dico.put(ConstRuntime.CHAIN_ID, chainId[i]);
                    /* Iterate over all adapters */
                Iterator it = ciliaContainer.getChain(chainId[i]).getAdapters()
                        .iterator();
                while (it.hasNext()) {
                    component = (MediatorComponent) it.next();
                    dico.put(ConstRuntime.UUID, component.uuid());
                    dico.put(ConstRuntime.NODE_ID, component.getId());
                    if (filter.match(dico)) {
                        if (proxy)
                            componentSet.add(MediatorModelProxy.getInstance()
                                    .makeMediatorModel(component));
                        else
                            componentSet.add(component);
                    }
                }
                    /* Iterate over all mediators */
                it = ciliaContainer.getChain(chainId[i]).getMediators().iterator();
                while (it.hasNext()) {
                    component = (MediatorComponent) it.next();
                    dico.put(ConstRuntime.UUID, component.uuid());
                    dico.put(ConstRuntime.NODE_ID, component.getId());
                    if (filter.match(dico)) {
                        if (proxy)
                            componentSet.add(MediatorModelProxy.getInstance()
                                    .makeMediatorModel(component));
                        else
                            componentSet.add(component);
                    }
                }
            }
            return (Node[]) componentSet.toArray(new Node[componentSet.size()]);
        } catch (Exception e) {
            logger.error("Interruped thread ", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        }
    }

    /**
     * Retreives all nodes matching the filter
     *
     * @param ldapFilter , keywords = uuid, chain, node
     * @return array of node matching the filter, array size =0 if no node
     * matching the filter
     * @throws CiliaInvalidSyntaxException , ldap syntax error
     */
    public Node[] findNodeByFilter(String ldapFilter) throws CiliaInvalidSyntaxException,
            CiliaIllegalParameterException {

        return findNodeByFilter(ldapFilter, true);
    }

    /**
     * Type = pattern matching
     *
     * @throws CiliaIllegalParameterException
     */
    private Node[] getEndpoints(String ldapFilter, PatternType type, boolean proxy)
            throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {

        Adapter adapter;
        Set adapterResult = new HashSet();
        Filter filter = ConstRuntime.createFilter(ldapFilter);

        Dictionary dico = new Hashtable();
        String chainId[] = getChainId();
        try {

            for (int i = 0; i < chainId.length; i++) {
                    /* retreive all adapters per all chain */
                dico.put(ConstRuntime.CHAIN_ID, chainId[i]);
					/* Iterate over all adapters per chain */
                Iterator it = ciliaContainer.getChain(chainId[i]).getAdapters()
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
                            if (proxy)
                                adapterResult.add(MediatorModelProxy.getInstance()
                                        .makeMediatorModel(adapter));
                            else
                                adapterResult.add(adapter);
                        }
                    }
                }
            }
            return (Node[]) adapterResult.toArray(new Node[adapterResult.size()]);

        } catch (Exception e) {
            logger.error("Interruped thread ", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
     * build the ldap filter for retreiving a specific nodes
     */
    private static String makefilter(String chain, String node) {
        StringBuffer sb = new StringBuffer("(&(");
        sb.append(ConstRuntime.CHAIN_ID).append("=").append(chain);
        sb.append(")(").append(ConstRuntime.NODE_ID).append("=").append(node);
        sb.append("))");
        return sb.toString();
    }

    /**
     * return array of node matching the filter
     */
    public Node[] connectedTo(String ldapFilter) throws CiliaInvalidSyntaxException,
            CiliaIllegalParameterException {
        return connectedTo(ldapFilter, true);
    }

    /**
     * return array of node matching the filter
     */
    public Node[] connectedTo(String ldapFilter, boolean proxy)
            throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
        Node[] nodes = new Node[0];
        Node[] source = findNodeByFilter(ldapFilter, false);
        try {
            for (int i = 0; i < source.length; i++) {
                nodes = ConstRuntime.concat(nodes, connectedTo(source[i], proxy));
            }
        } catch (CiliaIllegalStateException e) {
        }
        return nodes;
    }

    public Node[] connectedTo(Node node) throws CiliaIllegalStateException,
            CiliaIllegalParameterException {
        return connectedTo(node, true);
    }

    public Node[] connectedTo(Node node, boolean proxy)
            throws CiliaIllegalStateException, CiliaIllegalParameterException {
        Chain chain;
        Mediator mediator;
        Adapter adapter;
        Node[] nodes;

        if (node == null)
            throw new CiliaIllegalParameterException("parameter node is null");
		/* check is the node has disappear */
        getModel(node);
        try {
            chain = ciliaContainer.getChain(node.chainId());
            if (chain != null) {
					/* checks if the node is an adapter */
                adapter = chain.getAdapter(node.nodeId());
                if (adapter != null) {
                    nodes = getNextNodes(adapter.getOutBindings(), node, proxy);
                } else {
						/* Mediators */
                    mediator = chain.getMediator(node.nodeId());
                    if (mediator == null) {
                        nodes = new Node[0];
                    } else {
                        nodes = getNextNodes(mediator.getOutBindings(), node, proxy);
                    }
                }
            } else
					/* chain no found */
                nodes = new Node[0];
            return nodes;
        } catch (NullPointerException e) {
            throw new CiliaIllegalStateException("Node has disappeared");
        }
    }

    protected Node[] getNextNodes(Binding[] bindings, Node node, boolean proxy)
            throws CiliaIllegalStateException {
        if (bindings == null)
            return new Node[0];

        Set nodeSet = new HashSet();
        Set set = new HashSet();
		/* Retreive the mediators name in the cilia context (model) */
        for (int i = 0; i < bindings.length; i++) {
            set.add(bindings[i].getTargetMediator().getId());
        }
        Iterator it = set.iterator();
		/* Retreives real mediators connected */
        while (it.hasNext()) {
            String name = (String) it.next();
			/* construct the ldap filter */
            String filter = makefilter(node.chainId(), name);
            Node item[];
            try {
                item = findNodeByFilter(filter);
                for (int i = 0; i < item.length; i++) {
                    if (proxy)
                        nodeSet.add(MediatorModelProxy.getInstance().makeMediatorModel(
                                item[i]));
                    else
                        nodeSet.add(item[i]);
                }
            } catch (CiliaInvalidSyntaxException e) {
            } catch (CiliaIllegalParameterException e) {
            }
        }
        return (Node[]) nodeSet.toArray(new Node[nodeSet.size()]);
    }

    /*
     * Return the object MediatorComponent
     */
    private MediatorComponent getModel(String chainId, String component)
            throws CiliaIllegalStateException {
        Chain chain;
        MediatorComponent mc;
        try {
            chain = ciliaContainer.getChain(chainId);
            if (chain == null) /* chain not found */
                throw new CiliaIllegalStateException(chainId + " not existing !");
            mc = chain.getAdapter(component);
				/* checks Adapter or mediator */
            if (mc == null)
                mc = chain.getMediator(component);
            if (mc == null)
                throw new CiliaIllegalStateException(chainId + "/" + component
                        + " not existing !");
            return mc;

        } catch (Exception e) {
            logger.error("Interruped thread ", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage());
        }

    }

    /*
     * Return a proxy weak reference to the Object MediatorComponent openess
     * access ! (non-Javadoc)
     *
     * @see
     * fr.liglab.adele.cilia.knowledge.core.specification.Application#getModel
     * (fr.liglab.adele.cilia.knowledge.core.Node)
     */
    public MediatorComponent getModel(Node node) throws CiliaIllegalParameterException,
            CiliaIllegalStateException {
        if (node == null)
            throw new CiliaIllegalParameterException("node is null !");
        try {
            MediatorComponent mc = getModel(node.chainId(), node.nodeId());
            if (!mc.uuid().equals(node.uuid()))
                throw new CiliaIllegalStateException("node " + node.toString()
                        + " has disappeared");
            return mc;
        } catch (Throwable e) {
            throw new CiliaIllegalStateException("node " + node.toString()
                    + " has disappeared");
        }
    }

    /*
     * retreives all adapter in ( entries of mediation chain ) (non-Javadoc)
     *
     * @see
     * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#endpointIn
     * (java.lang.String)
     */
    public Node[] endpointIn(String ldapFilter) throws CiliaInvalidSyntaxException,
            CiliaIllegalParameterException {
        return getEndpoints(ldapFilter, PatternType.IN_ONLY, true);
    }

    /*
     * retreives all adapter in ( entries of mediation chain ) (non-Javadoc)
     *
     * @see
     * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#endpointIn
     * (java.lang.String)
     */
    public Node[] endpointIn(String ldapFilter, boolean proxy)
            throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
        return getEndpoints(ldapFilter, PatternType.IN_ONLY, proxy);
    }

    /*
     * retreives all adapter out ( exit of mediation chain ) (non-Javadoc)
     *
     * @see
     * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#endpointOut
     * (java.lang.String)
     */
    public Node[] endpointOut(String ldapFilter) throws CiliaInvalidSyntaxException,
            CiliaIllegalParameterException {
        return getEndpoints(ldapFilter, PatternType.OUT_ONLY, true);
    }

    /*
     * retreives all adapter out ( exit of mediation chain ) (non-Javadoc)
     *
     * @see
     * fr.liglab.adele.cilia.knowledge.core.execution.DynamicProperties#endpointOut
     * (java.lang.String)
     */
    public Node[] endpointOut(String ldapFilter, boolean proxy)
            throws CiliaInvalidSyntaxException, CiliaIllegalParameterException {
        return getEndpoints(ldapFilter, PatternType.OUT_ONLY, proxy);
    }

    /**
     * @return array of chain identification
     */
    public String[] getChainId() {
        Set chainSet;
        chainSet = ciliaContainer.getAllChains();

        Set setName = new HashSet();
        if (chainSet == null) {
            chainSet = Collections.EMPTY_SET;
        }
        Iterator it = chainSet.iterator();
        while (it.hasNext()) {
            Chain c = (Chain) it.next();
            setName.add(c.getId());
        }
        return (String[]) setName.toArray(new String[setName.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.liglab.adele.cilia.ApplicationSpecification#get(java.lang.String)
     */
    public Chain getChain(String chainId) throws CiliaIllegalParameterException {
        if (chainId == null)
            throw new CiliaIllegalParameterException("Chain id is null !");
        return ciliaContainer.getChain(chainId);
    }

    /**
     * @param type
     * @return Array of nodes matching the type
     * @throws CiliaIllegalParameterException
     */
    public Node[] nodeByType(String type) throws CiliaIllegalParameterException {
        Node[] node;
        Set set = new HashSet();
        if ((type == null) || (type.isEmpty()))
            throw new CiliaIllegalParameterException("Parameter is null");
        try {

            node = findNodeByFilter("(chain=*)");
			/* Iterate over all */
            for (int i = 0; i < node.length; i++) {
                String nodeType;
                try {
                    nodeType = getModel(node[i]).getType();
                    if (nodeType.compareToIgnoreCase(type) == 0) {
                        set.add(node[i]);
                    }
                } catch (CiliaIllegalStateException e) {
                }
            }
        } catch (CiliaInvalidSyntaxException e) {
            throw new CiliaIllegalParameterException(e.getMessage());
        }

        return (Node[]) set.toArray(new Node[set.size()]);
    }
}
