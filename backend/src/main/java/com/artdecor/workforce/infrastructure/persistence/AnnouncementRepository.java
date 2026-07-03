package com.artdecor.workforce.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {
    @Query("""
            select announcement
            from AnnouncementEntity announcement
            where announcement.visibleFrom <= :now
              and (announcement.visibleUntil is null or announcement.visibleUntil >= :now)
            order by announcement.createdAt desc
            """)
    List<AnnouncementEntity> findVisible(@Param("now") Instant now);
}
