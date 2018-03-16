/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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

package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.grouplens.samantha.modeler.metric.MetricResult;

import java.util.List;

public class Evaluation {
    @JsonProperty
    private final List<MetricResult> metrics;
    @JsonProperty
    private final boolean pass;

    Evaluation(List<MetricResult> metrics, boolean pass) {
        this.metrics = metrics;
        this.pass = pass;
    }

    Evaluation(List<MetricResult> metrics) {
        this.metrics = metrics;
        boolean pass = true;
        for (MetricResult metric : metrics) {
            if (!metric.getPass()) {
                pass = false;
            }
        }
        this.pass = pass;
    }

    public boolean getPass() {
        return pass;
    }
}
