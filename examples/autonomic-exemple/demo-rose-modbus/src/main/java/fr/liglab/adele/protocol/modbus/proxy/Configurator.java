package fr.liglab.adele.protocol.modbus.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.api.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;

public class Configurator {
	private static final Logger logger = LoggerFactory.getLogger("cilia.rose.modbus");
    private final Machine rose;

	public Configurator(BundleContext context) throws InvalidSyntaxException {

        rose= machine(context,"rose-modbus").create();
        //Discovery
        rose.instance("Modbus/TCP.discovery").create();

        //Importer
        rose.importer("Modbus/TCP.importer").create();

        //Connections
        rose.in("(service.imported=true)").protocol(getProtocolDevice()).add();
    }

	/**
	 * List of protocols actually managed
	 */
	
	public List getProtocolDevice() {
		List list = new ArrayList();
		list.add("Modbus/Tcp");
		return list;
	}

	protected void start() {
		rose.start();
	}

	protected void stop() {
		rose.stop();
	}

}