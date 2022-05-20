/*
 * Copyright (C) 2011 Google Inc.
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

package org.ros.internal.node.response;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class StringListResultFactory implements ResultFactory<List<String>> {

  @Override
  public List<String> newFromValue(Object value) {
    List<String> strings = Lists.newArrayList();
    List<Object> objects = Arrays.asList((Object[]) value);
    for (Object topic : objects) {
      strings.add((String) topic);
    }
    return strings;
  }
}