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
package fr.liglab.adele.cilia.admin.impl;

import fr.liglab.adele.cilia.CiliaContext;
import fr.liglab.adele.cilia.builder.Architecture;
import fr.liglab.adele.cilia.builder.Builder;
import fr.liglab.adele.cilia.exceptions.BuilderException;
import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.util.ChainParser;
import fr.liglab.adele.cilia.util.CiliaFileManager;
import fr.liglab.adele.cilia.util.Const;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.*;


/**
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project Team</a>
 */
public class CiliaFileManagerImpl implements CiliaFileManager {

    /**
     * OSGi Bundle Context.
     */
    BundleContext bcontext;
    /**
     * Cilia Context Service.
     */
    CiliaContext ccontext;
    /**
     * The Cilia logger.
     */
    protected static Logger logger = LoggerFactory.getLogger(Const.LOGGER_CORE);
    /**
     *
     */
    private Map<File, List<String>> handledChains; //It map a file with a list of chain id

    /**
     * Queue to handle bundle arrivals.
     */
    private CreatorThread creatorThread;

    private ChainParser parser;


    public CiliaFileManagerImpl(BundleContext context) {
        bcontext = context;
        handledChains = Collections.synchronizedMap(new HashMap<File, List<String>>());
        creatorThread = new CreatorThread();
    }

    public void start() {
        new Thread(creatorThread).start();
    }

    public void stop() {
        creatorThread.stop();
        Set<File> files = handledChains.keySet();
        File filesArray[];
        filesArray = (File[]) files.toArray(new File[files.size()]);
        for (int i = 0; i < filesArray.length; i++) {
            stopManagementFor(filesArray[i]);
        }
    }

    /**
     * Start a mediation chain.
     *
     * @param chains The chain identifier to start.
     */
    public void loadChain(File chains) {
        creatorThread.addFile(chains);
    }

    /**
     * Start a mediation chain.
     *
     * @param chainId The chain identifier to start.
     */
    public void unloadChain(File chains) {
        creatorThread.removeFile(chains);
        stopManagementFor(chains);
    }


    private void startManagementFor(File file) throws CiliaException {
        List<String> chainsList = new ArrayList<String>();
        Builder builders[] = null;
        logger.debug("Processing file: " + file.getName());
        try {
            builders = parser.obtainChains(file.toURI().toURL());
        } catch (FileNotFoundException e) {
            logger.error("File not found" + file.getAbsolutePath());
            throw new BuilderException("File not found: " + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            logger.error("Unable to open file: " + file.getAbsolutePath());
            throw new BuilderException("Unable to open file: " + file.getAbsolutePath());
        }
        if (builders != null && builders.length >= 1) {
            for (int i = 0; i < builders.length; i++) {
                builders[i].done();
                Architecture arch = builders[i].get(builders[i].current());
                if (arch.toCreate()) {
                    ccontext.getApplicationRuntime().startChain(builders[i].current());
                    chainsList.add(builders[i].current());
                }
            }
            synchronized (handledChains) {
                handledChains.put(file, chainsList);
            }
        } else {
            logger.warn("File [" + file.getName() + "] Doesn't have any chain to handle");
        }
    }

    private void stopManagementFor(File bundle) {
        List<String> chainList = null;
        synchronized (handledChains) {
            chainList = handledChains.remove(bundle);
        }
        if (chainList != null) {
            Object[] obs = chainList.toArray();
            String[] chains = new String[obs.length];
            if (chains != null) {
                chainList.toArray(chains);
                for (int i = 0; i < chains.length; i++) {
                    if (ccontext != null) { //CiliaContext could disappear and this service is stopping also.
                        try {
                            ccontext.getApplicationRuntime().stopChain(chains[i]);
                            ccontext.getBuilder().remove(chains[i]).done();
                        } catch (Exception ex) {
                        } //Exception when stoping iPOJO runtime.
                    }
                }
                chainList.clear();
            }
        }
    }

    /**
     * The creator thread analyzes arriving files to create Cilia instances.
     * Obtained from iPOJO
     */
    private class CreatorThread implements Runnable {

        /**
         * Is the creator thread started?
         */
        private boolean m_started = true;

        /**
         * The list of bundle that are going to be analyzed.
         */
        private List<File> chainFiles = new ArrayList<File>();

        /**
         * A bundle is arriving.
         * This method is synchronized to avoid concurrent modification of the waiting list.
         *
         * @param file the new bundle
         */
        public synchronized void addFile(File file) {
            chainFiles.add(file);
            notifyAll(); // Notify the thread to force the process.
        }

        /**
         * A bundle is leaving.
         * If the bundle was not already processed, the bundle is remove from the waiting list.
         * This method is synchronized to avoid concurrent modification of the waiting list.
         *
         * @param bundle the leaving bundle.
         */
        public synchronized void removeFile(File file) {
            chainFiles.remove(file);
        }

        /**
         * Stops the creator thread.
         */
        public synchronized void stop() {
            m_started = false;
            chainFiles.clear();
            notifyAll();
        }

        public void run() {
            boolean started;
            synchronized (this) {
                started = m_started;
            }
            while (started) {
                File file;
                synchronized (this) {
                    while (m_started && chainFiles.isEmpty()) {
                        try {
                            logger.debug("Creator thread is waiting - Nothing to do");
                            wait();
                        } catch (InterruptedException e) {
                            // Interruption, re-check the condition
                        }
                    }
                    if (!m_started) {
                        logger.debug("Creator thread is stopping");
                        return; // The thread must be stopped immediately.
                    } else {
                        // The bundle list is not empty, get the bundle.
                        // The bundle object is collected inside the synchronized block to avoid
                        // concurrent modification. However the real process is made outside the
                        // mutual exclusion area
                        file = (File) chainFiles.remove(0);
                    }
                }
                // Process ...
                try {
                    startManagementFor(file);
                } catch (Throwable e) {
                    // To be sure to not kill the thread, we catch all exceptions and errors
                    e.printStackTrace();
                    logger.error("An error occurs when analyzing the content or starting the management of " + file.getName());
                }
                synchronized (this) {
                    started = m_started;
                }
            }
        }

    }
}

