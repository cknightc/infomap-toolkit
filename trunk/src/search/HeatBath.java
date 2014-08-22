package search;

import graph_elements.Module;
import graph_elements.Network;
import graph_elements.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HeatBath implements Runnable {

    private final Network net;

    private static final int POOL_SIZE = Runtime.getRuntime()
        .availableProcessors();

    private static final ExecutorService executor = Executors
        .newFixedThreadPool(POOL_SIZE);

    private final double coolingRate;

    private double temp;

    public static CountDownLatch latch;

    public HeatBath(Network net, double startTemperature, double coolingRate) {
        this.net = net;
        this.coolingRate = coolingRate;
        this.temp = startTemperature;
    }

    @Override
    public void run() {
        Set<Module> partitionScheme = new HashSet<Module>();
        partitionScheme.addAll(net.getModules());
        ArrayList<Task> modEntropies = new ArrayList<Task>(
            partitionScheme.size() * partitionScheme.size());
        Set<Module> taskScheme = new HashSet<Module>();
        Collection<Callable<Object>> executeList;
        while (temp > 0) {
            executeList = new LinkedList<Callable<Object>>();
            System.out.println(temp);
            modEntropies.clear();
            // System.out.println("Made it into while.");
            for (Module parentMod : partitionScheme) {
                // System.out.println("Made it into for 1.");
                for (Node n : parentMod.getNodes()) {
                    // System.out.println("Made it into for 2.");
                    // Reset the partitioning scheme holder
                    taskScheme = new HashSet<Module>();
                    taskScheme.addAll(partitionScheme);
                    for (Module receivingMod : partitionScheme) {
                        Task t = new Task(net, n, parentMod, receivingMod,
                            taskScheme);
                        modEntropies.add(t);
                        executeList.add(Executors.callable(t));
                    }
                }
            }
            try {
                List<Future<Object>> f = executor.invokeAll(executeList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            double sum = 0.0;
            for (Task t : modEntropies) {
                sum += Math.exp(-1000 * t.getEntropy() / (temp)); // *
                // t.receivingMod.getSize()));
            }

            double probSum = 0.0;
            double rand = Math.random();
            Task acceptedTask = null;
            for (Task t : modEntropies) {
                probSum += Math.exp(-1000 * t.getEntropy() / (temp)) / sum;// *
                // t.receivingMod.getSize()))
                // / sum;
                if (probSum > rand) {
                    acceptedTask = t;
                    break;
                }
            }
            if (acceptedTask == null) {
                partitionScheme = modEntropies.get(modEntropies.size() - 1)
                    .getPartitioning();
            } else {
                partitionScheme = acceptedTask.getPartitioning();
            }
            if (partitionScheme.size() > 129) {
                int nodeCount = 0;
                for (Module m : partitionScheme) {
                    if (m.containsNode(acceptedTask.swapNode)) {
                        for (Node n : m.getNodes()) {
                            System.out.print(n.getIndex() + " ");
                        }
                    }
                    nodeCount += m.getSize();
                }
                System.out.println("Node Count: \t" + nodeCount);
            }
            temp = scheduling();
        }

        executor.shutdown();

        System.out.println("L(M): "
            + CostFunction.cost(partitionScheme, net.getNodeEntropy()));
        // Network.printModules(partitionScheme);

        latch.countDown();
    }

    public double scheduling() {
        return temp - coolingRate;
    }

    private static class Task implements Runnable {

        private Set<Module> currentPartitionScheme;

        private final Network net;

        private final Node swapNode;

        private final Module parentMod;

        private final Module receivingMod;

        private double entropy;

        public Task(Network net, Node swapNode, Module parentMod,
            Module receivingMod, Set<Module> currentPartitionScheme) {
            this.net = net;
            this.swapNode = swapNode;
            this.currentPartitionScheme = currentPartitionScheme;
            this.parentMod = parentMod;
            this.receivingMod = receivingMod;
        }

        @Override
        public synchronized void run() {
            Set<Module> swappedScheme = new HashSet<Module>(
                (int) (currentPartitionScheme.size() * 1.5));
            swappedScheme.addAll(currentPartitionScheme);
            boolean parentRemoveSuccess = swappedScheme.remove(parentMod);
            // Node n is going to be transfered to the receiving
            // module for the purposes of evaluating that
            // configuration's entropy and thus determining
            // the probability of keeping that state
            boolean receiverRemoveSuccess = swappedScheme.remove(receivingMod);
            // Copy the parent module and remove n from
            // the copy
            // if (!(parentRemoveSuccess && receiverRemoveSuccess)) {
            // System.out.println("Parent: " + parentRemoveSuccess
            // + " \t Receiver: " + receiverRemoveSuccess);
            // }
            Module parentModCpy = parentMod.deepCopy();
            parentModCpy.removeNode(swapNode);

            // Add the copies to the partitioning scheme to
            // be evaluated.
            // if (!parentModCpy.getNodes().isEmpty())
            swappedScheme.add(parentModCpy);

            if (!parentMod.equals(receivingMod)) {
                // Copy the receiving module and add
                // n to the copy
                Module receivingModCpy = receivingMod.deepCopy();
                receivingModCpy.addNode(swapNode);
                swappedScheme.add(receivingModCpy);
            }

            entropy = CostFunction.cost(swappedScheme, net.getNodeEntropy());
            currentPartitionScheme = swappedScheme;
        }

        public synchronized double getEntropy() {
            return entropy;
        }

        public synchronized Set<Module> getPartitioning() {
            return currentPartitionScheme;
        }
    }

}
