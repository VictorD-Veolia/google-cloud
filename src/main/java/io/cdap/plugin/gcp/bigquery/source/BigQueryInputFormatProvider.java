/*
 * Copyright © 2021 Cask Data, Inc.
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

package io.cdap.plugin.gcp.bigquery.source;

import io.cdap.cdap.api.data.batch.InputFormatProvider;
import org.apache.hadoop.conf.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Input format provider for BigQuery source.
 */
public class BigQueryInputFormatProvider implements InputFormatProvider {

  protected Map<String, String> inputFormatConfiguration;

  public BigQueryInputFormatProvider(Configuration configuration) {
    this.inputFormatConfiguration = StreamSupport
      .stream(configuration.spliterator(), false)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public String getInputFormatClassName() {
    return PartitionedBigQueryInputFormat.class.getName();
  }

  @Override
  public Map<String, String> getInputFormatConfiguration() {
    return inputFormatConfiguration;
  }
}
