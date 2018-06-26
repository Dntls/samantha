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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

public class ESBasedJoinExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(ESBasedJoinExpander.class);
    final private String elasticSearchIndex;
    final private ElasticSearchService elasticSearchService;
    final private List<Configuration> configList;

    public ESBasedJoinExpander(ElasticSearchService elasticSearchService,
                               String elasticSearchIndex,
                               List<Configuration> configList) {
        this.elasticSearchService = elasticSearchService;
        this.configList = configList;
        this.elasticSearchIndex = elasticSearchIndex;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new ESBasedJoinExpander(injector.instanceOf(ElasticSearchService.class),
                expanderConfig.getString("elasticSearchIndex"),
                expanderConfig.getConfigList("expandFields"));
    }


    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                  RequestContext requestContext) {
        ArrayNode arr = Json.newArray();
        for (ObjectNode one : initialResult) {
            arr.add(one);
        }
        for (Configuration config : configList) {
            String type = config.getString("type");
            List<String> keys = config.getStringList("keys");
            List<String> entityFields = config.getStringList("fields");
            Map<Map<String, String>, List<SearchHit>> keyVals = elasticSearchService
                    .searchFieldsByKeys(elasticSearchIndex, type, keys,
                            entityFields, arr);
            for (ObjectNode entity : initialResult) {
                Map<String, String> keyVal = IOUtilities.getKeyValueFromEntity(entity,
                        keys);
                if (keyVals.containsKey(keyVal) && keyVal.size() > 0 && keyVals.get(keyVal) != null) {
                    ExpanderUtilities.parseEntityFromSearchHit(entityFields,
                            null, keyVals.get(keyVal).get(0), entity);
                } else {
                    logger.warn("Can not find the key {} while joining: {}", keyVal.toString(),
                            entity.toString());
                }
            }
        }
        return initialResult;
    }
}
