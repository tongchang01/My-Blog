package com.tyb.myblog.v2.identity.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.identity.application.profile.PatchValue;
import com.tyb.myblog.v2.identity.application.profile.UpdateCurrentUserProfileCommand;
import lombok.Getter;

/**
 * 当前用户资料部分更新请求，通过 Jackson setter 保留字段是否出现的信息。
 */
@Getter
public class UpdateCurrentUserProfileRequest {

    private PatchValue<String> nickname = PatchValue.absent();
    private PatchValue<String> avatarUrl = PatchValue.absent();
    private PatchValue<String> bioZh = PatchValue.absent();
    private PatchValue<String> bioJa = PatchValue.absent();
    private PatchValue<String> bioEn = PatchValue.absent();
    private PatchValue<String> location = PatchValue.absent();
    private PatchValue<String> website = PatchValue.absent();
    private PatchValue<String> emailPublic = PatchValue.absent();
    private PatchValue<String> githubUrl = PatchValue.absent();
    private PatchValue<String> twitterUrl = PatchValue.absent();
    private PatchValue<String> linkedinUrl = PatchValue.absent();
    private PatchValue<String> zhihuUrl = PatchValue.absent();
    private PatchValue<String> qiitaUrl = PatchValue.absent();
    private PatchValue<String> juejinUrl = PatchValue.absent();

    @JsonSetter("nickname")
    public void setNickname(String value) {
        nickname = PatchValue.of(value);
    }

    @JsonSetter("avatarUrl")
    public void setAvatarUrl(String value) {
        avatarUrl = PatchValue.of(value);
    }

    @JsonSetter("bioZh")
    public void setBioZh(String value) {
        bioZh = PatchValue.of(value);
    }

    @JsonSetter("bioJa")
    public void setBioJa(String value) {
        bioJa = PatchValue.of(value);
    }

    @JsonSetter("bioEn")
    public void setBioEn(String value) {
        bioEn = PatchValue.of(value);
    }

    @JsonSetter("location")
    public void setLocation(String value) {
        location = PatchValue.of(value);
    }

    @JsonSetter("website")
    public void setWebsite(String value) {
        website = PatchValue.of(value);
    }

    @JsonSetter("emailPublic")
    public void setEmailPublic(String value) {
        emailPublic = PatchValue.of(value);
    }

    @JsonSetter("githubUrl")
    public void setGithubUrl(String value) {
        githubUrl = PatchValue.of(value);
    }

    @JsonSetter("twitterUrl")
    public void setTwitterUrl(String value) {
        twitterUrl = PatchValue.of(value);
    }

    @JsonSetter("linkedinUrl")
    public void setLinkedinUrl(String value) {
        linkedinUrl = PatchValue.of(value);
    }

    @JsonSetter("zhihuUrl")
    public void setZhihuUrl(String value) {
        zhihuUrl = PatchValue.of(value);
    }

    @JsonSetter("qiitaUrl")
    public void setQiitaUrl(String value) {
        qiitaUrl = PatchValue.of(value);
    }

    @JsonSetter("juejinUrl")
    public void setJuejinUrl(String value) {
        juejinUrl = PatchValue.of(value);
    }

    /**
     * 拒绝未声明字段，避免客户端拼写错误被静默忽略。
     *
     * @param name 未知字段名
     * @param value 未知字段值
     */
    @JsonAnySetter
    public void rejectUnknown(String name, JsonNode value) {
        throw new IllegalArgumentException("不支持的资料字段：" + name);
    }

    /**
     * 转换为应用层部分更新命令。
     *
     * @return 保留字段 presence 信息的更新命令
     */
    public UpdateCurrentUserProfileCommand toCommand() {
        return new UpdateCurrentUserProfileCommand(
                nickname,
                avatarUrl,
                bioZh,
                bioJa,
                bioEn,
                location,
                website,
                emailPublic,
                githubUrl,
                twitterUrl,
                linkedinUrl,
                zhihuUrl,
                qiitaUrl,
                juejinUrl);
    }
}
