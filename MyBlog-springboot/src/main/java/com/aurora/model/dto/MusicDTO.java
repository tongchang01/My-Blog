package com.aurora.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicDTO {

    private Integer id;

    private String musicName;

    private String artist;

    private String album;

    private String cover;

    private String url;

    private String lrc;

    private String theme;

    private Integer sort;

}
