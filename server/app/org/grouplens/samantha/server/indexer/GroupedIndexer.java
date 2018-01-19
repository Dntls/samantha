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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.CSVFileDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupedIndexer extends AbstractIndexer {
    private final Indexer indexer;
    private final String dataDir;
    private final int numBuckets;
    private final List<String> groupKeys;
    private final List<String> groupKeysTypes;
    private final List<String> orderFieldsTypes;
    private final List<String> dataFields;
    private final List<String> orderFields;
    private final Boolean descending;
    private final String separator;
    private final String filesKey;
    private final String daoNameKey;
    private final String daoName;
    private final String separatorKey;
    private final int usedBuckets;
    private final boolean skip;

    public GroupedIndexer(SamanthaConfigService configService,
                          Configuration config, Injector injector,
                          Configuration daoConfigs, String daoConfigKey,
                          Indexer indexer, String dataDir,
                          int numBuckets, List<String> groupKeys,
                          List<String> dataFields, String separator,
                          List<String> orderFields, Boolean descending,
                          String filesKey, String daoName, String daoNameKey,
                          String separatorKey, int usedBuckets, boolean skip,
                          List<String> groupKeysTypes, List<String> orderFieldsTypes) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.dataDir = dataDir;
        this.numBuckets = numBuckets;
        this.groupKeys = groupKeys;
        this.dataFields = dataFields;
        this.separator = separator;
        this.orderFields = orderFields;
        this.descending = descending;
        this.filesKey = filesKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.separatorKey = separatorKey;
        this.usedBuckets = usedBuckets;
        this.groupKeysTypes = groupKeysTypes;
        this.orderFieldsTypes = orderFieldsTypes;
        this.skip = skip;
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        String prefix = dataDir + "/";
        List<String> files = new ArrayList<>(usedBuckets);
        if (skip) {
            for (int i=0; i<usedBuckets; i++) {
                String resultFile = prefix + Integer.valueOf(i).toString() + ".csv";
                files.add(resultFile);
            }
        } else {
            EntityDAO entityDAO = indexer.getEntityDAO(requestContext);
            List<BufferedWriter> writers = new ArrayList<>();
            try {
                new File(prefix).mkdirs();
                for (int i = 0; i < usedBuckets; i++) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(prefix +
                            Integer.valueOf(i).toString() + ".tmp"));
                    IndexerUtilities.writeCSVHeader(dataFields, writer, separator);
                    writers.add(writer);
                }
                while (entityDAO.hasNextEntity()) {
                    ObjectNode entity = entityDAO.getNextEntity();
                    int idx = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys)
                            .hashCode() % numBuckets;
                    if (idx < usedBuckets) {
                        IndexerUtilities.writeCSVFields(entity, dataFields, writers.get(idx), separator);
                    }
                }
                for (int i = 0; i < usedBuckets; i++) {
                    writers.get(i).close();
                }
                writers.clear();
                for (int i = 0; i < usedBuckets; i++) {
                    String tmpFilePath = prefix + Integer.valueOf(i).toString() + ".tmp";
                    File tmpFile = new File(tmpFilePath);
                    if (tmpFile.isFile()) {
                        List<ObjectNode> buffer = new ArrayList<>();
                        EntityDAO csvDao = new CSVFileDAO(separator, tmpFilePath);
                        while (csvDao.hasNextEntity()) {
                            buffer.add(csvDao.getNextEntity());
                        }
                        csvDao.close();
                        tmpFile.delete();
                        Comparator<ObjectNode> comparator;
                        if (orderFields == null || orderFields.size() == 0) {
                            comparator = RetrieverUtilities.jsonTypedFieldsComparator(groupKeys, groupKeysTypes);
                        } else {
                            List<String> sortFields = new ArrayList<>();
                            sortFields.addAll(groupKeys);
                            sortFields.addAll(orderFields);
                            List<String> sortFieldsTypes = new ArrayList<>();
                            sortFieldsTypes.addAll(groupKeysTypes);
                            sortFieldsTypes.addAll(orderFieldsTypes);
                            comparator = RetrieverUtilities.jsonTypedFieldsComparator(sortFields, sortFieldsTypes);
                            if (descending != null && descending) {
                                comparator = comparator.reversed();
                            }
                        }
                        buffer.sort(comparator);
                        String resultFile = prefix + Integer.valueOf(i).toString() + ".csv";
                        BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
                        IndexerUtilities.writeCSVHeader(dataFields, writer, separator);
                        for (ObjectNode entity : buffer) {
                            IndexerUtilities.writeCSVFields(entity, dataFields, writer, separator);
                        }
                        buffer.clear();
                        writer.close();
                        files.add(resultFile);
                    }
                }
            } catch (IOException e) {
                throw new BadRequestException(e);
            } finally {
                entityDAO.close();
            }
        }
        ObjectNode reqDao = Json.newObject();
        reqDao.set(filesKey, Json.toJson(files));
        reqDao.put(separatorKey, separator);
        reqDao.put(daoNameKey, daoName);
        return reqDao;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        indexer.index(documents, requestContext);
    }
}
