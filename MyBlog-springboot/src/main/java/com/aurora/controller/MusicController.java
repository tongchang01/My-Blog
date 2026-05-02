package com.aurora.controller;

import com.aurora.annotation.OptLog;
import com.aurora.model.dto.MusicAdminDTO;
import com.aurora.model.dto.MusicDTO;
import com.aurora.model.dto.PageResultDTO;
import com.aurora.model.vo.ConditionVO;
import com.aurora.model.vo.MusicVO;
import com.aurora.model.vo.ResultVO;
import com.aurora.service.MusicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.aurora.constant.OptTypeConstant.DELETE;
import static com.aurora.constant.OptTypeConstant.SAVE_OR_UPDATE;

@Api(tags = "闊充箰妯″潡")
@RestController
public class MusicController {

    @Autowired
    private MusicService musicService;

    @ApiOperation(value = "鑾峰彇闊充箰鍒楄〃")
    @GetMapping("/musics")
    public ResultVO<List<MusicDTO>> listMusics() {
        return ResultVO.ok(musicService.listMusics());
    }

    @ApiOperation(value = "鏌ョ湅鍚庡彴闊充箰鍒楄〃")
    @GetMapping("/admin/musics")
    public ResultVO<PageResultDTO<MusicAdminDTO>> listBackMusics(ConditionVO conditionVO) {
        return ResultVO.ok(musicService.listBackMusics(conditionVO));
    }

    @ApiOperation(value = "鏍规嵁id鏌ョ湅鍚庡彴闊充箰")
    @ApiImplicitParam(name = "musicId", value = "闊充箰id", required = true, dataType = "Integer")
    @GetMapping("/admin/musics/{musicId}")
    public ResultVO<MusicAdminDTO> getBackMusicById(@PathVariable("musicId") Integer musicId) {
        return ResultVO.ok(musicService.getBackMusicById(musicId));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "淇濆瓨鎴栦慨鏀归煶涔?")
    @PostMapping("/admin/musics")
    public ResultVO<?> saveOrUpdateMusic(@Valid @RequestBody MusicVO musicVO) {
        musicService.saveOrUpdateMusic(musicVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @ApiOperation(value = "鍒犻櫎闊充箰")
    @DeleteMapping("/admin/musics")
    public ResultVO<?> deleteMusics(@RequestBody List<Integer> musicIdList) {
        musicService.deleteMusics(musicIdList);
        return ResultVO.ok();
    }

}
