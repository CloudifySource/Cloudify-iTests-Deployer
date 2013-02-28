package deployer.report.wiki;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WikiUtils {


    /**
     * Writes desired string data to appropriate file.
     *
     * @param context  String context.
     * @param fileName file name path.
     * @param append   if <code>true</code>, then bytes will be written
     *                 to the end of the file rather than the beginning
     * @throws IOException Failed to write data-context to file.
     */
    synchronized public static void writeToFile(String context, String fileName, boolean append)
            throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintStream ps = new PrintStream(new FileOutputStream(fileName, append));
        ps.println(context);
        ps.flush();
        ps.close();
    }

    /**
     * Read from supplied <b>text file</b> all lines and returns the String
     * list.
     *
     * @param fileName the filename to read from.
     * @return list of file lines
     * @throws IOException Failed to open/read from supplied file.
     */
    synchronized public static List<String> getFileLines(String fileName)
            throws IOException {
        ArrayList<String> lineArray = new ArrayList<String>();

        FileReader fReader = null;

        try {
            fReader = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(fReader);

            String line;
            while ((line = reader.readLine()) != null) {
                lineArray.add(line);
            }
        } finally {
            if (fReader != null)
                fReader.close();
        }

        return lineArray;
    }

    /**
     * format the duration into a readable string.
     */
    public static String formatDuration(long duration) {
        long value = duration;
        value = value / 1000;
        long seconds = value % 60;
        value = value / 60;
        long minutes = value % 60;
        value = value / 60;
        long hours = value % 24;
        long days = value / 24;

        String result = "";
        if (days > 0)
            result = days + (days > 1 ? " days" : " day") +
                    (hours > 0 ? ", " + hours + getHoursText(hours) : "") +
                    (minutes > 0 ? ", " + minutes + getMinutesText(minutes) : "") +
                    (seconds > 0 ? ", " + seconds + getSecondsText(seconds) : "");
        else if (hours > 0)
            result = hours + getHoursText(hours) +
                    (minutes > 0 ? ", " + minutes + getMinutesText(minutes) : "") +
                    (seconds > 0 ? ", " + seconds + getSecondsText(seconds) : "");
        else if (minutes > 0)
            result = minutes + getMinutesText(minutes) +
                    (seconds > 0 ? ", " + seconds + getSecondsText(seconds) : "");
        else if (seconds > 0)
            result = seconds + getSecondsText(seconds);
        else
            result = "0";
        return (result);
    }

    private static String getHoursText(long hours) {
        if (hours == 0)
            return (" hours");
        return ((hours > 1 ? " hours" : " hour"));
    }

    private static String getMinutesText(long minutes) {
        if (minutes == 0)
            return (" minutes");
        return ((minutes > 1 ? " minutes" : " minute"));
    }

    private static String getSecondsText(long seconds) {
        if (seconds == 0)
            return (" seconds");
        return ((seconds > 1 ? " seconds" : " second"));
    }

    /**
     * Converts the stack trace of the specified exception to a string.
     * NOTE: Pass the exception as an argument to the external exception
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();

        return sw.toString();
    }
}
