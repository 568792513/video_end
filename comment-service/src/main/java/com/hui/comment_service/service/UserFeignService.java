package com.hui.comment_service.service;

import com.hui.comment_service.entity.User;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("user-service")
public interface UserFeignService {
    @RequestMapping(value = "/api/web/user/getUserById", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    User getUserById(@RequestParam("userId") Long userId);
}
