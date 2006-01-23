package myapp;

import java.util.Arrays;
import filter.FilterProvider;
import filter.IFilter;

public class Main {
    
    public static void main(String[] args) {
        String toFilter[] = new String[]{"one", "two", "tree", "four"};
        IFilter filter = FilterProvider.getFilter();
        System.out.println("Filtering with:"+filter.getClass());
        String filtered[] = filter.filter(toFilter, "t");
        System.out.println("Result :"+Arrays.asList(filtered));
    }
}
