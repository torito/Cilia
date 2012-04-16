package fr.liglab.adele.cilia.components.schedulers.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;

import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.compendium.expressioncommands.CiliaExpression;
import fr.liglab.adele.cilia.compendium.expressioncommands.ExpressionFactory;
import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.framework.CiliaScheduler;

public abstract class AbstractCorrelationSchedulerImpl extends CiliaScheduler {

	/**
	 * Correlation expression to says which incoming messages belongs together.
	 */
	protected String correlation;

	/**
	 * Correlation expression by default.
	 */
	protected static final String CORRELATION_DEFAULT = "";

	/**
	 * List of dataSets.
	 */
	//protected Map/* <String, DataSet> */mapListData = new HashMap/*
	//															 * <String,
	//															 * List<Data>>
	//															 */();

	
	
	protected CiliaExpression expreParser;

	public AbstractCorrelationSchedulerImpl(BundleContext bcontext) {
		ExpressionFactory ef = new ExpressionFactory(bcontext);
		try {
			expreParser = ef.getExpressionParser("ldap");
		} catch (CiliaException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void setCorrelation(String corre) {
		if (corre == null) {
			corre = CORRELATION_DEFAULT;
		}
		correlation = corre;
	}

	public void notifyData(Data data) {
		String msg = "Data received ";
		if (logger.isTraceEnabled()) {
			msg = msg + data.toString();
		}
		logger.debug(msg);

		String corre = getCorrelationKey(data);
		List dataset = null;
		boolean complet = false;
		if (getData() == null) {
			logger.warn("data received map is null");
		}
		synchronized (getData()) {
			dataset = (List) getData().get(corre);
			if (logger.isDebugEnabled())
				logger.debug("data received in synchronized block" + corre);
			if (dataset == null) {
				dataset = new ArrayList();
				getData().put(corre, dataset);
				logger.debug("data received in synchronized block dataset is null");
			}

			dataset.add(data);

			complet = checkCompletness(dataset);

			if (complet) {
				getData().remove(corre);
			}

		}

		// If the dataset has been completed, it is no more present on the
		// DataSets structure and so, the process action is thread safe.
		//
		if (complet) {
			process(dataset);
		}

	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public String getCorrelationKey(Data data) {
		String corre = "";
		corre = expreParser.resolveVariables(correlation, data);
		return corre;
	}

	/**
	 * 
	 * @param dataset
	 * @return
	 */
	public abstract boolean checkCompletness(List dataset);

}