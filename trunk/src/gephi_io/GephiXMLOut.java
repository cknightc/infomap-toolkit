package gephi_io;

import graph_elements.Module;
import graph_elements.Network;
import graph_elements.Node;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GephiXMLOut {

    private static final String XML_HEADER = "<?xml version=\"1.0\"" +
        " encoding=\"UTF-8\"?>";

    private static final String XMLNS = "<gexf " +
        "xmlns=\"http://www.gexf.net/1.2draft\" " +
        "xmlns:viz=\"http://www.gexf.net/1.1draft/viz\" " +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        "xsi:schemaLocation=\"http://www.gexf.net/1.2draft " +
        "http://www.gexf.net/1.2draft/gexf.xsd\" version=\"1.2\">";

    private static final String EXT = ".gexf";

    private Network net;

    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public GephiXMLOut(Network net, String filename) {
        this.net = net;
        this.filename = filename;
    }

    public static String getModName(int val) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append((char) (val % 26 + 65));
            val /= 26;
        } while (val > 0);

        return sb.reverse().toString();
    }

    public static Color randomColor() {
        Random rand = new Random();
        int red = rand.nextInt(127) + 128;
        int green = rand.nextInt(127) + 128;
        int blue = rand.nextInt(127) + 128;
        int color = Color.OPAQUE;
        color = color | blue;
        color = color | (green << 8);
        color = color | (red << 16);
        return new Color(color);
    }

    public void createGEXFFromNet(boolean heat) {

        try (
            PrintWriter pw =
                new PrintWriter(new FileWriter("./" + filename + EXT))) {
            pw.println(XML_HEADER);
            pw.println(XMLNS);
            pw.println("\t<graph mode=\"static\" defaultedgetype=\"directed\">");
            pw.println("\t\t<attributes class=\"edge\" mode=\"static\">" +
                " \n\t\t\t<attribute id=\"weight\" title=\"Weight\"" +
                " type=\"float\"></attribute> \n\t\t</attributes>");
            pw.println("\t\t<nodes>");
            int val = 0;
            // Sort by module size in descending order.
            List<Module> mods = new ArrayList<Module>(net.getModules());
            Collections.sort(mods, new Comparator<Module>() {
                @Override
                public int compare(Module m1, Module m2) {
                    if (m1.getSize() > m2.getSize()) {
                        return -1;
                    } else if (m1.getSize() < m2.getSize()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            for (Module m : mods) {
                StringBuilder sb = new StringBuilder("\t\t\t<node id=\"m");
                String modName = getModName(val++);
                sb.append(modName);
                sb.append("\" label=\"");
                sb.append(modName);
                sb.append(": ");
                sb.append(m.getSize());
                sb.append("\">");
                pw.println(sb.toString());
                sb = new StringBuilder("\t\t\t\t<viz:color r=\"");
                Color c;
                if (m.getSize() > 1) {
                    c = randomColor();
                } else {
                    c = new Color(0);
                }
                sb.append(c.getRed());
                sb.append("\" g =\"");
                sb.append(c.getGreen());
                sb.append("\" b=\"");
                sb.append(c.getBlue());
                sb.append("\" a=\"0.5\"/>");
                pw.println(sb.toString());
                sb = new StringBuilder("\t\t\t\t<viz:size value=\"");
                sb.append(m.getSumNodeFrequencies() * 5000);
                sb.append("\"/>");
                pw.println(sb.toString());
                pw.println("\t\t\t\t<nodes>");
                for (Node n : m.getNodes()) {
                    StringBuilder subSb =
                        new StringBuilder("\t\t\t\t\t<node id=\"n");
                    subSb.append(n.getIndex());
                    subSb.append("\" label=\" ");
                    subSb.append(n.getRelativeFrequency());
                    subSb.append(" \">");
                    pw.println(subSb.toString());
                    subSb = new StringBuilder("\t\t\t\t\t\t<viz:color r=\"");
                    if (heat) {
                        c = new Color(n.getColor());
                    }
                    subSb.append(c.getRed());
                    subSb.append("\" g=\"");
                    subSb.append(c.getGreen());
                    subSb.append("\" b=\"");
                    subSb.append(c.getBlue());
                    subSb.append("\" a=\"0.75\"/>");
                    pw.println(subSb.toString());
                    subSb = new
                        StringBuilder("\t\t\t\t\t\t<viz:position x=\"");
                    subSb.append(n.getX());
                    subSb.append("\" y=\"");
                    subSb.append(n.getY());
                    subSb.append("\" z=\"0\"/>");
                    pw.println(subSb.toString());
                    pw.println("\t\t\t\t\t</node>");
                }
                pw.println("\t\t\t\t</nodes>");
                pw.println("\t\t\t</node>");
            }
            pw.println("\t\t</nodes>");
            pw.println("\t\t<edges>");
            int eId = 0;
            for (Node n : net.getFlatNodeList()) {
                for (Node m : n.getTransferProbs().keySet()) {
                    if (n.getTransferProbs().get(m) == 0.0) {
                        continue;
                    }
                    StringBuilder edgeSb =
                        new StringBuilder("\t\t\t<edge id=\"e ");
                    edgeSb.append(eId++);
                    edgeSb.append("\" source=\"n");
                    edgeSb.append(n.getIndex());
                    edgeSb.append("\" target=\"n");
                    edgeSb.append(m.getIndex());
                    edgeSb.append("\">");
                    pw.println(edgeSb.toString());
                    pw.println("\t\t\t\t<attvalues>");
                    edgeSb = new StringBuilder("\t\t\t\t\t<attvalue " +
                        "for=\"weight\" value=\"");
                    edgeSb.append(n.getTransferProbs().get(m));
                    edgeSb.append("\"></attvalue>");
                    pw.println(edgeSb.toString());
                    pw.println("\t\t\t\t</attvalues>");
                    pw.println("\t\t\t</edge>");
                }
            }
            pw.println("\t\t</edges>");
            pw.println("\t</graph>");
            pw.println("</gexf>");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
