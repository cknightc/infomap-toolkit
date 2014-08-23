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
package graph_operations;

import graph_io.MatrixReader;
import graph_io.MatrixReader.ReturnStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import math.SimbrainMath;

public class RandomWalker implements Runnable {

    private static int STOP_CRITERIA = (int) 1E04;

    private final double teleport_prob;

    private final double[][] mat; // Unsafe! Don't modify...

    private final long[] visitCount;

    private final int numNodes;

    public RandomWalker(final double[][] weightMat, double teleportProb) {
        this.teleport_prob = teleportProb;
        this.mat = weightMat;
        numNodes = weightMat.length;
        visitCount = new long[numNodes];
    }

    @Override
    public void run() {
        final int stop = STOP_CRITERIA * numNodes;
        int visits = 0;
        Random rand = ThreadLocalRandom.current();
        int currentNode = rand.nextInt(numNodes); // Initial node
        while (visits < stop) {
            if (rand.nextDouble() < teleport_prob) {
                currentNode = rand.nextInt(numNodes);
            } else {
                int rSelect = randSelect(mat[currentNode]);
                if (rSelect < 0) {
                    currentNode = rand.nextInt(numNodes);
                } else {
                    currentNode = rSelect;
                }
            }
            visitCount[currentNode]++;
            visits++;
        }
    }

    public int randSelect(double[] outProbs) {
        double p = Math.random();
        double tot = 0;
        for (int i = 0, n = outProbs.length; i < n; i++) {
            tot += outProbs[i];
            if (tot > p) {
                return i;
            }
        }
        if (tot == 0) {
            return -1;
        }
        throw new IllegalArgumentException("The sum of the probabilities is" +
            " not either zero or one.");
    }

    public long[] getVisitCounts() {
        return visitCount;
    }

    public BigDecimal[] getVisitFrequencies() {
        BigDecimal[] frequencies = new BigDecimal[visitCount.length];
        for (int i = 0, n = visitCount.length; i < n; i++) {
            frequencies[i] = new BigDecimal(visitCount[i]);
            frequencies[i] = frequencies[i]
                .divide(new BigDecimal(STOP_CRITERIA * numNodes), 20,
                    RoundingMode.HALF_UP);
        }
        return frequencies;
    }

    public static double[] generate_freqs(double[][] weightMat,
        double teleportProb) {
        if (MatrixReader.squareCheck(weightMat)
            .equals(ReturnStatus.FAILURE)) {
            throw new IllegalArgumentException("Adjacency Matrix isn't square");
        }
        for (int i = 0, n = weightMat.length; i < n; i++) {
            weightMat[i][i] = 0;
        }
        if (MatrixReader.sanityCheck(weightMat)
            .equals(ReturnStatus.FAILURE)) {
            for (int i = 0, n = weightMat.length; i < n; i++) {
                weightMat[i] = SimbrainMath.normalizeVec(weightMat[i]);
            }
        }
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(numThreads);
        RandomWalker[] rwArr = new RandomWalker[numThreads];
        for (int i = 0; i < numThreads; i++) {
            rwArr[i] = new RandomWalker(SimbrainMath
                .arr2DDeepCopy(weightMat), teleportProb);
            taskExecutor.execute(rwArr[i]);
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(1l, TimeUnit.DAYS); // Arbitrary
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            System.exit(1);
        }
        BigDecimal[] visitCounts = new BigDecimal[weightMat.length];
        for (int j = 0; j < weightMat.length; j++) {
            visitCounts[j] = BigDecimal.ZERO;
        }
        for (int i = 0; i < numThreads; i++) {
            BigDecimal[] nodeFreq = rwArr[i].getVisitFrequencies();
            for (int j = 0; j < weightMat.length; j++) {
                visitCounts[j] =
                    visitCounts[j].add(nodeFreq[j].divide(new BigDecimal(
                        numThreads), 20, RoundingMode.HALF_UP));
            }
        }
        double sum = 0;
        double[] vc = new double[weightMat.length];
        for (int j = 0; j < weightMat.length; j++) {
            vc[j] = visitCounts[j].doubleValue();
            sum += vc[j];
        }
        // double s = 0;
        double[] freqs = new double[weightMat.length];
        for (int i = 0, n = weightMat.length; i < n; i++) {
            freqs[i] = vc[i] / sum;
            // s += freqs[i];
        }
        // System.out.println(s);
        // System.out.println(Arrays.toString(freqs));
        return freqs;
    }

}
