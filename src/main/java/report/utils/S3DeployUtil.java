package deployer.report.utils;

import com.google.inject.Module;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.AccessControlList;
import org.jclouds.s3.domain.CannedAccessPolicy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class S3DeployUtil {
    protected static final String CREDENTIALS_FOLDER = System.getProperty("com.quality.sgtest.credentialsFolder",
            SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");

    private static final String S3_PROPERTIES = CREDENTIALS_FOLDER + "/s3.properties";

    public static void uploadLogFile(File source, String buildNumber, String suiteName, String testName){
        try {
            Properties props = getS3Properties();
            String container =  props.getProperty("container");
            String user =  props.getProperty("user");
            String key =  props.getProperty("key");
            String target = buildNumber + "/" + suiteName + "/" + testName;
            BlobStoreContext context;
            Set<Module> wiring = new HashSet<Module>();
            context = new BlobStoreContextFactory().createContext("aws-s3", user, key, wiring, new Properties());
            S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
            BlobStore store = context.getBlobStore();

            uploadLogFile(source, target, container, client, store);
            context.close();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    private static void uploadLogFile(File source, String target, String container, S3Client client, BlobStore store){
        if (source.isDirectory()){
            for (File f : source.listFiles()){
                uploadLogFile(new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName(), container, client, store);
            }
        }
        else{
            //LogUtils.log("Processing " + source + ", upload size is: " + (source).length() + ". Target: " + target);
            store.putBlob(container, store.blobBuilder(target)
                    .payload(source)
                    .build());
            //LogUtils.log("Upload of " + source + " was ended successfully");

                String ownerId = client.getObjectACL(container, target).getOwner().getId();
                client.putObjectACL(container, target,
                        AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
        }
    }


    private static Properties getS3Properties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(S3_PROPERTIES));
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + S3_PROPERTIES + " file - " + e, e);
        }

        return properties;
    }

}
