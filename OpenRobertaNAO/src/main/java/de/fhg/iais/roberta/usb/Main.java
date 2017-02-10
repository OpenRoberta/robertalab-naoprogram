package de.fhg.iais.roberta.usb;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.lang3.SystemUtils;

import de.fhg.iais.roberta.connection.NAOConnector;
import de.fhg.iais.roberta.ui.ConnectionView;
import de.fhg.iais.roberta.ui.UIController;
import de.fhg.iais.roberta.util.ORAFormatter;

public class Main {

    private static final String LOGFILENAME = "OpenRobertaNAO.log";
    private static Logger log = Logger.getLogger("Connector");
    private static ConsoleHandler consoleHandler = new ConsoleHandler();
    private static FileHandler fileHandler = null;

    private static File logFile = null;

    private static NAOConnector naoConnector = null;

    private static ConnectionView view = null;
    private static UIController<?> controller = null;

    private static boolean startupFinish = false;

    public static void main(String[] args) {

        configureLogger();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                prepareUI();
                ResourceBundle messages = getLocals();
                ResourceBundle serverProps = getServerProps();
                naoConnector = new NAOConnector(serverProps);
                view = new ConnectionView(messages);
                controller = new UIController<Object>(view, messages);

                startupFinish = true;
            }

            private void prepareUI() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
                UIManager.put("MenuBar.background", Color.white);
                UIManager.put("Menu.background", Color.white);
                UIManager.put("Menu.selectionBackground", Color.decode("#afca04"));
                UIManager.put("MenuItem.background", Color.white);
                UIManager.put("MenuItem.selectionBackground", Color.decode("#afca04"));
                UIManager.put("MenuItem.focus", Color.decode("#afca04"));
                UIManager.put("Menu.foreground", Color.decode("#333333"));
                UIManager.put("Menu.Item.foreground", Color.decode("#333333"));
                UIManager.put("Menu.font", new Font("Arial", Font.PLAIN, 12));
                UIManager.put("MenuItem.foreground", Color.decode("#333333"));
                UIManager.put("MenuItem.font", new Font("Arial", Font.PLAIN, 12));
            }

            private ResourceBundle getServerProps() {
                return ResourceBundle.getBundle("OpenRobertaNAO");
            }

            private ResourceBundle getLocals() {
                ResourceBundle rb;
                try {
                    rb = ResourceBundle.getBundle("messages", Locale.getDefault());
                } catch ( Exception e ) {
                    rb = ResourceBundle.getBundle("messages", Locale.ENGLISH);
                }
                log.config("Language " + rb.getLocale());
                return rb;
            }
        });

        Thread t = null;
        while ( true ) {
            if ( startupFinish ) {

                if ( naoConnector.findRobot() ) {
                    log.info("NAO found!");
                    controller.setConnector(naoConnector);
                    t = new Thread(naoConnector);
                    t.start();
                    break;
                }
            } else {
                try {
                    Thread.sleep(200);
                } catch ( InterruptedException e ) {
                    // ok
                }
            }
        }
    }

    /**
     * Flush and close the file handler before closing the USB program.
     */
    public static void stopFileLogger() {
        fileHandler.flush();
        fileHandler.close();
    }

    /**
     * Set up a file handler for writing a log file to either %APPDATA% on windows, or user.home on linux or mac. The USB program will log all important actions
     * and events.
     */
    private static void configureLogger() {
        String path = "";
        try {
            if ( SystemUtils.IS_OS_WINDOWS ) {
                path = System.getenv("APPDATA");
            } else if ( SystemUtils.IS_OS_LINUX ) {
                path = System.getProperty("user.home");
            } else if ( SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ) {
                path = System.getProperty("user.home");
            }
            logFile = new File(path, "OpenRobertaNAO");
            if ( !logFile.exists() ) {
                logFile.mkdir();
            }
            fileHandler = new FileHandler(new File(logFile, LOGFILENAME).getPath(), false);
            fileHandler.setFormatter(new ORAFormatter());
            fileHandler.setLevel(Level.ALL);
        } catch ( Exception e ) {
            // ok
        }
        consoleHandler.setFormatter(new ORAFormatter());
        consoleHandler.setLevel(Level.ALL);
        log.setLevel(Level.ALL);
        log.addHandler(consoleHandler);
        log.addHandler(fileHandler);
        log.setUseParentHandlers(false);
        log.info("Logging to file: " + new File(logFile, LOGFILENAME).getPath().toString());
    }
}
