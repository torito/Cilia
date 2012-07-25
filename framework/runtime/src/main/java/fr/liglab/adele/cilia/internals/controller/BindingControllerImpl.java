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

package fr.liglab.adele.cilia.internals.controller;

import java.util.Dictionary;

import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.framework.CiliaBindingService;
import fr.liglab.adele.cilia.model.Binding;
import fr.liglab.adele.cilia.model.Component;
import fr.liglab.adele.cilia.model.MediatorComponent;
import fr.liglab.adele.cilia.model.Port;
import fr.liglab.adele.cilia.model.impl.BindingImpl;
import fr.liglab.adele.cilia.model.impl.CollectorImpl;
import fr.liglab.adele.cilia.model.impl.SenderImpl;

/**
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 * 
 */
public class BindingControllerImpl implements TrackerCustomizer {
	/**
	 * Binding model to handle.
	 */
	private BindingImpl modelBinding;

	private MediatorControllerImpl sourceController;

	private MediatorControllerImpl targetController;

	private final Object lockObject = new Object();

	public void setSourceController(MediatorControllerImpl sourceController) {
		this.sourceController = sourceController;
	}

	public void setTargetController(MediatorControllerImpl targetController) {
		this.targetController = targetController;
	}

	/**
	 * Reference to the chain controller.
	 */
	private BundleContext bcontext;

	private static Logger log = LoggerFactory.getLogger("cilia.ipojo.runtime");
	private Tracker bindingTracker;
	/**
	 * Create a new binding controller.
	 * 
	 * @param binding
	 *            to handle.
	 */
	private static String DEFAULT_TYPE = "direct";

	public BindingControllerImpl(BundleContext context, Binding binding) {
		modelBinding = (BindingImpl)binding;
		bcontext = context;
		settingUp();
		// start();
	}

	/**
	 * Starting observing model.
	 */
	public void start() {
		// stop();
		registerTracker();
	}

	/**
	 * @throws CiliaException
	 * 
	 */
	private void createModels(ServiceReference ref) throws CiliaException {

		CiliaBindingService cbs = null;
		Component collector = null;
		Component sender = null;
		String bindingId = null;
		cbs = (CiliaBindingService) bcontext.getService(ref);

		if (log.isDebugEnabled())
			log.debug("Adding Binding:" + getBindingType());
		Component collectorModel = cbs.getCollectorModel(modelBinding.getProperties());
		Component senderModel = cbs.getSenderModel(modelBinding.getProperties());
		Dictionary colProps = null;
		Dictionary senProps = null;
		if (collectorModel != null) {
			colProps = collectorModel.getProperties();
		}
		if (senderModel != null) {
			senProps = senderModel.getProperties();
		}
		Dictionary componentProperties = cbs.getProperties(colProps, senProps,
				modelBinding);

		colProps = (Dictionary) componentProperties.get("cilia.collector.properties");
		senProps = (Dictionary) componentProperties.get("cilia.sender.properties");

		Port port1 = modelBinding.getSourcePort();
		Port port2 = modelBinding.getTargetPort();
		if (port1 != null) {
			if (log.isDebugEnabled())
				log.debug("Source Port in model binding is NOT null" + port1.getPortType()
						+ " " + port1.getName());
		} else {
			senderModel = null;
		}
		if (port2 != null) {
			if (log.isDebugEnabled())
				log.debug("Target Port in model binding is NOT null " + port2.getPortType()
						+ " " + port2.getName());
		} else {
			collectorModel = null;
		}

		bindingId = modelBinding.getSourceMediator().getId() + ":"
				+ modelBinding.getSourcePort().getName() + ":"
				+ modelBinding.getTargetMediator().getId() + ":"
				+ modelBinding.getTargetPort().getName() ;


		if (collectorModel != null) {
			colProps.put("cilia.collector.identifier", bindingId);
			colProps.put("cilia.collector.port", modelBinding.getTargetPort().getName());
			collector = new CollectorImpl(modelBinding.getTargetPort().getName(),
					collectorModel.getType(),null,  colProps);
		}
		if (senderModel != null) {
			senProps.put("cilia.sender.identifier", bindingId);
			senProps.put("cilia.sender.port", modelBinding.getSourcePort().getName());
			sender = new SenderImpl(modelBinding.getSourcePort().getName(),
					senderModel.getType(),null, senProps);
		}
		addModels(sender, collector);
	}

	/**
	 * Add sender and collector models to the mediators.
	 * 
	 * @param senderm
	 *            the sender model.
	 * @param collectorm
	 *            the collector model.
	 */
	private void addModels(Component senderm, Component collectorm) {
		MediatorComponent sm = modelBinding.getSourceMediator();
		MediatorComponent tm = modelBinding.getTargetMediator();


		if (sm != null && senderm != null) {
			synchronized (lockObject) {
				modelBinding.addSender(senderm);
				sourceController.createSender(senderm);
			}
		}

		if (tm != null && collectorm != null) {
			synchronized (lockObject) {
				modelBinding.addCollector(collectorm);
				targetController.createCollector(collectorm);
			}
		}
	}

