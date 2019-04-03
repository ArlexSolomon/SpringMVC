package cn.org.craftsmen.mcv.service;

import cn.org.craftsmen.mcv.annotation.Service;

import java.util.Map;

public interface StudyService {
    int insert(Map<String, Object> map);

    int delete(Map<String, Object> map);

    int update(Map<String, Object> map);

    int query(String key);
}
