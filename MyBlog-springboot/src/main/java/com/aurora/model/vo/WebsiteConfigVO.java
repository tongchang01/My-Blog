package com.aurora.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "网站配置")
public class WebsiteConfigVO {

    @ApiModelProperty(name = "name", value = "网站名称", required = true, dataType = "String")
    private String name;

    @ApiModelProperty(name = "nickName", value = "网站作者昵称", required = true, dataType = "String")
    private String englishName;

    @ApiModelProperty(name = "author", value = "网站作者", required = true, dataType = "String")
    private String author;

    @ApiModelProperty(name = "avatar", value = "网站头像", required = true, dataType = "String")
    private String authorAvatar;

    @ApiModelProperty(name = "description", value = "网站作者介绍", required = true, dataType = "String")
    private String authorIntro;

    @ApiModelProperty(name = "logo", value = "网站logo", required = true, dataType = "String")
    private String logo;

    @ApiModelProperty(name = "multiLanguage", value = "多语言", required = true, dataType = "Integer")
    private Integer multiLanguage;

    @ApiModelProperty(name = "notice", value = "网站公告", required = true, dataType = "String")
    private String notice;

    @ApiModelProperty(name = "websiteCreateTime", value = "网站创建时间", required = true, dataType = "LocalDateTime")
    private String websiteCreateTime;

    @ApiModelProperty(name = "beianNumber", value = "网站备案号", required = true, dataType = "String")
    private String beianNumber;

    @ApiModelProperty(name = "qqLogin", value = "QQ登录", required = true, dataType = "Integer")
    private Integer qqLogin;

    @ApiModelProperty(name = "github", value = "github", required = true, dataType = "String")
    private String github;

    @ApiModelProperty(name = "gitee", value = "gitee", required = true, dataType = "String")
    private String gitee;

    @ApiModelProperty(name = "qq", value = "qq", required = true, dataType = "String")
    private String qq;

    @ApiModelProperty(name = "weChat", value = "微信", required = true, dataType = "String")
    private String weChat;

    @ApiModelProperty(name = "weibo", value = "微博", required = true, dataType = "String")
    private String weibo;

    @ApiModelProperty(name = "csdn", value = "csdn", required = true, dataType = "String")
    private String csdn;

    @ApiModelProperty(name = "zhihu", value = "zhihu", required = true, dataType = "String")
    private String zhihu;

    @ApiModelProperty(name = "juejin", value = "juejin", required = true, dataType = "String")
    private String juejin;

    @ApiModelProperty(name = "twitter", value = "twitter", required = true, dataType = "String")
    private String twitter;

    @ApiModelProperty(name = "stackoverflow", value = "stackoverflow", required = true, dataType = "String")
    private String stackoverflow;

    @ApiModelProperty(name = "touristAvatar", value = "游客头像", required = true, dataType = "String")
    private String touristAvatar;

    @ApiModelProperty(name = "userAvatar", value = "用户头像", required = true, dataType = "String")
    private String userAvatar;

    @ApiModelProperty(name = "isCommentReview", value = "是否评论审核", required = true, dataType = "Integer")
    private Integer isCommentReview;

    @ApiModelProperty(name = "isEmailNotice", value = "是否邮箱通知", required = true, dataType = "Integer")
    private Integer isEmailNotice;

    @ApiModelProperty(name = "isReward", value = "是否打赏", required = true, dataType = "Integer")
    private Integer isReward;

    @ApiModelProperty(name = "weiXinQRCode", value = "微信二维码", required = true, dataType = "String")
    private String weiXinQRCode;

    @ApiModelProperty(name = "alipayQRCode", value = "支付宝二维码", required = true, dataType = "String")
    private String alipayQRCode;

    @ApiModelProperty(name = "favicon", value = "favicon", required = true, dataType = "String")
    private String favicon;

    @ApiModelProperty(name = "musicPlayerEnable", value = "鏄惁鍚敤闊充箰鎾斁鍣?", required = true, dataType = "Integer")
    private Integer musicPlayerEnable;

    @ApiModelProperty(name = "musicPlayerAutoPlay", value = "鏄惁鑷姩鎾斁", required = true, dataType = "Integer")
    private Integer musicPlayerAutoPlay;

    @ApiModelProperty(name = "musicPlayerFixed", value = "鏄惁鍥哄畾鏄剧ず", required = true, dataType = "Integer")
    private Integer musicPlayerFixed;

    @ApiModelProperty(name = "musicPlayerTheme", value = "鎾斁鍣ㄤ富棰滆壊", required = true, dataType = "String")
    private String musicPlayerTheme;

    @ApiModelProperty(name = "musicPlayerLoop", value = "寰幆妯″紡", required = true, dataType = "String")
    private String musicPlayerLoop;

    @ApiModelProperty(name = "musicPlayerOrder", value = "鎾斁椤哄簭", required = true, dataType = "String")
    private String musicPlayerOrder;

    @ApiModelProperty(name = "websiteTitle", value = "网页标题", required = true, dataType = "String")
    private String websiteTitle;

    @ApiModelProperty(name = "gonganBeianNumber", value = "公安部备案编号", required = true, dataType = "String")
    private String gonganBeianNumber;

}
