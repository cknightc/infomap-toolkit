package search;

import graph_elements.Module;
import graph_elements.Network;
import graph_elements.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class Annealing implements Runnable {

    private final Network net;

    private final double startTemperature;

    /** In units of T/iteration. */
    private final double coolingRate;

    private ArrayList<Module> modulesClassVar;

    private double temp;

    private static CyclicBarrier finish;

    /**
     * 
     * @param net
     *            the network on which annealing will be performed
     * @param startTemperature
     *            the starting temperature
     * @param coolingRate
     *            the rate of cooling in units of temperature per iteration
     */
    public Annealing(Network net, double startTemperature, double coolingRate) {
        this.net = net;
        this.startTemperature = startTemperature;
        this.coolingRate = coolingRate;
        this.modulesClassVar = new ArrayList<Module>();
        modulesClassVar.addAll(net.getModules());
    }

    @Override
    public void run() {
        temp = startTemperature;
        HashMap<ModuleTuple, Double> successorProbMapping = new HashMap<ModuleTuple, Double>();
        ArrayList<Module> modules = new ArrayList<Module>();
        for (Module m : modulesClassVar) {
            modules.add(m.deepCopy());
        }
        double rand;
        double sum = 0;
        double probSum;
        ModuleTuple acceptedMerger = null;
        boolean skip = false;
        while (temp > 0 && modules.size() > 1) {

            if (skip) {
                skip = false;
            } else {
                sum = 0;
                successorProbMapping = new HashMap<ModuleTuple, Double>();
                for (int i = 0, n = modules.size(); i < n - 1; i++) {
                    for (int j = i + 1; j < n; j++) {
                        // Preference for smaller modules merging... should also
                        // try preference for modules with lower between module
                        // entropy, i.e. higher prob of moving from one to
                        // the other
                        ModuleTuple proposedMerge = new ModuleTuple(
                                modules.get(i), modules.get(j));
                        // double prob = 1 - ((proposedMerge.module1.getSize() +
                        // proposedMerge.module2
                        // .getSize()) / net.getNumNodes());
                        double prob = 1.0 / (double) (modules.size()
                                * (modules.size() - 1.0) / 2.0);
                        successorProbMapping.put(proposedMerge, prob);
                        sum += prob;
                    }
                }
                // if (sum != 1) {
                // System.out.println("Sum: " + sum);
                // }
                // Normalize
                for (Entry<ModuleTuple, Double> ent : successorProbMapping
                        .entrySet()) {
                    ent.setValue(ent.getValue() / sum);
                }
            }
            // Randomly select the proposed merger
            probSum = 0;
            rand = Math.random();
            for (ModuleTuple propMerge : successorProbMapping.keySet()) {
                probSum += successorProbMapping.get(propMerge);
                if (probSum > rand) {
                    acceptedMerger = propMerge;
                    break;
                }
            }
            HashSet<Module> cpySet = new HashSet<Module>();
            cpySet.addAll(modules);
            cpySet.remove(acceptedMerger.module1);
            cpySet.remove(acceptedMerger.module2);
            cpySet.add(acceptedMerger.getCpyMerger());

            // If positive new merger gives LONGER avg path description
            double entropyDiff = CostFunction
                    .cost(cpySet, net.getNodeEntropy())
                    - CostFunction.cost(modules, net.getNodeEntropy());

            if (entropyDiff < 0) {
                // Remove the modules
                modules.remove(acceptedMerger.module2);
                modules.remove(acceptedMerger.module1);
                // Merge the second module into the first
                modules.add(acceptedMerger.getCpyMerger());
            } else {
                rand = Math.random();
                if (rand < Math.exp(-10 * entropyDiff / temp)) {
                    // Remove the modules
                    modules.remove(acceptedMerger.module2);
                    modules.remove(acceptedMerger.module1);
                    // Merge the second module into the a copy of the first
                    modules.add(acceptedMerger.getCpyMerger());
                } else {
                    skip = true; // Module tuples don't change since,
                    // nothing has changed.
                }
            }
            // System.out.println(SimbrainMath.roundDouble(startTemperature, 4)
            // + " \t" + SimbrainMath.roundDouble(temp, 5) + " \t"
            // + SimbrainMath.roundDouble(coolingRate, 6));
            temp -= coolingRate; // Lower the temperature
        }
        try {
            // System.out.println("Cooling Rate: " + coolingRate + " COMPLETE");
            modulesClassVar = modules;
            finish.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final class ModuleTuple {

        public final Module module1;

        public final Module module2;

        public ModuleTuple(Module module1, Module module2) {
            this.module1 = module1;
            this.module2 = module2;
        }

        public Module getCpyMerger() {
            Module merged = module1.deepCopy();
            return merged.mergeInto(module2, net.getTeleportProb(),
                    net.getNumNodes());
        }

    }

    public static CountDownLatch latch;

    public static void executeAnneal(Network net) {
        final int processors = Runtime.getRuntime().availableProcessors();
        finish = new CyclicBarrier(processors + 1);
        double startTemp = 20;
        double[] coolingRates = new double[processors];
        coolingRates[0] = 0.5;
        coolingRates[1] = 0.45;
        coolingRates[2] = 0.4;
        coolingRates[3] = 0.35;
        coolingRates[4] = 0.3;
        coolingRates[5] = 0.25;
        coolingRates[6] = 0.2;
        coolingRates[7] = 0.1;
        Annealing[] annealers = new Annealing[processors];
        for (int i = 0; i < processors; i++) {
            annealers[i] = new Annealing(net, startTemp, coolingRates[i]);
            new Thread(annealers[i]).start();
        }
        try {
            finish.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        int minCL = 0;
        double min = Double.MAX_VALUE;
        Annealing minAnneal = null;
        for (int i = 0; i < processors; i++) {
            double cost = CostFunction.cost(annealers[i].modulesClassVar,
                    net.getNodeEntropy());
            if (cost < min) {
                min = cost;
                minCL = i;
                minAnneal = annealers[i];
            }
        }
        HashSet<Node> repeat = new HashSet<Node>();
        for (Module m : minAnneal.modulesClassVar) {
            for (Node n : m.getNodes()) {
                if (repeat.contains(n)) {
                    System.out.println("Repeat of node: " + n.getIndex());
                } else {
                    repeat.add(n);
                }
            }
        }
        System.out.println("Cool Rate: " + coolingRates[minCL]);
        System.out.println("Final cost: " + min);
        Network.printModules(minAnneal.modulesClassVar);
        latch.countDown();

    }

}
