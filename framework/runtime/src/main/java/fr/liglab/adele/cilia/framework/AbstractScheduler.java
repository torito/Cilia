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
package fr.liglab.adele.cilia.framework;

import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.util.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project Team</a>
 */
public abstract class AbstractScheduler implements IScheduler {

    IScheduler scheduler;


    protected static Logger appLogger = LoggerFactory.getLogger(Const.LOGGER_APPLICATION);

    public void setConnectedScheduler(IScheduler sched) {
        scheduler = sched;
    }

    public abstract void notifyData(Data data);

    public void process(List dataSet) {
        if (scheduler == null) {
            appLogger.error("Unable to process data, Scheduler reference is not valid.");
            return;
        }
        scheduler.process(dataSet);
    }

    public List getSourcesIds() {
        return scheduler.getSourcesIds();
    }

    public void fireEvent(Map map) {
        appLogger.debug("fireEvent " + map);
        if (scheduler != null) {
            scheduler.fireEvent(map);
        }
    }

    public Map getData() {
        return scheduler.getData();
    }

    public void init() {
    }

}
