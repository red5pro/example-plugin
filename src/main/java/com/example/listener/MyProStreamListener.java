package com.example.listener;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;

import org.red5.io.flv.meta.MetaData;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.MyRed5ProPlugin;
import com.red5pro.cluster.streams.Provision;
import com.red5pro.io.GenericFileWriter;
import com.red5pro.media.MediaFile;
import com.red5pro.override.IProStream;
import com.red5pro.override.ProStream;
import com.red5pro.override.api.ProStreamTerminationEventListener;
import com.red5pro.restreamer.plugin.RestreamerPlugin;
import com.red5pro.server.stream.ProStreamService;
import com.red5pro.server.stream.Red5ProIO;
import com.red5pro.util.ScopeUtil;

/**
 * A custom stream listener for handling ProStream packets.
 *
 * @author Paul Gregoire
 */
public class MyProStreamListener implements IStreamListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    // this is used to queue up packets that are received from the ProStream
    private LinkedTransferQueue<IStreamPacket> packetQueue = new LinkedTransferQueue<>();

    // a/v file dumpers for debugging
    private GenericFileWriter audioDumpWriter, videoDumpWriter;

    // worker future for processing the packet queue
    private Future<?> queueProcessorFuture;

    // time for the latest incoming data
    private volatile long lastReceiveTime;

    // provision reference
    private Provision provision;

    // stream name for identification
    private String streamName;

    // scope reference
    private IScope scope;

    public MyProStreamListener(Provision provision, boolean dumpAV) {
        // store the provision
        this.provision = provision;
        // get context path or default to live
        String contextPath = provision.getContextPath();
        logger.debug("Context path: {} stream name: {}", contextPath, streamName);
        // store the scope
        scope = ScopeUtil.resolveScope(RestreamerPlugin.getGlobalScope(), contextPath);
        // stream name for file naming etc
        streamName = provision.getStreamName();
        // set up a/v dumpers if needed
        if (dumpAV) {
            // get a temp dir
            String tempDir = System.getProperty("java.io.tmpdir", "/tmp");
            audioDumpWriter = new GenericFileWriter(MediaFile.TYPE_AAC, tempDir, streamName + "_audio_dump");
            videoDumpWriter = new GenericFileWriter(MediaFile.TYPE_H264, tempDir, streamName + "_video_dump");
        }
    }

    public MyProStreamListener(IScope scope, String streamName, boolean dumpAV) {
        // get context path or default to live
        String contextPath = scope.getContextPath();
        logger.debug("Context path: {} stream name: {}", contextPath, streamName);
        // store the scope
        this.scope = scope;
        // stream name for file naming etc
        this.streamName = streamName;
        // set up a/v dumpers if needed
        if (dumpAV) {
            // get a temp dir
            String tempDir = System.getProperty("java.io.tmpdir", "/tmp");
            audioDumpWriter = new GenericFileWriter(MediaFile.TYPE_AAC, tempDir, streamName + "_audio_dump");
            videoDumpWriter = new GenericFileWriter(MediaFile.TYPE_H264, tempDir, streamName + "_video_dump");
        }
    }

    public void start() {
        logger.debug("Starting MyProStreamListener");
        // get the prostream
        ProStream proStream = ProStreamService.getProStream(scope, streamName);
        if (proStream != null) {
            // add listener to the pro stream
            proStream.addStreamListener(this);
            // create a queue processor
            queueProcessorFuture = MyRed5ProPlugin.submit(() -> {
                logger.debug("Starting packet queue processor for stream: {}", streamName);
                while (true) {
                    try {
                        // take packet from the queue, blocking
                        IStreamPacket packet = packetQueue.take();
                        if (packet != null) {
                            // process the packet
                            logger.trace("Processing packet from queue: {}", packet);
                        }
                    } catch (InterruptedException e) {
                        logger.debug("Packet queue processor interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            // add termination listener to handle cleanup
            proStream.addTerminationEventListener(new ProStreamTerminationEventListener() {

                @Override
                public void streamStopped(IProStream stream) {
                    logger.debug("ProStream termination event received");
                    // remove our listener
                    stream.removeStreamListener(MyProStreamListener.this);
                    // remove termination listener
                    ((ProStream) stream).removeTerminationEventListener(this);
                    // check packet queue before stopping
                    if (packetQueue != null && !packetQueue.isEmpty()) {
                        // wait for packets to be processed, rough assumption using video timing
                        try {
                            long waitTime = Math.min(10000L, packetQueue.size() * 16L);
                            logger.debug("Waiting {}ms for queued packets to be processed...", waitTime);
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            logger.debug("Interrupted while waiting for queued packets to be processed", e);
                        }
                    }
                    try {
                        stop();
                    } catch (Exception e) {
                        logger.warn("Exception stopping", e);
                    }
                }

            });
        }

    }

    public void stop() {
        logger.debug("Stopping MyProStreamListener");
        // remove our listener from the pro stream
        ProStream sourceStream = RestreamerPlugin.findStream(provision);
        if (sourceStream != null) {
            logger.trace("Stop forwarding guid: {} stream {}", provision.getGuid(), provision.getStreamName());
            sourceStream.removeStreamListener(this);
        }
        // clean up resources
        if (audioDumpWriter != null) {
            audioDumpWriter.close();
            audioDumpWriter = null;
        }
        if (videoDumpWriter != null) {
            videoDumpWriter.close();
            videoDumpWriter = null;
        }
        // cancel the queue processor
        if (queueProcessorFuture != null) {
            queueProcessorFuture.cancel(true);
            queueProcessorFuture = null;
        }
        // clear the packet queue
        if (packetQueue != null) {
            logger.debug("Caller clearing packet queue");
            packetQueue.clear();
            packetQueue = null;
        }
    }

    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        // this is called when a packet is received from the ProStream
        logger.trace("Packet received - packet: {}", packet);
        if (packet != null) {
            // update the last receive time
            lastReceiveTime = System.currentTimeMillis();
            // type of packet
            byte dataType = packet.getDataType();
            logger.debug("Packet data type: {}", dataType);
            // add the packet to the queue
            if (packetQueue != null) {
                try {
                    // copy the packet to avoid issues with the ProStream or other consumers
                    final IStreamPacket copy = Red5ProIO.copy(packet);
                    // offer to the queue, non-blocking
                    packetQueue.offer(copy);
                    // dump a/v if needed
                    if (audioDumpWriter != null && packet instanceof AudioData) {
                        logger.debug("Audio packet received: {}", packet);
                        Thread.ofVirtual().start(() -> {
                            try {
                                audioDumpWriter.write(copy.getData().array());
                            } catch (Exception e) {
                                logger.warn("Exception writing audio packet to dump", e);
                            }
                        });
                    } else if (videoDumpWriter != null && packet instanceof VideoData) {
                        logger.debug("Video packet received: {}", packet);
                        Thread.ofVirtual().start(() -> {
                            try {
                                videoDumpWriter.write(copy.getData().array());
                            } catch (Exception e) {
                                logger.warn("Exception writing video packet to dump", e);
                            }
                        });
                    } else if (packet instanceof MetaData) {
                        // handle metadata data if needed
                        logger.debug("Metadata packet received: {}", packet);
                    }
                } catch (Exception e) {
                    logger.warn("Exception copying packet", e);
                }
            }
        }
    }

    public LinkedTransferQueue<IStreamPacket> getPacketQueue() {
        return packetQueue;
    }

    public long getLastReceiveTime() {
        return lastReceiveTime;
    }

}
