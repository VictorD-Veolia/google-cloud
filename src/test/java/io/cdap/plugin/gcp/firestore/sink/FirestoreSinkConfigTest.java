/*
 * Copyright © 2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.gcp.firestore.sink;

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.validation.CauseAttributes;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.gcp.firestore.sink.util.FirestoreSinkConstants;
import io.cdap.plugin.gcp.firestore.sink.util.SinkIdType;
import io.cdap.plugin.gcp.firestore.util.FirestoreConstants;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 * Tests for {@link FirestoreSinkConfig}.
 */
public class FirestoreSinkConfigTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGetIdTypeUnknown() {
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(null)
      .build();

    try {
      MockFailureCollector collector = new MockFailureCollector();
      config.getIdType(collector);
    } catch (ValidationException e) {
      Assert.assertEquals(1, e.getFailures().size());
      Assert.assertEquals(FirestoreSinkConstants.PROPERTY_ID_TYPE, e.getFailures().get(0).getCauses().get(0)
        .getAttribute(CauseAttributes.STAGE_CONFIG));
    }
  }

  @Test
  public void testGetIdType() {
    SinkIdType idType = SinkIdType.CUSTOM_NAME;
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(idType.getValue())
      .build();

    MockFailureCollector collector = new MockFailureCollector();
    Assert.assertEquals(idType, config.getIdType(collector));
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testGetIdAliasNull() {
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdAlias(null)
      .build();

    Assert.assertEquals(FirestoreConstants.ID_PROPERTY_NAME, config.getIdAlias());
  }

  @Test
  public void testGetIdAliasNotEmpty() {
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdAlias(FirestoreSinkConfigHelper.TEST_ID_ALIAS)
      .build();

    Assert.assertEquals(FirestoreSinkConfigHelper.TEST_ID_ALIAS, config.getIdAlias());
  }

  @Test
  public void testIsUseAutoGeneratedIdFalse() {
    SinkIdType idType = SinkIdType.CUSTOM_NAME;
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(idType.getValue())
      .build();

    Assert.assertFalse(config.shouldUseAutoGeneratedId());
  }

  @Test
  public void testIsUseAutoGeneratedIdTrue() {
    FirestoreSinkConfig config = FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .build();

    Assert.assertTrue(config.shouldUseAutoGeneratedId());
  }

  @Test
  public void testValidateCustomIdWithKeyAliasInSchema() {
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setCollection(FirestoreSinkConfigHelper.TEST_COLLECTION)
      .setIdType(SinkIdType.CUSTOM_NAME.getValue())
      .setIdAlias(FirestoreSinkConfigHelper.TEST_ID_ALIAS)
      .setServiceFilePath(null)
      .setBatchSize(1)
      .build());
    Schema schema = Schema.recordOf("record",
      Schema.Field.of(FirestoreSinkConfigHelper.TEST_ID_ALIAS, Schema.of(Schema.Type.STRING)),
      Schema.Field.of("id", Schema.of(Schema.Type.LONG)));
    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateAutoGeneratedIdSchema() {
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setCollection(FirestoreSinkConfigHelper.TEST_COLLECTION)
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(1)
      .build());
    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("key", Schema.of(Schema.Type.LONG)));
    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateIdCustomTypeWithoutIdNameInSchema() {
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setCollection(FirestoreSinkConfigHelper.TEST_COLLECTION)
      .setIdType(SinkIdType.CUSTOM_NAME.getValue())
      .setServiceFilePath(null)
      .setBatchSize(1)
      .build());
    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("key", Schema.of(Schema.Type.LONG)));

    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(1, collector.getValidationFailures().size());
    Assert.assertEquals(FirestoreSinkConstants.PROPERTY_ID_ALIAS, collector.getValidationFailures().get(0)
      .getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }

  @Test
  public void testValidateBatchSizeWithinLimit() {
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(10)
      .build());

    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("id", Schema.of(Schema.Type.LONG)));
    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateBatchSizeZero() {
    int batchSize = 0;
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(batchSize)
      .build());

    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("name", Schema.of(Schema.Type.LONG)));
    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(1, collector.getValidationFailures().size());
    Assert.assertEquals(FirestoreSinkConstants.PROPERTY_BATCH_SIZE, collector.getValidationFailures().get(0)
      .getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }

  @Test
  public void testValidateBatchNegative() {
    int batchSize = -10;
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(batchSize)
      .build());

    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("name", Schema.of(Schema.Type.LONG)));

    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(1, collector.getValidationFailures().size());
    Assert.assertEquals(FirestoreSinkConstants.PROPERTY_BATCH_SIZE, collector.getValidationFailures().get(0)
      .getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }

  @Test
  public void testValidateBatchMax() {
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(FirestoreSinkConstants.MAX_BATCH_SIZE)
      .build());

    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("name", Schema.of(Schema.Type.LONG)));
    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(0, collector.getValidationFailures().size());
  }

  @Test
  public void testValidateBatchOverMax() {
    int batchSize = FirestoreSinkConstants.MAX_BATCH_SIZE + 1;
    FirestoreSinkConfig config = Mockito.spy(FirestoreSinkConfigHelper.newConfigBuilder()
      .setIdType(SinkIdType.AUTO_GENERATED_ID.getValue())
      .setServiceFilePath(null)
      .setBatchSize(batchSize)
      .build());

    Schema schema = Schema.recordOf("record",
      Schema.Field.of("testName", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("name", Schema.of(Schema.Type.LONG)));

    MockFailureCollector collector = new MockFailureCollector();
    Mockito.doNothing().when(config).validateFirestoreConnection(collector);
    config.validate(schema, collector);
    Assert.assertEquals(1, collector.getValidationFailures().size());
    Assert.assertEquals(FirestoreSinkConstants.PROPERTY_BATCH_SIZE, collector.getValidationFailures().get(0)
      .getCauses().get(0).getAttribute(CauseAttributes.STAGE_CONFIG));
  }
}
