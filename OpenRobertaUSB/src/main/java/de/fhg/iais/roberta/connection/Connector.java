package de.fhg.iais.roberta.connection;

/**
 * Defines a set of states, keywords and methods for handling the USB connection of a robot to the Open Roberta server. This interface is intended to be
 * implemented by all connector classes for different robot types.
 *
 * @author dpyka
 */
public interface Connector {

    public enum State {
        DISCOVER,
        RECONNECT,
        WAIT_FOR_CONNECT_BUTTON_PRESS,
        CONNECT_BUTTON_IS_PRESSED,
        WAIT_FOR_CMD,
        WAIT_EXECUTION,
        DISCONNECT,
        WAIT_FOR_SERVER,
        UPDATE,
        UPDATE_SUCCESS,
        UPDATE_FAIL,
        ERROR_HTTP,
        ERROR_UPDATE,
        ERROR_BRICK,
        ERROR_DOWNLOAD,
        TOKEN_TIMEOUT
    }

    static final String KEY_TOKEN = "token";
    public static final String KEY_CMD = "cmd";

    static final String CMD_REGISTER = "register";
    static final String CMD_PUSH = "push";
    static final String CMD_ISRUNNING = "isrunning";

    static final String CMD_REPEAT = "repeat";
    static final String CMD_ABORT = "abort";
    static final String CMD_UPDATE = "update";
    static final String CMD_DOWNLOAD = "download";
    static final String CMD_DOWNLOAD_RUN = "download_run";
    static final String CMD_CONFIGURATION = "configuration";

    /**
     * Search for a specific robot type for auto detection at the beginning of the program. The robot is considered to not run a user program at this time to be
     * available.
     *
     * @return true if a robot is connected, false otherwise
     */
    public boolean findRobot();

    /**
     * Tell the connector to collect necessary data from the robot and initialise a registration to Open Roberta.
     */
    public void userPressConnectButton();

    /**
     * Disconnect the current robot properly and search for robots again (start condition of the USB program).
     */
    public void userPressDisconnectButton();

    /**
     * Shut down the connector for closing the USB program.
     */
    public void close();

    /**
     * Tell the gui, that the connector state has changed.
     *
     * @param state
     */
    public void notifyConnectionStateChanged(State state);

    /**
     * Get the token to display in the gui.
     *
     * @return
     */
    public String getToken();

    /**
     * Get the robot name to display in the gui.
     *
     * @return robot name
     */
    public String getBrickName();

    /**
     * In this state, the connector will download system libraries from the server, and upload it to the robot.
     */
    public void update();

    /**
     * Update the server communicator's address to which it will connect.
     *
     * @param customServerAddress
     */
    public void updateCustomServerAddress(String customServerAddress);

    /**
     * If gui fields are empty but advanced options is checked, use the default server address.
     */
    public void resetToDefaultServerAddress();

    public void setRobotIp(String ip);

    public void setRobotUserName(String userName);

    public void setRobotPassword(String password);
}
