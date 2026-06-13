package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.identity.domain.profile.ProfileFieldPatch;
import com.tyb.myblog.v2.identity.domain.profile.UserProfilePatch;

/**
 * 当前用户资料部分更新命令，保留每个字段是否被提交的信息。
 */
public record UpdateCurrentUserProfileCommand(
        PatchValue<String> nickname,
        PatchValue<String> avatarUrl,
        PatchValue<String> bioZh,
        PatchValue<String> bioJa,
        PatchValue<String> bioEn,
        PatchValue<String> location,
        PatchValue<String> website,
        PatchValue<String> emailPublic,
        PatchValue<String> githubUrl,
        PatchValue<String> twitterUrl,
        PatchValue<String> linkedinUrl,
        PatchValue<String> zhihuUrl,
        PatchValue<String> qiitaUrl,
        PatchValue<String> juejinUrl
) {

    /**
     * 将空包装器统一视为未提交字段。
     */
    public UpdateCurrentUserProfileCommand {
        nickname = normalize(nickname);
        avatarUrl = normalize(avatarUrl);
        bioZh = normalize(bioZh);
        bioJa = normalize(bioJa);
        bioEn = normalize(bioEn);
        location = normalize(location);
        website = normalize(website);
        emailPublic = normalize(emailPublic);
        githubUrl = normalize(githubUrl);
        twitterUrl = normalize(twitterUrl);
        linkedinUrl = normalize(linkedinUrl);
        zhihuUrl = normalize(zhihuUrl);
        qiitaUrl = normalize(qiitaUrl);
        juejinUrl = normalize(juejinUrl);
    }

    /**
     * 判断命令是否至少提交了一个资料字段。
     *
     * @return 至少一个字段出现时返回 {@code true}
     */
    public boolean hasAnyPresentField() {
        return nickname.present()
                || avatarUrl.present()
                || bioZh.present()
                || bioJa.present()
                || bioEn.present()
                || location.present()
                || website.present()
                || emailPublic.present()
                || githubUrl.present()
                || twitterUrl.present()
                || linkedinUrl.present()
                || zhihuUrl.present()
                || qiitaUrl.present()
                || juejinUrl.present();
    }

    /**
     * 转换为不依赖应用层类型的领域补丁。
     *
     * @return 用户资料领域补丁
     */
    public UserProfilePatch toDomainPatch() {
        return new UserProfilePatch(
                toDomain(nickname),
                toDomain(avatarUrl),
                toDomain(bioZh),
                toDomain(bioJa),
                toDomain(bioEn),
                toDomain(location),
                toDomain(website),
                toDomain(emailPublic),
                toDomain(githubUrl),
                toDomain(twitterUrl),
                toDomain(linkedinUrl),
                toDomain(zhihuUrl),
                toDomain(qiitaUrl),
                toDomain(juejinUrl));
    }

    private static PatchValue<String> normalize(PatchValue<String> value) {
        return value == null ? PatchValue.absent() : value;
    }

    private static ProfileFieldPatch toDomain(PatchValue<String> value) {
        return value.present()
                ? ProfileFieldPatch.of(value.value())
                : ProfileFieldPatch.absent();
    }
}
