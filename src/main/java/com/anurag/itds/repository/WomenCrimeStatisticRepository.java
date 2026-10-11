package com.anurag.itds.repository;

import com.anurag.itds.entity.WomenCrimeStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WomenCrimeStatisticRepository extends JpaRepository<WomenCrimeStatistic, Long> {

    List<WomenCrimeStatistic> findAllByOrderByStateAscYearAsc();
}
