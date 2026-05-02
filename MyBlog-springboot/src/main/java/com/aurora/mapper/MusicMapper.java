package com.aurora.mapper;

import com.aurora.entity.Music;
import com.aurora.model.dto.MusicAdminDTO;
import com.aurora.model.dto.MusicDTO;
import com.aurora.model.vo.ConditionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicMapper extends BaseMapper<Music> {

    List<MusicDTO> listMusics();

    List<MusicAdminDTO> listBackMusics(@Param("current") Long current,
                                       @Param("size") Long size,
                                       @Param("conditionVO") ConditionVO conditionVO);

    MusicAdminDTO getBackMusicById(@Param("musicId") Integer musicId);

}
