package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseUserMenuReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseUserMenuReader.class)
class DatabaseUserMenuReaderTest {

    @Autowired
    private DatabaseUserMenuReader reader;

    @Test
    void loadsAdminMenuTreeByAuthId() {
        var menus = reader.findByAuthId("1");

        assertThat(menus).extracting("name").containsExactly("首页", "文章管理", "评论管理", "个人中心");
        var article = menus.get(1);
        assertThat(article.children()).extracting("name").containsExactly("文章列表", "草稿箱");
        assertThat(article.children().get(1).hidden()).isTrue();
    }

    @Test
    void wrapsLeafRootMenuWithEmptyChildPathForRouterCompatibility() {
        var menus = reader.findByAuthId("1");

        var home = menus.get(0);
        assertThat(home.path()).isEqualTo("/");
        assertThat(home.component()).isEqualTo("Layout");
        assertThat(home.children()).hasSize(1);
        assertThat(home.children().get(0).path()).isEqualTo("");
        assertThat(home.children().get(0).component()).isEqualTo("Layout");
    }

    @Test
    void returnsEmptyWhenAuthIdDoesNotExist() {
        assertThat(reader.findByAuthId("999")).isEmpty();
    }
}
