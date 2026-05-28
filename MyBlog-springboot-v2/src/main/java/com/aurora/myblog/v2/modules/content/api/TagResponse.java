package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.TagSummary;

public record TagResponse(int id, String name, long articleCount) {

    static TagResponse from(TagSummary tag) {
        return new TagResponse(tag.id(), tag.name(), tag.articleCount());
    }
}
