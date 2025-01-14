/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.alerting.core.model

import org.opensearch.common.CheckedFunction
import org.opensearch.common.ParseField
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.xcontent.NamedXContentRegistry
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParser.Token
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import org.opensearch.search.builder.SearchSourceBuilder
import java.io.IOException

data class SearchInput(val indices: List<String>, val query: SearchSourceBuilder) : Input {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readStringList(), // indices
        SearchSourceBuilder(sin) // query
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .startObject(SEARCH_FIELD)
            .field(INDICES_FIELD, indices.toTypedArray())
            .field(QUERY_FIELD, query)
            .endObject()
            .endObject()
    }

    override fun name(): String {
        return SEARCH_FIELD
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeStringCollection(indices)
        query.writeTo(out)
    }

    companion object {
        const val INDICES_FIELD = "indices"
        const val QUERY_FIELD = "query"
        const val SEARCH_FIELD = "search"

        val XCONTENT_REGISTRY = NamedXContentRegistry.Entry(Input::class.java, ParseField("search"), CheckedFunction { parseInner(it) })

        @JvmStatic @Throws(IOException::class)
        private fun parseInner(xcp: XContentParser): SearchInput {
            val indices = mutableListOf<String>()
            lateinit var searchSourceBuilder: SearchSourceBuilder

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()
                when (fieldName) {
                    INDICES_FIELD -> {
                        ensureExpectedToken(Token.START_ARRAY, xcp.currentToken(), xcp)
                        while (xcp.nextToken() != Token.END_ARRAY) {
                            indices.add(xcp.text())
                        }
                    }
                    QUERY_FIELD -> {
                        searchSourceBuilder = SearchSourceBuilder.fromXContent(xcp, false)
                    }
                }
            }

            return SearchInput(
                indices,
                requireNotNull(searchSourceBuilder) { "SearchInput query is null" }
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): SearchInput {
            return SearchInput(sin)
        }
    }
}
