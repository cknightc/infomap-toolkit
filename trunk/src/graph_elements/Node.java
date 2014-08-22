package graph_elements;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Node {

    private LinkedHashMap<Node, Double> transferProbs =
        new LinkedHashMap<Node, Double>();

    private double relativeFrequency;

    private final int index;

    private int color;

    private double x;

    private double y;

    private boolean useIndexForEquals = false;

    public Node(final int index) {
        this.index = index;
    }

    public Node(LinkedHashMap<Node, Double> transferProbs,
        double relativeFrequency, final int index) {
        this.setTransferProbs(transferProbs);
        this.setRelativeFrequency(relativeFrequency);
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

    public LinkedHashMap<Node, Double> getTransferProbs() {
        return transferProbs;
    }

    public void setTransferProbs(LinkedHashMap<Node, Double> transferProbs) {
        this.transferProbs = transferProbs;
    }

    public int getIndex() {
        return index;
    }

    public void addOutgoingEdge(Node target, double weight) {
        transferProbs.put(target, weight);
    }

    @Override
    public int hashCode() {
        final int sPrime = 17;
        int xVal = new Double(x).hashCode() >> 5;
        int yVal = new Double(y).hashCode();
        int indVal = (index * sPrime) ^ xVal;
        return xVal ^ yVal + indVal;
    }

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

    public static void main(String[] args) {
        Node n1 = new Node(3);
        n1.setX(-435.21);
        n1.setY(12.01);
        Node n2 = new Node(3);
        n2.setX(-435.21);
        n2.setY(12.01);
        System.out.println(n1.hashCode());
        System.out.println(n2.hashCode());
        System.out.println(n1.equals(n2));
        HashSet<Node> nSet = new HashSet<Node>();
        nSet.add(n1);
        System.out.println(nSet.size());
        nSet.remove(n2);
        System.out.println(nSet.size());

    }

}
