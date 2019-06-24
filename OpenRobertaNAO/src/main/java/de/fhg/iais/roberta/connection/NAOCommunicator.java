package de.fhg.iais.roberta.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;

import com.jcraft.jsch.JSchException;

import de.fhg.iais.roberta.util.SshConnection;

public class NAOCommunicator {

    private static Logger log = Logger.getLogger("Connector");
    private String ip;
    private String userName;
    private String password;

    int sshPort = 22;

    private String halZipPath;
    private String workingDirectory;
    private SshConnection ssh;
    private String firmwareversion;

    public NAOCommunicator(String ip, String username, String password) {
        this.ip = ip;
        this.userName = username;
        this.password = password;

        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.halZipPath = System.getenv("APPDATA") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getenv("APPDATA") + "/OpenRoberta/";
        } else {
            this.halZipPath = System.getProperty("user.home") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getProperty("user.home") + "/OpenRoberta/";
        }

    }

    public String getIp() {
        return this.ip;
    }

    public void uploadFile(byte[] binaryfile, String fileName) throws Exception {
        List<String> fileNames = new ArrayList<>();
        fileNames.add("__init__.py");
        fileNames.add("blockly_methods.py");
        fileNames.add("original_hal.py");
        fileNames.add("speech_recognition_module.py");
        fileNames.add("face_recognition_module.py");
        try {
            this.ssh = new SshConnection(this.ip, this.sshPort, this.userName, this.password);
            this.ssh.createSession();
            this.ssh.connect();
            this.ssh.command("rm -rf /home/" + this.userName + "/roberta");
            this.ssh.command("mkdir -p /home/" + this.userName + "/roberta");
            for ( String fname : fileNames ) {
                this.ssh.copyLocalToRemote(this.workingDirectory + "/roberta", "roberta", fname);
            }
            this.ssh.copyLocalToRemote(binaryfile, ".", fileName);
            String run_command = this.firmwareversion.equals("2-8") ? "eval \"export $(xargs < /etc/conf.d/naoqi)\"; python " : "python ";
            this.ssh.command(run_command + fileName);
        } finally {
            this.ssh.disconnect();
        }
        log.info("file transferred");

    }

    public String checkFirmwareVersion() {
        try {
            this.ssh = new SshConnection(this.ip, this.sshPort, this.userName, this.password);
            this.ssh.createSession();
            this.ssh.connect();
            String msg = this.ssh.command("naoqi-bin --version");
            String version = msg.split("\n")[0].split(":")[1].trim();
            this.firmwareversion = version.replace(".", "-");
            return this.firmwareversion;
        } catch ( JSchException | IOException | InterruptedException e ) {
            log.info(e.getMessage());
            return "";
        } finally {
            this.ssh.disconnect();
        }
    }

    public void updateRobotInformation(String ip, String userName, String password) throws JSchException, IOException, InterruptedException {
        this.ip = ip;
        this.userName = userName;
        this.password = password;

    }

    /**
     * @return true if a program is currently running, false otherwise
     * @throws IOException
     */
    public NAOState getNAOstate() {
        return NAOState.WAITING_FOR_PROGRAM;
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", "Nao");
        deviceInfo.put("robot", "nao");
        deviceInfo.put("firmwareversion", this.firmwareversion);
        deviceInfo.put("macaddr", "usb");
        deviceInfo.put("brickname", "nao");
        deviceInfo.put("battery", "1.0");
        return deviceInfo;
    }

    public String getFirmwareVersion() {
        return this.firmwareversion;
    }

}
