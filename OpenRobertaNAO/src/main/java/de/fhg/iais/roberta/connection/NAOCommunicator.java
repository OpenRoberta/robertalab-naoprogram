package de.fhg.iais.roberta.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import de.fhg.iais.roberta.util.Key;

public class NAOCommunicator {

    private static final Logger LOG = LoggerFactory.getLogger(NAOCommunicator.class);
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
        //        this.ftpClient.setConnectTimeout(20000);

    }

    public String getIp() {
        return this.ip;
    }

    public Key uploadFile(byte[] binaryfile, String fileName) throws Exception {
        // LOG.info("Robot IP: " + this.ip + "  Username: " + this.username + "  Password: " + this.password);
        Key key;
        List<String> fileNames = new ArrayList<String>();
        fileNames.add("roberta/__init__.py");
        fileNames.add("roberta/blockly_methods.py");
        fileNames.add("roberta/original_hal.py");
        fileNames.add("roberta/speech_recognition_module.py");
        byte[] fileContents;
        try {
            for ( String fname : fileNames ) {
                fileContents = getFileFromResources(fname);
                key = ftpTransfer(fname, fileContents);
            }
            key = ftpTransfer(fileName, binaryfile);
            key = sshCommand("python " + fileName);
            key = sshCommand("rm " + fileName);
            key = sshCommand("rm -r roberta");
        } catch ( Exception e ) {
            key = Key.NAO_PROGRAM_UPLOAD_ERROR_CONNECTION_NOT_ESTABLISHED;
        }
        return key;
    }

    private byte[] getFileFromResources(String fileName) {
        try {

            String input = "";
            Scanner scanner = new Scanner(NAOCommunicator.class.getClassLoader().getResourceAsStream(fileName));
            while ( scanner.hasNextLine() ) {
                input += scanner.nextLine() + '\n';
            }
            scanner.close();
            //replace the line
            //System.out.println(this.ip);
            //            input = input.replace("self.NAO_IP = \"\"", "self.NAO_IP = \"" + this.ip + "\"");
            return input.getBytes();

        } catch ( Exception e ) {
            System.out.println(e.getMessage());
            System.out.println("Problem reading or writing " + fileName + " file.");
        }
        return null;

    }

    private Key sshCommand(String command) {

        //implement the abstract class MyUserInfo
        UserInfo ui = new MyUserInfo();

        try {
            Session session = this.jsch.getSession(this.username, this.ip, this.sshPort);
            session.setPassword(this.password);
            session.setUserInfo(ui);
            session.setTimeout(50000);

            //fingerprint is not accepted
            //uncomment this part if key fingerprint is not accepted
            Properties config = new Properties();
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
                    //System.out.print(new String(tmp, 0, i));
                }
                if ( channel.isClosed() ) {
                    if ( in.available() > 0 ) {
                        continue;
                    }
                    // LOG.info("exit-status: " + channel.getExitStatus());
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
            return Key.NAO_PROGRAM_UPLOAD_SUCCESSFUL;
        } catch ( Exception e ) {
            System.out.println(e);
            return Key.NAO_PROGRAM_UPLOAD_ERROR_SSH_CONNECTION;
        }
    }

    private void showServerReply() {

        String[] replies = this.ftpClient.getReplyStrings();

        if ( replies != null && replies.length > 0 ) {
            for ( String aReply : replies ) {
                LOG.info("NAO-SERVER: " + aReply);
            }
        }
    }

    private Key ftpTransfer(String fileName, byte[] binaryFile) throws Exception {
        LOG.info(String.format("transferring file: %s to NAO...", fileName));
        try {
            //connect to ftp server(NAO)
            this.ftpClient.connect(this.ip, this.ftpPort);
            boolean success = this.ftpClient.login(this.username, this.password);
            FTPFile[] directories = this.ftpClient.listDirectories();
            boolean isRobertaDir = false;
            for ( FTPFile dir : directories ) {
                if ( dir.getName().equals("roberta") ) {
                    isRobertaDir = true;
                }
            }
            if ( !isRobertaDir ) {
                this.ftpClient.makeDirectory("roberta");
                //            this.ftpClient.changeWorkingDirectory("roberta");
            }

            showServerReply();
            int replyCode = this.ftpClient.getReplyCode();

            //print error message if connection is not established
            if ( !FTPReply.isPositiveCompletion(replyCode) ) {
                LOG.error("Operation failed. Server reply code: " + replyCode);
                return Key.NAO_PROGRAM_UPLOAD_ERROR_CONNECTION_NOT_ESTABLISHED;
            }
            showServerReply();

            if ( !success ) {
                LOG.error("Could not login to the FTP server!");
                return Key.NAO_PROGRAM_UPLOAD_ERROR_FTP_LOGIN_FAILD;
            } else {

                //Use local passive mode to avoid problems with firewalls
                this.ftpClient.enterLocalPassiveMode();

                //store file on the robot
                InputStream input = new ByteArrayInputStream(binaryFile);
                this.ftpClient.storeFile(fileName, input);
                System.out.println(this.ftpClient.getReplyCode());
                input.close();
            }

            //logout
            this.ftpClient.logout();
            LOG.info("file transferred");
            return Key.NAO_PROGRAM_UPLOAD_SUCCESSFUL;
        } catch ( ConnectException e ) {
            LOG.error("There is no connection!");
            return Key.NAO_PROGRAM_UPLOAD_ERROR_CONNECTION_NOT_ESTABLISHED;
        } catch ( IOException e ) {
            LOG.error("There was an error during transfer:");
            e.printStackTrace();
            return Key.NAO_PROGRAM_UPLOAD_ERROR_IO;
        }
    }

    //abstract MyUserInfo needed for ssh connection.
    public class MyUserInfo implements UserInfo, UIKeyboardInteractive {
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
