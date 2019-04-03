package cn.org.craftsmen.mcv.service.impl;

import cn.org.craftsmen.mcv.annotation.Service;
import cn.org.craftsmen.mcv.service.StudyService;

import java.util.Map;

@Service("studyService")
public class StudyServiceImpl implements StudyService {
    @Override
    public int insert(Map<String, Object> map) {
        System.out.println("I am here in StudyService insert method");
        return 0;
    }

    @Override
    public int delete(Map<String, Object> map) {
        System.out.println("I am here in StudyService delete method");
        return 0;
    }

    @Override
    public int update(Map<String, Object> map) {
        System.out.println("I am here in StudyService update method");
        return 0;
    }

    @Override
    public int query(String key) {
        System.out.println("I am here in StudyService query method");
        return 0;
    }
}
