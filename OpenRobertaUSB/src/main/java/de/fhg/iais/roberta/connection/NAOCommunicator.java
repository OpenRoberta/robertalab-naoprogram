package de.fhg.iais.roberta.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import lejos.pc.comm.NXTCommException;

public class NAOCommunicator {

    private static Logger log = Logger.getLogger("NAOCommunicator");
    private final String ip;
    private final String username;
    private final String password;

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

    public void uploadFile(byte[] binaryfile, String nxtFileName) throws IOException, NXTCommException {
        File file = new File(nxtFileName);
        FileOutputStream fs = new FileOutputStream(file);
        fs.write(binaryfile);
        fs.flush();
        fs.close();
        log.info("File downloaded and saved.");
        ftpTransfer(nxtFileName);
        sshCommand("python " + nxtFileName);
        sshCommand("rm " + nxtFileName);
    }

    private void sshCommand(String command) {

        //sshport on the robot
        int port = 22;

        //implement the abstract class MyUserInfo
        UserInfo ui = new MyUserInfo() {
        };

        try {
            Session session = this.jsch.getSession(this.username, this.ip, port);
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

    private void ftpTransfer(String file) {
        int port = 21;
        log.info("transferring file to NAO...");
        try {
            //connect to ftp server(NAO)
            this.ftpClient.connect(this.ip, port);

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
                InputStream input = new FileInputStream(file);
                this.ftpClient.storeFile(file, input);
                input.close();
            }
            //logout
            this.ftpClient.logout();
            log.info("file transferred");
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
}
