package deployer.report.wiki;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Gathers statistics based on a String-Integer relation.
 * 
 * @author moran
 * @see WikiChartStatistics
 */
public class Statistics extends HashMap<String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Merges the statistics gathered by the provided statistics instance and this instance.
     * 
     * @param statistics Gathered statistics.
     */
    public void merge(Statistics statistics) {
        Set<Entry<String, Integer>> entrySet = statistics.entrySet();
        for (Entry<String, Integer> entry : entrySet) {
            Integer integer = super.get(entry.getKey());
            if (integer == null) {
                super.put(entry.getKey(), entry.getValue());
            } else {
                super.put(entry.getKey(), Integer.valueOf(integer.intValue() + entry.getValue().intValue()));
            }

        }
    }
}
