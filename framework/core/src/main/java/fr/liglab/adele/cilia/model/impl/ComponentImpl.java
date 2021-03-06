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

package fr.liglab.adele.cilia.model.impl;

import fr.liglab.adele.cilia.exceptions.CiliaIllegalParameterException;
import fr.liglab.adele.cilia.model.Component;
import fr.liglab.adele.cilia.util.FrameworkUtils;
import fr.liglab.adele.cilia.util.concurrent.ReadWriteLock;
import fr.liglab.adele.cilia.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

import java.util.*;


/**
 * This class is the basic representation model, which contains the information
 * which will be shared by all the representation model entities.
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */
public class ComponentImpl extends Observable implements Component {

    /**
     * Model representation identificator.
     */
    private String id;
    /**
     * Model representation type.
     */
    private String type;
    /**
     * ComponentImpl namespace.
     */
    private volatile String namespace;

    protected final ReadWriteLock mutex;

    /**
     * Model representation properties.
     */
    private volatile Hashtable /* <CiliaProperty> */properties = new Hashtable();


    /**
     * Model representation basic constructor.
     *
     * @param id         model representation identificator.
     * @param type       model representation type.
     * @param cat        model representation category.
     * @param properties model representation properties.
     * @throws CiliaIllegalParameterException
     */
    public ComponentImpl(String id, String type, String nspace, Dictionary properties) {
        mutex = new ReentrantWriterPreferenceReadWriteLock();
        if (id == null || id.length() == 0) {
            id = String.valueOf(this.hashCode());
        } else FrameworkUtils.checkIdentifier(id);

        this.id = id;
        this.type = type;
        this.namespace = nspace;
        if (properties != null) {
            Enumeration e = properties.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement().toString();
                Object value = properties.get(key);
                this.properties.put(key, value);
            }
        }
    }

    /**
     * Set new properties as String separated as Strings.
     *
     * @param propertiesAsString
     */
    public void setProperties(String propertiesAsString) {

        Hashtable tmpProperties = new Hashtable();
        if (propertiesAsString != null && propertiesAsString.trim().length() > 0) {
            String[] propertiesTokens = propertiesAsString.split(",");
            if (propertiesTokens != null) {
                for (int i = 0; i < propertiesTokens.length; i++) {
                    String[] entries = propertiesTokens[i].split("=");
                    tmpProperties.put(entries[0], entries[1]);
                }
            }
        }
        setProperties(tmpProperties);
    }

    /**
     * get the model representation identificator.
     *
     * @return the model representation identificator.
     */
    public String getId() {
        return id;
    }

    /**
     * Get this model representation type.
     *
     * @return the model type.
     */
    public String getType() {
        return type;
    }

    /**
     * Get a copy of the properties.
     *
     * @return properties.
     */
    public Hashtable getProperties() {
        Hashtable rp = null;
        try {
            mutex.writeLock().acquire();
        } catch (InterruptedException e) {
        }
        rp = new Hashtable(properties);
        mutex.writeLock().release();
        return rp;
    }

    /**
     * Get the specified property.
     *
     * @param key property name.
     * @return the property asociated to the given key.
     */
    public Object getProperty(Object key) {
        Object value = null;
        try {
            mutex.readLock().acquire();
        } catch (InterruptedException e) {
        }
        value = properties.get(key);
        mutex.readLock().release();
        return value;
    }

    /**
     * Set new properties.
     *
     * @param newProps the new properties.
     */
    public void setProperties(Dictionary newProps) {
        if (newProps != null) {
            Map mapProp = null;
            if (newProps instanceof Map) {
                mapProp = ((Map) newProps);
            } else {
                mapProp = new Hashtable(newProps.size());
                Enumeration e = newProps.keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement().toString();
                    Object value = newProps.get(key);
                    mapProp.put(key, value);
                }
            }
            try {
                mutex.writeLock().acquire();
            } catch (InterruptedException e) {
            }

            this.properties.putAll(mapProp);
            mutex.writeLock().release();
        }
        setChanged();
        notifyObservers(new UpdateEvent(UpdateActions.UPDATE_PROPERTIES, this));
    }

    /**
     * Set a new property.
     *
     * @param key   property key.
     * @param value property value.
     */
    public void setProperty(Object key, Object value) {
        try {
            mutex.writeLock().acquire();
        } catch (InterruptedException e) {
        }
        properties.put(key, value);
        mutex.writeLock().release();
        setChanged();
        notifyObservers(new UpdateEvent(UpdateActions.UPDATE_PROPERTIES, this));
    }

    /**
     * This method returns the identificator of the model representation.
     *
     * @return the model representation identificator.
     */
    public String toString() {
        return getId();
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        try {
            mutex.writeLock().acquire();
        } catch (InterruptedException e) {
        }
        this.namespace = namespace;
        mutex.writeLock().release();
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }


    /**
     *
     */
    public void dispose() {
        try {
            mutex.writeLock().acquire();
        } catch (InterruptedException e) {
        }
        this.id = null;
        this.namespace = null;
        this.type = null;
        this.properties.clear();
        this.properties = null;
        mutex.writeLock().release();
    }
}
