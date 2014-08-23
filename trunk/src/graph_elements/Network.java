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
package graph_elements;

import graph_operations.CostFunction;
import graph_operations.RandomWalker;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import math.SimbrainMath;

public class Network {

    private Set<Module> modules = Collections
        .synchronizedSet(new HashSet<Module>());
    private final List<Node> flatNodeList;
    private double teleportProb;
    private int numNodes;
    private final int originalNumNodes;
    private double nodeEntropy;
    private double hierarchicalEntropy;
    private double maxFreq;
    private double stdDev;
    private double maxDev;
    private double minDev;
    private double mean;
    private boolean hasDeadNodes;

    /**
     * 
     * @param list
     */
    public Network(List<Node> list, Set<Module> modules) {
        this.flatNodeList = list;
        this.modules = modules;
        this.numNodes = list.size();
        this.originalNumNodes = numNodes;
        this.nodeEntropy = calcFlatEntropy();
        this.hierarchicalEntropy = CostFunction.cost(modules, nodeEntropy);
        calcStatistics();
        initializeColoring();
    }

    /**
     * A constructor that does not specify relative node visit frequency. Note
     * that this constructor automatically runs the random walker to generate
     * these frequencies.
     * @param adjacencyMat
     * @param coordinateFileName
     * @param teleportProb
     */
    public Network(double[][] adjacencyMat, String coordinateFileName,
        double teleportProb, boolean removeDeadNodes) {
        this(adjacencyMat, RandomWalker.generate_freqs(adjacencyMat,
            teleportProb), coordinateFileName, teleportProb, removeDeadNodes);
    }

    /**
     * 
     * @param weightMat
     * @param relativeFreqs
     * @param coordinateFileName
     * @param teleportProb
     */
    public Network(double[][] weightMat, double[] relativeFreqs,
        String coordinateFileName, double teleportProb,
        boolean removeDeadNodes) {
        numNodes = weightMat.length;
        originalNumNodes = numNodes;
        flatNodeList = new ArrayList<Node>(numNodes);
        this.teleportProb = teleportProb;
        // Construct Nodes
        constructNodes(relativeFreqs);
        // Assign xy positions
        readInAndSetXYCoordinates(coordinateFileName);
        // Connect nodes
        connectNodes(flatNodeList, weightMat);
        // Calculate statistics and/or remove dead nodes
        if (removeDeadNodes) {
            // remove dead nodes, also calculates entropy and statistics
            removeDeadNodes();
        } else {
            // Flat Entropy
            nodeEntropy = calcFlatEntropy();
            // Set values like mean, stdDev, max/minDev, and maxFreq
            calcStatistics();
        }
        // Construct singleton modules.
        initializeModules(flatNodeList);
        // Set the color of each of the nodes based on their relative
        // frequency.
        initializeColoring();
        System.out.println("Flat Entropy: " + nodeEntropy);
    }

    /**
     * 
     * @param relativeFrequencies
     */
    private void constructNodes(double[] relativeFrequencies) {
        for (int i = 0, n = relativeFrequencies.length; i < n; i++) {
            Node node = new Node(i);
            node.setRelativeFrequency(relativeFrequencies[i]);
            flatNodeList.add(node);
        }
    }

    /**
     * Connect nodes to each other based on 
     * @param flatNodeList
     * @param weightMatrix
     */
    private void connectNodes(List<Node> flatNodeList,
        double[][] weightMatrix) {
        int numNodes = flatNodeList.size();
        checkNodeMatrixConsistency(numNodes, weightMatrix.length);
        for (int i = 0; i < numNodes; i++) {
            checkNodeMatrixConsistency(numNodes, weightMatrix[i].length);
            Node n1 = flatNodeList.get(i);
            for (int j = 0; j < numNodes; j++) {
                double weight = weightMatrix[i][j];
                if (weight != 0) {
                    Node n2 = flatNodeList.get(j);
                    n1.addOutgoingEdge(n2, weight);
                    n2.addIncomingEdge(n1, weight);
                }
            }
        }
    }

    /**
     * Permanently removes dead nodes from the network. A dead node is defined
     * as a node with an in and out degree of 0. After this operation the index
     * variable of each node will no longer correspond to its position in the
     * {@link #flatNodeList}, so the nodes index value should be used for
     * any reconstruction. Updates the network's entropy and statistics after
     * all dead nodes have been removed.
     */
    public void removeDeadNodes() {
        Iterator<Node> nodeIter = flatNodeList.iterator();
        while (nodeIter.hasNext()) {
            Node n = nodeIter.next();
            if (n.getTransferProbsIn().size() == 0
                && n.getTransferProbsOut().size() == 0) {
                nodeIter.remove(); // Dead Node
                numNodes--;
                //                if (n.getParentModule() != null) {
                //                    n.getParentModule().removeNode(n);
                //                }
            }
        }
        hasDeadNodes = numNodes != originalNumNodes;
        nodeEntropy = calcFlatEntropy();
        calcStatistics();
    }

    /**
     * 
     * @param flatNodeList
     */
    private void initializeModules(List<Node> flatNodeList) {
        for (Node n : flatNodeList) {
            Module m = new Module(n, teleportProb);
            modules.add(m);
            m.setTotNumNodesInNetwork(numNodes);
            //            n.setParentModule(m);
        }
    }

    /**
     * 
     * @param size1
     * @param size2
     */
    private void checkNodeMatrixConsistency(int size1, int size2) {
        if (size1 != size2) {
            throw new IllegalStateException("Nodes in the flat node list are"
                + " inconsistent with the size of the weight matrix or the"
                + " weight matrix is not square");
        }
    }

    /**
     * 
     * @return
     */
    public double[][] getMatrix() {
        double[][] mat = new double[originalNumNodes][originalNumNodes];
        for (Node n : flatNodeList) {
            for (Node m : n.getTransferProbsOut().keySet()) {
                mat[n.getIndex()][m.getIndex()] =
                    n.getTransferProbsOut().get(m);
            }
        }
        return mat;
    }

    /**
     * 
     * @param n
     */
    public void removeNode(Node n) {
        flatNodeList.remove(n);
        numNodes--;
        Iterator<Module> modIter = modules.iterator();
        while (modIter.hasNext()) {
            Module m = modIter.next();
            if (m.containsNode(n)) {
                m.removeNode(n);
                return;
            }
        }
        nodeEntropy = calcFlatEntropy();
        calcStatistics();
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

    /**
     * @return the hasDeadNodes
     */
    public boolean isHasDeadNodes() {
        return hasDeadNodes;
    }

}
