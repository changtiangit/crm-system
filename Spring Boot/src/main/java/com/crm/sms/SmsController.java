package com.crm.sms;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms")
public class SmsController {
    private final SmsService service;

    public SmsController(SmsService service) {
        this.service = service;
    }

    public record SendCodeRequest(String phone, String scene) {
    }

    /** 发送登录验证码（公开接口，内部按手机号是否绑定员工 + 频控兜底） */
    @PostMapping("/code")
    public SmsSendResult sendCode(@RequestBody SendCodeRequest request) {
        return service.send(request.phone(), request.scene());
    }
}
