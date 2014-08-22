package search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import executives.Task;
import graph_elements.Module;
import graph_elements.Network;

public class GreedySearch {

    private static final byte NUM_PRODUCERS = 1;

    private static final int NUM_CONSUMERS = Runtime.getRuntime()
        .availableProcessors();

    private final BlockingQueue<Task> taskQueue =
        new LinkedBlockingQueue<Task>();

    private Set<Module> partitioning = Collections
        .synchronizedSet(new HashSet<Module>());

    private final Network net;

    private final double nodeEntropy;

    private final Object partitionEntropyLock = new Object();

    private double partitionEntropy = Double.MAX_VALUE;

    private SearchTask minimalTask = null;

    /**
     * A barrier to force consumers to wait so that the chosen partition can be
     * accepted.
     */
    private CyclicBarrier mergeCycleLatch;

    /**
     * 
     * @param net
     */
    public GreedySearch(Network net) {
        this.net = net;
        nodeEntropy = net.getNodeEntropy();
        partitioning.addAll(net.getModules());
    }

    /**
     * Performs the greedy search. Initializes all producer and consumer
     * threads.
     */
    public void search() {
        Consumer[] consumers = new Consumer[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumers[i] = new Consumer();
        }
        new Thread(new Producer(consumers)).start();
        for (Consumer c : consumers) {
            new Thread(c).start();
        }

    }

    /**
     * 
     * @author zach
     * 
     */
    private class SearchTask implements Task {

        private final Module m1;

        private final Module m2;

        private Module merged;

        public SearchTask(Module m1, Module m2) {
            this.m1 = m1;
            this.m2 = m2;
        }

        public Collection<Module> getProposedMapping() {
            HashSet<Module> cpy = new HashSet<Module>();
            cpy.addAll(partitioning);
            cpy.remove(m1);
            cpy.remove(m2);
            Module modcpy = m1.deepCopy();
            merged = modcpy.mergeInto(m2, net.getTeleportProb(),
                net.getNumNodes());
            cpy.add(merged);
            return cpy;
        }

        /**
         * @return the m1
         */
        public Module getM1() {
            return m1;
        }

        /**
         * @return the m2
         */
        public Module getM2() {
            return m2;
        }

        public Module getMergedMod() {
            return merged;
        }

        @Override
        public void perform() {
            double val = CostFunction.cost(getProposedMapping(), nodeEntropy);
            synchronized (partitionEntropyLock) {
                if (val < partitionEntropy) {
                    partitionEntropy = val;
                    minimalTask = this;
                }
            }
        }

        @Override
        public boolean isPoison() {
            return false;
        }

    }

    public class PoisonTask implements Task {

        @Override
        public void perform() {
            try {
                mergeCycleLatch.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public boolean isPoison() {
            return true;
        }

    }

    public CountDownLatch latch;

    private class Producer implements Runnable {

        private final Consumer[] consumers;

        public Producer(Consumer[] consumers) {
            this.consumers = consumers;
        }

        public void shutdownConsumers() {
            for (Consumer c : consumers) {
                c.shutdown();
            }
        }

        @Override
        public void run() {
            double partEntropy = Double.POSITIVE_INFINITY;
            while (partEntropy > partitionEntropy) {
                partEntropy = partitionEntropy;
                Module[] modules = partitioning.toArray(new Module[partitioning
                    .size()]);
                mergeCycleLatch = new CyclicBarrier(NUM_PRODUCERS
                    + NUM_CONSUMERS);
                for (int i = 0, n = modules.length - 1; i < n; i++) {
                    for (int j = i + 1; j < n + 1; j++) {
                        try {
                            taskQueue
                                .put(new SearchTask(modules[i], modules[j]));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }
                for (int i = 0; i < NUM_CONSUMERS; i++) {
                    try {
                        taskQueue.put(new PoisonTask());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                try {
                    mergeCycleLatch.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                if (partEntropy > partitionEntropy) {
                    partitioning.remove(minimalTask.getM1());
                    partitioning.remove(minimalTask.getM2());
                    partitioning.add(minimalTask.getMergedMod());
                }
            }
            shutdownConsumers();
            // int counter = 0;
            // for (Module m : partitioning) {
            // System.out.println("Module [ " + counter++ + " ]");
            // for (Node n : m.getNodes()) {
            // System.out.print(n.getIndex() + " ");
            // }
            // System.out.println();
            // }
            System.out.println("Hierarchical Entropy: " + partitionEntropy);
            net.setModules(partitioning);
            net.setHierarchicalEntropy(partitionEntropy);
            latch.countDown();
        }
    }

    private class Consumer implements Runnable {

        private volatile boolean continueRunning = true;

        @Override
        public void run() {
            while (continueRunning) {
                try {
                    taskQueue.take().perform();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        public void shutdown() {
            continueRunning = false;
        }

    }

    // public static void main(String[] args) {
    // Network net = new Network(
    // MatrixReader.matrixReader("./resources/TE04-0"));
    // GreedySearch searcher = new GreedySearch(net);
    // searcher.search();
    // }

}
