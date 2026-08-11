package com.schoolerp.modules.academic.service;

import com.schoolerp.modules.academic.dto.AcademicYearDto;
import com.schoolerp.modules.academic.dto.ClassDto;
import com.schoolerp.modules.academic.dto.CreateClassRequest;
import com.schoolerp.modules.academic.dto.CreateSectionRequest;
import com.schoolerp.modules.academic.dto.SectionDto;
import com.schoolerp.modules.academic.entity.AcademicYear;
import com.schoolerp.modules.academic.entity.SchoolClass;
import com.schoolerp.modules.academic.entity.Section;
import com.schoolerp.modules.academic.repository.AcademicYearRepository;
import com.schoolerp.modules.academic.repository.ClassRepository;
import com.schoolerp.modules.academic.repository.SectionRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicStructureService {

    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final JdbcTemplate jdbcTemplate;

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

    public ClassDto createClass(CreateClassRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        
        List<SchoolClass> existingClasses = classRepository.findBySchoolIdAndActiveTrueOrderByNumericOrderAsc(schoolId);
        int maxOrder = existingClasses.isEmpty() ? -1 : existingClasses.get(existingClasses.size() - 1).getNumericOrder();
        int targetOrder = maxOrder + 1;
        
        if (request.getNumericOrder() != null) {
            if (request.getNumericOrder() > targetOrder) {
                targetOrder = targetOrder;
            } else {
                targetOrder = request.getNumericOrder();
                final int checkOrder = targetOrder;
                if (existingClasses.stream().anyMatch(c -> c.getNumericOrder() == checkOrder)) {
                    throw new BusinessException("Display order " + checkOrder + " is already in use by another class.");
                }
            }
        }

        SchoolClass newClass = SchoolClass.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .numericOrder(targetOrder)
                .active(true)
                .build();
        SchoolClass saved = classRepository.save(newClass);

        // Find the closest lower class to copy fee structures from
        SchoolClass sourceClass = null;
        for (int i = existingClasses.size() - 1; i >= 0; i--) {
            if (existingClasses.get(i).getNumericOrder() <= targetOrder) {
                sourceClass = existingClasses.get(i);
                break;
            }
        }
        // If no lower class exists, try to get the lowest one available just to have a baseline
        if (sourceClass == null && !existingClasses.isEmpty()) {
            sourceClass = existingClasses.get(0);
        }

        if (sourceClass != null) {
            UUID currentYearId = getCurrentAcademicYearId();
            String sql = "INSERT INTO monthly_fee_structures (school_id, academic_year_id, class_id, fee_category_id, default_amount) " +
                         "SELECT school_id, ?, ?, fee_category_id, default_amount " +
                         "FROM monthly_fee_structures " +
                         "WHERE class_id = ? AND academic_year_id = ?";
            jdbcTemplate.update(sql, currentYearId, saved.getId(), sourceClass.getId(), currentYearId);
        }

        return ClassDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .numericOrder(saved.getNumericOrder())
                .sections(List.of())
                .build();
    }

    public ClassDto updateClass(UUID classId, CreateClassRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        SchoolClass existing = classRepository.findById(classId)
                .filter(c -> c.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getNumericOrder() != null && request.getNumericOrder() != existing.getNumericOrder()) {
            List<SchoolClass> existingClasses = classRepository.findBySchoolIdAndActiveTrueOrderByNumericOrderAsc(schoolId);
            int maxOrder = existingClasses.isEmpty() ? -1 : existingClasses.get(existingClasses.size() - 1).getNumericOrder();
            int targetOrder = request.getNumericOrder();
            
            if (targetOrder > maxOrder + 1) {
                targetOrder = maxOrder + 1;
            }
            
            final int checkOrder = targetOrder;
            if (existingClasses.stream().anyMatch(c -> !c.getId().equals(classId) && c.getNumericOrder() == checkOrder)) {
                throw new BusinessException("Display order " + checkOrder + " is already in use by another class.");
            }
            existing.setNumericOrder(targetOrder);
        }
        SchoolClass saved = classRepository.save(existing);
        
        List<SectionDto> sections = getSectionsByClass(saved.getId());
        
        return ClassDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .numericOrder(saved.getNumericOrder())
                .sections(sections)
                .build();
    }

    public void deleteClass(UUID classId) {
        UUID schoolId = TenantContext.getSchoolId();
        SchoolClass existing = classRepository.findById(classId)
                .filter(c -> c.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        existing.setActive(false);
        classRepository.save(existing);
    }

    public SectionDto createSection(UUID classId, CreateSectionRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        SchoolClass existingClass = classRepository.findById(classId)
                .filter(c -> c.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Section newSection = Section.builder()
                .schoolId(schoolId)
                .classId(existingClass.getId())
                .name(request.getName())
                .active(true)
                .build();
        Section saved = sectionRepository.save(newSection);
        return SectionDto.builder().id(saved.getId()).name(saved.getName()).build();
    }

    public SectionDto updateSection(UUID sectionId, CreateSectionRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        Section existing = sectionRepository.findById(sectionId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        
        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        Section saved = sectionRepository.save(existing);
        return SectionDto.builder().id(saved.getId()).name(saved.getName()).build();
    }

    public void deleteSection(UUID sectionId) {
        UUID schoolId = TenantContext.getSchoolId();
        Section existing = sectionRepository.findById(sectionId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        existing.setActive(false);
        sectionRepository.save(existing);
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
