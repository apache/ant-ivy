package filter.hmimpl;

import java.util.ArrayList;
import java.util.List;
import filter.IFilter;

public class HMFilter implements IFilter {
    
    public String[] filter(String[] values, String prefix) {
        if(values == null) {
            return null;
        }
        if(prefix == null) {
            return values;
        }
        List result = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            String string = values[i];
            if(string != null && string.startsWith(prefix)) {
                result.add(string);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }
}
