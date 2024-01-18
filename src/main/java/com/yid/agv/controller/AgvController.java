package com.yid.agv.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/agv")
public class AgvController {

    @GetMapping(value = "/")
    public String pageAgv() {
        return "agv";
    }
    @GetMapping(value = "/send")
    public String pageSend() {
        return "send";
    }
    @GetMapping(value = "/grid")
    public String pageGrid() {
        return "grid";
    }
    @GetMapping(value = "/analysis")
    public String pageAnalysis() {
        return "analysis";
    }
    @GetMapping(value = "/history")
    public String pageHistory() {
        return "history";
    }
//    @GetMapping(value = "/setting")
//    public String pageSetting() {
//        return "setting";
//    }

}
