/*
 * Copyright 2018 Crown Copyright
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
import * as React from "react";
import { storiesOf } from "@storybook/react";

import StroomDecorator from "../../lib/storybook/StroomDecorator";
import { testDictionaries } from "./test";
import DictionaryEditor from "./DictionaryEditor";

import "../../styles/main.css";

const stories = storiesOf("Document Editors/Dictionary", module).addDecorator(
  StroomDecorator
);

Object.entries(testDictionaries)
  .map(k => ({
    name: k[0],
    data: k[1]
  }))
  .forEach(dictionary =>
    stories.add(dictionary.name, () => (
      <DictionaryEditor dictionaryUuid={dictionary.name} />
    ))
  );