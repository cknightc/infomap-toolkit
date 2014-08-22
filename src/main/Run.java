package main;

import graph_elements.Network;
import graph_io.MatrixReader;
import graph_io.gephi_io.GephiXMLOut;
import graph_operations.searches.GreedySearch;

import java.io.FileReader;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Run {

    public static final double DEFAULT_TELEPORT_PROBABILITY = 0.15;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Scanner horc = null;
        System.out.println("Begin? y/n");
        try {
            try {
                String responseString = scanner.next();
                if (!responseString.matches("n")) {
                    for (int i = 1; i <= 60; i++) {
                        horc = new Scanner(new FileReader(
                            "./resources/Cor_OR_Hip.txt"));
                        String type = "";
                        if (horc.next().equals("C")) {
                            type = "Cortex";
                        } else {
                            type = "Hip";
                        }
                        String xyFilename =
                            "./resources/XYCoordinates/Hip_XY/Hip" + i + "XY";
                        for (int j = 1; j <= 10; j++) {
                            String teFilename =
                                "./resources/TEMatrixFiles/TE_Hip_Normal/"
                                    + "TEHip" + i + "_" + j;
                            String graphFilename =
                                "./resources/GephiXMLFiles/" + type + i + "_"
                                    + j;
                            String graphFileNameHeat =
                                "./resources/GephiXMLFiles/" + type + i + "_"
                                    + j
                                    + "NodeHeat";
                            System.out.println(teFilename);
                            Network net =
                                new Network(MatrixReader
                                    .matrixReader(teFilename),
                                    xyFilename, DEFAULT_TELEPORT_PROBABILITY,
                                    false);
                            CountDownLatch c = new CountDownLatch(1);
                            GreedySearch searcher = new GreedySearch(net);
                            searcher.setExternalLatch(c);
                            searcher.search();
                            try {
                                c.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            GephiXMLOut gxmlWriter =
                                new GephiXMLOut(net, graphFilename);
                            gxmlWriter.createGEXFFromNet(false);
                            gxmlWriter.setFilename(graphFileNameHeat);
                            gxmlWriter.createGEXFFromNet(true);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            scanner.close();
            horc.close();
            System.exit(0); // Done.
        }

    }
}
