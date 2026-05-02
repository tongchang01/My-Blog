package com.aurora.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicAdminDTO {

    private Integer id;

    private String musicName;

    private String artist;

    private String album;

    private String cover;

    private String url;

    private String lrc;

    private String theme;

    private Integer sort;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
