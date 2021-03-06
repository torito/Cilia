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

package fr.liglab.adele.cilia.knowledge;

import fr.liglab.adele.cilia.Measure;
import fr.liglab.adele.cilia.util.Watch;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Class holding values generated by the cilia runtime
 *
 * @author <a href="mailto:cilia-devel@lists.ligforge.imag.fr">Cilia Project
 *         Team</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MeasureImpl implements Measure {
    private final HashMap map;

    public MeasureImpl(Object value, long ticks) {
        map = new HashMap(2);
        map.put("value", value);
        map.put("ticks", new Long(ticks));
    }

    private MeasureImpl(Measure from) {
        this.map = new HashMap(((MeasureImpl) from).map);
    }

    public Object value() {
        return map.get("value");
    }

    public boolean hasNoValue() {
        boolean isnull = false;
        if (value() instanceof String) {
            if (((String) value()).compareTo(Measure.NO_VALUE) == 0)
                isnull = true;
        }
        return isnull;
    }

    public long timeStampMs() {
        return Watch.fromTicksToMs(((Long) map.get("ticks")).longValue());
    }

    public Measure clone() {
        return new MeasureImpl(this);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer('[');
        if (value() == null)
            sb.append("Value: null");
        else
            sb.append("Value: ").append(value().toString()).append("");
        sb.append(", Timestamp:").append(timeStampMs()).append("");
        sb.append("]");
        return sb.toString();
    }

    public Map toMap() {
        Map result = new Hashtable();
        result.put("Value", value());
        result.put("Timestamp", timeStampMs());
        return result;
    }
}
