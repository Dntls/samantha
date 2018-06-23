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

package org.grouplens.samantha.server.objective;

import org.grouplens.samantha.modeler.ranking.MAPLoss;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankerUtilities;
import play.Configuration;
import play.inject.Injector;

public class MAPLossConfig {
    static public ObjectiveFunction getObjectiveFunction(Configuration objectiveConfig,
                                                         Injector injector,
                                                         RequestContext requestContext) {
        double sigma = 1.0;
        int N = RankerUtilities.defaultPageSize;
        double threshold = 0.5;
        if (objectiveConfig.asMap().containsKey("sigma")) {
            sigma = objectiveConfig.getDouble("sigma");
        }
        if (objectiveConfig.asMap().containsKey("N")) {
            N = objectiveConfig.getInt("N");
        }
        if (objectiveConfig.asMap().containsKey("threshold")) {
            threshold = objectiveConfig.getDouble("threshold");
        }
        return new MAPLoss(N, sigma, threshold);
    }
}
