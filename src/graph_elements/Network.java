package graph_elements;

import java.awt.Color;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import matrix.SimbrainMath;
import search.CostFunction;
import executives.RandomWalker;

public class Network {

    private Set<Module> modules = Collections
        .synchronizedSet(new HashSet<Module>());

    private Map<Integer, Node> nodeIDMap = new HashMap<Integer, Node>();

    public Map<Integer, Node> getNodeIDMap() {
        return nodeIDMap;
    }

    private final List<Node> flatNodeList;

    private double teleportProb;

    private int numNodes;

    private final double nodeEntropy;

    private double hierarchicalEntropy;

    private double maxFreq;

    private double stdDev;

    private double maxDev;

    private double minDev;

    private double mean;

    /**
     * 
     * @param list
     */
    public Network(List<Node> list, Set<Module> modules) {
        this.flatNodeList = list;
        this.modules = modules;
        this.numNodes = list.size();
        this.nodeEntropy = calcFlatEntropy();
        this.hierarchicalEntropy = CostFunction.cost(modules, nodeEntropy);
        calcStatistics();
        initializeColoring();
        for (Node n : list) {
            nodeIDMap.put(n.getIndex(), n);
        }
    }

    /**
     * 
     * @param adjacencyMat
     * @param coordinateFileName
     * @param teleportProb
     */
    public Network(double[][] adjacencyMat, String coordinateFileName,
        double teleportProb, boolean weightedSums) {
        this(adjacencyMat, RandomWalker.generate_freqs(adjacencyMat,
            weightedSums), coordinateFileName, teleportProb);
    }

    /**
     * 
     * @param adjacencyMat
     * @param relativeFreqs
     * @param coordinateFileName
     * @param teleportProb
     */
    public Network(double[][] adjacencyMat, double[] relativeFreqs,
        String coordinateFileName, double teleportProb) {
        numNodes = adjacencyMat.length;
        flatNodeList = new ArrayList<Node>(numNodes);
        this.teleportProb = teleportProb;
        // Construct Nodes
        for (int i = 0; i < numNodes; i++) {
            Node n = new Node(i);
            n.setRelativeFrequency(relativeFreqs[i]);
            flatNodeList.add(n);
        }
        readInAndSetXYCoordinates(coordinateFileName);
        // Connect nodes
        for (int i = 0; i < numNodes; i++) {
            LinkedHashMap<Node, Double> transferProbs =
                new LinkedHashMap<Node, Double>();
            for (int j = 0; j < numNodes; j++) {
                transferProbs.put(flatNodeList.get(j), adjacencyMat[i][j]);
            }
            flatNodeList.get(i).setTransferProbs(transferProbs);
        }

        // Construct singleton modules using only visited nodes.
        Iterator<Node> nodeIter = flatNodeList.iterator();
        while (nodeIter.hasNext()) {
            Node n = nodeIter.next();
            if (n.getRelativeFrequency() != 0) {
                Module m = new Module(n, teleportProb);
                modules.add(m);
            } else {
                nodeIter.remove();
                nodeIDMap.remove(n.getIndex());
                numNodes--;
            }
        }
        for (Module m : modules) {
            m.setTotNumNodesInNetwork(numNodes);
        }
        // Flat Entropy
        nodeEntropy = calcFlatEntropy();
        System.out.println("Flat Node entropy: " + nodeEntropy);

        // Set values like mean, stdDev, max/minDev, and maxFreq
        calcStatistics();

        // Set the color of each of the nodes based on their relative
        // frequency.
        initializeColoring();
    }

    public double[][] getMatrix() {
        double[][] mat = new double[flatNodeList.size()][flatNodeList.size()];
        for (Node n : flatNodeList) {
            for (Node m : n.getTransferProbs().keySet()) {
                mat[n.getIndex()][m.getIndex()] = n.getTransferProbs().get(m);
            }
        }
        return mat;
    }

    public static boolean isDeadNode(double[][] transferMat, int rc) {
        boolean isDead = true;
        for (int i = 0; i < transferMat.length; i++) {
            isDead &= transferMat[rc][i] == 0 && transferMat[i][rc] == 0;
            if (!isDead) {
                return isDead;
            }
        }
        return isDead;
    }

    public void removeNode(Node n) {
        flatNodeList.remove(n);
        nodeIDMap.remove(n.getIndex());
        Iterator<Module> modIter = modules.iterator();
        while (modIter.hasNext()) {
            Module m = modIter.next();
            if (m.containsNode(n)) {
                m.removeNode(n);
                return;
            }
        }
    }

    /**
     * 
     */
    private void initializeColoring() {
        double[] vals = new double[flatNodeList.size()];
        for (int i = 0, n = flatNodeList.size(); i < n; i++) {
            vals[i] = Math.tanh((flatNodeList.get(i).getRelativeFrequency()
                - mean) / stdDev);
        }
        double tanMin = SimbrainMath.getMinimum(vals);
        for (int i = 0, n = flatNodeList.size(); i < n; i++) {
            vals[i] -= tanMin;
            flatNodeList.get(i).setColor(getColor(vals[i]));
        }
    }

