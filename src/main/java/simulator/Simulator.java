package simulator;

import java.io.BufferedReader;  
import java.io.File;
import java.io.FileReader;  
import java.io.FileWriter;
import java.io.IOException;
// import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;
import java.lang.Thread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import network.Address;
import network.Network;
import org.json.simple.JSONObject;
import utils.Config;
import utils.metric.*;

import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;

public class Simulator {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());

    private static final List<Address> addresses = new ArrayList<>();
    private static final HashMap<Address, Server> servers = new HashMap<>();

    private static void initialize(String[] args) {
        if (args.length != 1) {
            LOG.log(Level.SEVERE, "You should provide the config file as the first argument");
            System.exit(1);
        }

        LOG.log(Level.INFO, "Initializing...");
        Config.parse(args[0]); // this must be called before anything else

        // build ips
        for (int id = 0; id < Config.numServers; id++) {
            Address ip = new Address(id);
            addresses.add(ip);
            Server server = new Server(ip);
            servers.put(ip, server);
        }

        Network.initialize(addresses);
        EventService.initialize(servers);
        NetworkMetric.initialize(addresses);
    }


    private static void loadTrace(int num_nodes) {
        String line = "";  
        // String splitBy = ",";
        // long time;
        Long running_time = Long.valueOf(0);
        Long interarrival = running_time;
        int requester=0;
        //parsing a CSV file into BufferedReader class constructor
        Random rand = new Random();
        double lambda = Config.critDuration; 
    
        RandomDataGenerator randomData = new RandomDataGenerator(); // for weibull
        double shape = 2.0; // Weibull param
        double scale = 2*lambda / Math.sqrt(Math.PI); // THIS DEPENDS ON the shape
        // https://en.wikipedia.org/wiki/Particular_values_of_the_gamma_function

        ZipfDistribution zipfDistribution = new ZipfDistribution(num_nodes, 2.0);

        for (int i = 0; i < Config.concRequesters; ++i) {
            if (Config.timeDistro.equals("exponential")) {
                // exponential distribution
                interarrival = (long)(-Math.log(1 - rand.nextDouble()) * lambda);
            } else if (Config.timeDistro.equals("weibull")) {
                interarrival = (long)(randomData.nextWeibull(shape, scale));
            } else {
                System.out.println("UNKNOWN time DISTRIBUTION");
                System.exit(1);
            }
            running_time += (long)(interarrival*Config.irRatio); 

            if (Config.spatialDistro.equals("zipfian")) {
                requester = zipfDistribution.sample();
                requester--;
            } else if (Config.spatialDistro.equals("uniform")) {
                requester = rand.nextInt(num_nodes);
            } else {
                System.out.println("UNKNOWN spatial DISTRIBUTION");
                System.exit(1);
            }
            Address ip = new Address(requester);
            servers.get(ip).loadRequest(LogicalTime.time + running_time);
        }
    }

    private static void run() {
        LogicalTime.time = 0;
        int num_nodes = Config.numServers;        

        if (!Config.fromTrace) {
            int i = 0;
            for (Map.Entry<Address,Server> elem : servers.entrySet()) {
                // System.out.println("Initiating another request with i = " + String.valueOf(i));
                elem.getValue().initiateRequest();
                if (++i >= Config.concRequesters) break;
            }
        } else {
            // going to read in from the tracefile
            loadTrace(num_nodes);
        }

        if (Config.useChurn) {

            File flag = new File(Config.membershipFile+"flag");
            while (!flag.exists()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    ;
                }
                flag = new File(Config.membershipFile+"flag");
            }

            System.out.println("flag hit");
            try {
                Runtime.getRuntime().exec("rm "+Config.membershipFile+"flag");
            } catch (Exception e) {
                ;
            }
        }

        EventService.processAll();

        if (Config.useChurn) {
            File flag = new File(Config.membershipFile+"done");
            while (!flag.exists()) {
                loadTrace(num_nodes);
                System.out.println("Running more requests");
                EventService.processAll();
                flag = new File(Config.membershipFile+"done");
            }

            System.out.println("done flag hit");
            try {
                Runtime.getRuntime().exec("rm "+Config.membershipFile+"done");
            } catch (Exception e) {
                ;
            }
        }
        
    }

    @SuppressWarnings("unchecked")
    private static void conclude() {
        for (Map.Entry<Address, Server> entry: servers.entrySet()) {
            AlgorithmMetric.reportMaxRecOK(entry.getValue().max_rec_OK);
        }

        JSONObject obj = new JSONObject();
        obj.put("networkMetric", NetworkMetric.getStat());
        obj.put("algorithmMetric", AlgorithmMetric.getStat());
        obj.put("qualityMetric", QualityMetric.getStat(LogicalTime.time));
        obj.put("conc_requesters",Config.concRequesters);
        obj.put("msg_drop_rate",Config.msgDropRate);
        obj.put("N", Config.numServers);
        obj.put("ir_ratio", Config.irRatio);
        obj.put("churn_ratio",Config.churnRatio);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(obj.toJSONString());
        String prettyJson = gson.toJson(je);
        try {
            FileWriter file = new FileWriter(Config.statsFile);
            file.write(prettyJson);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, "Saved stats to config file: " + Config.statsFile);
    }

    public static void main(String[] args) {
        initialize(args);
        run();
        conclude();
    }
}
