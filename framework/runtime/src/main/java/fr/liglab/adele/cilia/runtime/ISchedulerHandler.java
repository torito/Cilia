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

package fr.liglab.adele.cilia.runtime;

import fr.liglab.adele.cilia.framework.IScheduler;
import fr.liglab.adele.cilia.runtime.impl.SchedulerInstanceManager;

public interface ISchedulerHandler extends IScheduler {

    public void lock();

    public void unlock();

    public void setSchedulerManager(SchedulerInstanceManager sm);

    //public void addCollector(String collectorType, String collectorId, Dictionary dictionary);

}
