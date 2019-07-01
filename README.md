# Leader Election Service
Distributed fault-tolerant cron based on Zookeeper.

## How it works
You configure a set of hosts as part of the distributed cron.

One host acts as the master and it is the only one running the cron jobs. If the master fails other host
takes the leadership.

## Installation

### Compile and distribute service jar
Generate the required jar using maven:

    git clone https://github.com/javicacheiro/distributed-cron.git
    mvn package

Copy the generated jar file: target/leaderElection-0.1.0-jar-with-dependencies.jar to the hosts that will run the service.

### Start the service
For old systems you can use supervisord to run and monitor the service. A template configuration is provided
in scripts/supervisord.conf. Just set the zookeeper location and the paths and it should be ready to run.
Then simply execute a reload to start the new service:

    supervisorctl reload
    supervisorctl status

For new systems systemd is a good option.

### Copy the run and `sync_cron` helper scripts
Edit the `scripts/run` and `scripts/sync_cron` helper scripts and set the zookeeper location and other required vars.

Copy them to the target hosts and, for easier use, place them under some directory that is in the
PATH (eg. /usr/local/bin).

## Usage
Just prepend `run` to the command in the cron job definition:

    PATH=/sbin:/usr/sbin:/usr/local/sbin:/bin:/usr/bin:/usr/local/bin

    * * * * * run echo hello

and the command will run just in one host (the current leader of the service).

To synchronize the changes across all cron service hosts you can use:

    sync_cron

## Generic leader election service
The leader election service can also be used in a generic way for other services similar to cron:

- Join a pool of services providing a given service:

    java -jar leaderElection.jar --server <zookeeper_servers> --name <service_name> --join

- Verify if current host is the leader:

    java -jar leaderElection.jar --server <zookeeper_servers> --name <service_name> --is-leader

## Debugging
Using zookeeper-client (wrapper over zkCli.sh) to debug:

    zookeeper-client -server <zookeeper_servers>
    >> ls /locks/<service_name>
    >> get /masters/<service_name>

## Monitoring active servers
To monitor the number of active servers you can run the following command:

    zookeeper-client ls /locks/<service_name> | tail -n1 | wc -w
