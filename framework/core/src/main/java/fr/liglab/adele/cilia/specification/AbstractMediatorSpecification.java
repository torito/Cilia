package fr.liglab.adele.cilia.specification;


import fr.liglab.adele.cilia.model.impl.ComponentImpl;
import fr.liglab.adele.cilia.model.impl.MediatorImpl;
import fr.liglab.adele.cilia.model.impl.PortImpl;
import fr.liglab.adele.cilia.model.impl.PortType;
import fr.liglab.adele.cilia.util.Const;

import java.util.HashMap;

public abstract class AbstractMediatorSpecification implements MediatorSpecification {

    private MediatorImpl mediatorSpec;

    protected HashMap inports = new HashMap();

    protected HashMap outports = new HashMap();

    private ComponentImpl dispatcherDef;

    private ComponentImpl processorDef;

    private ComponentImpl schedulerDef;


    private static final String DEFAULT_CAT = "generic";


    public AbstractMediatorSpecification(String name, String namespace, String category) {
        mediatorSpec = new MediatorImpl(name, name, namespace, category, null, null, null);
    }

    /**
     * Set the mediator category.
     *
     * @param category the given category.
     */
    public MediatorSpecification setCategory(String category) {
        mediatorSpec.setCategory(category);
        return this;
    }

    /**
     * Get the mediator category.
     *
     * @return the mediator category.
     */
    public String getCategory() {
        if (mediatorSpec.getCategory() == null) {
            return DEFAULT_CAT;
        }
        return mediatorSpec.getCategory();
    }

    /**
     * Set the mediator namespace.
     *
     * @param namespace the mediator namespace.
     */
    public MediatorSpecification setNamespace(String namespace) {
        mediatorSpec.setNamespace(namespace);
        return this;
    }

    /**
     * Get the mediator namespace.
     *
     * @return the mediator namespace.
     */
    public String getNamespace() {
        return mediatorSpec.getNamespace();
    }


    /**
     * Retrieve the mediator specification name.
     *
     * @return
     */
    public String getName() {
        return mediatorSpec.getType();
    }

    /**
     * Assign the scheduler info to the mediator.
     *
     * @param schedulerName
     * @param schedulerNamespace
     */
    public MediatorSpecification setScheduler(String schedulerName, String schedulerNamespace) {
        schedulerDef = new ComponentImpl(schedulerName, schedulerName, schedulerNamespace, null);
        return this;
    }

    /**
     * Retrieve the scheduler assigned name.
     *
     * @return the scheduler name.
     */
    public String getSchedulerName() {
        if (schedulerDef == null) {
            return null;
        }
        return schedulerDef.getType();
    }

    /**
     * Retrieve the chosen scheduler namespace.
     *
     * @return the scheduler namespace. NULL when there is not scheduler assigned.
     */
    public String getSchedulerNamespace() {
        if (schedulerDef == null) {
            return null;
        }
        if (schedulerDef.getNamespace() == null) {
            return Const.CILIA_NAMESPACE;
        }
        return schedulerDef.getNamespace();
    }


    /**
     * Set the chosen processor info.
     *
     * @param processorName      the processor name.
     * @param processorNamespace the processor namesâce.
     */
    public MediatorSpecification setProcessor(String processorName, String processorNamespace) {
        processorDef = new ComponentImpl(processorName, processorName, processorNamespace, null);
        return this;
    }

    /**
     * Retrieve the chosen processor name.
     *
     * @return the processor name.
     */
    public String getProcessorName() {
        if (processorDef == null) {
            return null;
        }
        return processorDef.getType();
    }

    /**
     * Retrieve the processor namespace.
     *
     * @return the processor namespace.
     */
    public String getProcessorNamespace() {
        if (processorDef == null) {
            return null;
        }
        if (processorDef.getNamespace() == null) {
            return Const.CILIA_NAMESPACE;
        }
        return processorDef.getNamespace();
    }


    /**
     * Assign the chosen dispatcher name.
     *
     * @param dispatcherName      the chosen dispatcher name.
     * @param dispatcherNamespace the chosen dispatcher namespace.
     */
    public MediatorSpecification setDispatcher(String dispatcherName, String dispatcherNamespace) {
        dispatcherDef = new ComponentImpl(dispatcherName, dispatcherName, dispatcherNamespace, null);
        return this;
    }

    /**
     * Retrieve the chosen dispatcher name.
     *
     * @return the dispatcher name.
     */
    public String getDispatcherName() {
        if (dispatcherDef == null) {
            return null;
        }
        return dispatcherDef.getType();
    }

    /**
     * Retrieve the dispatcher namespace.
     *
     * @return the dispatcher namespace.
     */
    public String getDispatcherNamespace() {
        if (dispatcherDef == null) {
            return null;
        }
        if (dispatcherDef.getNamespace() == null) {
            return Const.CILIA_NAMESPACE;
        }
        return dispatcherDef.getNamespace();
    }


    public void setInPort(String name, String type) {
        inports.put(name, new PortImpl(name, type, PortType.INPUT, null));
    }

    public void setOutPort(String name, String type) {
        outports.put(name, new PortImpl(name, type, PortType.OUTPUT, null));
    }

    /**
     * Initialize the mediator specification type.
     */
    public abstract MediatorSpecification initializeSpecification();

    /**
     * Stop the mediator specification type.
     */
    public abstract MediatorSpecification stopSpecification();


}
