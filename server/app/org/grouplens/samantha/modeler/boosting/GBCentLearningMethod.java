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

package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.instance.StandardLearningInstance;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class GBCentLearningMethod implements LearningMethod {
    private static Logger logger = LoggerFactory.getLogger(GBCentLearningMethod.class);
    private final OnlineOptimizationMethod optimizationMethod;
    private final boolean learnSvdfea;

    private final int minSupport;
    private final double minTreeGain;
    private final TreeLearningMethod treeLearningMethod;

    @Inject
    public GBCentLearningMethod(OnlineOptimizationMethod optimizationMethod,
                                TreeLearningMethod treeLearningMethod) {
        this.optimizationMethod = optimizationMethod;
        this.treeLearningMethod = treeLearningMethod;
        this.minSupport = 50;
        this.learnSvdfea = false;
        this.minTreeGain = 0.0;
    }

    public GBCentLearningMethod(OnlineOptimizationMethod optimizationMethod,
                                TreeLearningMethod treeLearningMethod,
                                int minSupport, boolean learnSvdfea,
                                double minTreeGain) {
        this.optimizationMethod = optimizationMethod;
        this.treeLearningMethod = treeLearningMethod;
        this.minSupport = minSupport;
        this.learnSvdfea = learnSvdfea;
        this.minTreeGain = minTreeGain;
    }

    private void initializeFeatureCount(List<double[]> feaCnt, int size) {
        for (int i=0; i<size; i++) {
            double[] one = {0.0, 0.0};
            feaCnt.add(one);
        }
    }

    private void initializeTreeDatas(List<List<StandardLearningInstance>> treeDatas, int size) {
        for (int i=0; i<size; i++) {
            treeDatas.add(new ArrayList<>());
        }
    }

    private void initializeSubset(List<IntList> subset, int size) {
        for (int i=0; i<size; i++) {
            subset.add(new IntArrayList());
        }
    }

    private void initializeValidObjs(DoubleList validObjs, int size) {
        for (int i=0; i<size; i++) {
            validObjs.add(0.0);
        }
    }

    private void initializeLearnObjs(DoubleList learnObjs, int size) {
        for (int i=0; i<size; i++) {
            learnObjs.add(0.0);
        }
    }

    public void learn(PredictiveModel model, LearningData learnData, LearningData validData) {
        GBCent cent = (GBCent) model;
        SVDFeature svdfeaModel = cent.getSVDFeatureModel();
        if (learnSvdfea) {
            GBCentSVDFeatureData gblearnData = new GBCentSVDFeatureData(learnData);
            GBCentSVDFeatureData gbvalidData = null;
            if (validData != null) {
                gbvalidData = new GBCentSVDFeatureData(validData);
            }
            optimizationMethod.minimize(svdfeaModel, gblearnData, gbvalidData);
        } else {
            learnData.startNewIteration();
            while (learnData.getLearningInstance() != null) {}
        }
        int numBiases = svdfeaModel.getScalarVarSizeByName(SVDFeatureKey.BIASES.get());
        List<double[]> feaCnt = new ArrayList<>(numBiases);
        initializeFeatureCount(feaCnt, numBiases);
        List<List<StandardLearningInstance>> learnTreeDatas = new ArrayList<>(numBiases);
        List<IntList> learnSubset = new ArrayList<>(numBiases);
        DoubleList learnObjs = new DoubleArrayList(numBiases);
        initializeTreeDatas(learnTreeDatas, numBiases);
        initializeSubset(learnSubset, numBiases);
        initializeLearnObjs(learnObjs, numBiases);
        DoubleList learnPreds = new DoubleArrayList();
        ObjectiveFunction objectiveFunction = svdfeaModel.getObjectiveFunction();
        learnData.startNewIteration();
        int cnt = 0;
        List<LearningInstance> instances;
        while ((instances = learnData.getLearningInstance()).size() > 0) {
            List<StochasticOracle> oracles = new ArrayList<>(instances.size());
            BoostingUtilities.setStochasticOracles(instances, oracles, svdfeaModel, learnPreds);
            objectiveFunction.wrapOracle(oracles);
            for (int i=0; i<instances.size(); i++) {
                GBCentLearningInstance centIns = (GBCentLearningInstance) instances.get(i);
                for (Feature feature : centIns.getSvdfeaIns().getBiasFeatures()) {
                    int idx = feature.getIndex();
                    double[] one = feaCnt.get(idx);
                    one[0] = idx;
                    one[1] += 1;
                    learnTreeDatas.get(idx).add(centIns.getTreeIns());
                    learnSubset.get(idx).add(cnt);
                    learnObjs.set(idx, oracles.get(i).getObjectiveValue() + learnObjs.getDouble(idx));
                }
                cnt++;
            }
        }
        DoubleList validPreds = null;
        List<List<StandardLearningInstance>> validTreeDatas = null;
        List<IntList> validSubset = null;
        DoubleList validObjs = null;
        if (validData != null) {
            validPreds = new DoubleArrayList();
            validTreeDatas = new ArrayList<>(numBiases);
            validSubset = new ArrayList<>(numBiases);
            validObjs = new DoubleArrayList(numBiases);
            initializeTreeDatas(validTreeDatas, numBiases);
            initializeSubset(validSubset, numBiases);
            initializeValidObjs(validObjs, numBiases);
            validData.startNewIteration();
            cnt = 0;
            while ((instances = validData.getLearningInstance()).size() > 0) {
                List<StochasticOracle> oracles = new ArrayList<>(instances.size());
                BoostingUtilities.setStochasticOracles(instances, oracles, svdfeaModel, validPreds);
                objectiveFunction.wrapOracle(oracles);
                for (int i=0; i<instances.size(); i++) {
                    GBCentLearningInstance centIns = (GBCentLearningInstance) instances.get(i);
                    for (Feature feature : centIns.getSvdfeaIns().getBiasFeatures()) {
                        int idx = feature.getIndex();
                        validTreeDatas.get(idx).add(centIns.getTreeIns());
                        validSubset.get(idx).add(cnt);
                        validObjs.set(idx, oracles.get(i).getObjectiveValue() + validObjs.getDouble(idx));
                    }
                    cnt++;
                }
            }
        }
        GradientBoostingMachine gbm = new GradientBoostingMachine(objectiveFunction);
        feaCnt.sort(SortingUtilities.pairDoubleSecondReverseComparator());
        int acceptedTrees = 0;
        int learnedTrees = 0;
        for (double[] one : feaCnt) {
            if (one[1] < minSupport) {
                break;
            }
            learnedTrees++;
            int treeIdx = (int)one[0];
            PredictiveModel tree = cent.getNumericalTree(treeIdx);
            LearningData treeLearnData = cent.getLearningData(learnTreeDatas.get(treeIdx));
            LearningData treeValidData = null;
            IntList validSub = null;
            if (validData != null) {
                treeValidData = cent.getLearningData(validTreeDatas.get(treeIdx));
                validSub = validSubset.get(treeIdx);
            }
            IntList learnSub = learnSubset.get(treeIdx);
            gbm.boostModel(learnPreds, validPreds, learnSub, validSub, tree,
                    treeLearningMethod, treeLearnData, treeValidData);
            if (validData != null) {
                double curVal = gbm.evaluate(validPreds, tree, treeValidData, validSub);
                double oldVal = validObjs.getDouble(treeIdx);
                logger.debug("Before adding: {}, after adding: {}", oldVal, curVal);
                if ((oldVal - curVal) / validSub.size() <= minTreeGain) {
                    continue;
                }
                validObjs.set(treeIdx, curVal);
                gbm.boostPrediction(validPreds, tree, treeValidData, validSub);
            } else if (minTreeGain > 0.0) {
                double curLearn = gbm.evaluate(learnPreds, tree, treeLearnData, learnSub);
                double oldLearn = learnObjs.getDouble(treeIdx);
                logger.debug("Before adding: {}, after adding: {}", oldLearn, curLearn);
                if ((oldLearn - curLearn) / learnSub.size() <= minTreeGain) {
                    continue;
                }
            }
            gbm.boostPrediction(learnPreds, tree, treeLearnData, learnSub);
            cent.setNumericalTree(treeIdx, tree);
            acceptedTrees++;
        }
        logger.info("Learned {} trees and accepted {} trees.", learnedTrees, acceptedTrees);
    }

    public double updateSVDFeatureModel(GBCent cent, LearningData learnData) {
        SVDFeature svdFeature = cent.getSVDFeatureModel();
        return optimizationMethod.update(svdFeature, learnData);
    }

    public void updateDecisionTree(GBCent cent, LearningData learnData, LearningData validData) {
        //TODO: implementation, needs support from cent to predict without using one treeIdx
        //TODO: this is very similar to iterative back-fitting algorithm now
    }
}
