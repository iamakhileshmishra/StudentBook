package com.schoolerp.modules.academic.repository;

import com.schoolerp.modules.academic.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findBySchoolIdAndClassIdAndActiveTrueOrderByNameAsc(UUID schoolId, UUID classId);
    List<Section> findBySchoolIdAndActiveTrueOrderByNameAsc(UUID schoolId);
}
