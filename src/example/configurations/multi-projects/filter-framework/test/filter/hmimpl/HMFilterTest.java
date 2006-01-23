package filter.hmimpl;

import filter.AbstractTestFilter;
import filter.IFilter;

public class HMFilterTest extends AbstractTestFilter {
    public IFilter getIFilter() {
        return new HMFilter();
    }
}
