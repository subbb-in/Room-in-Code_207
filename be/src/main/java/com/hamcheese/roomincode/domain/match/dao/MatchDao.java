package com.hamcheese.roomincode.domain.match.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface MatchDao {

    void saveAiReport(Map<String, Object> params);

    void saveHistory(Map<String, Object> params);
}
