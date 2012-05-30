package cilia.runtime.specification.test;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import org.apache.felix.ipojo.test.helpers.OSGiHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.liglab.adele.cilia.ApplicationSpecification;
import fr.liglab.adele.cilia.CiliaContext;
import fr.liglab.adele.cilia.builder.Architecture;
import fr.liglab.adele.cilia.builder.Builder;
import fr.liglab.adele.cilia.core.tests.tools.CiliaTools;
import fr.liglab.adele.cilia.exceptions.BuilderConfigurationException;
import fr.liglab.adele.cilia.exceptions.BuilderException;
import fr.liglab.adele.cilia.exceptions.BuilderPerformerException;
import fr.liglab.adele.cilia.model.Adapter;
import fr.liglab.adele.cilia.model.Mediator;
import fr.liglab.adele.cilia.model.impl.AdapterImpl;
import fr.liglab.adele.cilia.model.impl.ChainImpl;
import fr.liglab.adele.cilia.model.impl.MediatorImpl;



@RunWith(JUnit4TestRunner.class)
public class CiliaSpecificationTester {

	@Inject
	private BundleContext context;

	private OSGiHelper osgi;

	@Before
	public void setUp() {
		osgi = new OSGiHelper(context);
	}

	@After
	public void tearDown() {
		osgi.dispose();
	}

	@Configuration
	public static Option[] configure() {
		Option[] platform = options(felix());

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo").versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo.test.helpers")
						.versionAsInProject(), mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.versionAsInProject(), mavenBundle().groupId("org.slf4j")
						.artifactId("slf4j-simple").version("1.6.1"), mavenBundle()
						.groupId("fr.liglab.adele.cilia").artifactId("cilia-core")
						.versionAsInProject(),
				mavenBundle().groupId("fr.liglab.adele.cilia")
						.artifactId("cilia-runtime").versionAsInProject())); 
		Option[] r = OptionUtils.combine(platform, bundles);
		return r;
	}

	/**
	 * Mockito bundle
	 * 
	 * @return
	 */
	@Configuration
	public static Option[] mockitoBundle() {
		return options(JUnitOptions.mockitoBundles());
	}

	// Verify the service is present
	@Test
	public void validateService() {
		CiliaTools.waitToInitialize();
		ServiceReference sr[] = null;
		sr = osgi.getServiceReferences(CiliaContext.class.getName(), null);
		assertNotNull(sr[0]);
		CiliaContext ccontext = (CiliaContext) context.getService(sr[0]);
		assertNotNull(ccontext);
	}

	public CiliaContext getCiliaContextService() {
		ServiceReference sr[] = null;
		sr = osgi.getServiceReferences(CiliaContext.class.getName(), null);
		assertNotNull(sr[0]);
		CiliaContext ccontext = (CiliaContext) context.getService(sr[0]);
		assertNotNull(ccontext);
		return ccontext;
	}

	public void chainCreation() {
		CiliaTools.waitToInitialize();

		String chainId = "Chain1";
		ChainImpl chain = new ChainImpl(chainId, "type", "", null);

		Mediator m1 = new MediatorImpl("mediator_1", "type");
		Mediator m2 = new MediatorImpl("mediator_2", "type");

		Adapter a1 = new AdapterImpl("entryAdapter", "type");
		Adapter a2 = new AdapterImpl("exitAdapter", "type");
		chain.add(m1);
		chain.add(m2);
		chain.add(a1);
		chain.add(a2);

		Assert.assertNotNull(chain.bind(a1.getOutPort("out"), m1.getInPort("in"))); // adapter
																					// to
																					// mediator
		Assert.assertNotNull(chain.bind(m1.getOutPort("out"), m2.getInPort("in"))); // adapter
																					// to
																					// mediator
		Assert.assertNotNull(chain.bind(m2.getOutPort("out"), a2.getInPort("in"))); // adapter
																					// to
																		// mediator
	
	}
	
	public void chainTest(CiliaContext ciliaContext) {

		Builder builder = ciliaContext.getBuilder();
//		try {
//			builder.create("Chain1");
//			builder.done();
//			Architecture chain = builder.get("Chain1");
//			Assert.assertNotNull(chain);
//			chain.create().adapter().type("type").id("entryAdapter");
//			builder.done();
//			chain.create().adapter().type("type").namespace("sample").id("exitAdapter");
//			builder.done();
//			chain.create().mediator().type("type").namespace("sample").id("mediator_1");
//			builder.done();
//			chain.create().mediator().type("type").namespace("sample").id("mediator_2");
//			builder.done();
//			chain.bind().from("entryAdapter").to("mediator_1:in");
//			builder.done();
//			chain.bind().from("mediator_1:out").to("mediator_2:in");
//			builder.done();
//			chain.bind().from("mediator_2:out").to("exitAdapter");
//		} catch (BuilderException e) {
//			Assert.fail(e.getMessage());
//		} catch (BuilderConfigurationException e) {
//			Assert.fail(e.getMessage());
//		} catch (BuilderPerformerException e) {
//			Assert.fail(e.getMessage());
//		}
	}
	
	@Test
	public void EndpointsIn() {
		CiliaTools.waitToInitialize();
		CiliaContext ciliaContext = getCiliaContextService();
		ApplicationSpecification application = ciliaContext.getApplicationSpecification();
		assertNotNull(application) ;
//		chainTest(ciliaContext);
//		
//		String [] chainId = application.getChainId();
//		if (chainId.length != 1) {
//			Assert.fail("Expected length = 1");
//		}
//		Assert.assertEquals("Chain1", chainId[0]);
	}

}
