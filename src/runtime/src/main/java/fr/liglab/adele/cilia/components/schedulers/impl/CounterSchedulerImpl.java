package fr.liglab.adele.cilia.components.schedulers.impl;

import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;

import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.compendium.data.manipulation.DataCount;

public class CounterSchedulerImpl extends AbstractCorrelationSchedulerImpl{
	/**
	 * Number of message needed to start processing.
	 */
	private Map m_counter;

	/**
	 * Condition to start processing
	 */
	private String m_condition;

	private final static String NAME = "counter-scheduler";

	private DataCount datacount = null;

	public CounterSchedulerImpl(BundleContext bcontext) {
		super(bcontext);
		datacount = new DataCount(bcontext);
	}

	public boolean checkCompletness(List dataset) {
		boolean completness = false;

		int count = 0;

		Dictionary variables = new Properties();
		count = dataset.size();
		
		if (m_counter==null) logger.error("m_counter==null" );
		else if (m_counter.keySet() ==null) logger.error("m_counter.keySet==null") ;
		
		Iterator keys = (m_counter.keySet()).iterator();

		while (keys.hasNext()) {
			String key = (String) keys.next();
			int localcount = 0;
			try {
				localcount = datacount.count(dataset, m_counter.get(key)+"");
			} catch (CiliaException e) {
				e.printStackTrace();
			}
			variables.put(key, localcount + "");
		}
		Data dataVariables = new Data("","variables",variables);
		completness = expreParser.evaluateBooleanExpression(m_condition, dataVariables);


		logger.debug(" count = " + count);

		return completness;
	}

	public void init() {
	}
}
