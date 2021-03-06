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
package fr.liglab.adele.cilia.runtime.impl;

import fr.liglab.adele.cilia.internals.factories.MediatorComponentManager;
import fr.liglab.adele.cilia.model.Component;
import fr.liglab.adele.cilia.runtime.CiliaInstanceWrapper;
import org.apache.felix.ipojo.ComponentInstance;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Observable;

/**
 * Handle the scheduler relation between collectors and their life cycle.
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project Team</a>
 */
public abstract class ConstituentInstanceManager extends CiliaInstanceManagerSet {

    CiliaInstanceWrapper constituant;

    Component constituantInfo;


    protected MediatorComponentManager mediatorInstance;


    BundleContext bcontext;

    public ConstituentInstanceManager(BundleContext context, Component schedulerInfo, MediatorComponentManager mmanager) {
        bcontext = context;
        constituantInfo = schedulerInfo;
        mediatorInstance = mmanager;
    }

    public void start() {
        constituant = new CiliaInstanceWrapper(bcontext, constituantInfo.getId(), createFilter(), constituantInfo.getProperties(), this);
        constituant.start();
        this.startInstances();
    }

    public void stop() {
        constituant.stop();
        this.removeAllInstances();
    }

    protected abstract String createFilter();

    protected abstract String createConstituantFilter(Component component);

    protected abstract void organizeReferences(CiliaInstanceWrapper instance);

    public void reconfigureConstituant(Dictionary dico) {
        constituant.updateInstanceProperties(dico);
    }

    protected CiliaInstanceWrapper addComponent(String port, Component component) {
        return addComponent(port, component, true);
    }

    protected CiliaInstanceWrapper addComponent(String port, Component component, boolean start) {
        CiliaInstanceWrapper elementInstance = new CiliaInstanceWrapper(bcontext, component.getId(), createConstituantFilter(component), component.getProperties(), this);
        synchronized (lockObject) {
            super.addInstance(port, elementInstance);
            if (start) {
                elementInstance.start();
            }
        }
        return elementInstance;
    }

    protected boolean removeComponent(String port, Component component) {
        synchronized (lockObject) {
            return super.removeInstance(port, component.getId());
        }
    }

    public synchronized void update(Observable o, Object arg) {
        CiliaInstanceWrapper instance = (CiliaInstanceWrapper) o;
        Integer state = (Integer) arg;
        //Some component instance state has change scheduler/dispatcher or collector/sender
        //fire an event.
        synchronized (lockObject) {
            if (instance.equals(constituant)) { // Its the constituant
                if (state == ComponentInstance.VALID) {
                    mediatorInstance.stateChanged(null, ComponentInstance.VALID);// try to make it valid

                } else {
                    mediatorInstance.stateChanged(null, ComponentInstance.INVALID);// try to make it invalid
                }
            } else {
                this.checkAvailability();
            }
        }
        organizeReferences(instance);
    }

    protected MediatorComponentManager getMediatorComponentManager() {
        return mediatorInstance;
    }
}
