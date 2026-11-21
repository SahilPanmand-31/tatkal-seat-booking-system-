package com.sahilpanmand.tatkal.repository;

import com.sahilpanmand.tatkal.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JourneyRepository extends JpaRepository<Journey, Long> {

    boolean existsByTrainIdAndJourneyDate(Long trainId, LocalDate journeyDate);

    @Query("""
            select j from Journey j join fetch j.train t
            where lower(t.source) = lower(:source)
              and lower(t.destination) = lower(:destination)
              and j.journeyDate = :date
            order by t.departureTime
            """)
    List<Journey> search(@Param("source") String source,
                         @Param("destination") String destination,
                         @Param("date") LocalDate date);
}
