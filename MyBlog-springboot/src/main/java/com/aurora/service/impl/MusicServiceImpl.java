package com.aurora.service.impl;

import com.aurora.entity.Music;
import com.aurora.mapper.MusicMapper;
import com.aurora.model.dto.MusicAdminDTO;
import com.aurora.model.dto.MusicDTO;
import com.aurora.model.dto.PageResultDTO;
import com.aurora.model.vo.ConditionVO;
import com.aurora.model.vo.MusicVO;
import com.aurora.service.MusicService;
import com.aurora.util.BeanCopyUtil;
import com.aurora.util.PageUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MusicServiceImpl extends ServiceImpl<MusicMapper, Music> implements MusicService {

    @Autowired
    private MusicMapper musicMapper;

    @Override
    public List<MusicDTO> listMusics() {
        return musicMapper.listMusics();
    }

    @Override
    public PageResultDTO<MusicAdminDTO> listBackMusics(ConditionVO conditionVO) {
        LambdaQueryWrapper<Music> queryWrapper = new LambdaQueryWrapper<Music>()
                .and(Objects.nonNull(conditionVO.getKeywords()),
                        wrapper -> wrapper.like(Music::getMusicName, conditionVO.getKeywords())
                                .or()
                                .like(Music::getArtist, conditionVO.getKeywords()))
                .eq(Objects.nonNull(conditionVO.getStatus()), Music::getStatus, conditionVO.getStatus());
        Integer count = musicMapper.selectCount(queryWrapper);
        if (count == 0) {
            return new PageResultDTO<>();
        }
        List<MusicAdminDTO> musicDTOList = musicMapper.listBackMusics(PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);
        return new PageResultDTO<>(musicDTOList, count);
    }

    @Override
    public MusicAdminDTO getBackMusicById(Integer musicId) {
        return musicMapper.getBackMusicById(musicId);
    }

    @Override
    public void saveOrUpdateMusic(MusicVO musicVO) {
        Music music = BeanCopyUtil.copyObject(musicVO, Music.class);
        if (Objects.isNull(music.getTheme())) {
            music.setTheme("#409EFF");
        }
        this.saveOrUpdate(music);
    }

    @Override
    public void deleteMusics(List<Integer> musicIdList) {
        musicMapper.deleteBatchIds(musicIdList);
    }
}
