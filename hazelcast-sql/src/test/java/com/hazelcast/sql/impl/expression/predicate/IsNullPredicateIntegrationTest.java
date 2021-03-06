/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.expression.predicate;

import com.hazelcast.sql.SqlColumnType;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.impl.expression.SqlExpressionIntegrationTestSupport;
import com.hazelcast.sql.support.expressions.ExpressionType;
import com.hazelcast.sql.support.expressions.ExpressionValue;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hazelcast.sql.support.expressions.ExpressionTypes.BIG_DECIMAL;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.BIG_INTEGER;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.BOOLEAN;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.BYTE;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.CHARACTER;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.DOUBLE;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.FLOAT;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.INTEGER;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.LOCAL_DATE;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.LOCAL_DATE_TIME;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.LOCAL_TIME;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.LONG;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.OBJECT;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.OFFSET_DATE_TIME;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.SHORT;
import static com.hazelcast.sql.support.expressions.ExpressionTypes.STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for IS (NOT) NULL predicates.
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class IsNullPredicateIntegrationTest extends SqlExpressionIntegrationTestSupport {
    @Test
    public void testLiteral() {
        Class<? extends ExpressionValue> clazz = ExpressionValue.createClass(INTEGER);

        int key = 0;
        ExpressionValue value = ExpressionValue.create(clazz, 0, 1);

        put(key, value);

        checkLiteral("1", false);
        checkLiteral("'1'", false);

        checkLiteral("'a'", false);

        checkLiteral("TRUE", false);
        checkLiteral("true", false);
        checkLiteral("'TRUE'", false);
        checkLiteral("'true'", false);

        checkLiteral("FALSE", false);
        checkLiteral("false", false);
        checkLiteral("'FALSE'", false);
        checkLiteral("'false'", false);

        checkLiteral("NULL", true);
        checkLiteral("null", true);
        checkLiteral("'NULL'", false);
        checkLiteral("'null'", false);
    }

    private void checkLiteral(String literal, boolean expectedResult) {
        checkLiteral(literal, "IS NULL", expectedResult);
        checkLiteral(literal, "IS NOT NULL", !expectedResult);
    }

    private void checkLiteral(String literal, String function, boolean expectedResult) {
        String expression = literal + " " + function;
        String sql = "SELECT " + expression + " FROM map WHERE " + expression;

        List<SqlRow> rows = execute(member, sql);

        if (expectedResult) {
            assertEquals(1, rows.size());

            SqlRow row = rows.get(0);

            assertEquals(SqlColumnType.BOOLEAN, row.getMetadata().getColumn(0).getType());
            assertTrue(row.getObject(0));
        } else {
            assertEquals(0, rows.size());
        }
    }

    @Test
    public void testColumn() {
        checkColumn(BOOLEAN);

        checkColumn(BYTE);
        checkColumn(SHORT);
        checkColumn(INTEGER);
        checkColumn(LONG);
        checkColumn(BIG_INTEGER);
        checkColumn(BIG_DECIMAL);
        checkColumn(FLOAT);
        checkColumn(DOUBLE);

        checkColumn(STRING);
        checkColumn(CHARACTER);

        checkColumn(LOCAL_DATE);
        checkColumn(LOCAL_TIME);
        checkColumn(LOCAL_DATE_TIME);
        checkColumn(OFFSET_DATE_TIME);

        checkColumn(OBJECT);
    }

    private void checkColumn(ExpressionType<?> type) {
        Class<? extends ExpressionValue> clazz = ExpressionValue.createClass(type);

        int keyNull = 0;
        int keyNotNull = 1;

        Map<Integer, Object> entries = new HashMap<>();
        entries.put(keyNull, ExpressionValue.create(clazz, keyNull, null));
        entries.put(keyNotNull, ExpressionValue.create(clazz, keyNotNull, type.valueFrom()));
        putAll(entries);

        checkColumn("IS NULL", set(keyNull));
        checkColumn("IS NOT NULL", set(keyNotNull));
    }

    private void checkColumn(String function, Set<Integer> expectedKeys) {
        String expression = "field1 " + function;
        String sql = "SELECT key, " + expression + " FROM map WHERE " + expression;

        List<SqlRow> rows = execute(member, sql);

        assertEquals(expectedKeys.size(), rows.size());

        for (SqlRow row : rows) {
            assertEquals(SqlColumnType.BOOLEAN, row.getMetadata().getColumn(1).getType());

            int key = row.getObject(0);
            boolean value = row.getObject(1);

            assertTrue("Key is not returned: " + key, expectedKeys.contains(key));
            assertTrue(value);
        }
    }

    @Test
    public void testParameter() {
        Class<? extends ExpressionValue> clazz = ExpressionValue.createClass(INTEGER);

        int key = 0;
        ExpressionValue value = ExpressionValue.create(clazz, 0, 1);

        put(key, value);

        assertEquals(set(key), keys("SELECT key FROM map WHERE ? IS NULL", new Object[] { null }));
        assertEquals(set(), keys("SELECT key FROM map WHERE ? IS NOT NULL", new Object[] { null }));

        checkNotNullParameter(key, BOOLEAN);

        checkNotNullParameter(key, BYTE);
        checkNotNullParameter(key, SHORT);
        checkNotNullParameter(key, INTEGER);
        checkNotNullParameter(key, LONG);
        checkNotNullParameter(key, BIG_INTEGER);
        checkNotNullParameter(key, BIG_DECIMAL);
        checkNotNullParameter(key, FLOAT);
        checkNotNullParameter(key, DOUBLE);

        checkNotNullParameter(key, STRING);
        checkNotNullParameter(key, CHARACTER);

        checkNotNullParameter(key, LOCAL_DATE);
        checkNotNullParameter(key, LOCAL_TIME);
        checkNotNullParameter(key, LOCAL_DATE_TIME);
        checkNotNullParameter(key, OFFSET_DATE_TIME);

        checkNotNullParameter(key, OBJECT);
    }

    private void checkNotNullParameter(int key, ExpressionType<?> type) {
        Object parameter = type.valueFrom();

        assertNotNull(parameter);

        assertEquals(set(), keys("SELECT key FROM map WHERE ? IS NULL", parameter));
        assertEquals(set(key), keys("SELECT key FROM map WHERE ? IS NOT NULL", parameter));
    }

    private Set<Integer> keys(String sql, Object... params) {
        List<SqlRow> rows = execute(member, sql, params);

        if (rows.size() == 0) {
            return Collections.emptySet();
        }

        assertEquals(1, rows.get(0).getMetadata().getColumnCount());

        Set<Integer> keys = new HashSet<>();

        for (SqlRow row : rows) {
            int key = row.getObject(0);

            boolean added = keys.add(key);

            assertTrue("Key is not unique: " + key, added);
        }

        return keys;
    }

    private static Set<Integer> set(Integer... values) {
        Set<Integer> res = new HashSet<>();

        if (values != null) {
            res.addAll(Arrays.asList(values));
        }

        return res;
    }
}
