package filter;


public class FilterProvider {
    
    public static IFilter getFilter() {
        try {
            Class clazz = Class.forName("filter.ccimpl.CCFilter");
            return (IFilter) clazz.newInstance();
        } catch (Exception e) {
            try {
                Class clazz = Class.forName("filter.hmimpl.HMFilter");
                return (IFilter) clazz.newInstance();
            } catch (Exception e1) {
                System.err.println("No filter implementation found in classpath !");
            }
            return null;
        }
    }
}
