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
package graph_io;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

import math.SimbrainMath;

public class MatrixReader {

    public static double[][] matrixReader(String filename) {
        LinkedList<LinkedList<Double>> holder =
            new LinkedList<LinkedList<Double>>();
        try (Scanner scan = new Scanner(new FileReader(filename))) {
            while (scan.hasNextLine()) {
                Scanner lineScan = new Scanner(scan.nextLine());
                LinkedList<Double> currLine = new LinkedList<Double>();
                holder.add(currLine);
                while (lineScan.hasNextDouble()) {
                    currLine.add(lineScan.nextDouble());
                }
                lineScan.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[][] mat = new double[holder.size()][];
        int r_count = 0;
        while (holder.size() > 0) {
            int c_count = 0;
            LinkedList<Double> row = holder.pollFirst();
            double[] f_row = new double[row.size()];
            while (row.size() > 0) {
                f_row[c_count++] = row.pollFirst();
            }
            //            mat[r_count++] = SimbrainMath.normalizeVec(f_row);
            mat[r_count++] = f_row;
        }
        if (squareCheck(mat).equals(ReturnStatus.SUCCESS)) {
            return mat;
        } else {
            throw new IllegalArgumentException("Matrix is either non-square" +
                " and/or has non-uniform columns.");
        }
    }

    public static ReturnStatus squareCheck(double[][] mat) {
        if (mat.length > 0) {
            int len = mat.length;
            for (double[] row : mat) {
                if (row.length != len) {
                    return ReturnStatus.FAILURE; // Array has rows of different
                    // lengths and/or is non-square
                }
            }
            return ReturnStatus.SUCCESS;
        } else {
            return ReturnStatus.FAILURE; // Matrix is of size 0
        }
    }

    public static ReturnStatus sanityCheck(double[][] mat) {
        for (double[] row : mat) {
            double sum = SimbrainMath.sum(row);
            if (!(sum == 0 || sum == 1)) {
                return ReturnStatus.FAILURE;
            }
        }
        return ReturnStatus.SUCCESS;
    }

    public enum ReturnStatus {
        SUCCESS, FAILURE;
    }

    public static void main(String[] args) {
        double[][] mat = matrixReader("./resources/TE09-1");
        for (double[] row : mat) {
            System.out.println(Arrays.toString(row));
        }
    }

}
