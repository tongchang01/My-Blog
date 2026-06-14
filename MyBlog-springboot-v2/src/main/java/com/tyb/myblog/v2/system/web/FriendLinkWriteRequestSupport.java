package com.tyb.myblog.v2.system.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

/**
 * 友链完整写请求的 JSON presence 支持。
 */
abstract class FriendLinkWriteRequestSupport {

    private SubmittedField<String> name = SubmittedField.absent();
    private SubmittedField<String> url = SubmittedField.absent();
    private SubmittedField<String> avatarUrl = SubmittedField.absent();
    private SubmittedField<String> description = SubmittedField.absent();
    private SubmittedField<Integer> sortOrder = SubmittedField.absent();
    private SubmittedField<FriendLinkStatus> status =
            SubmittedField.absent();

    @JsonSetter("name")
    public void setName(String value) {
        name = SubmittedField.of(value);
    }

    @JsonSetter("url")
    public void setUrl(String value) {
        url = SubmittedField.of(value);
    }

    @JsonSetter("avatarUrl")
    public void setAvatarUrl(String value) {
        avatarUrl = SubmittedField.of(value);
    }

    @JsonSetter("description")
    public void setDescription(String value) {
        description = SubmittedField.of(value);
    }

    @JsonSetter("sortOrder")
    public void setSortOrder(Integer value) {
        sortOrder = SubmittedField.of(value);
    }

    @JsonSetter("status")
    public void setStatus(FriendLinkStatus value) {
        status = SubmittedField.of(value);
    }

    @JsonAnySetter
    public void rejectUnknown(String fieldName, JsonNode value) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "不支持的友链字段：" + fieldName);
    }

    protected Values values() {
        if (!allPresent()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "请求必须包含全部友链字段");
        }
        if (sortOrder.value() == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链排序值不能为空");
        }
        return new Values(
                name.value(),
                url.value(),
                avatarUrl.value(),
                description.value(),
                sortOrder.value(),
                status.value());
    }

    private boolean allPresent() {
        return name.present()
                && url.present()
                && avatarUrl.present()
                && description.present()
                && sortOrder.present()
                && status.present();
    }

    protected record Values(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status
    ) {
    }
}
