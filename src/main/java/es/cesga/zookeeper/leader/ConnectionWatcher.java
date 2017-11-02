package es.cesga.zookeeper.leader;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Establishes a connection to the Zookeeper and watches if there are changes in it
 */
public class ConnectionWatcher implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionWatcher.class);

    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private String hosts;
    private int sessionTimeout = 5000;

    public ConnectionWatcher(String hosts) {
        this.hosts = hosts;
    }

    public ConnectionWatcher(String hosts, int sessionTimeout) {
        this.hosts = hosts;
        this.sessionTimeout = sessionTimeout;
    }

    public ZooKeeper connect() throws IOException, InterruptedException {
        logger.info("Establishing connection to zookeeper");
        zk = new ZooKeeper(hosts, sessionTimeout, this);
        connectedSignal.await();
        logger.info("Connected to zookeeper");
        return zk;
    }

    public void close() throws InterruptedException {
        zk.close();
    }

    public ZooKeeper getZooKeeper() {
        return zk;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
    }
}
