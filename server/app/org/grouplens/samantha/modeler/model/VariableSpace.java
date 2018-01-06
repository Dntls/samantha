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

package org.grouplens.samantha.modeler.model;

import com.google.inject.ImplementedBy;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.solver.RandomInitializer;

import java.io.Serializable;
import java.util.List;

/**
 * Every method needs to be thread-safe.
 */
@ImplementedBy(SynchronizedVariableSpace.class)
public interface VariableSpace extends Serializable {
    default void initializeVector(RealVector vec, double initial,
                                  boolean randomize, boolean normalize) {
        if (randomize) {
            RandomInitializer randInit = new RandomInitializer();
            randInit.randInitVector(vec, normalize);
        } else {
            if (initial != 0.0) {
                vec.set(initial);
            }
        }
    }

    default double initialScalarVar(double initial, boolean randomize) {
        if (randomize) {
            RandomInitializer randInit = new RandomInitializer();
            return randInit.randInitValue();
        } else {
            return initial;
        }
    }

    void setSpaceState(String spaceName, SpaceMode spaceMode);
    void publishSpaceVersion();
    void requestScalarVar(String name, int size, double initial, boolean randomize);
    boolean hasScalarVar(String name);
    void ensureScalarVar(String name, int size, double initial, boolean randomize);
    void requestVectorVar(String name, int size, int dim, double initial,
                                       boolean randomize, boolean normalize);
    boolean hasVectorVar(String name);
    void ensureVectorVar(String name, int size, int dim, double initial,
                                      boolean randomize, boolean normalize);
    void freeSpace();
    void freeScalarVar(String name);
    void freeVectorVar(String name);
    RealVector getScalarVarByName(String name);
    int getScalarVarSizeByName(String name);
    void setScalarVarByName(String name, RealVector vars);
    double getScalarVarByNameIndex(String name, int index);
    void setScalarVarByNameIndex(String name, int index, double var);

    List<RealVector> getVectorVarByName(String name);
    RealMatrix getMatrixVarByName(String name);
    int getVectorVarSizeByName(String name);
    int getVectorVarDimensionByName(String name);
    RealVector getVectorVarByNameIndex(String name, int index);
    void setVectorVarByNameIndex(String name, int index, RealVector var);

    List<String> getAllScalarVarNames();
    List<String> getAllVectorVarNames();
}
