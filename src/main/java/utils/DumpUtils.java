package utils;

import com.j_spaces.kernel.PlatformVersion;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.rest.response.GetMachinesDumpFileResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import utils.exceptions.FailedToCreateDumpException;
import utils.exceptions.WrongMessageException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DumpUtils{

    public static void dumpServiceLogs(String restUrl, String destPath) throws RestClientException, WrongMessageException, FailedToCreateDumpException, IOException {
        dumpMachinesNewRestAPI(restUrl, null, null, destPath);
    }

    public static void dumpMachinesNewRestAPI(
            final String restUrl,
            final String username,
            final String password,
            final String destinationPath) throws FailedToCreateDumpException, RestClientException, WrongMessageException,
            IOException {
        System.out.println("Downloading machines dump");
        Map<String, File> machinesDumpFiles = DumpUtils.getMachinesDumpFile(restUrl, username, password);
        System.out.println("Machines dump downloaded successfully");

        DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat hour = new SimpleDateFormat("HH-mm-ss-SSS");
        for (Map.Entry<String, File> entry : machinesDumpFiles.entrySet()) {
            Date date = new Date();
            File zipFile = new File(destinationPath
                    + File.separator + date1.format(date) + "_" + hour.format(date) + "_ip" + entry.getKey() + "_dump.zip");
            zipFile.deleteOnExit();
            entry.getValue().renameTo(zipFile);
            System.out.println("> Logs: " + zipFile.getAbsolutePath() + "\n");
        }
    }

    public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
            final String username,
            final String password) throws IOException, RestClientException,
            WrongMessageException, FailedToCreateDumpException {
        return getMachinesDumpFile(restUrl, username, password, null, 0, null);
    }

    public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
            final String username,
            final String password,
            final String processors,
            final long fileZiseLimit,
            final String errMessageContain) throws IOException, RestClientException,
            WrongMessageException, FailedToCreateDumpException {

        // connect to the REST
        RestClient restClient = createAndConnect(restUrl, username, password);

        // get dump data using REST API
        GetMachinesDumpFileResponse response;
        try {
            response = restClient.getMachinesDumpFile(processors, fileZiseLimit);
            if (errMessageContain != null) {
                Assert.fail("RestClientException expected [" + errMessageContain + "]");
            }
            // write the result data to a temporary file.
            Map<String, byte[]> dumpBytesPerIP = response.getDumpBytesPerIP();
            Map<String, File> dumpFilesPerIP = new HashMap<String, File>(dumpBytesPerIP.size());
            for (Map.Entry<String, byte[]> entry : dumpBytesPerIP.entrySet()) {
                File file = File.createTempFile("dump", ".zip");
                file.deleteOnExit();
                FileUtils.writeByteArrayToFile(file , entry.getValue());
                dumpFilesPerIP.put(entry.getKey(), file);
            }
            return dumpFilesPerIP;
        } catch (RestClientException e) {
            String message = e.getMessageFormattedText();
            if (errMessageContain == null) {
                throw new FailedToCreateDumpException(message);
            } else {
                if (!message.contains(errMessageContain)) {
                    throw new WrongMessageException(message, errMessageContain);
                }
                return null;
            }
        }
    }

    public static RestClient createAndConnect(final String restUrl, final String username, final String password)
            throws RestClientException, MalformedURLException {
        RestClient restClient = create(restUrl, username, password);
        restClient.connect();
        return restClient;
    }

    public static RestClient create(final String restUrl, final String username, final String password)
            throws MalformedURLException, RestClientException {
        RestClient restClient = null;
        final String apiVersion = PlatformVersion.getVersion();
        restClient = new RestClient(new URL(restUrl), username, password, apiVersion);
        return restClient;
    }
}