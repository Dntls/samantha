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

package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For pairwise ranking learning. E.g. if the negative item attribute is item_neg, 
 * then attrName should be item_neg, toReplace should be _neg.
 */
public class NegativeStringValueExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(NegativeStringValueExtractor.class);
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String toReplace;

    public NegativeStringValueExtractor(String indexName,
                                        String attrName,
                                        String feaName, 
                                        String toReplace) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.toReplace = toReplace;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            String key = FeatureExtractorUtilities.composeKey(
                attrName.replace(toReplace, ""), 
                entity.get(attrName).asText()
            );
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                    indexSpace, indexName, key, -1.0);
            feaMap.put(feaName, features);
        } else {
            logger.warn("{} is not present in {}", attrName, entity);
        }
        return feaMap;
    }
}
