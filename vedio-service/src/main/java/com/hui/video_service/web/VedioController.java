package com.hui.video_service.web;


import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.hui.video_service.common.Result;
import com.hui.video_service.entity.User;
import com.hui.video_service.entity.Video;
import com.hui.video_service.entity.vo.VideoPageVo;
import com.hui.video_service.mapper.VedioMapper;
import com.hui.video_service.service.IVedioService;
import com.hui.video_service.service.UploadFeignService;
import com.hui.video_service.common.utils.CookieUtils;
import com.hui.video_service.service.UserFeignService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author
 * @since 2018-03-30
 */
@RestController
@RequestMapping("/api/web/video")
public class VedioController {

    @Value("${FtpVideoPath}")
    private String ftpVideoPath;

    @Value("${FtpVideoImgPath}")
    private String FtpVideoImgPath;

    @Resource
    private UserFeignService userFeignService;

    @Resource
    private UploadFeignService uploadFeignService;

    @Resource
    private IVedioService vedioService;

    @Resource
    private VedioMapper vedioMapper;

    /**
     * 添加一个新的视频
     * @param imgFile
     * @param videoFile
     * @param videoName
     * @param videoType
     * @param videoIntroduction
     * @param request
     * @return
     */
    @Transactional
    @PostMapping(value = "/addVideo")
    public Result addVideo(@RequestParam("imgFile") MultipartFile imgFile, @RequestParam("videoFile") MultipartFile videoFile,
                           @RequestParam("name") String videoName, @RequestParam("type") Integer videoType, @RequestParam("desc") String videoIntroduction,
                           HttpServletRequest request){
        Long userId = CookieUtils.getUserIdByToken(request);
        User user = userFeignService.getUserById(userId);
        if (user != null) {
            Video vedio = new Video();
            if (!StringUtils.isEmpty(videoName) && videoType != null && !StringUtils.isEmpty(videoIntroduction) &&
            !imgFile.isEmpty() && !videoFile.isEmpty()){
                vedio.setName(videoName);
                vedio.setType(videoType);
                vedio.setIntroduction(videoIntroduction);
                vedio.setSize((double) videoFile.getSize());
                vedio.setUserId(user.getId());
                vedio.setFormat("video/mp4");

                // 先上传视频
                List<Video> userVideoList = vedioService.getAllVideoByUserId(user.getId());
                StringBuilder videoFileName = new StringBuilder();
                videoFileName.append("video_" + user.getId().toString() + "_rank_");
                if (userVideoList != null){
                    videoFileName.append( userVideoList.size() + ".mp4");
                } else {
                    videoFileName.append( 0 + ".mp4");
                }
                Boolean videoUploadFlag = uploadFeignService.uploadVideoFile(videoFile, videoFileName.toString());
                if (!videoUploadFlag) {
                    return new Result<>(Result.SERVER_ERROR, "视频上传失败", "");
                }
                vedio.setVideoFileInFtp(videoFileName.toString());
                // 存入数据库中的视频地址
                String newFileName = "http://" + ftpVideoPath + videoFileName.toString();
                vedio.setVideoUrl(newFileName);

                // 再上传封面
                StringBuilder imgFileName = new StringBuilder();
                imgFileName.append("videoImg_" + user.getId().toString() + "_rank_");
                if (userVideoList != null || userVideoList.size() > 0){
                    imgFileName.append( userVideoList.size() + ".jpg");
                } else {
                    imgFileName.append( 0 + ".jpg");
                }
                String ImgFileName = "videoImg_" + user.getId().toString() + "_rank_" + userVideoList.size() + ".jpg";
                Boolean videoImgUploadFlag = uploadFeignService.uploadFile(imgFile, ImgFileName);
                if (!videoImgUploadFlag) {
                    return new Result<>(Result.SERVER_ERROR, "封面图片上传失败", "");
                }
                vedio.setVideoImgInFtp(imgFileName.toString());
                // 存入数据库中的视频图片地址
                String newImgFileName = "http://" + FtpVideoImgPath + ImgFileName;
                vedio.setVideoImg(newImgFileName);
                Integer result = vedioMapper.insert(vedio);
                return result > 0 ? new Result<>(Result.SUCCESS, "上传成功", "") : new Result<>(Result.SERVER_ERROR, "入库失败", "" );

            }
            return new Result<>(Result.PARAM_ERROR, "检查参数是否为空", "");
        }
        return new Result<>(Result.PARAM_ERROR, "无法获取到用户", "");
    }

