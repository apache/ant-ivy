package filter;

import junit.framework.TestCase;

public abstract class AbstractTestFilter extends TestCase {
    
    public void testFilterNull() {
        Exception err = null;
        try {
            getIFilter().filter(null, null);
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    }

    /**
     * @return
     */
    public abstract IFilter getIFilter() ;
    
    public void testFilterNullValues() {
        Exception err = null;
        try {
            getIFilter().filter(null, "test");
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    }
    
    public void testFilterNullPrefix() {
        Exception err = null;
        try {
            getIFilter().filter(new String[]{"test"}, null);
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    } 
    
    public void testFilter() {
        String[] result = getIFilter().filter(new String[]{"test", "nogood", "mustbe filtered"}, "t");
        assertNotNull(result);
        assertEquals(result.length, 1);
    }    
    
    public void testFilterWithNullValues() {
        String[] result = getIFilter().filter(new String[]{"test", null, "mustbe filtered"}, "t");
        assertNotNull(result);
        assertEquals(result.length, 1);
    }
}
