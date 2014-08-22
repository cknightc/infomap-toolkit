package executives;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import matrix.MatrixReader;
import matrix.MatrixReader.ReturnStatus;
import matrix.SimbrainMath;

public class RandomWalker implements Runnable {

    private static int STOP_CRITERIA = (int) 1E05;

    private static double DEFAULT_TELEPORTATION_PROBABILITY = 0.15;

    private double teleport_prob = DEFAULT_TELEPORTATION_PROBABILITY;

    private final double[][] weightMat; // Unsafe! Don't modify...

    private final long[] visitCount;

    private double[] sums;

    private ArrayList<Integer> visitableNodes;

    private HashMap<Integer, Integer> visitableNodesInverse;

    private final int numNodes;

    private final boolean weighted;

    /**
     * 
     * @param weightMat
     */
    public RandomWalker(final double[][] weightMat, boolean weighted) {
        this.weighted = weighted;
        if (MatrixReader.squareCheck(weightMat)
            .equals(ReturnStatus.FAILURE)) {
            throw new IllegalArgumentException("Adjacency Matrix isn't square");
        }
        numNodes = weightMat.length;
        for (int i = 0, n = weightMat.length; i < n; i++) {
            weightMat[i][i] = 0;
        }
        if (!weighted) {
            if (MatrixReader.sanityCheck(weightMat)
                .equals(ReturnStatus.FAILURE)) {
                for (int i = 0, n = weightMat.length; i < n; i++) {
                    weightMat[i] = SimbrainMath.normalizeVec(
                        weightMat[i]);
                }
            }
        } else {
            if (MatrixReader.sanityCheck(weightMat)
                .equals(ReturnStatus.FAILURE)) {
                for (int i = 0, n = weightMat.length; i < n; i++) {
                    weightMat[i] = SimbrainMath.normalizeVec(
                        weightMat[i]);
                }
            }
            //            // Find the maximum sum
            sums = new double[weightMat.length];
            for (int i = 0; i < weightMat.length; i++) {
                sums[i] = SimbrainMath.sum(weightMat[i]);
            }
            visitableNodes = new ArrayList<Integer>();
            visitableNodesInverse = new HashMap<Integer, Integer>();
            int index = 0;
            for (int i = 0; i < numNodes; i++) {
                for (int j = 0; j < numNodes; j++) {
                    if (weightMat[j][i] != 0) {
                        visitableNodes.add(i);
                        visitableNodesInverse.put(i, index);
                        index++;
                        break;
                    }
                }
            }

            //            // Scale all other weights in relation to that
            //            double maxSum = SimbrainMath.getMaximum(sums);
            //            for (int i = 0; i < weightMat.length; i++) {
            //                for (int j = 0; j < weightMat.length; j++) {
            //                    weightMat[i][j] /= maxSum;
            //                }
            //                sums[i] /= maxSum;
            //            }
            //            System.out.println(Arrays.toString(sums));
            //            System.out.println("Visitable: " + visitableNodes.size()
            //                + " Total: "
            //                + numNodes);
        }
        this.weightMat = weightMat;

        visitCount = new long[numNodes];
    }

    /**
     * 
     */
    @Override
    public void run() {
        if (weighted) {
            runWeighted();
        } else {
            runNormal();
        }
    }

    public void runWeighted() {
        int numVNodes = visitableNodes.size();
        final int stop = STOP_CRITERIA * numVNodes;
        //        double percent = 0.01;
        int visits = 0;
        Random rand = ThreadLocalRandom.current();

        int currentNode = rand.nextInt(numVNodes); // Initial node
        while (visits < stop) {
            if (rand.nextDouble() < teleport_prob) {
                currentNode = visitableNodes.get(rand.nextInt(numVNodes));
            } else {
                int rSelect =
                    randSelectW(weightMat[currentNode], sums[currentNode]);
                if (rSelect <= 0) {
                    currentNode = visitableNodes.get(rand.nextInt(numVNodes));
                    continue;
                } else {
                    currentNode = rSelect;
                    visitCount[currentNode]++; // moved
                    visits++; // moved
                    //                    if ((double) visits / stop >= percent) {
                    //                        System.out.println((int) (100 * percent) + "%  ");
                    //                        percent += 0.01;
                    //                    }
                }
            }
        }
    }

