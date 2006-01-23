package filter.ccimpl;

import filter.AbstractTestFilter;
import filter.IFilter;

public class CCFilterTest extends AbstractTestFilter {
    public IFilter getIFilter() {
        return new CCFilter();
    }
}
