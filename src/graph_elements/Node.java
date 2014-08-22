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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An object representing the nodes or vertices of the graph on which infomap
 * will be performed. In addition to values like relative random walk visit
 * frequency and transfer probabilities to other nodes, each node also contains
 * several other values like an x and y coordinate, color (for representation
 * in Gephi), and its index relative to other nodes.
 * 
 * Note that a Node will be considered equal to another node iff it has the same
 * xy coordinates and index as the other node. Whether or not they are the same
 * object is irrelevant. This allows two networks with the same nodes to be more
 * easily compared. 
 * 
 * @author Zach Tosi
 *
 */
public class Node {

    private Map<Node, Double> transferProbsOut =
        new LinkedHashMap<Node, Double>();
    private Map<Node, Double> transferProbsIn =
        new LinkedHashMap<Node, Double>();
    private Module parentModule;
    private double relativeFrequency;
    private final int index;
    private int color;
    private double x;
    private double y;

    public Node(final int index) {
        this.index = index;
    }

    public Node(Map<Node, Double> transferProbsOut,
        Map<Node, Double> transferProbsIn, double relativeFrequency,
        final int index) {
        setTransferProbsOut(transferProbsOut);
        setTransferProbsIn(transferProbsIn);
        setRelativeFrequency(relativeFrequency);
        this.index = index;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public double getRelativeFrequency() {
        return relativeFrequency;
    }

    public void setRelativeFrequency(double relativeFrequency) {
        this.relativeFrequency = relativeFrequency;
    }

    public Map<Node, Double> getTransferProbsOut() {
        return transferProbsOut;
    }

    public void setTransferProbsOut(Map<Node, Double> transferProbsOut) {
        this.transferProbsOut = transferProbsOut;
    }

    public Map<Node, Double> getTransferProbsIn() {
        return transferProbsIn;
    }

    public void setTransferProbsIn(Map<Node, Double> transferProbsIn) {
        this.transferProbsIn = transferProbsIn;
    }

    public int getIndex() {
        return index;
    }

    public void addOutgoingEdge(Node target, double weight) {
        transferProbsOut.put(target, weight);
    }

    public void addIncomingEdge(Node source, double weight) {
        transferProbsIn.put(source, weight);
    }

    /**
     * @return the parentModule
     */
    public Module getParentModule() {
        return parentModule;
    }

    /**
     * @param parentModule the parentModule to set
     */
    public void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    @Override
    public int hashCode() {
        final int sPrime = 17;
        int xVal = new Double(x).hashCode() >> 5;
        int yVal = new Double(y).hashCode();
        int indVal = (index * sPrime) ^ xVal;
        return xVal ^ yVal + indVal;
    }

    /**
     * Equals based on if the xy coordinates and index are the same for the
     * nodes being compared.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;
        return this.hashCode() == obj.hashCode();
    }

}
