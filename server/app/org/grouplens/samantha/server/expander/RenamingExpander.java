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
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RenamingExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(RenamingExpander.class);
    private final List<String> fieldNames;
    private final List<String> newFieldNames;
    private final Boolean appendix;

    private RenamingExpander(List<String> fieldNames, Boolean appendix,
                             List<String> newFieldNames) {
        this.fieldNames = fieldNames;
        this.appendix = appendix;
        this.newFieldNames = newFieldNames;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new RenamingExpander(expanderConfig.getStringList("fieldNames"),
                expanderConfig.getBoolean("appendix"),
                expanderConfig.getStringList("newFieldNames"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            for (int i=0; i<fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                if (entity.has(fieldName)) {
                    if (newFieldNames != null && newFieldNames.size() == fieldNames.size()) {
                        entity.set(newFieldNames.get(i), entity.get(fieldName));
                    } else if (appendix != null) {
                        entity.set(fieldName + appendix, entity.get(fieldName));
                    } else {
                        entity.set(fieldName, entity.get(fieldName));
                    }
                } else {
                    logger.warn("The field {} is not present: {}", fieldName, entity.toString());
                }
            }
        }
        return initialResult;
    }
}
