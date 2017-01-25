/**
 * This file is part of eps4j-choco, http://github.com/eps4j/eps4j-choco
 *
 * Copyright (c) 2017, Arnaud Malapert, Université Nice Sophia Antipolis. All rights reserved.
 *
 * Licensed under the BSD 3-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.eps4j.chocosolver.samples;

import static org.chocosolver.solver.search.strategy.Search.inputOrderLBSearch;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.eps4j.EPSComm;
import org.kohsuke.args4j.Option;

/**
 * CSPLib prob006:<br/>
 * A Golomb ruler may be defined as a set of m integers 0 = a_1 < a_2 < ... < a_m such that
 * the m(m-1)/2 differences a_j - a_i, 1 <= i < j <= m are distinct.
 * Such a ruler is said to contain m marks and is of length a_m.
 * <br/>
 * The objective is to find optimal (minimum length) or near optimal rulers.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 31/03/11
 */
public class GolombRuler extends AbstractProblem {

    @Option(name = "-m", usage = "Golomb ruler order.", required = false)
    private int m = 10;

    IntVar[] ticks;
    IntVar[] diffs;
    IntVar[][] m_diffs;


    @Override
    public void buildModel() {
        model = new Model("GolombRuler");
        ticks = model.intVarArray("a", m, 0, (m < 31) ? (1 << (m + 1)) - 1 : 9999, false);

        model.arithm(ticks[0], "=", 0).post();

        for (int i = 0; i < m - 1; i++) {
            model.arithm(ticks[i + 1], ">", ticks[i]).post();
        }

        diffs = model.intVarArray("d", (m * m - m) / 2, 0, (m < 31) ? (1 << (m + 1)) - 1 : 9999, false);
        m_diffs = new IntVar[m][m];
        for (int k = 0, i = 0; i < m - 1; i++) {
            for (int j = i + 1; j < m; j++, k++) {
                // d[k] is m[j]-m[i] and must be at least sum of first j-i integers
                // <cpru 04/03/12> it is worth adding a constraint instead of a view
                model.scalar(new IntVar[]{ticks[j], ticks[i]}, new int[]{1, -1}, "=", diffs[k]).post();
                model.arithm(diffs[k], ">=", (j - i) * (j - i + 1) / 2).post();
                model.arithm(diffs[k], "-", ticks[m - 1], "<=", -((m - 1 - j + i) * (m - j + i)) / 2).post();
                model.arithm(diffs[k], "<=", ticks[m - 1], "-", ((m - 1 - j + i) * (m - j + i)) / 2).post();
                m_diffs[i][j] = diffs[k];
            }
        }
        model.allDifferent(diffs, "BC").post();

        // break symetries
        if (m > 2) {
            model.arithm(diffs[0], "<", diffs[diffs.length - 1]).post();
        }
    }

    @Override
    public void configureSearch() {
        model.getSolver().setSearch(inputOrderLBSearch(ticks));
        model.setObjective(false, (IntVar) model.getVars()[m - 1]);        
    }

    @Override
    public void solve() {
        while (model.getSolver().solve()) {
            // nothing to do
        }
       EPSComm.LOGGER.info("<" + EPSComm.rank() + ">\n" + model.getSolver().toDimacsString());
       EPSComm.LOGGER.info(model.getSolver().toCSV());
    }
    public static void main(String[] args) {
        final GolombRuler ruler = new GolombRuler();
        ruler.execute(args);
        System.out.println(ruler.model.getSolver().toDimacsString());
    }
}
