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
/**
 *
 */
package cilia.runtime.context.test;

import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.builder.Architecture;
import fr.liglab.adele.cilia.builder.Builder;
import fr.liglab.adele.cilia.exceptions.BuilderException;
import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.helper.CiliaHelper;
import fr.liglab.adele.cilia.helper.MediatorTestHelper;
import fr.liglab.adele.cilia.model.MediatorComponent;
import fr.liglab.adele.cilia.runtime.MediatorRuntimeSpecification;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.runner.test.ChameleonRunner;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;

/**
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */

@RunWith(ChameleonRunner.class)
public class ReliabilityTest {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private CiliaHelper cilia;


    private static final Logger logger = LoggerFactory.getLogger("cilia.debug");

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        cilia = new CiliaHelper(context);
    }

    @After
    public void tearDown() {
        cilia.dispose();
        osgi.dispose();
    }


    @Test
    public void replaceWithoutLoose() {
        CiliaHelper.waitSomeTime(2000);
        createEnricherMediator();
        String chainID = "reliableTest";
        URL url = context.getBundle().getResource("reliableTest.dscilia");
        cilia.load(url);

        //wait to be added.
        logger.info("will wait");
        cilia.waitToChain(chainID, 6000);
        logger.info("Chain Ready");
        MediatorTestHelper qd = cilia.instrumentChain(chainID, "firstMediator:unique", "lastMediator:unique");
        //chain must exist, and helper should be well constructed.
        logger.info("Chain has been instrumented");
        Assert.assertNotNull(qd);
        //wait first mediator to be valid.
        if (!cilia.checkComponentState("reliableTest", "firstMediator", MediatorComponent.State.VALID, 3000)) {
            Assert.fail("Failed to retrieve firstMediator component, expected state valid.");
        }
        logger.info("Injecting First Data");
        qd.injectData(new Data("data ONE", "dda"));
        CiliaHelper.waitSomeTime(2000);
        Assert.assertEquals(0, qd.getAmountData());
        logger.info("Injecting Second Data");
        qd.injectData(new Data("data TWO", "dda"));
        CiliaHelper.waitSomeTime(2000);
        //wait some time to arrive message.

        //Now we replace component
        Builder b = cilia.getBuilder();
        Architecture arch = null;
        try {
            arch = b.get(chainID);
        } catch (BuilderException e) {
            Assert.fail("Unable to load chain" + chainID);
        }
        try {
            arch.replace().id("enricher1").to("enricher2");
        } catch (BuilderException e) {
            Assert.fail("Replace operation in builder fail" + chainID);
        }
        try {
            b.done();
        } catch (CiliaException e) {
            Assert.fail("Fail while done builder performer" + chainID);
        }//wait to replaced component to be valid
        if (!cilia.checkComponentState("reliableTest", "enricher2", MediatorComponent.State.VALID, 3000)) {
            Assert.fail("Failed to retrieve enricher2 component, expected state valid.");
        }
        logger.info("Replace in builder done");
        //We inject the last data. Now processing must be performed on replaced mediator.
        Assert.assertEquals(0, qd.getAmountData());
        logger.info("Injecting third Data");
        CiliaHelper.waitSomeTime(2000);
        qd.injectData(new Data("data THREE", "dda"));
        CiliaHelper.waitSomeTime(3000);
        logger.info("Assert data");
        Assert.assertEquals(3, qd.getAmountData());

        Data lastData = qd.getLastData();
        Assert.assertEquals("data THREE", lastData.getContent());
        Assert.assertEquals("dda", lastData.getName());

        Assert.assertEquals("new enricher content", lastData.getProperty("enricher"));
        System.out.println("Received NEW processed data: " + lastData.getAllData());
    }

    @Test
    public void testCount() {
        CiliaHelper.waitSomeTime(2000);
        createEnricherMediator();
        URL url = context.getBundle().getResource("reliableTest.dscilia");
        cilia.load(url);
        //wait to be added.
        logger.info("will wait");
        cilia.waitToChain("reliableTest", 6000);
        logger.info("Chain is ready");
        MediatorTestHelper qd = cilia.instrumentChain("reliableTest", "firstMediator:unique", "lastMediator:unique");
        logger.info("Chain has been instrumented");
        Assert.assertNotNull(qd);
        if (!cilia.checkComponentState("reliableTest", "firstMediator", MediatorComponent.State.VALID, 3000)) {
            Assert.fail("Failed to retrieve firstMediator component, expected state valid.");
        }
        logger.info("Injecting first Data");
        qd.injectData(new Data("data ONE", "dda"));
        CiliaHelper.waitSomeTime(2000);
        Assert.assertEquals(0, qd.getAmountData());
        logger.info("Injecting second Data");
        qd.injectData(new Data("data TWO", "dda"));
        CiliaHelper.waitSomeTime(2000);
        Assert.assertEquals(0, qd.getAmountData());
        logger.info("Injecting third Data");
        qd.injectData(new Data("data THREE", "dda"));
        //wait some time to arrive message.
        CiliaHelper.waitSomeTime(3000);
        logger.info("Analize Injected Data");
        Assert.assertEquals(3, qd.getAmountData());


        Data lastData = qd.getLastData();
        Assert.assertEquals("data THREE", lastData.getContent());
        Assert.assertEquals("dda", lastData.getName());
        System.out.println("Received data: " + lastData.getAllData());
    }


    private void createEnricherMediator() {
        MediatorRuntimeSpecification mrs = new MediatorRuntimeSpecification("ReliableTest", null, null, context);
        mrs.setDispatcher("multicast-dispatcher", "fr.liglab.adele.cilia");
        mrs.setScheduler("counter-scheduler", "fr.liglab.adele.cilia");
        mrs.setProcessor("SimpleEnricherProcessor", "fr.liglab.adele.cilia");
        mrs.setInPort("unique", "*");
        mrs.setOutPort("unique", "*");
        mrs.initializeSpecification();
    }

}
