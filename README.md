# Leader Election Service
Distributed fault-tolerant cron based on Zookeeper.

## How it works
You configure a set of hosts as part of the distributed cron.

One host acts as the master and it is the only one running the cron jobs. If the master fails other host
takes the leadership.

## Installation

### Compile and distribute jar
Generate the required jar using maven:

    git clone https://github.com/javicacheiro/distributed-cron.git
    mvn package

Copy the generated jar file: target/leaderElection-0.1.0-jar-with-dependencies.jar to the hosts that will run the service.

### Create required znodes
For the moment, you have to create the required base znodes in zookeeper:

    zookeeper-client -server <zookeeper_servers>
    >> create /locks lock_storage
    >> create /masters master_location

### Run service
For old systems you can use supervisord to run and monitor the service. A template configuration is provided
in scripts/supervisord.conf. Just set the zookeeper location and the paths and it should be ready to run.
Then simply execute a reload to start the new service:

    supervisorctl reload
    supervisorctl status

For new systems systemd is a good option.

### Copy the run helper script
Edit the run helper script and set the zookeeper location and other required vars.
Copy the `scripts/run` helper to the target hosts and, for easier use, place it under some directory that is in the
PATH (eg. /usr/local/bin).


## Usage
Just prepend `run` to the command in the cron job definition:

    * * * * * run echo hello

and the command will run just in one host (the current leader of the service).

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


