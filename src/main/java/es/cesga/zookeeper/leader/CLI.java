package es.cesga.zookeeper.leader;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Command Line Interface: supports both participating in leader election
 * and checking if the current node is the leader
 */
public class CLI {
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    @Parameter(names = {"--server", "-s"}, description = "Zookeeper server addresses separated by commas", required = true)
    private String servers = "";
    @Parameter(names = {"--name", "-n"}, description = "Name of the service: eg. cron-ft2", required = true)
    private String name = "";
    @Parameter(names = {"--join", "-j"}, description = "Join the pool of servers of this service")
    private boolean join = false;
    @Parameter(names = {"--is-leader", "-i"}, description = "Check if this server is the leader")
    private boolean check = false;
    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help = false;

    public static void main(String[] args) throws IOException, InterruptedException {

        CLI cli = new CLI();
        JCommander cmd = JCommander.newBuilder().addObject(cli).build();
        cmd.parse(args);
        if (cli.help) {
            cmd.usage();
            System.exit(0);
        }

        ConnectionWatcher conn = new ConnectionWatcher(cli.servers);
        Master master = null;
        try {
            master = new Master(conn, cli.name);
        } catch (UnknownHostException e) {
            logger.error("Unable to resolve the hostname of the current server");
            System.exit(2);
        }

        if (cli.join) {
            master.start();
        } else if (cli.check) {
            if (master.isLeader()) {
                logger.info("This server is the service leader");
                System.exit(0);
            } else {
                logger.info("This server is NOT the service leader");
                System.exit(1);
            }
        }
    }
}
