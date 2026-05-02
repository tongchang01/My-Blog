package com.aurora.service;

import com.aurora.entity.Music;
import com.aurora.model.dto.MusicAdminDTO;
import com.aurora.model.dto.MusicDTO;
import com.aurora.model.dto.PageResultDTO;
import com.aurora.model.vo.ConditionVO;
import com.aurora.model.vo.MusicVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MusicService extends IService<Music> {

    List<MusicDTO> listMusics();

    PageResultDTO<MusicAdminDTO> listBackMusics(ConditionVO conditionVO);

    MusicAdminDTO getBackMusicById(Integer musicId);

    void saveOrUpdateMusic(MusicVO musicVO);

    void deleteMusics(List<Integer> musicIdList);

}
