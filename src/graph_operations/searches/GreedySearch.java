/*
 * Part of infomap-toolkit--a java based concurrent toolkit for running the
 * infomap algorithm (all credit for the algorithm goes to Martin Rosvall and
 * Carl T. Bergstrom).
 * 
 * Copyright (C) 2014 Zach Tosi
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package graph_operations.searches;

import graph_elements.Module;
import graph_elements.Network;
import graph_operations.CostFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import concurrency_tools.AbstractProducer;
import concurrency_tools.Consumer;
import concurrency_tools.Task;

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
     * A count down latch which can be set by classes calling this search
     * allowing the search to complete before any other actions are taken.
     */
    private CountDownLatch externalLatch;

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
            consumers[i] = new Consumer(taskQueue);
        }
        new Thread(new GSTaskProducer(consumers)).start();
        for (Consumer c : consumers) {
            new Thread(c).start();
        }

    }

    public CountDownLatch getExternalLatch() {
        return externalLatch;
    }

    public void setExternalLatch(CountDownLatch externalLatch) {
        this.externalLatch = externalLatch;
    }

    /**
     * The task performed during the greedy search.
     * 
     * @author Zach Tosi
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
            if (!Module.areConnected(m1, m2)) {
                return;
            }
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

    /**
     * A  task telling the consumer to wait at a cyclic barrier.
     * @author Zach Tosi
     */
    private class WaitTask implements Task {

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

    /**
     * An producer specifically for greedy search tasks.
     * 
     * @author Zach Tosi
     *
     */
    private class GSTaskProducer extends AbstractProducer {

        public GSTaskProducer(concurrency_tools.Consumer[] consumers) {
            super(consumers);
        }

        /**
         * 
         */
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
                        taskQueue.put(new WaitTask());
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
                    minimalTask.getMergedMod().claimOwnershipOfChildren();
                }
            }
            for (Module mod : partitioning) {
                mod.claimOwnershipOfChildren();
            }
            shutdownConsumers();
            //            System.out.println("Hierarchical Entropy: " + partitionEntropy);
            net.setModules(partitioning);
            net.setHierarchicalEntropy(partitionEntropy);
            if (externalLatch != null) {
                externalLatch.countDown();
            }
        }
    }

}
