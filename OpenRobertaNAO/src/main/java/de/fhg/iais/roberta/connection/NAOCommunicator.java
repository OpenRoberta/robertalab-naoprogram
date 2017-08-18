package de.fhg.iais.roberta.connection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class NAOCommunicator {

    private static Logger log = Logger.getLogger("NAOCommunicator");
    private String ip;
    private String username;
    private String password;
    //ports on the robot
    int sshPort = 22;
    int ftpPort = 21;

    private final JSch jsch = new JSch();
    private final FTPClient ftpClient = new FTPClient();

    public NAOCommunicator(String ip, String username, String password) {
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("firmwarename", "Nao");
        deviceInfo.put("robot", "nao");
        deviceInfo.put("firmwareversion", "2.1");
        deviceInfo.put("macaddr", "1");
        deviceInfo.put("brickname", "nao");
        deviceInfo.put("battery", "1.0");
        return deviceInfo;
    }

    public void playAscending() throws IOException {

    }

    public void playDescending() throws IOException {

    }

    public void playProgramDownload() throws IOException {

    }

    /**
     * @return true if a program is currently running, false otherwise
     * @throws IOException
     */
    public NAOState getNAOstate() {
        return NAOState.WAITING_FOR_PROGRAM;
    }

    public void uploadFile(byte[] binaryfile, String fileName) throws Exception {
        log.info("Robot IP: " + this.ip + "  Username: " + this.username + "  Password: " + this.password);
        List<String> fileNames = new ArrayList<String>();
        fileNames.add("__init__.py");
        fileNames.add("blockly_methods.py");
        fileNames.add("original_hal.py");
        fileNames.add("speech_recognition_module.py");
        byte[] fileContents;
        for ( String fname : fileNames ) {
            fileContents = getFileFromResources(fname);
            ftpTransfer(fname, fileContents);
        }
        ftpTransfer(fileName, binaryfile);
        sshCommand("python " + fileName);
        sshCommand("rm " + fileName);
    }

    private byte[] getFileFromResources(String fileName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());
            String input = "";
            Scanner scanner = new Scanner(file);
            while ( scanner.hasNextLine() ) {
                input += scanner.nextLine() + '\n';
            }
            scanner.close();
            return input.getBytes();

        } catch ( Exception e ) {
            System.out.println("Problem reading or writing " + fileName + " file.");
        }
        return null;

    }

    private void sshCommand(String command) {

        //implement the abstract class MyUserInfo
        UserInfo ui = new MyUserInfo() {
        };

        try {
            Session session = this.jsch.getSession(this.username, this.ip, this.sshPort);
            session.setPassword(this.password);
            session.setUserInfo(ui);
            session.setTimeout(50000);

            //fingerprint is not accepted
            //uncomment this part if key fingerprint is not accepted
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            //establish session connection
            session.connect();

            //open channel and send command
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            //set streams
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();

            //establish channel connection
            channel.connect();

            //show messages on the ssh channel
            byte[] tmp = new byte[1024];
            while ( true ) {
                while ( in.available() > 0 ) {
                    int i = in.read(tmp, 0, 1024);
                    if ( i < 0 ) {
                        break;
                    }
                    System.out.print(new String(tmp, 0, i));
                }
                if ( channel.isClosed() ) {
                    if ( in.available() > 0 ) {
                        continue;
                    }
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch ( Exception ee ) {
                }
            }

            //disconnect from channel and session
            channel.disconnect();
            session.disconnect();
        } catch ( Exception e ) {
            System.out.println(e);
        }
    }

    private void showServerReply() {

        String[] replies = this.ftpClient.getReplyStrings();

        if ( replies != null && replies.length > 0 ) {
            for ( String aReply : replies ) {
                System.out.println("SERVER: " + aReply);
            }
        }
    }

    private void ftpTransfer(String fileName, byte[] binaryFile) throws Exception {
        log.info(String.format("transferring file: %s to NAO...", fileName));
        try {
            //connect to ftp server(NAO)
            this.ftpClient.connect(this.ip, this.ftpPort);
            this.ftpClient.makeDirectory("roberta");
            this.ftpClient.changeWorkingDirectory("roberta");

            showServerReply();
            int replyCode = this.ftpClient.getReplyCode();

            //print error message if connection is not established
            if ( !FTPReply.isPositiveCompletion(replyCode) ) {
                log.warning("Operation failed. Server reply code: " + replyCode);
                return;
            }
            boolean success = this.ftpClient.login(this.username, this.password);
            showServerReply();

            if ( !success ) {
                log.warning("Could not login to the FTP server!");
                return;
            } else {

                //Use local passive mode to avoid problems with firewalls
                this.ftpClient.enterLocalPassiveMode();

                //store file on the robot
                InputStream input = new ByteArrayInputStream(binaryFile);
                this.ftpClient.storeFile(fileName, input);
                input.close();
            }
            //logout
            this.ftpClient.logout();
            log.info("file transferred");
        } catch ( ConnectException e ) {
            log.warning("There is no connection!");
            throw new Exception("No connection with the robot!");
        } catch ( IOException e ) {
            log.warning("There was an error during transfer:");
            e.printStackTrace();
        }
    }

    //abstract MyUserInfo needed for ssh connection.
    public abstract class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptYesNo(String str) {
            return false;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            return null;
        }
    }

    public void updateRobotInformation(String ip, String username, String password) {
        this.ip = ip;
        this.username = username;
        this.password = password;
    }
}