    /**
     * 我的视频页面获取我的视频
     * @param request
     * @return
     */
    @GetMapping(value = "/getMyVideo")
    public Result getMyVideo(Page<Video> page, HttpServletRequest request){
        Long userId = CookieUtils.getUserIdByToken(request);
        if (userId == null) {
            return new Result<>(Result.PARAM_ERROR, "无法获取到用户", "");
        }
        Page<Video> vedioPage = vedioService.selectVideoPage(page, userId);
        VideoPageVo videoPageVo = new VideoPageVo();
        videoPageVo.setPage(vedioPage);
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.where("user_id={0}", userId);
        List<Video> list = vedioMapper.selectList(wrapper);
        videoPageVo.setTotal(list.size());
        return new Result<>(Result.SUCCESS, "获取成功", "", videoPageVo);
    }

    /**
     * 我的视频页面修改视频信息
     * @param id
     * @param name
     * @param introduction
     * @param type
     * @return
     */
    @PostMapping(value = "/editVideo")
    public Result editVideo(@RequestParam Long id, @RequestParam String name, @RequestParam String introduction,@RequestParam Integer type){
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(introduction) || id == null || type == null){
            return new Result<>(Result.PARAM_ERROR, "参数有空值", "");
        }
        Video vedioInDb = vedioMapper.selectById(id);
        if (vedioInDb == null){
            return new Result<>(Result.PARAM_ERROR, "无此视频", "");
        }
        vedioInDb.setName(name);
        vedioInDb.setIntroduction(introduction);
        vedioInDb.setType(type);
        // 更新库中记录
        Integer flag = vedioMapper.updateById(vedioInDb);
        return flag > 0 ? new Result<>(Result.SUCCESS, "修改成功", "") : new Result<>(Result.SERVER_ERROR, "服务器发生错误", "");
    }

    /**
     * 我的视频页面删除视频
     * @param
     * @return
     */
    @Transactional
    @PostMapping(value = "/removeVideo")
    public Result removeVide(Long videoId){
        if (StringUtils.isEmpty(videoId)){
            return new Result<>(Result.PARAM_ERROR, "参数有空值", "");
        }
        Video vedioInDb = vedioMapper.selectById(videoId);
        if (vedioInDb == null){
            return new Result<>(Result.PARAM_ERROR, "无此视频", "");
        }

        //删除ftp上的文件
        Boolean removeVideoFileFlag = uploadFeignService.removeVideoFile(vedioInDb.getVideoFileInFtp());
        if (!removeVideoFileFlag){
            return new Result<>(Result.SERVER_ERROR, "删除服务器视频文件失败", "");
        }
        //删除ftp上的视频封面
        Boolean removeVideoImgFlag = uploadFeignService.removeVideoImg(vedioInDb.getVideoImgInFtp());
        if (!removeVideoImgFlag){
            return new Result<>(Result.SERVER_ERROR, "删除服务器视频文件封面失败", "");
        }

        //删除库中记录
        vedioInDb.setDelFlag(1);
        Integer flag = vedioMapper.updateById(vedioInDb);
        if (flag <= 0){
            return new Result<>(Result.SERVER_ERROR, "服务器发生错误", "");
        }
        return new Result<>(Result.SUCCESS, "删除成功", "");
    }

    /**
     * 通过id获取视频
     * @param videoId
     * @return
     */
    @PostMapping(value = "/getVideoById")
    public Result getVideoById(String videoId){
        if (StringUtils.isEmpty(videoId)){
            return new Result<>(Result.PARAM_ERROR, "参数有空值", "");
        }
        Long id = Long.valueOf(videoId);
        Video vedioInDb = vedioMapper.selectById(id);
        if (vedioInDb == null){
            return new Result<>(Result.PARAM_ERROR, "无此视频", "");
        }
        // 添加一次播放量
        vedioInDb.setPlayAmount(vedioInDb.getPlayAmount()+1);
        vedioMapper.updateById(vedioInDb);
        return new Result<>(Result.SUCCESS, "获取成功", "", vedioInDb);
    }

    /**
     * 通过视频ID获取用户
     * @param videoId
     * @return
     */
    @RequestMapping(value = "/getUserByVideoId")
    public Result getUserByVideoId(String videoId){
        if (StringUtils.isEmpty(videoId)){
            return new Result<>(Result.PARAM_ERROR, "参数有空值", "");
        }
        Long id = Long.valueOf(videoId);
        User user = vedioService.getUserByVideoId(id);
        return new Result<>(Result.SUCCESS, "获取成功", "", user);
    }

    @GetMapping(value = "/getHomePageVideo")
    public Result getAllVideo(){
        EntityWrapper<Video> wrapper = new EntityWrapper<>();
        wrapper.where("del_flag={0}", 0);
        List<Video> vedioList = vedioMapper.selectList(wrapper);
        List<Video> timeList = vedioList;

        // 每种类型选取前八个
        Map<String, List<Video>> result = new HashMap<>();
        List<Video> zero = new ArrayList<>();
        List<Video> two = new ArrayList<>();
        List<Video> three = new ArrayList<>();
        List<Video> four = new ArrayList<>();
        List<Video> one = new ArrayList<>();

        // 按时间排序
        timeList.sort(new Comparator<Video>() {
            @Override
            public int compare(Video dt1, Video dt2) {
//                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    if (dt1.getCreateTime().getTime() > dt2.getCreateTime().getTime()) {
                        return -1;
                    } else if (dt1.getCreateTime().getTime() < dt2.getCreateTime().getTime()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        zero = timeList.stream().filter(n -> n.getType() == 0).limit(8).collect(Collectors.toList());
        one = timeList.stream().filter(n -> n.getType() == 1).limit(8).collect(Collectors.toList());
        two = timeList.stream().filter(n -> n.getType() == 2).limit(8).collect(Collectors.toList());
        three = timeList.stream().filter(n -> n.getType() == 3).limit(8).collect(Collectors.toList());
        four = timeList.stream().filter(n -> n.getType() == 4).limit(8).collect(Collectors.toList());
        result.put("zero", zero);
        result.put("one", one);
        result.put("two", two);
        result.put("three", three);
        result.put("four", four);
        return new Result<>(Result.SUCCESS, "获取成功", "", result);
    }

    @GetMapping(value = "/getHomeMarquee")
    public Result getHomeMarquee(){
        EntityWrapper<Video> wrapper = new EntityWrapper<>();
        wrapper.where("del_flag={0}", 0);
        List<Video> vedioList = vedioMapper.selectList(wrapper);
        List<Video> playList = vedioList;
        Map<String, List<Video>> result = new HashMap<>();
        List<Video> marquee = new ArrayList<>();
        // 按播放量排序
        playList.sort(new Comparator<Video>() {
            @Override
            public int compare(Video dt1, Video dt2) {
                try {
                    if (dt1.getPlayAmount() > dt2.getPlayAmount()) {
                        return -1;
                    } else if (dt1.getPlayAmount() < dt2.getPlayAmount()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        marquee = playList.stream().limit(4).collect(Collectors.toList());
        result.put("marquee", marquee);
        return new Result<>(Result.SUCCESS, "获取成功", "", result);
    }

    /**
     * 根据类型获取视频
     * @param page
     * @param type
     * @return
     */
    @GetMapping(value = "/getVideoByType")
    public Result getVideoByType(Page<Video> page, String type){
        if (page.getSize()==0 || page.getCurrent()==0 || StringUtils.isEmpty(type)){
            return new Result<>(Result.PARAM_ERROR, "参数有空值", "");
        }
        EntityWrapper<Video> wrapper = new EntityWrapper<>();
        wrapper.where("del_flag={0}", 0).andNew("type={0}", type);
        List<Video> list = vedioMapper.selectList(wrapper);
        List<Video> vedioList = vedioMapper.selectPage(page, wrapper);
        // 按时间排序
        vedioList.sort(new Comparator<Video>() {
            @Override
            public int compare(Video dt1, Video dt2) {
//                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    if (dt1.getCreateTime().getTime() > dt2.getCreateTime().getTime()) {
                        return -1;
                    } else if (dt1.getCreateTime().getTime() < dt2.getCreateTime().getTime()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        page.setTotal(vedioList.size());
        page.setRecords(vedioList);
        VideoPageVo videoPageVo = new VideoPageVo();
        videoPageVo.setPage(page);
        videoPageVo.setTotal(list.size());
        return new Result<>(Result.SUCCESS, "获取成功", "", videoPageVo);
    }

    /**
     * 为视频添加评论数
     * @param videoId
     * @return
     */
    @PostMapping(value = "addCommentCount")
    Integer addCommentCount(Long videoId){
        if (StringUtils.isEmpty(videoId)){
            return -1;
        }
        Video vedioInDb = vedioMapper.selectById(videoId);
        vedioInDb.setCommentAmount(vedioInDb.getCommentAmount() + 1);
        vedioMapper.updateById(vedioInDb);
        return vedioInDb.getCommentAmount() + 1;
    }

}
