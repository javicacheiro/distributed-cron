package es.cesga.zookeeper.leader;

import es.cesga.zookeeper.leader.locking.LockListener;
import es.cesga.zookeeper.leader.locking.WriteLock;
import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * This class represents a host in a given service pool.
 * It tries to obtain service leadership and keeps listening for changes
 * so it can take control if it is necessary.
 *
 * You start the process of joining a given service pool by invoking {@link #start()}
 * You can also check if this host is the leader by invoking {@link #isLeader()}
 */
public class Master {

    private static final Logger logger = LoggerFactory.getLogger(Master.class);
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String LOCK_BASE_PATH = "/locks";
    private static final String MASTER_BASE_PATH = "/masters";
    private static String hostname;
    private final String groupName;
    private ConnectionWatcher connection;
    private WriteLock lock;
    private ZooKeeper zk;

    /**
     * Creates a new service instance belonging to the given groupName making the current host eligible for leadership
     * @param connection A connection to the Zookeeper
     * @param groupName The name of the group of this service
     */
    public Master(ConnectionWatcher connection, String groupName) throws UnknownHostException {
        this.hostname = obtainHostname();
        this.connection = connection;
        this.groupName = groupName;
    }

    private static String obtainHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    private String getLockPath() {
        return LOCK_BASE_PATH + "/" + groupName;
    }

    private String getMasterPath() {
        return MASTER_BASE_PATH + "/" + groupName;
    }

    /**
     * Start the service instance waiting to obtain leadership
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {
        zk = connection.connect();

        createPath(MASTER_BASE_PATH, "master_location");
        createPath(LOCK_BASE_PATH, "lock_management");

        logger.info("Trying to obtain the lock to be the service leader");
        lock = new WriteLock(zk, getLockPath(), Ids.OPEN_ACL_UNSAFE, new LeaderChangeListener());

        try {
            if (lock.lock()) {
                logger.info("We are now the leader of the service");
            }
        } catch (KeeperException e) {
            logger.error("Zookeeper returned error while trying to obtain the lock for the service.");
            lock.unlock();
            System.exit(2);
        } catch (InterruptedException e) {
            logger.error("Interrupted while trying to obtain the lock for the service.");
            lock.unlock();
            System.exit(2);
        }

        registerShutdownHook();

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            logger.info("Stopping");
            logger.info("Releasing lock");
            lock.unlock();
            try {
                logger.info("Closing connection to zookeeper");
                connection.close();
            } catch (InterruptedException e2) {
                logger.error("Interrupted while closing connection to zookeeper");
            }
        }
    }

    private void createPath(String path, String value) {
        try {
            Stat stat = zk.exists(path, false);
            if (stat == null) {
                logger.info("Creating master base path znode: {}", path);
                zk.create(path, value.getBytes(CHARSET), Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            logger.error("Unable to create znode: {}", path);
            System.exit(2);
        } catch (InterruptedException e) {
            logger.error("Interrupted while creating znode: {}", path);
            System.exit(2);
        }
    }

    /**
     * Check if the current host is the leader of the service
     * @return if this host is the leader of not
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean isLeader() throws IOException, InterruptedException {
        if (zk == null) {
            zk = connection.connect();
        }
        String host;
        byte[] value;
        try {
            value = zk.getData(getMasterPath(), false, null);
            host = new String(value, CHARSET);
            logger.info("Current service leader: " + host);
        } catch (KeeperException e) {
            return false;
        }

        if (host.equals(obtainHostname())) {
            return true;
        } else {
            return false;
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping");
            logger.info("Releasing lock");
            lock.unlock();
            logger.info("Closing connection to zookeeper");
            try {
                connection.close();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while closing connection to zookeeper");
            }
        }));
    }

    private class LeaderChangeListener implements LockListener {
        @Override
        public void lockAcquired() {
            logger.info("Lock acquired");
            String masterPath = getMasterPath();
            try {
                Stat stat = zk.exists(masterPath, false);
                if (stat == null) {
                    logger.info("Creating master znode: " + masterPath + " with value " + hostname);
                    zk.create(masterPath, hostname.getBytes(CHARSET), Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                } else {
                    logger.info("Updating master name to: " + hostname);
                    zk.setData(masterPath, hostname.getBytes(CHARSET), -1);
                }
            } catch (KeeperException e) {
                logger.error("Error while writing master name to zookeeper");
                lock.unlock();
                System.exit(2);
            } catch (InterruptedException e) {
                logger.error("Interrupted while writing master name to zookeeper");
                lock.unlock();
                System.exit(2);
            }
        }

        @Override
        public void lockReleased() {
            logger.info("Lock released");
        }
    }
}





