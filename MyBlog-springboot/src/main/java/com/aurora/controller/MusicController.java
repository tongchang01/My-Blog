package com.aurora.controller;

import com.aurora.annotation.OptLog;
import com.aurora.enums.FilePathEnum;
import com.aurora.exception.BizException;
import com.aurora.model.dto.MusicAdminDTO;
import com.aurora.model.dto.MusicDTO;
import com.aurora.model.dto.PageResultDTO;
import com.aurora.model.vo.ConditionVO;
import com.aurora.model.vo.MusicVO;
import com.aurora.model.vo.ResultVO;
import com.aurora.service.MusicService;
import com.aurora.strategy.context.UploadStrategyContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

import static com.aurora.constant.OptTypeConstant.DELETE;
import static com.aurora.constant.OptTypeConstant.SAVE_OR_UPDATE;
import static com.aurora.constant.OptTypeConstant.UPLOAD;

@Api(tags = "音乐模块")
@RestController
public class MusicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @ApiOperation(value = "获取音乐列表")
    @GetMapping("/musics")
    public ResultVO<List<MusicDTO>> listMusics() {
        return ResultVO.ok(musicService.listMusics());
    }

    @ApiOperation(value = "获取后台音乐列表")
    @GetMapping("/admin/musics")
    public ResultVO<PageResultDTO<MusicAdminDTO>> listBackMusics(ConditionVO conditionVO) {
        return ResultVO.ok(musicService.listBackMusics(conditionVO));
    }

    @ApiOperation(value = "根据 id 获取后台音乐")
    @ApiImplicitParam(name = "musicId", value = "音乐 id", required = true, dataType = "Integer")
    @GetMapping("/admin/musics/{musicId}")
    public ResultVO<MusicAdminDTO> getBackMusicById(@PathVariable("musicId") Integer musicId) {
        return ResultVO.ok(musicService.getBackMusicById(musicId));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "保存或更新音乐")
    @PostMapping("/admin/musics")
    public ResultVO<?> saveOrUpdateMusic(@Valid @RequestBody MusicVO musicVO) {
        musicService.saveOrUpdateMusic(musicVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPLOAD)
    @ApiOperation(value = "上传音乐资源")
    @ApiImplicitParam(name = "file", value = "音乐文件", required = true, dataType = "MultipartFile")
    @PostMapping("/admin/musics/upload")
    public ResultVO<String> uploadMusicFile(MultipartFile file) {
        try {
            return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.MUSIC.getPath()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("音乐上传失败");
        }
    }

    @OptLog(optType = DELETE)
    @ApiOperation(value = "删除音乐")
    @DeleteMapping("/admin/musics")
    public ResultVO<?> deleteMusics(@RequestBody List<Integer> musicIdList) {
        musicService.deleteMusics(musicIdList);
        return ResultVO.ok();
    }

}
