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
package graph_io.gephi_io;

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

/**
 * Reads in GephiXML files and converts them into a network object. Works with
 * .gexf files written by GephiXMLOut, but may not work for all .gexf files,
 * particularly those with attributes that have no counterpart in this program.
 * 
 * @author Zach Tosi
 */
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
        Scanner horc = null;
        PrintWriter pw = null;
        PrintWriter pw2 = null;
        PrintWriter pw3 = null;
        System.out.println("Begin? y/n");
        //        int checkNum = 2;

        try {
            try {
                String responseString = scanner.next();
                if (!responseString.matches("n")) {
                    GephiXMLIn gephIn;
                    String mutualInfoFN =
                        "../Neuro-Infomap_Resources/MutualInfoRelNS";
                    String percentDecreaseFN =
                        "../Neuro-Infomap_Resources/PercentEDecrease";
                    String sizesFN = "../Neuro-Infomap_Resources/NetworkSizes";
                    pw = new PrintWriter(new FileWriter(mutualInfoFN));
                    pw2 = new PrintWriter(new FileWriter(percentDecreaseFN));
                    pw3 = new PrintWriter(new FileWriter(sizesFN));
                    horc = new Scanner(new FileReader(
                        "../Neuro-Infomap_Resources/Cor_OR_Hip.txt"));
                    for (int i = 1; i <= 60; i++) {
                        Network[] networks = new Network[10];
                        String type = "";
                        if (horc.next().equals("C")) {
                            type = "Cortex";
                        } else {
                            type = "Hip";
                        }
                        for (int j = 1; j <= 10; j++) {
                            String graphFilename =
                                "../Neuro-Infomap_Resources/GephiXMLFiles/"
                                    + type + i + "_" + j
                                    + ".gexf";
                            gephIn = new GephiXMLIn(graphFilename, 0.15);
                            networks[j - 1] = gephIn.createNetFromGEXF();
                            pw2.print((networks[j - 1].getNodeEntropy()
                                - networks[j - 1].getHierarchicalEntropy())
                                / networks[j - 1].getNodeEntropy() + " \t");
                            pw3.print(networks[j - 1].getNumNodes() + "\t");
                        }
                        pw2.println();
                        pw3.println();
                        for (int k = 0; k < 9; k++) {
                            double kMI = NetworkComparison.mutualInformation(
                                networks[k + 1], networks[k + 1]);
                            double kMI1 = NetworkComparison.mutualInformation(
                                networks[k + 1], networks[k]);
                            pw.print(kMI1 / kMI + "\t");
                            //                                                        if (k == checkNum) {
                            //                                                            System.out
                            //                                                                .println(NetworkComparison
                            //                                                                    .mutualInformation(networks[k],
                            //                                                                        networks[k + 1]));
                            //                                                        }
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
            horc.close();
            pw.close();
            pw2.close();
            pw3.close();
            System.exit(0); // Done.
        }

    }

}
