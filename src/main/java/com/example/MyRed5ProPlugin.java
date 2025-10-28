package com.example;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.listeners.ScopeListenerAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.example.listener.MyProStreamListener;
import com.red5pro.license.LicenseManager;
import com.red5pro.override.IProStream;
import com.red5pro.override.ISideStream;
import com.red5pro.override.ProStream;
import com.red5pro.override.api.ProStreamTerminationEventListener;
import com.red5pro.plugin.Red5ProPlugin;
import com.red5pro.server.stream.ProStreamService;
import com.red5pro.whip.WhepSubscriber;

/**
 * Example Red5 Pro Plugin implementation.
 *
 * @author Paul Gregoire
 */
public class MyRed5ProPlugin extends Red5ProPlugin {

    private static Logger log = LoggerFactory.getLogger(MyRed5ProPlugin.class);

    public static final String NAME = "MyRed5ProPlugin";

    private ScopeListenerAdapter scopeListener;

    private PublishAlertHandler publishAlertHandler = new PublishAlertHandler(this);

    private Set<String> activePublishers = new HashSet<>();

    @Override
    public void doStartProPlugin(FileSystemXmlApplicationContext activationContext) throws IOException {
        log.debug("Start with activation context: {}", activationContext);
        // this call matches the name to node in the pom.xml jar plugin definition
        String mfversion = getManifestValue("MyRed5ProPlugin-Version");
        log.info("Starting MyRed5ProPlugin version {}", mfversion);
        // add scope listener for creation and removal events
        scopeListener = new ScopeListenerAdapter() {

            @Override
            public void notifyScopeCreated(IScope scope) {
                log.debug("Scope created: {}", scope);
                ScopeType scopeType = scope.getType();
                // configure the websocket scopes
                if (scopeType == ScopeType.APPLICATION) {
                    log.debug("Adding services to application scope: {}", scope);
                    // TODO configure your application scope services etc here

                    // hook into publish create/start without needing to recreate the live app
                    MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
                    if (adapter != null) {
                        log.debug("Adding hook to fire when a publisher is being set up in scope: {}", scope.getName());
                        adapter.registerStreamPublishSecurity(publishAlertHandler);
                    }

                } else if (scopeType == ScopeType.ROOM) {
                    // TODO configure your room scope services etc here

                }
            }

            @Override
            public void notifyScopeRemoved(IScope scope) {
                log.trace("Scope removed: {}", scope);
                ScopeType scopeType = scope.getType();
                if (scopeType == ScopeType.APPLICATION) {
                    // TODO perform clean up for things added at create

                    // remove the publish hook
                    MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
                    if (adapter != null) {
                        log.debug("Removing publish hook on scope: {}", scope.getName());
                        adapter.unregisterStreamPublishSecurity(publishAlertHandler);
                    }

                } else if (scopeType == ScopeType.ROOM) {
                    // TODO perform clean up for things added at create

                }
            }

        };
        log.debug("Setting server scope listener");
        server.addListener(scopeListener);
        // do your plugin startup logic here


        log.info("Plugin started");
    }

    @Override
    public void doStopProPlugin() throws Exception {
        log.info("Stop plugin");
        // do your plugin stop logic here

        // calling into super stops the executors
        super.doStopProPlugin();
        log.info("Plugin stopped");
    }

    @Override
    public String getName() {
        return MyRed5ProPlugin.NAME;
    }

    public boolean addPublisher(IScope scope, String name) {
        log.debug("Adding publisher - scope: {} name: {}", scope.getName(), name);
        return activePublishers.add(scope.getName() + "/" + name);
    }

    public boolean removePublisher(IScope scope, String name) {
        log.debug("Removing publisher - scope: {} name: {}", scope.getName(), name);
        return activePublishers.remove(scope.getName() + "/" + name);
    }

    public boolean isPublisherActive(IScope scope, String name) {
        return activePublishers.contains(scope.getName() + "/" + name);
    }

    public Set<String> getActivePublishers() {
        return activePublishers;
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

    public class PublishAlertHandler implements IStreamPublishSecurity {

        private MyRed5ProPlugin plugin;

        public PublishAlertHandler(MyRed5ProPlugin plugin) {
            this.plugin = plugin;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isPublishAllowed(IScope scope, String name, String mode) {
            // fires when a publisher is being set up, so perform any processing on this new publish as needed
            log.debug("isPublishAllowed - scope: {} name: {} mode: {}", scope, name, mode);
            // create a task to attach a listener after publish starts
            submit(() -> {
                log.debug("Publish setup task executing for stream: {} in scope: {}", name, scope.getName());
                // attempts to attach the listener
                int attempts = 3;
                // whether we successfully attached the listener
                boolean attached = false;
                do {
                    ProStream proStream = ProStreamService.getProStream(scope, name);
                    if (proStream != null) {
                        proStream.addStreamListener(new MyProStreamListener(scope, name, true));
                        plugin.addPublisher(scope, name);
                        attached = true;
                        log.debug("Attached MyProStreamListener to ProStream: {} in scope: {}", name, scope.getName());
                        // add a termination listener to clean up when done
                        proStream.addTerminationEventListener(new ProStreamTerminationEventListener() {

                            @Override
                            public void streamStopped(IProStream stream) {
                                log.debug("ProStream stopped: {} in scope: {}", name, scope.getName());
                                // perform any clean up needed here
                                plugin.removePublisher(scope, name);
                            }
                            
                        });
                    } else {
                        try {
                            // wait a bit before trying again
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            log.warn("Interrupted while waiting to re-attempt ProStream fetch for stream: {} in scope: {}", name, scope.getName());
                        }
                    }
                } while (!attached && attempts-- > 0);
            });
            // we dont perform security checks here, let it pass
            return true;
        }

    }

}
