/**
 * Copyright � 2010 France Telecom R&D
 */
package fr.liglab.adele.cilia.framework.components;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;

import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.framework.CiliaDispatcher;
/**
 * DescriptorBasedDispatcher
 * This Dispatcher will analize Data in order to 
 * 
 * @author dbqw4458
 *
 */
public class DescriptorBasedDispatcherImpl extends CiliaDispatcher {

	/**
	 * List destinations.
	 * injected by iPOJO.
	 */
	protected Map routeConditions;

	/**
	 * Property used to dispatch data
	 */
	protected String property;

	public DescriptorBasedDispatcherImpl(BundleContext context){
		super(context);
	}


	public void setRouteConditions(Map conditions) {
		routeConditions = conditions;
	}

	public void setProperty(String  property){
		this.property=property;
	}
	/**
	 * Method to call when processing is finished
	 * and used to send data to destinations.
	 * @throws CiliaException 
	 */
	public void dispatch(List dataSet) throws CiliaException {

		int dataCount = 0;

		if (routeConditions == null) {
			throw new CiliaException("There is any configuration to dispatch");
		}

		if (dataSet != null ) {
			dataCount = dataSet.size();
		}
		for (int i = 0; i < dataCount ; i++) {

			Data data = (Data) dataSet.get(i);
			synchronized (routeConditions) {
				Iterator it = routeConditions.keySet().iterator();

				while (it.hasNext()) {
					String condition = (String) it.next();
					if(((String)data.getProperty(property)).equalsIgnoreCase(condition)) {
						String senderName = (String)routeConditions.get(condition);
						send(senderName, data);
					}
				}
			}

		}
	}

}
