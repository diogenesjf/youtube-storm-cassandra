package com.skunkworks.spout;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class YoutubeSpout extends BaseRichSpout{
	SpoutOutputCollector _collector;
	YouTube _youTube;
	String _queryTerm;
	YouTube.Search.List search;
	SearchListResponse _response;
	
	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		_collector = collector;
		_response = null;
		_youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("data-api-test").build();
		_queryTerm = "Cadbury";
		try {
			search = _youTube.search().list("id,snippet,statistics");
		} catch (IOException e) {
			e.printStackTrace();
		}
		search.setKey("AIzaSyDhfgjKUG6JW5_NWSdJt2refa5EFdMrf48");
        search.setQ(_queryTerm);
        search.setType("video");
        search.setFields("items(id/kind,id/videoId,snippet/title)");
	}

	@Override
	public void nextTuple() {
		 SearchListResponse searchResponse;
		try {
			searchResponse = search.execute();
			if (searchResponse != _response){
				 _response = searchResponse;
				 List<SearchResult> searchResultList = searchResponse.getItems();
				 for(SearchResult searchResult:searchResultList){
					 ResourceId rId = searchResult.getId();
					 if (rId.getKind().equals("youtube#video")) {
						 _collector.emit(new Values(rId.getVideoId() + "," + searchResult.getSnippet().getTitle() + "," + searchResult.getSnippet().getUnknownKeys().toString()));
					 }
				 }
			 }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("line"));		
	}

}
