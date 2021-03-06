package fr.liglab.adele.cilia.framework.components;

import fr.liglab.adele.cilia.Data;
import fr.liglab.adele.cilia.exceptions.CiliaException;
import fr.liglab.adele.cilia.framework.AbstractDispatcher;
import fr.liglab.adele.cilia.framework.data.CiliaExpression;
import fr.liglab.adele.cilia.framework.data.ExpressionFactory;
import org.osgi.framework.BundleContext;

import java.util.Iterator;
import java.util.Map;

/**
 * ContentBasedDispatcher
 * This Dispatcher will analize Data in order to
 *
 * @author torito
 */
public class ContentBasedDispatcher extends AbstractDispatcher {

    /**
     * List destinations.
     * injected by iPOJO.
     */
    protected Map routeConditions;
    /**
     * Expression langage to compare the content.
     */

    private CiliaExpression expre = null;


    public ContentBasedDispatcher(BundleContext context) {
        super(context);
    }

    public void setLanguage(String lang) {
        ExpressionFactory ef = new ExpressionFactory(bcontext);
        try {
            expre = ef.getExpressionParser(lang);
        } catch (CiliaException e) {
            e.printStackTrace();
            expre = null;
        }
    }

    public void setRouteConditions(Map conditions) {
        routeConditions = conditions;
    }

    /**
     * Method to call when processing is finished
     * and used to send data to destinations.
     *
     * @throws CiliaException
     */
    public void dispatch(Data data) throws CiliaException {

        if (routeConditions == null) {
            throw new CiliaException("There is any configuration to dispatch");
        }

        if (expre == null) {
            throw new CiliaException("Expression parser is null, set language first.");
        }

        synchronized (routeConditions) {
            Iterator it = routeConditions.keySet().iterator();
            while (it.hasNext()) {
                String condition = (String) it.next();
                boolean temp = expre.evaluateBooleanExpression(condition, data);
                if (temp) {
                    String senderName = (String) routeConditions.get(condition);
                    send(senderName, data);
                }
            }
        }

    }

}
