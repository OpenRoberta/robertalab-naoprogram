package de.fhg.iais.roberta.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshConnection {
    private static Logger log = Logger.getLogger("Connector");
    private final Properties config = new java.util.Properties();
    private final JSch jSch = new JSch();
    private String host;
    private int port;
    private String username;
    private String password;
    private Session session;

    public SshConnection(String host, int port, String username, String password) throws JSchException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        this.config.put("StrictHostKeyChecking", "no");

    }

    public void createSession() throws JSchException {
        this.session = this.jSch.getSession(this.username, this.host, this.port);
        this.session.setConfig(this.config);
        this.session.setPassword(this.password);

    }

    public void updateRobotInfo(String host, int port, String username, String password) throws JSchException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        createSession();
    }

    /**
     * create an ssh command. If not successful, throw an exception
     * @throws InterruptedException
     * @throws IOException
     * @throws JSchException
     * @throws
     */
    public String command(String command) throws JSchException, IOException, InterruptedException {
        ChannelExec channel = null;
        String msg = null;
        try {
            this.connect();
            channel = (ChannelExec) this.session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getExtInputStream();
            channel.connect();
            msg = logChanngelMessages(channel, in, err);
        } finally {
            try {
                if ( channel != null ) {
                    channel.disconnect();
                }
            } catch ( Exception e ) {
                // OK
            }
        }
        return msg;
    }

    /**
     * copy local file to remote. If not successful, throw an exception
     *
     * @throws JSchException
     * @throws IOException
     */
    public void copyLocalToRemote(byte[] content, String to, String fileName) throws Exception {
        ChannelExec channel = null;
        InputStream fis = null;
        try {
            String command = "scp -p -t " + to;
            channel = (ChannelExec) this.session.openChannel("exec");
            channel.setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();
            checkAck(in);

            long filesize = content.length;
            command = "C0644 " + filesize + " " + fileName + "\n";
            out.write(command.getBytes());
            out.flush();
            checkAck(in);
            fis = new ByteArrayInputStream(content);
            sendFileContent(fis, out);
            checkAck(in);
        } finally {
            try {
                if ( channel != null ) {
                    channel.disconnect();
                }
            } catch ( Exception e ) {
                // OK
            }
            try {
                if ( fis != null ) {
                    fis.close();
                }
            } catch ( Exception e ) {
                // OK
            }
        }
    }

    /**
     * copy local file to remote. If not successful, throw an exception
     */
    public void copyLocalToRemote(String from, String to, String fileName) throws Exception {
        ChannelExec channel = null;
        InputStream fis = null;
        try {
            this.connect();
            from = from + "/" + fileName;
            File _lfile = new File(from);
            fis = new FileInputStream(from);

            String command = "scp -p -t " + to;
            channel = (ChannelExec) this.session.openChannel("exec");
            channel.setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            setLastModified(out, _lfile);
            checkAck(in);
            setFileSize(from, _lfile, out);
            checkAck(in);
            sendFileContent(fis, out);
            checkAck(in);

            out.close();
            in.close();
        } finally {
            try {
                if ( channel != null ) {
                    channel.disconnect();
                }
            } catch ( Exception e ) {
                // OK
            }
            try {
                if ( fis != null ) {
                    fis.close();
                }
            } catch ( Exception e ) {
                // OK
            }
        }
    }

    private String logChanngelMessages(ChannelExec channel, InputStream in, InputStream err) throws InterruptedException, IOException {
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();
        byte[] tmp = new byte[1024];
        while ( true ) {
            readStream(in, outputBuffer, tmp);
            readStream(err, errorBuffer, tmp);
            if ( channel.isClosed() ) {
                if ( (in.available() > 0) || (err.available() > 0) ) {
                    continue;
                }
                if ( channel.getExitStatus() == 0 ) {
                    return outputBuffer.toString();
                } else {
                    log.info(errorBuffer.toString());
                    return "";
                }

            }
            Thread.sleep(1000);
        }
    }

    private void readStream(InputStream in, StringBuilder outputBuffer, byte[] tmp) throws IOException {
        while ( in.available() > 0 ) {
            int i = in.read(tmp, 0, 1024);
            if ( i < 0 ) {
                break;
            }
            outputBuffer.append(new String(tmp, 0, i));
        }
    }

    private void sendFileContent(InputStream fis, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        while ( true ) {
            int len = fis.read(buf, 0, buf.length);
            if ( len <= 0 ) {
                break;
            }
            out.write(buf, 0, len);
        }

        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
    }

    private void setFileSize(String from, File _lfile, OutputStream out) throws IOException {
        String command;
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if ( from.lastIndexOf('/') > 0 ) {
            command += from.substring(from.lastIndexOf('/') + 1);
        } else {
            command += from;
        }
        command += "\n";
        out.write(command.getBytes());
        out.flush();
    }

    private void setLastModified(OutputStream out, File _lfile) throws IOException {
        String command;
        command = "T" + _lfile.lastModified() / 1000 + " 0";
        command += " " + _lfile.lastModified() / 1000 + " 0\n";
        out.write(command.getBytes());
        out.flush();
    }

    /**
     * check the response from a channel. Return if ok; if an error occured, throw an exception
     *
     * @throws IOException
     * @throws Exception
     */
    private void checkAck(InputStream in) throws Exception {
        int b = in.read();
        // b may be 0 for success, 1 for error, 2 for fatal error, -1 for success (???)
        if ( b == 0 || b == -1 ) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        int c;
        do {
            c = in.read();
            sb.append((char) c);
        } while ( c != '\n' );
        throw new Exception("Error. code: " + b + " msg: " + sb.toString());
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public void disconnect() {
        if ( this.session != null && this.session.isConnected() ) {
            this.session.disconnect();
        }
    }

    public void connect() throws JSchException {
        if ( !this.session.isConnected() ) {
            this.session.connect(5000);
        }
    }
}
