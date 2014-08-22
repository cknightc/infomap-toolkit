package analysis;

import gephi_io.GephiXMLIn;
import gephi_io.GephiXMLOut;
import graph_elements.Module;
import graph_elements.Network;
import graph_elements.Node;

import java.util.HashSet;
import java.util.Set;

public class NetworkComparison {

    //    public static double adjustedRandFunction(Network net1, Network net2) {
    //        double[][] mat1 = net1.getMatrix();
    //        double[][] mat2 = net2.getMatrix();
    //        Iterator<Node> nodeIter1 = net1.getFlatNodeList().iterator();
    //        Iterator<Node> nodeIter2 = net2.getFlatNodeList().iterator();
    //        while ()
    //        for (int i = 0; i < net1.getFlatNodeList().size(); i++) {
    //            if (Network.isDeadNode(mat1, i) && Network.isDeadNode(mat2, i)) {
    //                net1.
    //            }
    //        }
    //    }

    public static double mutualInformation(Network net1, Network net2) {
        assert (net1.getNumNodes() == net2.getNumNodes());
        double numNodes = net1.getNumNodes();
        double mutInfo = 0.0;
        double entropy1 = 0;
        double entropy2 = 0;
        int singletons1 = 0;
        int singletons2 = 0;
        //        for (Module n1m : net2.getModules()) {
        //            if (n1m.getSize() < 2)
        //                singletons1++;
        //        }
        //        for (Module n2m : net1.getModules()) {
        //            if (n2m.getSize() < 2)
        //                singletons2++;
        //        }
        //        if (singletons1 < singletons2) {
        //            numNodes -= singletons1;
        //        } else {
        //            numNodes -= singletons2;
        //        }
        for (Module n1m : net1.getModules()) {
            double prob1 = n1m.getSize() / numNodes;
            for (Module n2m : net2.getModules()) {
                //                if (n1m.getSize() == 1 && n2m.getSize() == 1) {
                //                    continue;
                //                }
                double prob2 = n2m.getSize() / numNodes;
                double intersectProb = intersection(n1m.getNodes(),
                    n2m.getNodes()).size() / numNodes;
                mutInfo +=
                    intersectProb * log2(intersectProb / (prob1 * prob2));
            }
        }
        return mutInfo;
    }

    public static double log2(double num) {
        if (num == 0) {
            return 0;
        }
        return Math.log(num) / Math.log(2);
    }

    public static Set<Node> intersection(Set<Node> set1, Set<Node> set2) {
        Set<Node> dummy = new HashSet<Node>(set1);
        dummy.retainAll(set2);
        return dummy;
    }

    //    public static double percentNodeOverlap(Network net1, Network net2) {
    //        double percentOverlap = 0.0;
    //        int sigNodes = 0;
    //        double sum = 0.0;
    //        double[] vec1 = new double[net1.getNumNodes()];
    //        double[] vec2 = new double[net1.getNumNodes()];
    //        for (int i = 0; i < net1.getNumNodes(); i++) {
    //            sum +=
    //                net1.getFlatNodeList()[i].getRelativeFrequency()
    //                    * net2.getFlatNodeList()[i].getRelativeFrequency();
    //            vec1[i] = net1.getFlatNodeList()[i].getRelativeFrequency();
    //            vec2[i] = net2.getFlatNodeList()[i].getRelativeFrequency();
    //        }
    //        System.out.println(sum
    //            / (SimbrainMath.getVectorNorm(vec1) * SimbrainMath
    //                .getVectorNorm(vec2)));
    //        for (Module net1Mod : net1.getModules()) {
    //            if (net1Mod.getSize() == 1) {
    //                continue;
    //            }
    //            for (Node net1Node : net1Mod.getNodes()) {
    //                sigNodes++;
    //                Module other = findParentModule(net1Node, net2);
    //                Set<Node> otherModNodes = new HashSet<Node>(other.getNodes());
    //                int original = otherModNodes.size();
    //                otherModNodes.retainAll(net1Mod.getNodes());
    //                percentOverlap += (double) otherModNodes.size()
    //                    / (original + net1Mod.getSize());
    //            }
    //        }
    //        return percentOverlap / sigNodes;
    //    }
    //
    //    public static Module findParentModule(Node n, Network net) {
    //        for (Module m : net.getModules()) {
    //            if (m.containsNode(n))
    //                return m; // success
    //        }
    //        return null; // failure
    //    }

    public static void main(String[] args) {
        GephiXMLIn netReader =
            new GephiXMLIn("./resources/GephiXMLFiles/Hip2_1.gexf", 0.15);
        Network norm = netReader.createNetFromGEXF();
        GephiXMLOut netWriter =
            new GephiXMLOut(norm, "./resources/GephiXMLFiles/Hip2_1RelFreq");
        netWriter.createGEXFFromNet(false);
        netWriter
            .setFilename("./resources/GephiXMLFiles/Hip2_1RelFreqNodeHeat");
        netWriter.createGEXFFromNet(true);
    }

}
