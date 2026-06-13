package com.tyb.myblog.v2.identity.domain.profile;

/**
 * 用户资料领域补丁，承载全部可编辑字段的出现状态和值。
 */
public record UserProfilePatch(
        ProfileFieldPatch nickname,
        ProfileFieldPatch avatarUrl,
        ProfileFieldPatch bioZh,
        ProfileFieldPatch bioJa,
        ProfileFieldPatch bioEn,
        ProfileFieldPatch location,
        ProfileFieldPatch website,
        ProfileFieldPatch emailPublic,
        ProfileFieldPatch githubUrl,
        ProfileFieldPatch twitterUrl,
        ProfileFieldPatch linkedinUrl,
        ProfileFieldPatch zhihuUrl,
        ProfileFieldPatch qiitaUrl,
        ProfileFieldPatch juejinUrl
) {
}
