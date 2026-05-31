package com.tyb.myblog.v2.content.domain;

public record ArticleAccessCheck(int id, int status, boolean deleted, String password) {

    public boolean publicArticle() {
        return !deleted && status == 1;
    }

    public boolean protectedArticle() {
        return !deleted && status == 2;
    }
}
