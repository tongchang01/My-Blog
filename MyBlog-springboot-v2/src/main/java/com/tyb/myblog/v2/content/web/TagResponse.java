package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.TagSummary;

public record TagResponse(int id, String name, long articleCount) {

    static TagResponse from(TagSummary tag) {
        return new TagResponse(tag.id(), tag.name(), tag.articleCount());
    }
}
