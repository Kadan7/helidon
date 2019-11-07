/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.dbclient.mongodb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonReaderFactory;

import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbMapperManager;
import io.helidon.dbclient.DbStatement;
import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.common.AbstractStatement;
import io.helidon.dbclient.common.InterceptorSupport;

import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;

/**
 * Common MongoDB statement builder.
 *
 * @param <S> MongoDB statement type
 * @param <R> Statement execution result type
 */
abstract class MongoDbStatement<S extends DbStatement<S, R>, R> extends AbstractStatement<S, R> {

    private static final Logger LOGGER = Logger.getLogger(MongoDbStatementQuery.class.getName());

    /**
     * Empty JSON object.
     */
    static final Document EMPTY = Document.parse(Json.createObjectBuilder().build().toString());

    /**
     * Operation JSON parameter name.
     */
    protected static final String JSON_OPERATION = "operation";
    /**
     * Collection JSON parameter name.
     */
    protected static final String JSON_COLLECTION = "collection";
    /**
     * Query JSON parameter name.
     */
    protected static final String JSON_QUERY = "query";
    /**
     * Value JSON parameter name.
     */
    protected static final String JSON_VALUE = "value";
    /**
     * Projection JSON parameter name: Defines projection to restrict returned fields.
     */
    protected static final String JSON_PROJECTION = "projection";
    /**
     * JSON reader factory.
     */
    protected static final JsonReaderFactory READER_FACTORY = Json.createReaderFactory(Collections.emptyMap());

    private final MongoDatabase db;

    MongoDbStatement(DbStatementType dbStatementType,
                     MongoDatabase db,
                     String statementName,
                     String statement,
                     DbMapperManager dbMapperManager,
                     MapperManager mapperManager,
                     InterceptorSupport interceptors) {

        super(dbStatementType,
              statementName,
              statement,
              dbMapperManager,
              mapperManager,
              interceptors);

        this.db = db;
    }

    String build() {
        switch (paramType()) {
        // Statement shall not contain any parameters, no conversion is needed.
        case UNKNOWN:
            return statement();
        case INDEXED:
            return StatementParsers.indexedParser(statement(), indexedParams()).convert();
        // Replace parameter identifiers with values from name to value map
        case NAMED:
            return StatementParsers.namedParser(statement(), namedParams()).convert();
        default:
            throw new IllegalStateException("Unknown SQL statement type: " + paramType());
        }
    }

    /**
     * Statement name.
     *
     * @return name of this statement (never null, may be generated)
     */
    @Override
    public String statementName() {
        return super.statementName();
    }

    MongoDatabase db() {
        return db;
    }

    /**
     * Db mapper manager.
     *
     * @return mapper manager for DB types
     */
    @Override
    protected DbMapperManager dbMapperManager() {
        return super.dbMapperManager();
    }

    /**
     * Mapper manager.
     *
     * @return generic mapper manager
     */
    @Override
    protected MapperManager mapperManager() {
        return super.mapperManager();
    }

    @Override
    protected String dbType() {
        return MongoDbClientProvider.DB_TYPE;
    }

    /**
     * Mongo operation enumeration.
     */
    enum MongoOperation {
        QUERY("query", "find", "select"),
        INSERT("insert"),
        UPDATE("update"),
        DELETE("delete"),
        // Database command not related to a specific collection
        // Only executable using generic statement
        COMMAND("command");

        private static final Map<String, MongoOperation> NAME_TO_OPERATION = new HashMap<>();

        static {
            for (MongoOperation value : MongoOperation.values()) {
                for (String name : value.names) {
                    NAME_TO_OPERATION.put(name.toLowerCase(), value);
                }
            }
        }

        static MongoOperation operationByName(String name) {
            if (name == null) {
                return null;
            }
            return NAME_TO_OPERATION.get(name.toLowerCase());
        }

        private final String[] names;

        MongoOperation(String... names) {
            this.names = names;
        }
    }

    static class MongoStatement {
        private final String preparedStmt;

        private static Document/*JsonObject*/ readStmt(JsonReaderFactory jrf, String preparedStmt) {
            return Document.parse(preparedStmt);
        }

        private final Document/*JsonObject*/ jsonStmt;
        private final MongoOperation operation;
        private final String collection;
        private final Document query;
        private final Document value;
        private final Document projection;

        MongoStatement(DbStatementType dbStatementType, JsonReaderFactory jrf, String preparedStmt) {
            this.preparedStmt = preparedStmt;
            this.jsonStmt = readStmt(jrf, preparedStmt);

            MongoOperation operation;
            if (jsonStmt.containsKey(JSON_OPERATION)) {
                operation = MongoOperation.operationByName(jsonStmt.getString(JSON_OPERATION));
                // make sure we have alignment between statement type and operation
                switch (dbStatementType) {
                case QUERY:
                case GET:
                    validateOperation(dbStatementType, operation, MongoOperation.QUERY);
                    break;
                case INSERT:
                    validateOperation(dbStatementType, operation, MongoOperation.INSERT);
                    break;
                case UPDATE:
                    validateOperation(dbStatementType, operation, MongoOperation.UPDATE);
                    break;
                case DELETE:
                    validateOperation(dbStatementType, operation, MongoOperation.DELETE);
                    break;
                case DML:
                    validateOperation(dbStatementType, operation, MongoOperation.INSERT,
                                      MongoOperation.UPDATE, MongoOperation.DELETE);
                    break;
                case UNKNOWN:
                    validateOperation(dbStatementType, operation, MongoOperation.QUERY,
                                      MongoOperation.INSERT, MongoOperation.UPDATE,
                                      MongoOperation.DELETE, MongoOperation.COMMAND);
                    break;
                default:
                    throw new IllegalStateException(
                            "Operation type is not defined in statement, and cannot be inferred from statement type: "
                                    + dbStatementType);
                }
            } else {
                switch (dbStatementType) {
                case QUERY:
                    operation = MongoOperation.QUERY;
                    break;
                case GET:
                    operation = MongoOperation.QUERY;
                    break;
                case INSERT:
                    operation = MongoOperation.INSERT;
                    break;
                case UPDATE:
                    operation = MongoOperation.UPDATE;
                    break;
                case DELETE:
                    operation = MongoOperation.DELETE;
                    break;
                case DML:
                case UNKNOWN:
                default:
                    throw new IllegalStateException(
                            "Operation type is not defined in statement, and cannot be inferred from statement type: "
                                    + dbStatementType);
                }
            }
            this.operation = operation;
            this.collection = jsonStmt.getString(JSON_COLLECTION);
            this.value = jsonStmt.get(JSON_VALUE, Document.class);
            this.query = jsonStmt.get(JSON_QUERY, Document.class);
            this.projection = jsonStmt.get(JSON_PROJECTION, Document.class);
        }

        private static void validateOperation(DbStatementType dbStatementType,
                                              MongoOperation actual,
                                              MongoOperation... expected) {

            // PERF: time complexity of this check is terrible
            for (MongoOperation operation : expected) {
                if (actual == operation) {
                    return;
                }
            }

            throw new IllegalStateException("Statement type is "
                                                    + dbStatementType
                                                    + ", yet operation in statement is: "
                                                    + actual);
        }

        Document/*JsonObject*/ getJsonStmt() {
            return jsonStmt;
        }

        MongoOperation getOperation() {
            return operation;
        }

        String getCollection() {
            return collection;
        }

        Document getQuery() {
            return query;
        }

        Document getValue() {
            return value;
        }

        Document getProjection() {
            return projection;
        }

        @Override
        public String toString() {
            return preparedStmt;
        }
    }

}
