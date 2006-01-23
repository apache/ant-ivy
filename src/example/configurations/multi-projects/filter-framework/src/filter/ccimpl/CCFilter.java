package filter.ccimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import filter.IFilter;

public class CCFilter implements IFilter {
    
    public String[] filter(String[] values, final String prefix) {
        if(values == null) {
            return null;
        }
        if(prefix == null) {
            return values;
        }

        List result = new ArrayList(Arrays.asList(values));
        CollectionUtils.filter(result, new Predicate() {
            public boolean evaluate(Object o) {
                return o!= null && o.toString().startsWith(prefix);
            }
        });
        return (String[]) result.toArray(new String[result.size()]);
    }
}
