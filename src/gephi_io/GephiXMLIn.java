package gephi_io;

import graph_elements.Module;
import graph_elements.Network;
import graph_elements.Node;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import analysis.NetworkComparison;

public class GephiXMLIn {

    private String filename;

    private double teleportProb;

    public GephiXMLIn(String filename, double teleportProb) {
        this.filename = filename;
        this.teleportProb = teleportProb;
    }

    public Network createNetFromGEXF() {
        Network net = null;
        try (Scanner scan = new Scanner(new FileReader(filename))) {
            scan.useDelimiter("\"");
            while (scan.hasNextLine() && scan.findInLine("<nodes>") == null) {
                scan.nextLine();
            }
            String line;
            Set<Module> mods = Collections
                .synchronizedSet(new HashSet<Module>());
            int depth = 0;
            int numNodes = 0;
            ArrayList<String> txtLines = new ArrayList<String>();
            do {
                line = scan.nextLine();
                if (depth >= 2) {
                    txtLines.add(line);
                }
                if (scan.findInLine("<edges>") != null) {
                    break;
                }
                if (scan.findInLine("<nodes>") != null) {
                    depth++;
                }
                if (scan.findInLine("<node") != null) {
                    depth++;
                }
                if (scan.findInLine("</node") != null) {
                    depth--;
                }
                if (depth == 0) {
                    Module module = parseModule(txtLines);
                    mods.add(module);
                    numNodes += module.getSize();
                    txtLines = new ArrayList<String>();
                }
            } while (scan.hasNextLine() && !line.matches("<edges>"));
            Node[] flatNodeList = new Node[numNodes];
            for (Module m : mods) {
                m.calcSumNodeFreqs();
                m.calcExitProb(teleportProb, numNodes);
                for (Node n : m.getNodes()) {
                    flatNodeList[n.getIndex()] = n;
                }
            }
            net = new Network(Arrays.asList(flatNodeList), mods);
            txtLines = new ArrayList<String>();
            do {
                line = scan.nextLine();
                txtLines.add(line);
                if (scan.findInLine("</edge>") != null) {
                    parseEdge(txtLines, flatNodeList);
                    txtLines = new ArrayList<String>();
                }
            } while (scan.hasNextLine());

        } catch (IOException | NullPointerException
            | ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return net;
    }

    public void parseEdge(ArrayList<String> lines, Node[] flatNodeList) {
        Node src = null;
        Node tar = null;
        for (String s : lines) {
            Scanner sc = new Scanner(s);
            sc.useDelimiter("[\"n\\s]+");
            if (sc.findInLine("<edge id=\"e") != null) {
                sc.next();
                sc.next();
                src = flatNodeList[sc.nextInt()];
                sc.next();
                tar = flatNodeList[sc.nextInt()];
            }
            if (sc.findInLine("attvalue for=") != null) {
                sc.next();
                sc.next();
                src.addOutgoingEdge(tar, sc.nextDouble());
                sc.close();
                break;
            }
            sc.close();
        }
    }

    public Module parseModule(ArrayList<String> lines) {
        Module m = new Module(teleportProb);
        Node currentNode = null;
        for (String s : lines) {
            Scanner sc = new Scanner(s);
            sc.useDelimiter("[\"\\s]+");
            if (sc.findInLine("id=\"n") != null) {
                currentNode = new Node(sc.nextInt());
                sc.next();
                currentNode.setRelativeFrequency(sc.nextDouble());
            }
            if (sc.findInLine("<viz:position") != null) {
                sc.next();
                currentNode.setX(Double.parseDouble(sc.next()));
                sc.next();
                currentNode.setY(Double.parseDouble(sc.next()));
                m.addNodeQuiet(currentNode);
            }
            sc.close();
        }
        return m;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PrintWriter pw = null;
        //        PrintWriter pw2 = null;
        System.out.println("Begin? y/n");
        //        int checkNum = 2;
        try {
            try {
                String responseString = scanner.next();
                if (!responseString.matches("n")) {
                    GephiXMLIn gephIn;
                    String mutualInfoFN = "./resources/MutualInfoRelNS";
                    //                    String percentDecreaseFN = "./resources/PercentEDecrease";
                    pw = new PrintWriter(new FileWriter(mutualInfoFN));
                    //                    pw2 = new PrintWriter(new FileWriter(percentDecreaseFN));
                    for (int i = 1; i <= 60; i++) {
                        Network[] networks = new Network[10];
                        for (int j = 1; j <= 10; j++) {
                            String graphFilename =
                                "./resources/GephiXMLFiles/Hip" + i + "_" + j
                                    + ".gexf";
                            gephIn = new GephiXMLIn(graphFilename, 0.15);
                            networks[j - 1] = gephIn.createNetFromGEXF();
                            //                            pw2.print((networks[j - 1].getNodeEntropy()
                            //                                - networks[j - 1].getHierarchicalEntropy())
                            //                                / networks[j - 1].getNodeEntropy() + " \t");
                        }
                        //                        pw2.println();
                        for (int k = 0; k < 9; k++) {
                            double kMI = NetworkComparison.mutualInformation(
                                networks[k + 1], networks[k + 1]);
                            double kMI1 = NetworkComparison.mutualInformation(
                                networks[k + 1], networks[k]);
                            pw.print(kMI1 / kMI + "\t");
                            //                            if (k == checkNum) {
                            //                                System.out
                            //                                    .println(NetworkComparison
                            //                                        .mutualInformation(networks[k],
                            //                                            networks[k + 1]));
                            //                            }
                        }
                        if (i != 60) {
                            pw.println();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            scanner.close();
            pw.close();
            //            pw2.close();
            System.exit(0); // Done.
        }

    }

}