	/**
	 * Get the binding type.
	 * 
	 * @return the binding type.
	 */
	private String getBindingType() {
		String bindingType = modelBinding.getType();
		if (bindingType == null) {
			bindingType = DEFAULT_TYPE;
		}
		return bindingType;
	}

	/**
	 * Stop observing model.
	 */
	public void stop() {
		unregisterTracker();
		if (sourceController != null) {
			Component s = modelBinding.getSender(); 
			modelBinding.addSender(null);
			sourceController.removeSender(s);
		}
		if (targetController != null) {
			Component c = modelBinding.getCollector();
			modelBinding.addCollector(null);
			targetController.removeCollector(c);
		}
	}

	/**
	 * Update binding properties.
	 * 
	 * @param binding
	 *            binding to modify.
	 */
	public void updateInstanceProperties(Binding binding) {
		Dictionary properties = binding.getProperties();
		if (sourceController != null) {
			sourceController.updateInstanceProperties(properties);
		}
		if (targetController != null) {
			targetController.updateInstanceProperties(properties);
		}
	}

	protected void registerTracker() {
		String filter = createFilter();
		if (bindingTracker == null) {
			try {
				bindingTracker = new Tracker(bcontext, bcontext.createFilter(filter),
						this);
				bindingTracker.open();
			} catch (InvalidSyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	protected void unregisterTracker() {
		if (bindingTracker != null) {
			bindingTracker.close();
		}
	}

	private String createFilter() {
		String filter = "(|(cilia.binding.protocol=" + getBindingType()
				+ ")(cilia.binding.type=" + getBindingType() + "))";
		return filter;
	}

	public void settingUp() {
		String defaultProtocol = null;
		if (bcontext != null) {
			defaultProtocol = bcontext.getProperty("cilia.default.protocol");
		}
		if (defaultProtocol != null) {
			DEFAULT_TYPE = defaultProtocol;
		}
	}

	/**
	 * Methods from TrackerCustomizer.
	 */
	public void addedService(ServiceReference reference) {
		boolean performBinding = true;
		try {
			if (validatePort(modelBinding.getSourcePort(), modelBinding.getTargetPort()))
				createModels(reference);
		} catch (CiliaException e) {
			e.printStackTrace();
		}
	}

	public boolean addingService(ServiceReference reference) {
		Object cbs = null;
		cbs = bcontext.getService(reference);
		if (cbs instanceof CiliaBindingService) {
			return true;
		}
		return false;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		if (sourceController != null) {
			sourceController.removeSender(modelBinding.getSender());
		}
		if (targetController != null) {
			targetController.removeCollector(modelBinding.getCollector());
		}
	}

	private boolean validatePort(Port exitPort, Port entryPort) {
		boolean forceBinding;
		boolean valid = true;
		try {
			String fb = bcontext.getProperty("cilia.forcebindings");
			forceBinding = Boolean.parseBoolean(fb);
		} catch (Exception ex) {
			forceBinding = false;
		}

		Port componentExitPort = sourceController.getOutPort(exitPort.getName());
		Port componentEntryPort = targetController.getInPort(entryPort.getName());
		if (componentExitPort == null) {
			log.error("Mediator {} does not have a port named {}", exitPort.getMediator().getId(), exitPort.getName() );
			valid = false;
		}
		if (valid && componentExitPort.getDataType() == null) {
			log.error("Mediator port {} in {} does not have a well defined data type",  exitPort.getName(), exitPort.getMediator().getId() );
			valid = false;
		}
		if (valid && componentEntryPort == null) {
			log.error("Mediator {} does not have a port named {}", entryPort.getMediator().getId(), entryPort.getName() );
			valid = false;
		}
		if (valid && componentEntryPort.getDataType() == null) {
			log.error("Mediator port {} in {} does not have a well defined data type",entryPort.getName(), entryPort.getMediator().getId() );
			valid = false;
		}
		if (valid && ((componentEntryPort.getDataType().compareToIgnoreCase(componentExitPort.getDataType()) !=0)
				&& (componentEntryPort.getDataType().compareTo("*") !=0 && (componentExitPort.getDataType().compareTo("*")!=0))
				)) {
			log.error("Trying to bind incompatible ports: ExitPort[" + componentExitPort.getName() + " = "+componentExitPort.getDataType()+"] & EntryPort[" + componentEntryPort.getName() + " = "+componentEntryPort.getDataType()+"]");
			valid = false;
		}
		return valid || forceBinding;
	}


}