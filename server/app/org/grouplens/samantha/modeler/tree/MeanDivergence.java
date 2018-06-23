/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.modeler.tree;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.abs;

public class MeanDivergence implements RegressionCriterion {
    private static Logger logger = LoggerFactory.getLogger(MeanDivergence.class);
    private static final long serialVersionUID = 1L;
    private int sumWeight = 0;
    private double sumVal = 0.0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = Double.MIN_VALUE;

    public MeanDivergence() {}

    public void add(IntList idxList, List<double[]> resps) {
        for (int i=0; i<idxList.size(); i++) {
            add(idxList.getInt(i), resps);
        }
    }

    public void add(int idx, List<double[]> resps) {
        double[] resp = resps.get(idx);
        sumWeight += resp[1];
        sumVal += resp[0];
        if (minVal > resp[0]) {
            minVal = resp[0];
        }
        if (maxVal < resp[0]) {
            maxVal = resp[0];
        }
    }

    public void remove(IntList idxList, List<double[]> resps) {
        for (int i=0; i<idxList.size(); i++) {
            remove(idxList.getInt(i), resps);
        }
    }

    public void remove(int idx, List<double[]> resps) {
        double[] resp = resps.get(idx);
        sumWeight -= resp[1];
        sumVal -= resp[0];
    }

    public double getValue(List<double[]> resps) {
        if (sumWeight > 0.0) {
            return sumVal / sumWeight;
        } else {
            return 0.0;
        }
    }

    public double getSplittingGain(double beforeValue,
                                   SplittingCriterion leftSplit,
                                   SplittingCriterion rightSplit,
                                   List<double[]> resps) {
        MeanDivergence left = (MeanDivergence) leftSplit;
        MeanDivergence right = (MeanDivergence) rightSplit;
        if (left.sumWeight == 0.0 || right.sumWeight == 0.0) {
            return 0.0;
        }
        double sumWeight = left.sumWeight + right.sumWeight;
        double minVal = left.minVal < right.minVal ? left.minVal : right.minVal;
        double maxVal = left.maxVal > right.maxVal ? left.maxVal : right.maxVal;
        if (minVal == maxVal) {
            return 0.0;
        }
        double gain = abs(left.getValue(resps) - right.getValue(resps)) / (maxVal - minVal) *
                (left.sumWeight / sumWeight) * (right.sumWeight / sumWeight);
        return gain;
    }

    public MeanDivergence create() {
        return new MeanDivergence();
    }
}
