package com.tyb.myblog.v2.system.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.application.siteconfig.UpdateSiteConfigCommand;

/**
 * 站点配置全量更新请求，保留每个 JSON 字段的 presence 信息。
 */
public class UpdateSiteConfigRequest {

    private SubmittedField<String> siteTitleZh = SubmittedField.absent();
    private SubmittedField<String> siteTitleJa = SubmittedField.absent();
    private SubmittedField<String> siteTitleEn = SubmittedField.absent();
    private SubmittedField<String> siteSubtitleZh = SubmittedField.absent();
    private SubmittedField<String> siteSubtitleJa = SubmittedField.absent();
    private SubmittedField<String> siteSubtitleEn = SubmittedField.absent();
    private SubmittedField<String> aboutMdZh = SubmittedField.absent();
    private SubmittedField<String> aboutMdJa = SubmittedField.absent();
    private SubmittedField<String> aboutMdEn = SubmittedField.absent();
    private SubmittedField<String> logoUrl = SubmittedField.absent();
    private SubmittedField<String> faviconUrl = SubmittedField.absent();
    private SubmittedField<String> icpNo = SubmittedField.absent();
    private SubmittedField<String> spotifyPlaylistId = SubmittedField.absent();

    @JsonSetter("siteTitleZh")
    public void setSiteTitleZh(String value) {
        siteTitleZh = SubmittedField.of(value);
    }

    @JsonSetter("siteTitleJa")
    public void setSiteTitleJa(String value) {
        siteTitleJa = SubmittedField.of(value);
    }

    @JsonSetter("siteTitleEn")
    public void setSiteTitleEn(String value) {
        siteTitleEn = SubmittedField.of(value);
    }

    @JsonSetter("siteSubtitleZh")
    public void setSiteSubtitleZh(String value) {
        siteSubtitleZh = SubmittedField.of(value);
    }

    @JsonSetter("siteSubtitleJa")
    public void setSiteSubtitleJa(String value) {
        siteSubtitleJa = SubmittedField.of(value);
    }

    @JsonSetter("siteSubtitleEn")
    public void setSiteSubtitleEn(String value) {
        siteSubtitleEn = SubmittedField.of(value);
    }

    @JsonSetter("aboutMdZh")
    public void setAboutMdZh(String value) {
        aboutMdZh = SubmittedField.of(value);
    }

    @JsonSetter("aboutMdJa")
    public void setAboutMdJa(String value) {
        aboutMdJa = SubmittedField.of(value);
    }

    @JsonSetter("aboutMdEn")
    public void setAboutMdEn(String value) {
        aboutMdEn = SubmittedField.of(value);
    }

    @JsonSetter("logoUrl")
    public void setLogoUrl(String value) {
        logoUrl = SubmittedField.of(value);
    }

    @JsonSetter("faviconUrl")
    public void setFaviconUrl(String value) {
        faviconUrl = SubmittedField.of(value);
    }

    @JsonSetter("icpNo")
    public void setIcpNo(String value) {
        icpNo = SubmittedField.of(value);
    }

    @JsonSetter("spotifyPlaylistId")
    public void setSpotifyPlaylistId(String value) {
        spotifyPlaylistId = SubmittedField.of(value);
    }

    /**
     * 拒绝未声明字段，避免客户端拼写错误被静默忽略。
     */
    @JsonAnySetter
    public void rejectUnknown(String name, JsonNode value) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "不支持的站点配置字段：" + name);
    }

    /**
     * 校验全部字段均已提交并转换为应用命令。
     */
    public UpdateSiteConfigCommand toCommand() {
        if (!allPresent()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "PUT 请求必须包含全部站点配置字段");
        }
        return new UpdateSiteConfigCommand(
                siteTitleZh.value(),
                siteTitleJa.value(),
                siteTitleEn.value(),
                siteSubtitleZh.value(),
                siteSubtitleJa.value(),
                siteSubtitleEn.value(),
                aboutMdZh.value(),
                aboutMdJa.value(),
                aboutMdEn.value(),
                logoUrl.value(),
                faviconUrl.value(),
                icpNo.value(),
                spotifyPlaylistId.value());
    }

    private boolean allPresent() {
        return siteTitleZh.present()
                && siteTitleJa.present()
                && siteTitleEn.present()
                && siteSubtitleZh.present()
                && siteSubtitleJa.present()
                && siteSubtitleEn.present()
                && aboutMdZh.present()
                && aboutMdJa.present()
                && aboutMdEn.present()
                && logoUrl.present()
                && faviconUrl.present()
                && icpNo.present()
                && spotifyPlaylistId.present();
    }
}
