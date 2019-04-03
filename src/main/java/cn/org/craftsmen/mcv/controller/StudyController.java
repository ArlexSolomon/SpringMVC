package cn.org.craftsmen.mcv.controller;

import cn.org.craftsmen.mcv.annotation.Controller;
import cn.org.craftsmen.mcv.annotation.Qualifier;
import cn.org.craftsmen.mcv.annotation.RequestMapping;
import cn.org.craftsmen.mcv.annotation.RequestParam;
import cn.org.craftsmen.mcv.service.StudyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/study")
public class StudyController {

    @Qualifier("studyService")
    private StudyService studyService;

    @RequestMapping("/aaa")
    public void test(HttpServletRequest request, HttpServletResponse response, @RequestParam("param") String param) {
        studyService.insert(null);
        studyService.delete(null);
        studyService.update(null);
        studyService.query(null);
        try {
            response.getWriter().write("param: " + param + "\n");
            response.getWriter().write("test success! \n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
