package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchService {

    @Autowired
    DiscussPostRepository discussRepository;

    @Autowired
    ElasticsearchRestTemplate elasticRestTemplate;


    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    public List<DiscussPost> searchDiscussPost(String keyword, int current /*current是索引*/, int limit) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();


        SearchHits<DiscussPost> search = elasticRestTemplate.search(searchQuery, DiscussPost.class);
        //得到查询结果返回的内容
        List<org.springframework.data.elasticsearch.core.SearchHit<DiscussPost>> searchHits = search.getSearchHits();
        //设置一个需要返回的实体类集合
        List<DiscussPost> discussPosts = new ArrayList<>();

        //遍历返回的内容进行处理
        for (SearchHit<DiscussPost> searchHit : searchHits) {
            //高亮的内容
            Map<String, List<String>> highLightFields = searchHit.getHighlightFields();
            //将高亮的内容填充到content中
            searchHit.getContent().setTitle
                    (highLightFields.get("title") == null ? searchHit.getContent().getTitle() : highLightFields.get("title").get(0));
            searchHit.getContent().setContent
                    (highLightFields.get("content") == null ? searchHit.getContent().getTitle() : highLightFields.get("content").get(0));
            //放入实体类中
            discussPosts.add(searchHit.getContent());

        }
        return discussPosts;


    }

}
