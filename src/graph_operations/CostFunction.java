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
package graph_operations;

import graph_elements.Module;

import java.util.Collection;

import math.SimbrainMath;

/**
 * A class containing the infomap cost function.
 * 
 * @author Zach Tosi
 *
 */
public final class CostFunction {

    /**
     * The third term here differs from reference paper, in that the node
     * entropy as a whole term (which includes a negative value to reverse the
     * sign as a part of the entropy function) is passed in as a positive value,
     * thus it is added, not subtracted.
     * 
     * @param proposedPartitioning
     * @param nodeEntropy
     * @return
     */
    public static double cost(Collection<Module> proposedPartitioning,
        double nodeEntropy) {
        /*
         * som: Sum over modules son: Sum over nodes sonim: Sum over nodes in
         * module
         * 
         * 1st term: som ( exitProb ) * log_2( som (exitProb) )
         * 
         * 2nd term: (1 + 1) * som ( exitProb * log_2 (exitProb) )
         * 
         * 3rd term: son ( node_freq * log_2 ( node_freq ) )
         * 
         * 4th term: som (
         * 
         * ( exitProb + sonim ( node_freq ) ) * log_2 ( exitProb + sonim (
         * node_freq ) )
         * 
         * )
         * 
         * result = 1 - 2 +** 3 + 4
         * 
         * **see javadoc
         */
        double firstTerm = 0;
        double sumExitProbs = 0;
        for (Module m : proposedPartitioning) {
            sumExitProbs += m.getExitProbability();
        }
        firstTerm += sumExitProbs * SimbrainMath.log2(sumExitProbs);

        // System.out.println("FT: " + firstTerm);

        double secondTerm = 0;
        for (Module m : proposedPartitioning) {
            secondTerm += m.getExitProbability()
                * SimbrainMath.log2(m.getExitProbability());
        }
        secondTerm *= 2;

        // System.out.println("ST: " + secondTerm);

        double thirdTerm = nodeEntropy;

        // System.out.println("TT: " + thirdTerm);

        double fourthTerm = 0;
        for (Module m : proposedPartitioning) {
            double subTerm = m.getExitProbability() + m.getSumNodeFrequencies();
            fourthTerm += subTerm * SimbrainMath.log2(subTerm);
        }
        // System.out.println("4T: " + fourthTerm);
        // System.out.println("FINAL: " + (firstTerm - secondTerm - thirdTerm +
        // fourthTerm));
        return (firstTerm - secondTerm + thirdTerm + fourthTerm);
    }

}
