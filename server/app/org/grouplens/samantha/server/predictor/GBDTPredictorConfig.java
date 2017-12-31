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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.boosting.GBDTProducer;
import org.grouplens.samantha.modeler.boosting.GBDT;
import org.grouplens.samantha.modeler.boosting.StandardBoostingMethod;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.tree.TreeLearningMethod;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class GBDTPredictorConfig implements PredictorConfig {
    private final String modelName;
    private final String modelFile;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final List<String> features;
    private final List<String> groupKeys;
    private final String labelName;
    private final String weightName;
    private final Configuration daoConfigs;
    private final List<Configuration> expandersConfig;
    private final Injector injector;
    private final TreeLearningMethod method;
    private final StandardBoostingMethod boostingMethod;
    private final Configuration objectiveConfig;
    private final String daoConfigKey;
    private final Configuration config;

    private GBDTPredictorConfig(String modelName, List<FeatureExtractorConfig> feaExtConfigs,
                                List<String> features, String labelName, String weightName,
                                Configuration daoConfigs, List<Configuration> expandersConfig,
                                Injector injector, TreeLearningMethod method,
                                List<String> groupKeys, String modelFile, Configuration objectiveConfig,
                                StandardBoostingMethod boostingMethod, String daoConfigKey,
                                Configuration config) {
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.features = features;
        this.labelName = labelName;
        this.weightName = weightName;
        this.daoConfigs = daoConfigs;
        this.groupKeys = groupKeys;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.method = method;
        this.modelFile = modelFile;
        this.objectiveConfig = objectiveConfig;
        this.boostingMethod = boostingMethod;
        this.daoConfigKey = daoConfigKey;
        this.config = config;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daoConfigs = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        int maxIter = predictorConfig.getInt("maxNumTrees");
        StandardBoostingMethod boostingMethod = new StandardBoostingMethod(maxIter);
        return new GBDTPredictorConfig(predictorConfig.getString("modelName"),
                feaExtConfigs, predictorConfig.getStringList("features"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), daoConfigs, expanders, injector,
                injector.instanceOf(TreeLearningMethod.class),
                predictorConfig.getStringList("groupKeys"),
                predictorConfig.getString("modelFile"),
                predictorConfig.getConfig("objectiveConfig"), boostingMethod,
                predictorConfig.getString("daoConfigKey"),
                predictorConfig);
    }

    private class GBDTModelManager extends AbstractModelManager {

        public GBDTModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile, new ArrayList<>());
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            GBDTProducer producer = injector.instanceOf(GBDTProducer.class);
            ObjectiveFunction objectiveFunction = PredictorUtilities.getObjectiveFunction(objectiveConfig,
                    injector, requestContext);
            GBDT model = producer.createGBRT(modelName, spaceMode, objectiveFunction, method,
                    features, groupKeys, featureExtractors, labelName, weightName);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            GBDT gbdt = (GBDT) model;
            JsonNode reqBody = requestContext.getRequestBody();
            LearningData data = PredictorUtilities.getLearningData(gbdt, requestContext,
                    reqBody.get("learningDaoConfig"), daoConfigs, expandersConfig,
                    injector, true, groupKeys);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(gbdt, requestContext,
                        reqBody.get("validationDaoConfig"), daoConfigs, expandersConfig,
                        injector, false, groupKeys);
            }
            boostingMethod.learn(gbdt, data, valid);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelManager modelManager = new GBDTModelManager(modelName, modelFile, injector);
        GBDT model = (GBDT) modelManager.manage(requestContext);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
