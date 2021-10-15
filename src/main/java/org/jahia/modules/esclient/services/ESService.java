package org.jahia.modules.esclient.services;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jahia.modules.databaseConnector.services.ConnectionService;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnectionRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component(service = ESService.class, immediate = true)
public class ESService {

    private static final Logger logger = LoggerFactory.getLogger(ESService.class);

    private RestHighLevelClient elasticSearchClient;
    private ElasticSearchConnectionRegistry elasticSearchConnectionRegistry;

    @Reference(service = ElasticSearchConnectionRegistry.class)
    public void setElasticSearchConnectionRegistry(ElasticSearchConnectionRegistry elasticSearchConnectionRegistry) {
        this.elasticSearchConnectionRegistry = elasticSearchConnectionRegistry;
    }

    public boolean isClientActive() {
        return elasticSearchClient != null;
    }

    public void loadClient(String connectionID) throws ESServiceException {
        ConnectionService connectionService = null;
        Optional<Map<String, Object>> elasticSearchConnection = elasticSearchConnectionRegistry.getConnectionsInfo(ElasticSearchConnection.DATABASE_TYPE).stream()
                .filter(esConnections -> connectionID.equals(esConnections.get("id"))).findFirst();
        if (elasticSearchConnection.isPresent()) {
            connectionService = (ConnectionService) elasticSearchConnectionRegistry
                    .getConnectionService(ElasticSearchConnection.DATABASE_TYPE, (String) elasticSearchConnection.get().get("id"));
        }
        if (connectionService == null) {
            throw new ESServiceException("ElasticSearchConnection unavailable");
        }
        elasticSearchClient = (RestHighLevelClient) connectionService.getClient();
    }

    public Set<String> listConnections() {
        final Set<String> connections = new HashSet<>();
        for (Map<String, Object> esConnections : elasticSearchConnectionRegistry.getConnectionsInfo(ElasticSearchConnection.DATABASE_TYPE)) {
            connections.add((String) esConnections.get("id"));
        }
        return connections;
    }

    public Map<String, String> listIndices() {
        try {
            /*
            // According to the documentation, this is the proper way, but for some reason, response.getIndices() returns an empty map
            final ClusterHealthResponse response = elasticSearchClient.cluster().health(new ClusterHealthRequest("*"), RequestOptions.DEFAULT);
            return response.getIndices().keySet();
             */

            final GetIndexResponse response = elasticSearchClient.indices().get(new GetIndexRequest("*"), RequestOptions.DEFAULT);
            final String[] indices = response.getIndices();
            final Map<String, List<AliasMetadata>> aliases = response.getAliases();
            final Map<String, String> indicesMap = new HashMap<>(indices.length);
            for (String index : indices) {
                if (!aliases.containsKey(index))
                    indicesMap.put(index, null);
                else {
                    StringBuilder sb = new StringBuilder();
                    for (AliasMetadata aliasMetaData : aliases.get(index)) {
                        sb.append(" , ").append(aliasMetaData.getAlias());
                    }
                    indicesMap.put(index, StringUtils.substring(sb.toString(), 3));
                }
            }

            return indicesMap;
        } catch (IOException e) {
            logger.error("", e);
            return Collections.emptyMap();
        }
    }

    public Map<String,Object> getHealthInfo() {
        final ClusterHealthResponse response;
        try {
            response = elasticSearchClient.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("", e);
            return Collections.emptyMap();
        }
        final Map<String,Object> info = new LinkedHashMap<>();
        info.put("Cluster name", response.getClusterName());
        info.put("Status", response.getStatus());
        info.put("Number of nodes", response.getNumberOfNodes());
        info.put("Number of data nodes", response.getNumberOfDataNodes());
        info.put("REST status", response.status());
        info.put("Is timed out", response.isTimedOut());
        info.put("Active shards", response.getActiveShards());
        info.put("Active primary shards", response.getActivePrimaryShards());
        info.put("Active shards percent", response.getActiveShardsPercent());
        info.put("Delayed unassigned shards", response.getDelayedUnassignedShards());
        info.put("Initializing shards", response.getInitializingShards());
        info.put("Relocating shards", response.getRelocatingShards());
        info.put("Unassigned shards", response.getUnassignedShards());
        info.put("Number of in flight fetch", response.getNumberOfInFlightFetch());
        info.put("Number of pending tasks", response.getNumberOfPendingTasks());
        info.put("Task max waiting time", response.getTaskMaxWaitingTime().toHumanReadableString(3));

        return info;
    }

    public boolean removeIndex(String index) {
        try {
            if (elasticSearchClient.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT)) {
                elasticSearchClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
                return true;
            }
            logger.warn("Index {} doesn't exist anymore", index);
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public Map<String, String> getIndexContent(String esIndex)
            throws IOException {
        if (!elasticSearchClient.indices().exists(new GetIndexRequest(esIndex), RequestOptions.DEFAULT)) {
            logger.warn(String.format("The index %s doesn't exist", esIndex));
            return Collections.emptyMap();
        }

        final Map<String, String> data = new HashMap<>();
        final TimeValue timeValue = new TimeValue(5, TimeUnit.MINUTES);
        final SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .query(QueryBuilders.matchAllQuery())
                .size(100);
        final SearchRequest searchRequest = new SearchRequest(esIndex);
        searchRequest.source(sourceBuilder).scroll(timeValue);
        SearchResponse searchResponse;
        final List<String> scrollIds = new LinkedList<>();
        try {
            searchResponse = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT);
            do {
                for (SearchHit hit : searchResponse.getHits()) {
                    data.put(hit.getId(), hit.getSourceAsString());
                }
                final SearchScrollRequest scrollRequest = new SearchScrollRequest();
                scrollRequest.scrollId(searchResponse.getScrollId());
                scrollRequest.scroll(timeValue);
                scrollIds.add(searchResponse.getScrollId());
                searchResponse = elasticSearchClient.scroll(scrollRequest, RequestOptions.DEFAULT);
            }
            // Zero hits mark the end of the scroll and the while loop.
            while (searchResponse.getHits().getHits().length != 0);
            scrollIds.add(searchResponse.getScrollId());
            final ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.setScrollIds(scrollIds);
            elasticSearchClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Error while querying the index: {}", e.getMessage(), e);
        }

        return data;
    }
}
