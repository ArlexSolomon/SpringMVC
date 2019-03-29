package cn.org.craftsmen.mcv.controller;

import cn.org.craftsmen.mcv.annotation.Controller;
import cn.org.craftsmen.mcv.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/study")
public class StudyController {
    @RequestMapping("/aaa")
    public String test(HttpServletRequest request, HttpServletResponse response) {
        return "test success";
    }
}