    /**
     * 
     */
    private void calcStatistics() {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double tot = 0.0;
        for (Node n : flatNodeList) {
            if (n.getRelativeFrequency() > max) {
                max = n.getRelativeFrequency();
            }
            if (n.getRelativeFrequency() < min) {
                min = n.getRelativeFrequency();
            }
            tot += n.getRelativeFrequency();
        }
        mean = tot / flatNodeList.size();
        maxFreq = max;
        tot = 0.0;
        for (Node n : flatNodeList) {
            tot += Math.pow((mean - n.getRelativeFrequency()), 2);
        }
        stdDev = Math.sqrt(tot / flatNodeList.size());
        maxDev = (maxFreq - mean) / stdDev;
        minDev = (min - mean) / stdDev;
    }

    /**
     * 
     * @return
     */
    public double calcFlatEntropy() {
        double entropy = 0;
        for (Node n : flatNodeList) {
            entropy += n.getRelativeFrequency()
                * (Math.log(n.getRelativeFrequency()) / Math.log(2));
        }
        return -entropy;
    }

    /**
     * 
     * @param coordinateFileName
     */
    private void readInAndSetXYCoordinates(String coordinateFileName) {
        try (Scanner sc = new Scanner(new FileReader(coordinateFileName))) {
            sc.useDelimiter(Pattern.compile("[\\r\\n\\s,]+"));
            for (int i = 0; i < flatNodeList.size(); i++) {
                flatNodeList.get(i).setX(Double.parseDouble(sc.next()));
            }
            sc.nextLine();
            for (int i = 0; i < flatNodeList.size(); i++) {
                flatNodeList.get(i).setY(Double.parseDouble(sc.next()));
            }
        } catch (NumberFormatException | IOException ie) {
            ie.printStackTrace();
        }
    }

    public static void printModules(Collection<Module> moduleCollection) {
        int count = 0;
        for (Module m : moduleCollection) {
            System.out.println("Module [ " + count++ + " ]");
            for (Node n : m.getNodes()) {
                System.out.print(n.getIndex() + " ");
            }
            System.out.println();
        }
    }

    public void printNetworkToFile(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("<Meta>");
            pw.println("\t Size: " + numNodes);
            pw.println("\t Flat Entropy: " + nodeEntropy);

            for (Node n : flatNodeList) {
                pw.print(n.getIndex());
            }
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 
     * @param col
     * @return
     */
    public static int getColor(double col) {
        int blue;
        if (col < 0.25) {
            blue = (int) (255 * (2 * col + 0.5));
        } else if (col < 0.5 && col >= 0.25) {
            blue = 255;
        } else {
            blue = (int) (255 * (-2 * col + 2));
        }

        int green;
        if (col < 0.4) {
            green = (int) (255 * (2.5 * col));
        } else if (col < 0.75 && col >= 0.4) {
            green = 255;
        } else {
            green = (int) (255 * (-4 * col + 4));
        }

        int red;
        if (col > 0.75) {
            red = 255;
        } else if (col > 0.25 && col <= 0.75) {
            red = (int) (255 * (2 * col - 0.5));
        } else {
            red = 0;
        }

        int color = Color.OPAQUE;
        color = color | blue;
        color = color | (green << 8);
        color = color | (red << 16);

        return color;

    }

    /**
     * 
     * @param n
     * @return
     */
    public Color getNodeRGB(Node n) {
        float dev = (float) ((n.getRelativeFrequency() - mean) / stdDev);
        dev = (float) Math.tanh(dev);

        float red = 0.5f * dev / (float) maxDev + 0.5f;
        float blue = 0.5f * dev / (float) minDev + 0.5f;
        red = red < 0 ? 0 : red;
        blue = blue < 0 ? 0 : blue;
        int R = (int) (red * 0xFF);
        int B = (int) (blue * 0xFF);
        int color = 0;
        color = color | B;
        color = color | (R << 16);
        return new Color(color);
    }

    public int getSigModCount() {
        int sigMods = 0;
        for (Module m : modules) {
            if (m.getSize() > 1) {
                sigMods++;
            }
        }
        return sigMods;
    }

    public double getNodeEntropy() {
        return nodeEntropy;
    }

    public Set<Module> getModules() {
        return modules;
    }

    public void setModules(Set<Module> modules) {
        this.modules = modules;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public List<Node> getFlatNodeList() {
        return flatNodeList;
    }

    public double getHierarchicalEntropy() {
        return hierarchicalEntropy;
    }

    public void setHierarchicalEntropy(double hierarchicalEntropy) {
        this.hierarchicalEntropy = hierarchicalEntropy;
    }

    public double getTeleportProb() {
        return teleportProb;
    }

    public void setTeleportProb(double teleportProb) {
        this.teleportProb = teleportProb;
    }

}
