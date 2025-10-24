package com.example;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.red5pro.license.LicenseManager;
import com.red5pro.override.ISideStream;
import com.red5pro.plugin.Red5ProPlugin;
import com.red5pro.whip.WhepSubscriber;

/**
 * Example Red5 Pro Plugin implementation.
 *
 * @author Paul Gregoire
 */
public class MyRed5ProPlugin extends Red5ProPlugin {

    private static Logger log = LoggerFactory.getLogger(MyRed5ProPlugin.class);

    public static final String NAME = "MyRed5ProPlugin";

    @Override
    public void doStartProPlugin(FileSystemXmlApplicationContext activationContext) throws IOException {
        log.debug("Start with activation context: {}", activationContext);
        // this call matches the name to node in the pom.xml jar plugin definition
        String mfversion = getManifestValue("MyRed5ProPlugin-Version");
        log.info("Starting MyRed5ProPlugin version {}", mfversion);
        // do your plugin startup logic here
    }

    @Override
    public void doStopProPlugin() throws Exception {
        log.info("Stop plugin");
        // do your plugin stop logic here

        // calling into super stops the executors
        super.doStopProPlugin();
    }

    @Override
    public String getName() {
        return MyRed5ProPlugin.NAME;
    }

    /**
     * Request a service switch for the given WHEP subscriber to another side stream.
     *
     * @param subscriber the WHEP subscriber
     * @param sideStream the target side stream
     * @param isImmediate whether the switch should be immediate
     * @return true if successful, false otherwise
     */
    public boolean requestSwitchService(WhepSubscriber subscriber, ISideStream sideStream, boolean isImmediate) {
        log.debug("requestSwitchService - subscriber: {} switch to {} immediate: {}", subscriber, sideStream, isImmediate);
        if (subscriber != null) {
            log.debug("client: {} switch to {}", subscriber.getId(), sideStream.getId());
            return subscriber.requestService(sideStream, isImmediate);
        }
        return false;
    }

    /** {@link Red5ProPlugin#submit(Runnable) task} */
    public static Future<?> submit(Runnable task) {
        Red5ProPlugin self = LicenseManager.getInstance().getPlugin(NAME);
        return self != null ? self.submitTask(task) : null;
    }

    /** {@link Red5ProPlugin#schedule(Runnable, long, long) schedule} */
    public static Future<?> schedule(Runnable task, long initialDelay, long delay) {
        Red5ProPlugin self = LicenseManager.getInstance().getPlugin(NAME);
        return self != null ? self.scheduleTask(task, initialDelay, delay) : null;
    }

}
