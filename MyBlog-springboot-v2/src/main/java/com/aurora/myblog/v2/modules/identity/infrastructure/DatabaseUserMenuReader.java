package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.UserMenu;
import com.aurora.myblog.v2.modules.identity.domain.UserMenuReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DatabaseUserMenuReader implements UserMenuReader {

    private static final String ROOT_COMPONENT = "Layout";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserMenuReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<UserMenu> findByAuthId(String authId) {
        if (authId == null || authId.isBlank()) {
            return List.of();
        }
        List<MenuRow> rows = jdbcTemplate.query("""
                        select distinct
                            m.id,
                            m.name,
                            m.path,
                            m.component,
                            m.icon,
                            m.is_hidden,
                            m.parent_id,
                            m.order_num
                        from t_user_auth ua
                        join t_user_role ur on ua.user_info_id = ur.user_id
                        join t_role_menu rm on ur.role_id = rm.role_id
                        join t_menu m on rm.menu_id = m.id
                        join t_role r on ur.role_id = r.id
                        where ua.id = ?
                          and r.is_disable = 0
                        order by m.order_num, m.id
                        """,
                (rs, rowNum) -> new MenuRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("path"),
                        rs.getString("component"),
                        rs.getString("icon"),
                        rs.getInt("is_hidden") == 1,
                        (Integer) rs.getObject("parent_id"),
                        rs.getInt("order_num")),
                authId.trim());
        return buildTree(rows);
    }

    private List<UserMenu> buildTree(List<MenuRow> rows) {
        Map<Integer, List<MenuRow>> childrenByParent = rows.stream()
                .filter(row -> row.parentId() != null)
                .collect(Collectors.groupingBy(MenuRow::parentId));
        return rows.stream()
                .filter(row -> row.parentId() == null)
                .sorted(Comparator.comparing(MenuRow::orderNum).thenComparing(MenuRow::id))
                .map(row -> toUserMenu(row, childrenByParent.getOrDefault(row.id(), List.of())))
                .toList();
    }

    private UserMenu toUserMenu(MenuRow row, List<MenuRow> children) {
        if (children.isEmpty()) {
            List<UserMenu> leaf = new ArrayList<>();
            leaf.add(new UserMenu(row.name(), "", row.component(), row.icon(), false, List.of()));
            return new UserMenu(row.name(), row.path(), ROOT_COMPONENT, row.icon(), row.hidden(), leaf);
        }
        List<UserMenu> childMenus = children.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MenuRow::orderNum).thenComparing(MenuRow::id))
                .map(child -> new UserMenu(child.name(), child.path(), child.component(), child.icon(), child.hidden(), List.of()))
                .toList();
        return new UserMenu(row.name(), row.path(), row.component(), row.icon(), row.hidden(), childMenus);
    }

    private record MenuRow(
            Integer id,
            String name,
            String path,
            String component,
            String icon,
            boolean hidden,
            Integer parentId,
            Integer orderNum
    ) {
    }
}
