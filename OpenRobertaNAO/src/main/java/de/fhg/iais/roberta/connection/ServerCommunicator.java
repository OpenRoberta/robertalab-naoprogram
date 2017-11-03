package de.fhg.iais.roberta.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        this.serverpushAddress = customServerAddress + "/rest/pushcmd";
        this.serverdownloadAddress = customServerAddress + "/rest/download";
        this.serverUpdateAddress = customServerAddress + "/update/nao/v2-1-4-3/hal";
        this.serverUpdateChecksumAddress = customServerAddress + "/update/nao/v2-1-4-3/hal/checksum";
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
        this.post = new HttpPost("http://" + this.serverpushAddress);
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

        HttpPost post = new HttpPost("http://" + this.serverdownloadAddress);
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

    /**
     * Basically the same as downloading a user program but without any information about the EV3. It uses http GET(!).
     *
     * @param fwFile name of the file in the url as suffix ( .../rest/update/ev3menu)
     * @return
     * @throws IOException if the server is unreachable or something is wrong with the binary content.
     */
    public byte[] downloadFirmwareFile() throws IOException {
        URL url = new URL("http://" + this.serverUpdateAddress);
        url.openConnection();
        InputStream is = url.openStream();
        byte[] binaryfile = new byte[is.available()];
        is.read(binaryfile);
        return binaryfile;
    }

    public boolean verifyHalChecksum() throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        Path path = Paths.get(System.getProperty("user.dir") + "/roberta.zip");
        try {
            digest.update(Files.readAllBytes(path));
        } catch ( IOException e ) {
            return false;
        }
        byte[] result = digest.digest();
        logger.log(Level.INFO, "Current hals checksum: {0} ", Base64.getEncoder().encodeToString(result));

        HttpGet get = new HttpGet("http://" + this.serverUpdateChecksumAddress);
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
        byte[] data = this.downloadFirmwareFile();
        File dataFile = new File(System.getProperty("user.dir") + "/roberta.zip");
        dataFile.delete();
        dataFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(dataFile);
        fos.write(data);
        fos.close();
        ZipFile zipFile = new ZipFile(dataFile);
        zipFile.extractAll(System.getProperty("user.dir"));
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
