/* Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/

package cloudcode.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure request-parsing / response-building helpers in {@link HelloWorld}.
 * These exercise no network calls (Vertex AI, Geocoding) and need no credentials.
 */
class HelloWorldTest {

  private static final Gson GSON = new Gson();

  @Test
  void extractLatLng_parsesBigQueryRemoteFunctionPayload() {
    // Exact payload shape from the README's `gcloud functions call` example.
    JsonObject requestJson =
        GSON.fromJson("{\"calls\":[[\"40.714224,-73.961452\"]]}", JsonObject.class);

    String latlng = HelloWorld.extractLatLng(requestJson);

    assertEquals("40.714224,-73.961452", latlng);
  }

  @Test
  void extractLatLng_missingCallsArray_throws() {
    JsonObject requestJson = GSON.fromJson("{}", JsonObject.class);

    // NullPointerException, since getAsJsonArray("calls") returns null and .get(0) is
    // called on it. Asserted here so the migration doesn't silently change this behavior.
    assertThrows(NullPointerException.class, () -> HelloWorld.extractLatLng(requestJson));
  }

  @Test
  void extractLatLng_emptyCallsArray_throws() {
    JsonObject requestJson = GSON.fromJson("{\"calls\":[]}", JsonObject.class);

    assertThrows(IndexOutOfBoundsException.class, () -> HelloWorld.extractLatLng(requestJson));
  }

  @Test
  void buildRepliesJson_stripsNewlinesAndTrims() {
    String rawResult = "  \n{ \"CITY\": \"Brooklyn\" }\n  ";

    String result = HelloWorld.buildRepliesJson(rawResult);

    assertEquals("{\"replies\":[\"{ \\\"CITY\\\": \\\"Brooklyn\\\" }\"]}", result);
  }

  @Test
  void buildRepliesJson_escapesEmbeddedQuotes() {
    // The real model output is a JSON string containing JSON (see README sample output).
    String rawResult = "{\"STREET_ADDRESS\": \"277 Bedford Ave\", \"CITY\": \"Brooklyn\"}";

    String result = HelloWorld.buildRepliesJson(rawResult);

    assertEquals(
        "{\"replies\":[\"{\\\"STREET_ADDRESS\\\": \\\"277 Bedford Ave\\\", "
            + "\\\"CITY\\\": \\\"Brooklyn\\\"}\"]}",
        result);
  }

  @Test
  void buildRepliesJson_emptyString_producesEmptyReply() {
    String result = HelloWorld.buildRepliesJson("");

    assertEquals("{\"replies\":[\"\"]}", result);
  }
}
