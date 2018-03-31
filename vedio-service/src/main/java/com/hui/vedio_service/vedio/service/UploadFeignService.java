package com.hui.vedio_service.vedio.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

// feign图片上传问题解决： http://www.spring4all.com/article/283

@FeignClient(name = "file-service")
public interface UploadFeignService {
    @PostMapping(value = "/File/uploadFile", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Boolean uploadFile(@RequestPart("imgFile") MultipartFile multipartFile, @RequestParam("ImgFileName") String newName);

    @PostMapping(value = "/File/uploadVideoFile", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Boolean uploadVideoFile(@RequestPart("videoFile") MultipartFile multipartFile, @RequestParam("videoFileName") String newName);

//    class MultipartSupportConfig {
//        @Bean
//        public Encoder feignFormEncoder() {
//            return new SpringFormEncoder();
//        }
//    }
}
