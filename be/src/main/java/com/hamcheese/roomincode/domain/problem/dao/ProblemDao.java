package com.hamcheese.roomincode.domain.problem.dao;

import com.hamcheese.roomincode.domain.problem.dto.Problem;
import com.hamcheese.roomincode.domain.problem.dto.ProblemHint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProblemDao {
    // 알고리즘 유형과 난이도에 맞는 랜덤 문제 1개 조회
    Problem findRandomProblem(@Param("algorithm") String algorithm, @Param("difficulty") String difficulty);

    // 문제 ID에 해당하는 힌트 목록 조회
    List<ProblemHint> findHintsByProblemId(Long problemId);

    Problem findById(@Param("problemId") Long problemId);
}
