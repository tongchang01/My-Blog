package com.aurora.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "闊充箰")
public class MusicVO {

    @ApiModelProperty(name = "id", value = "闊充箰id", dataType = "Integer")
    private Integer id;

    @NotBlank(message = "姝屾洸鍚嶇О涓嶈兘涓虹┖")
    @ApiModelProperty(name = "musicName", value = "姝屾洸鍚嶇О", required = true, dataType = "String")
    private String musicName;

    @NotBlank(message = "姝屾墜涓嶈兘涓虹┖")
    @ApiModelProperty(name = "artist", value = "姝屾墜", required = true, dataType = "String")
    private String artist;

    @ApiModelProperty(name = "album", value = "涓撹緫", dataType = "String")
    private String album;

    @ApiModelProperty(name = "cover", value = "灏侀潰鍦板潃", dataType = "String")
    private String cover;

    @NotBlank(message = "闊充箰鍦板潃涓嶈兘涓虹┖")
    @ApiModelProperty(name = "url", value = "闊充箰鍦板潃", required = true, dataType = "String")
    private String url;

    @ApiModelProperty(name = "lrc", value = "姝岃瘝鍦板潃", dataType = "String")
    private String lrc;

    @ApiModelProperty(name = "theme", value = "涓婚鑹?", dataType = "String")
    private String theme;

    @NotNull(message = "鎺掑簭涓嶈兘涓虹┖")
    @ApiModelProperty(name = "sort", value = "鎺掑簭", required = true, dataType = "Integer")
    private Integer sort;

    @NotNull(message = "鐘舵€佷笉鑳戒负绌?")
    @ApiModelProperty(name = "status", value = "鐘舵€? 0鍏抽棴 1鍚敤", required = true, dataType = "Integer")
    private Integer status;

    @ApiModelProperty(name = "remark", value = "澶囨敞", dataType = "String")
    private String remark;

}
