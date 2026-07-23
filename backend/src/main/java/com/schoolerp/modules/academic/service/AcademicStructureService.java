package com.schoolerp.modules.academic.service;

import com.schoolerp.modules.academic.dto.AcademicYearDto;
import com.schoolerp.modules.academic.dto.ClassDto;
import com.schoolerp.modules.academic.dto.SectionDto;
import com.schoolerp.modules.academic.entity.AcademicYear;
import com.schoolerp.modules.academic.entity.SchoolClass;
import com.schoolerp.modules.academic.entity.Section;
import com.schoolerp.modules.academic.repository.AcademicYearRepository;
import com.schoolerp.modules.academic.repository.ClassRepository;
import com.schoolerp.modules.academic.repository.SectionRepository;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicStructureService {

    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    public List<AcademicYearDto> getAcademicYears() {
        UUID schoolId = TenantContext.getSchoolId();
        return yearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().map(this::toYearDto).toList();
    }

    public AcademicYearDto getCurrentAcademicYear() {
        UUID schoolId = TenantContext.getSchoolId();
        return yearRepository.findCurrentBySchoolId(schoolId)
                .map(this::toYearDto)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year configured"));
    }

    public UUID getCurrentAcademicYearId() {
        UUID schoolId = TenantContext.getSchoolId();
        return yearRepository.findCurrentBySchoolId(schoolId)
                .map(AcademicYear::getId)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year configured"));
    }

    public List<ClassDto> getClasses() {
        UUID schoolId = TenantContext.getSchoolId();
        List<SchoolClass> classes = classRepository.findBySchoolIdAndActiveTrueOrderByNumericOrderAsc(schoolId);
        List<Section> allSections = sectionRepository.findBySchoolIdAndActiveTrueOrderByNameAsc(schoolId);

        return classes.stream().map(c -> {
            List<SectionDto> sections = allSections.stream()
                    .filter(s -> s.getClassId().equals(c.getId()))
                    .map(s -> SectionDto.builder().id(s.getId()).name(s.getName()).build())
                    .toList();
            return ClassDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .numericOrder(c.getNumericOrder())
                    .sections(sections)
                    .build();
        }).toList();
    }

    public List<SectionDto> getSectionsByClass(UUID classId) {
        UUID schoolId = TenantContext.getSchoolId();
        return sectionRepository
                .findBySchoolIdAndClassIdAndActiveTrueOrderByNameAsc(schoolId, classId)
                .stream()
                .map(s -> SectionDto.builder().id(s.getId()).name(s.getName()).build())
                .toList();
    }

    private AcademicYearDto toYearDto(AcademicYear y) {
        return AcademicYearDto.builder()
                .id(y.getId())
                .name(y.getName())
                .startDate(y.getStartDate())
                .endDate(y.getEndDate())
                .current(y.isCurrent())
                .build();
    }
}
