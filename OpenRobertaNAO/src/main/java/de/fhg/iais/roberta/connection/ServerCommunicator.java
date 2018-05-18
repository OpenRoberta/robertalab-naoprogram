package de.fhg.iais.roberta.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * The server communicator runs the server protocol on behalf of the actual robot hardware.
 * This class provides access to push requests, downloads the user program and download system libraries for
 * the upload funtion.
 *
 * @author dpyka
 */
public class ServerCommunicator {

    private String serverpushAddress;
    private String serverdownloadAddress;
    private String serverUpdateAddress;
    private String serverUpdateChecksumAddress;

    private static Logger logger = Logger.getLogger(ServerCommunicator.class.getName());

    private final CloseableHttpClient httpclient;
    private HttpPost post = null;

    private String filename = "";
    private String halZipPath;
    private String workingDirectory;

    /**
     * @param serverAddress either the default address taken from the properties file or the custom address entered in the gui.
     */
    public ServerCommunicator(String serverAddress) {
        updateCustomServerAddress(serverAddress);
        this.httpclient = HttpClients.createDefault();
    }

    /**
     * Update the server address if the user wants to use an own installation of open roberta with a different IP address.
     *
     * @param customServerAddress for example localhost:1999 or 192.168.178.10:1337
     */
    public void updateCustomServerAddress(String customServerAddress) {
        String prefix;
        if ( customServerAddress.contains("443") ) {
            prefix = "https://";
        } else {
            prefix = "http://";
        }
        this.serverpushAddress = prefix + customServerAddress + "/rest/pushcmd";
        this.serverdownloadAddress = prefix + customServerAddress + "/rest/download";
        this.serverUpdateAddress = prefix + customServerAddress + "/update/nao/v2-1-4-3/hal";
        this.serverUpdateChecksumAddress = prefix + customServerAddress + "/update/nao/v2-1-4-3/hal/checksum";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.halZipPath = System.getenv("APPDATA") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getenv("APPDATA") + "/OpenRoberta/";
        } else {
            this.halZipPath = System.getProperty("user.home") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getProperty("user.home") + "/OpenRoberta/";
        }
    }

    /**
     * @return the file name of the last binary file downloaded of the server communicator object.
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Sends a push request to the open roberta server for registration or keeping the connection alive. This will be hold by the server for approximately 10
     * seconds and then answered.
     *
     * @param requestContent data from the EV3 plus the token and the command send to the server (CMD_REGISTER or CMD_PUSH)
     * @return response from the server
     * @throws IOException if the server is unreachable for whatever reason.
     */
    public JSONObject pushRequest(JSONObject requestContent) throws IOException, JSONException {
        this.post = new HttpPost(this.serverpushAddress);
        this.post.setHeader("User-Agent", "Java/1.7.0_60");
        StringEntity requestEntity = new StringEntity(requestContent.toString(), ContentType.create("application/json", "UTF-8"));
        this.post.setEntity(requestEntity);

        CloseableHttpResponse response = this.httpclient.execute(this.post);
        HttpEntity responseEntity = response.getEntity();
        String responseText = "";
        if ( responseEntity != null ) {
            responseText = EntityUtils.toString(responseEntity);
        }
        response.close();

        return new JSONObject(responseText);
    }

    /**
     * Downloads a user program from the server as binary. The http POST is used here.
     *
     * @param requestContent all the content of a standard push request.
     * @return
     * @throws IOException if the server is unreachable or something is wrong with the binary content.
     */
    public byte[] downloadProgram(JSONObject requestContent) throws IOException {

        HttpPost post = new HttpPost(this.serverdownloadAddress);
        post.setHeader("User-Agent", "Java/1.7.0_60");
        StringEntity requestEntity = new StringEntity(requestContent.toString(), ContentType.create("application/json", "UTF-8"));
        post.setEntity(requestEntity);
        CloseableHttpResponse response = this.httpclient.execute(post);
        HttpEntity responseEntity = response.getEntity();
        byte[] binaryfile = null;
        if ( responseEntity != null ) {
            this.filename = response.getFirstHeader("Filename").getValue();
            binaryfile = EntityUtils.toByteArray(responseEntity);
        }
        response.close();

        return binaryfile;
    }

    public boolean verifyHalChecksum() throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        Path path = Paths.get(this.halZipPath);
        try {
            digest.update(Files.readAllBytes(path));
        } catch ( IOException e ) {
            return false;
        }
        byte[] result = digest.digest();
        logger.log(Level.INFO, "Current hals checksum: {0} ", Base64.getEncoder().encodeToString(result));

        HttpGet get = new HttpGet(this.serverUpdateChecksumAddress);
        get.setHeader("User-Agent", "Java/1.7.0_60");
        CloseableHttpResponse response = this.httpclient.execute(get);
        HttpEntity responseEntity = response.getEntity();

        BufferedReader rd = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
        String line;
        if ( (line = rd.readLine()) != null ) {
            logger.log(Level.INFO, "Received checksum from server: {0} ", line);
            if ( Base64.getEncoder().encodeToString(result).equals(line) ) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void updateHal() throws IOException, ZipException {
        FileUtils.copyURLToFile(new URL(this.serverUpdateAddress), new File(this.halZipPath));
        File dataFile = new File(this.halZipPath);
        ZipFile zipFile = new ZipFile(dataFile);
        zipFile.extractAll(this.workingDirectory);
        logger.info("New HAL downloaded and unzipped");
    }

    /**
     * Cancel a pending push request (which is blocking in another thread), if the user wants to disconnect.
     */
    public void abort() {
        if ( this.post != null ) {
            this.post.abort();
        }
    }

    /**
     * Shut down the http client.
     */
    public void shutdown() {
        try {
            this.httpclient.close();
        } catch ( IOException e ) {
            // ok
        }
    }
}
