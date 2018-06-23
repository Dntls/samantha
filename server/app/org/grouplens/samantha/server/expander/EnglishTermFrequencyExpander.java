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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnglishTermFrequencyExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(EnglishTermFrequencyExpander.class);
    final private List<String> textFields;
    final private String termField;
    final private String termFreqField;
    final private Analyzer analyzer;

    public EnglishTermFrequencyExpander(String termField, List<String> textFields, String termFreqField) {
        this.termFreqField = termFreqField;
        this.textFields = textFields;
        this.termField = termField;
        this.analyzer = new EnglishAnalyzer();
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new EnglishTermFrequencyExpander(expanderConfig.getString("termField"),
                expanderConfig.getStringList("textFields"),
                expanderConfig.getString("termFreqField"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                  RequestContext requestContext) {
        List<ObjectNode> expandedList = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<String> textList = new ArrayList<>();
            for (String textField : textFields) {
                if (entity.has(textField)) {
                    textList.add(entity.get(textField).asText());
                } else {
                    logger.warn("The text field {} is not present: {}", textField, entity.toString());
                }
            }
            String text = StringUtils.join(textList, ". ");
            if (!"".equals(text)) {
                Map<String, Integer> termFreq = FeatureExtractorUtilities.getTermFreq(analyzer, text, termField);
                for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                    ObjectNode newEntity = Json.newObject();
                    IOUtilities.parseEntityFromJsonNode(entity, newEntity);
                    newEntity.put(termField, entry.getKey());
                    newEntity.put(termFreqField, entry.getValue());
                    expandedList.add(newEntity);
                }
            }
        }
        return expandedList;
    }
}
