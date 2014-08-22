package graph_elements;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Module {

    private final Set<Node> nodes = Collections
        .synchronizedSet(new HashSet<Node>());

    private double sumNodeFrequencies;

    private double exitProbability;

    private double teleportProb;

    private int totNumNodesInNetwork;

    public Module() {
    }

    public Module(double teleportProb) {
        this.teleportProb = teleportProb;
    }

    public Module(Node initialNode, double teleportProb,
        int totNumNodesInNetwork) {
        nodes.add(initialNode);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
        this.teleportProb = teleportProb;
        this.totNumNodesInNetwork = totNumNodesInNetwork;
    }

    public Module(Node initialNode, double teleportProb) {
        nodes.add(initialNode);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
        this.teleportProb = teleportProb;
    }

    public Module deepCopy() {
        Module cpy = new Module();
        cpy.mergeIntoNoCalc(this);
        cpy.setExitProbability(getExitProbability());
        cpy.setSumNodeFrequencies(getSumNodeFrequencies());
        cpy.teleportProb = teleportProb;
        cpy.totNumNodesInNetwork = totNumNodesInNetwork;
        return cpy;
    }

    public synchronized double calcExitProb(double teleportProb,
        int totNumNodes) {
        this.totNumNodesInNetwork = totNumNodes;
        double prob = teleportProb * (totNumNodes - nodes.size())
            / (totNumNodes - 1);
        double sum = 0;
        double wtSum = 0;
        // double s = 0;
        for (Node n : nodes) {
            sum += n.getRelativeFrequency();
            for (Node m : n.getTransferProbs().keySet()) {
                boolean cont = nodes.contains(m);
                try {
                    if (!cont) {
                        wtSum += n.getRelativeFrequency()
                            * n.getTransferProbs().get(m);
                    }
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
                // s = wtSum;
            }
        }
        // if (wtSum == 0 || s ==0) {
        // System.out.println("wtSum is 0");
        // }
        wtSum *= (1 - teleportProb);
        sum *= prob;
        exitProbability = sum + wtSum;
        return sum + wtSum;
    }

    public double calcSumNodeFreqs() {
        double sum = 0;
        for (Node n : nodes) {
            sum += n.getRelativeFrequency();
        }
        sumNodeFrequencies = sum;
        return sum;
    }

    public synchronized Module mergeInto(Module toMerge, double teleportProb,
        int totNumNodes) {
        nodes.addAll(toMerge.getNodes());
        calcExitProb(teleportProb, totNumNodes);
        calcSumNodeFreqs();
        return this;
    }

    private void mergeIntoNoCalc(Module toMerge) {
        nodes.addAll(toMerge.getNodes());
    }

    /**
     * Don't abuse me!
     * 
     * @return
     */
    public Set<Node> getNodes() {
        return nodes; // No defensive copy for speed
    }

    public void addNodeQuiet(Node n) {
        nodes.add(n);
    }

    public double getSumNodeFrequencies() {
        return sumNodeFrequencies;
    }

    private void setSumNodeFrequencies(double sumNodeFrequencies) {
        this.sumNodeFrequencies = sumNodeFrequencies;
    }

    public double getExitProbability() {
        return exitProbability;
    }

    private void setExitProbability(double exitProbability) {
        this.exitProbability = exitProbability;
    }

    public int getSize() {
        return nodes.size();
    }

    public boolean containsNode(Node n) {
        return nodes.contains(n);
    }

    /**
     * Make sure this is a copy of the original module or this will remove the
     * node from the module
     * 
     * @param n
     * @return
     */
    public boolean removeNode(Node n) {
        boolean remove = nodes.remove(n);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
        return remove;
    }

    /**
     * Make sure this is a copy of the original module or this will add the node
     * to this module
     * 
     * @param n
     */
    public void addNode(Node n) {
        nodes.add(n);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
    }

    public int getTotNumNodesInNetwork() {
        return totNumNodesInNetwork;
    }

    public void setTotNumNodesInNetwork(int totNumNodesInNetwork) {
        this.totNumNodesInNetwork = totNumNodesInNetwork;
    }
}