    public void runNormal() {
        final int stop = STOP_CRITERIA * numNodes;
        int visits = 0;
        Random rand = ThreadLocalRandom.current();
        int currentNode = rand.nextInt(numNodes); // Initial node
        while (visits < stop) {
            if (rand.nextDouble() < teleport_prob) {
                currentNode = rand.nextInt(numNodes);
            } else {
                int rSelect = randSelect(weightMat[currentNode]);
                if (rSelect < 0) {
                    currentNode = rand.nextInt(numNodes);
                    continue;
                } else {
                    currentNode = rSelect;
                    visitCount[currentNode]++;
                    visits++;
                }
            }

        }
    }

    /**
     * 
     * @param outProbs
     * @return
     */
    public int randSelect(double[] outProbs) {
        double p = Math.random();

        double tot = 0;
        for (int i = 0, n = outProbs.length; i < n; i++) {
            tot += outProbs[i];
            if (tot >= p) {
                return i;
            }
        }
        return -1; // had to teleport no other options
        //        if (tot == 0) {
        //            return -1;
        //        }
        //        throw new IllegalArgumentException("The sum of the probabilities is" +
        //            " not either zero or one.");
    }

    /**
     * 
     * @param outProbs
     * @return
     */
    public int randSelectW(double[] outProbs, double sum) {
        double p = Math.random();
        if (p > sum) {
            return -1;
        }
        double tot = 0;
        for (int i = 0, n = outProbs.length; i < n; i++) {
            tot += outProbs[i];
            if (tot >= p) {
                return visitableNodesInverse.get(i);
            }
        }
        return -1; // had to teleport no other options
        //        if (tot == 0) {
        //            return -1;
        //        }
        //        throw new IllegalArgumentException("The sum of the probabilities is" +
        //            " not either zero or one.");
    }

    public long[] getVisitCounts() {
        return visitCount;
    }

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @param adjacencyMat
     * @return
     */
    public static double[] generate_freqs(double[][] adjacencyMat,
        boolean weightedSums) {
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        RandomWalker[] rwArr = new RandomWalker[numThreads];
        for (int i = 0; i < numThreads; i++) {
            rwArr[i] = new RandomWalker(adjacencyMat, weightedSums);
        }
        ExecutorService taskExecutor =
            Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            taskExecutor.execute(rwArr[i]);
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(1l, TimeUnit.DAYS); // Arbitrary
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            System.exit(1);
        }
        BigDecimal[] visitCounts = new BigDecimal[adjacencyMat.length];
        for (int j = 0; j < adjacencyMat.length; j++) {
            visitCounts[j] = BigDecimal.ZERO;
        }
        for (int i = 0; i < numThreads; i++) {
            BigDecimal[] nodeFreq = rwArr[i].getVisitFrequencies();
            for (int j = 0; j < adjacencyMat.length; j++) {
                visitCounts[j] =
                    visitCounts[j].add(nodeFreq[j].divide(new BigDecimal(
                        numThreads), 20, RoundingMode.HALF_UP));
            }
        }
        double sum = 0;
        double[] vc = new double[adjacencyMat.length];
        for (int j = 0; j < adjacencyMat.length; j++) {
            vc[j] = visitCounts[j].doubleValue();
            sum += vc[j];
        }
        double[] freqs = new double[adjacencyMat.length];
        for (int i = 0, n = adjacencyMat.length; i < n; i++) {
            freqs[i] = vc[i] / sum;
        }

        return freqs;
    }

}
