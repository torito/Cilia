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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.cilia.dynamic.Measure;
import fr.liglab.adele.cilia.dynamic.RawData;
import fr.liglab.adele.cilia.dynamic.SetUp;
import fr.liglab.adele.cilia.dynamic.Thresholds;
import fr.liglab.adele.cilia.dynamic.ThresholdsCallback;
import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.exceptions.CiliaInvalidSyntaxException;
import fr.liglab.adele.cilia.framework.monitor.statevariable.ComponentStateVarService;
import fr.liglab.adele.cilia.runtime.ConstRuntime;
import fr.liglab.adele.cilia.util.FrameworkUtils;
import fr.liglab.adele.cilia.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import fr.liglab.adele.cilia.util.concurrent.SyncList;
import fr.liglab.adele.cilia.util.concurrent.SyncMap;

/**
 * Node = [mediator,adapter] at execution time
 * 
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DynamicNode implements SetUp, RawData, Thresholds {

	private final Logger logger = LoggerFactory.getLogger(ConstRuntime.LOG_NAME);
	private ComponentStateVarService mediatorHandler;

	private static final int NB_THRESHOLD = 4;

	private final NodeListenerSupport nodeListeners;
	/* unique identifier */
	private final String uuid;
	private final String chainId;
	private final String nodeId;

	/* list of variables managed by this component */
	private SyncMap variablesId = new SyncMap(new HashMap(),
			new ReentrantWriterPreferenceReadWriteLock());

	public DynamicNode(final String uuid, final RuntimeRegistry registry,
			final NodeListenerSupport nodeListeners) {
		RegistryItem item = registry.findByUuid(uuid);
		this.chainId = item.chainId();
		this.nodeId = item.nodeId();
		this.uuid = uuid;
		mediatorHandler = item.runtimeReference();
		this.nodeListeners = nodeListeners;
	}

	public String uuid() {
		return uuid;
	}

	public String chainId() {
		return chainId;
	}

	public String nodeId() {
		return nodeId;
	}

	public void setLow(String variableId, double low, double verylow)
			throws CiliaIllegalParameterException {
		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id must not be null !");
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		measures.setLow(low);
		measures.setVeryLow(verylow);
		/* notify on modification all listeners */
		nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
	}

	public void setHigh(String variableId, double high, double veryhigh)
			throws CiliaIllegalParameterException {
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		measures.setHigh(high);
		measures.setVeryHigh(veryhigh);
		/* notify on modification all listeners */
		nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
	}

	public double getLow(String variableId) throws CiliaIllegalParameterException {
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		return measures.getLow();
	}

	public double getVeryLow(String variableId) throws CiliaIllegalParameterException {
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		return measures.getVeryLow();
	}

	public double getHigh(String variableId) throws CiliaIllegalParameterException {
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		return measures.getHigh();
	}

	public double getVeryHigh(String variableId) throws CiliaIllegalParameterException {
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures == null)
			throw new CiliaIllegalParameterException(
					"Monitoring has not been set for this variable " + variableId);
		return measures.getVeryHigh();
	}

	public Measure[] measures(String variableId) throws CiliaIllegalParameterException {
		Measure[] m;
		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id must not be null !");
		/* retreive the component */
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures != null) {
			m = measures.getMeasure();
		} else
			m = new Measure[0];
		return m;
	}

	public void addMeasure(String variableId, Measure obj) {
		int evt;
		/* retreive the component */
		Observations measures = (Observations) variablesId.get(variableId);
		if (measures != null) {
			/* insert a new measure */
			evt = measures.addMeasure(obj);
			if (evt == 0) {
				nodeListeners.fireMeasureReceived(this, variableId, obj);
			} else {
				nodeListeners.fireThresholdEvent(this, variableId, obj, evt);
			}
			logger.info("Received variable [{},{}]", variableId,obj.toString());
		}
	}

	/**
	 * Observation store measures from runtime
	 * 
	 */
	private class Observations {

		public int queueSize;
		public SyncList measures;
		public double[] threshold; /* list of threshold */

		public Observations(int size) {
			queueSize = size;
			measures = new SyncList(new ArrayList(size),
					new ReentrantWriterPreferenceReadWriteLock());
			threshold = new double[NB_THRESHOLD];
			for (int i = 0; i < NB_THRESHOLD; i++) {
				threshold[i] = Double.NaN;
			}
		}

		/* circular fifo management */
		public int addMeasure(Measure m) {
			try {
				measures.writerSync().acquire();
				try {
					measures.add(0, m);
					if (measures.size() > queueSize)
						measures.remove(queueSize - 1);
					return viability(m);
				} finally {
					measures.writerSync().release();
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(ex.getMessage());
			}
		}

		public Measure[] getMeasure() {
			try {
				measures.readerSync().acquire();
				try {
					ArrayList m = new ArrayList(measures);
					return (Measure[]) m.toArray(new Measure[m.size()]);
				} finally {
					measures.readerSync().release();
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(ex.getMessage());
			}
		}

		public void setQueueSize(int queue) {
			if (queue < 1)
				return;
			try {
				measures.writerSync().acquire();
				try {
					for (int i = queueSize; i < queue; i++)
						measures.remove(i - 1);
					queueSize = queue;
				} finally {
					measures.writerSync().release();
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(ex.getMessage());
			}
		}

		public void setVeryLow(double d) {
			threshold[0] = d;
		}

		public void setLow(double d) {
			threshold[1] = d;
		}

		public void setHigh(double d) {
			threshold[2] = d;
		}

		public void setVeryHigh(double d) {
			threshold[3] = d;
		}

		public double getLow() {
			return threshold[1];
		}

		public double getVeryLow() {
			return threshold[0];
		}

		public double getHigh() {
			return threshold[2];
		}

		public double getVeryHigh() {
			return threshold[3];
		}

		public int viability(Measure m) {
			if ((m != null) && (m.value() instanceof Long)) {
				Long l = (Long) m.value();
				if ((threshold[0] != Double.NaN) && (l.longValue() < threshold[0]))
					return ThresholdsCallback.VERY_LOW;
				if ((threshold[1] != Double.NaN) && (l.longValue() < threshold[1]))
					return ThresholdsCallback.LOW;
				if ((threshold[3] != Double.NaN) && (l.longValue() > threshold[3]))
					return ThresholdsCallback.VERY_HIGH;
				if ((threshold[2] != Double.NaN) && (l.longValue() > threshold[2]))
					return ThresholdsCallback.VERY_HIGH;
			}
			return 0;
		}
	}

	public String[] getCategories() {
		return mediatorHandler.getCategories();
	}

	public String[] variablesByCategory(String category) {
		return mediatorHandler.getStateVarIdCategory(category);
	}

	public String[] enabledVariable() {
		return mediatorHandler.getEnabledId();
	}

	public void setMonitoring(String variableId, int queueSize, String ldapFilter,
			boolean enable) throws CiliaIllegalParameterException,
			CiliaInvalidSyntaxException {

		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id is null !");

		if (queueSize < 1)
			throw new CiliaIllegalParameterException(
					"queue size must be a positive integer");
		try {
			variablesId.writerSync().acquire();
			try {
				if (!variablesId.containsKey(variableId)) {
					variablesId.put(variableId, new Observations(queueSize));
				}
				mediatorHandler.setCondition(variableId, ldapFilter);
				if (enable)
					mediatorHandler.enableStateVar(variableId);
				else
					mediatorHandler.disableStateVar(variableId);
				/* notify on modification all listeners */
				nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
			} finally {
				variablesId.writerSync().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}

	}

	public void setMonitoring(String variableId, int queueSize)
			throws CiliaIllegalParameterException {
		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id is null !");

		if (queueSize < 1)
			throw new CiliaIllegalParameterException(
					"Queue size must be a positive integer value=" + queueSize);
		try {
			variablesId.writerSync().acquire();
			try {
				if (!variablesId.containsKey(variableId)) {
					try {
						/* a variable with defaults configuration is created */
						variablesId.put(variableId, new Observations(queueSize));
						mediatorHandler.disableStateVar(variableId);
						mediatorHandler.setCondition(variableId,
								ConstRuntime.DEFAULT_CONDITION);
					} catch (CiliaInvalidSyntaxException e) {
						/* never happens! */
					}
				} else {
					Observations observation = (Observations) variablesId.get(variableId);
					observation.setQueueSize(queueSize);
				}
				/* notify on modification all listeners */
				nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
			} finally {
				variablesId.writerSync().release();
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}

	}

	public void setMonitoring(String variableId, String ldapFilter)
			throws CiliaIllegalParameterException, CiliaInvalidSyntaxException {

		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id is null !");
		try {
			variablesId.writerSync().acquire();
			try {
				if (!variablesId.containsKey(variableId)) {
					/* a variable with defaults configuration is created */
					variablesId.put(variableId, new Observations(
							ConstRuntime.DEFAULT_QUEUE_SIZE));
					mediatorHandler.disableStateVar(variableId);
				}
				mediatorHandler.setCondition(variableId, ldapFilter);
				/* notify on modification all listeners */
				nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
			} finally {
				variablesId.writerSync().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	/*
	 * Enable / Disable the variable if variable is not existing , ldapcondition
	 * is null, Queue size = 10 (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.execution.SetUp#setMonitoring(java
	 * .lang.String, boolean)
	 */
	public void setMonitoring(String variableId, boolean enable)
			throws CiliaIllegalParameterException {
		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id is null !");
		try {
			variablesId.writerSync().acquire();
			try {
				if (!variablesId.containsKey(variableId)) {
					/* a variable with defaults configuration is created */
					variablesId.put(variableId, new Observations(
							ConstRuntime.DEFAULT_QUEUE_SIZE));
					try {
						mediatorHandler.setCondition(variableId,
								ConstRuntime.DEFAULT_CONDITION);
					} catch (CiliaInvalidSyntaxException e) {
						/* never happens */
					}
				}
				if (enable)
					mediatorHandler.enableStateVar(variableId);
				else
					mediatorHandler.disableStateVar(variableId);

				nodeListeners.fireNodeEvent(NodeListenerSupport.EVT_MODIFIED, this);
			} finally {
				variablesId.writerSync().release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage());
		}
	}

	/*
	 * Return the number the capability of measures stored for this variable
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.execution.SetUp#queueSize(java.lang
	 * .String)
	 */
	public int queueSize(String variableId) throws CiliaIllegalParameterException {
		if (variableId == null)
			throw new CiliaIllegalParameterException("Variable id is null !");

		Observations observations = (Observations) variablesId.get(variableId);
		if (observations == null)
			throw new CiliaIllegalParameterException(toString()
					+ " missing configuration !");
		return observations.queueSize;
	}

	/*
	 * return the condition to publish data (non-Javadoc)
	 * 
	 * @see
	 * fr.liglab.adele.cilia.knowledge.core.execution.SetUp#flowControl(java
	 * .lang.String)
	 */
	public String flowControl(String variableId) throws CiliaIllegalParameterException {
		if (variableId == null)
			throw new CiliaIllegalParameterException("variable id must not be null !");

		if (!variablesId.containsKey(variableId))
			throw new CiliaIllegalParameterException(variableId
					+ " missing configuration !");

		return mediatorHandler.getCondition(variableId);
	}

	public String getQualifiedId() {
		return FrameworkUtils.makeQualifiedId(chainId(), nodeId(), uuid());
	}
	
	public String toString() {
		StringBuffer sb= new StringBuffer(getQualifiedId()).append("{");
		Iterator it = variablesId.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			sb.append(pairs.getKey()).append(",");		
		}
		sb.append("}");
		return sb.toString() ;
	}
}