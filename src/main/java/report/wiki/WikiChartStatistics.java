package deployer.report.wiki;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Provides a WikiChart representation on top of a {@link Statistics} instance.
 * 
 * @author moran
 */
public class WikiChartStatistics {

    private final Statistics statistics;
    private String title = "";
    private String subTitle = "";
    private String type = "pie";
    private boolean legend = true;
    private HashMap<String, String> colorMap;

    public WikiChartStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    /** The title of the chart. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** A subtitle for the chart using a smaller font. */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    /**
     * @param type
     *            pie (default), line, bar, area.. (see chart macro docs)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * By default a legend is shown for a chart, unless you set this parameter to false
     * (recommended for pie charts).
     */
    public void showLegend(boolean show) {
        this.legend = show;
    }

    /**
     * Assigns a color to a key, to generate a Comma separated list of colors used to customize
     * category, sections, and series colors. e.g. "#99CC00,#FF0000" or "green, red"
     */
    public void assignColor(String key, String color) {
        if (colorMap == null) {
            colorMap = new HashMap<String, String>();
        }

        colorMap.put(key, color);
    }

    public String toWikiString() {
        StringBuilder report = new StringBuilder();
        if (!statistics.isEmpty()) {
            report.append("{chart:title=")
            .append(title)
            .append("|subTitle=")
            .append(subTitle)
            .append("|type=")
            .append(type)
            .append("|legend=")
            .append(legend)
            .append("|pieSectionLabel=%0% (%1%) %2%")
            .append(addColors())
            .append("}\n")
            .append("|| || ")
            .append(addKeysWithDelimiter("||"))
            .append("\n")
            .append("| | ")
            .append(addValuesWithDelimiter("|"))
            .append("\n")
            .append("{chart}\n");
        }
        return report.toString();
    }

    private String addColors() {
        String colors = "";
        if (colorMap != null) {
            Set<String> keys = statistics.keySet();
            for (String key : keys) {
                String color = colorMap.get(key);
                if (color != null) {
                    if (colors == "") {
                        colors += "|colors="+color;
                    }else {
                        colors += ","+color;
                    }
                }
            }
        }

        return colors;
    }

    private String addKeysWithDelimiter(String delimiter) {
        String toString = "";
        Set<String> keys = statistics.keySet();
        for (String key : keys) {
            toString += (key + delimiter);
        }
        return toString;
    }

    private String addValuesWithDelimiter(String delimiter) {
        String toString = "";
        Collection<Integer> values = statistics.values();
        for (Integer value : values) {
            toString += (value + delimiter);
        }
        return toString;
    }
}
