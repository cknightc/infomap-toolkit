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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Zach Tosi
 *
 */
public class Module {

    private final Set<Node> nodes = Collections
        .synchronizedSet(new HashSet<Node>());
    private double sumNodeFrequencies;
    private double exitProbability;
    private final double teleportProb;
    private int totNumNodesInNetwork;

    public static boolean areConnected(Module m1, Module m2) {
        Module smaller;
        Module larger;
        if (m1.getSize() < m2.getSize()) {
            smaller = m1;
            larger = m2;
        } else {
            smaller = m2;
            larger = m1;
        }
        for (Node n : smaller.getNodes()) {
            Set<Node> tempSet = new HashSet<Node>(n.getTransferProbsOut()
                .keySet());
            tempSet.retainAll(larger.getNodes());
            if (!tempSet.isEmpty()) {
                return true;
            }
            tempSet = new HashSet<Node>(n.getTransferProbsIn().keySet());
            tempSet.retainAll(larger.getNodes());
            if (!tempSet.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param teleportProb
     */
    public Module(double teleportProb) {
        this.teleportProb = teleportProb;
    }

    /**
     * 
     * @param initialNode
     * @param teleportProb
     * @param totNumNodesInNetwork
     */
    public Module(Node initialNode, double teleportProb,
        int totNumNodesInNetwork) {
        nodes.add(initialNode);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
        this.teleportProb = teleportProb;
        this.totNumNodesInNetwork = totNumNodesInNetwork;
    }

    /**
     * 
     * @param initialNode
     * @param teleportProb
     */
    public Module(Node initialNode, double teleportProb) {
        nodes.add(initialNode);
        calcExitProb(teleportProb, totNumNodesInNetwork);
        calcSumNodeFreqs();
        this.teleportProb = teleportProb;
    }

    /**
     * 
     * @return
     */
    public Module deepCopy() {
        Module cpy = new Module(this.teleportProb);
        cpy.mergeIntoNoCalc(this);
        cpy.setExitProbability(getExitProbability());
        cpy.setSumNodeFrequencies(getSumNodeFrequencies());
        cpy.totNumNodesInNetwork = totNumNodesInNetwork;
        return cpy;
    }

    /**
     * 
     * @param teleportProb
     * @param totNumNodes
     * @return
     */
    public synchronized double calcExitProb(double teleportProb,
        int totNumNodes) {
        this.totNumNodesInNetwork = totNumNodes;
        double prob = teleportProb * (totNumNodes - nodes.size())
            / (totNumNodes - 1);
        double sum = 0;
        double wtSum = 0;
        for (Node n : nodes) {
            sum += n.getRelativeFrequency();
            Set<Node> outNodes = n.getTransferProbsOut().keySet();
            for (Node m : outNodes) {
                boolean cont = nodes.contains(m);
                if (!cont) {
                    wtSum += n.getRelativeFrequency()
                        * n.getTransferProbsOut().get(m);
                }
            }
        }
        wtSum *= (1 - teleportProb);
        sum *= prob;
        exitProbability = sum + wtSum;
        return sum + wtSum;
    }

    /**
     * 
     * @return
     */
    public double calcSumNodeFreqs() {
        double sum = 0;
        for (Node n : nodes) {
            sum += n.getRelativeFrequency();
        }
        sumNodeFrequencies = sum;
        return sum;
    }

    /**
     * 
     * @param toMerge
     * @param teleportProb
     * @param totNumNodes
     * @return
     */
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

    /**
     * Sets the parent module of all the child nodes of this module to this
     * module. Used after some operation which exchanges nodes between modules.
     */
    public void claimOwnershipOfChildren() {
        for (Node n : nodes) {
            n.setParentModule(this);
        }
    }

    public int getTotNumNodesInNetwork() {
        return totNumNodesInNetwork;
    }

    public void setTotNumNodesInNetwork(int totNumNodesInNetwork) {
        this.totNumNodesInNetwork = totNumNodesInNetwork;
    }
}
