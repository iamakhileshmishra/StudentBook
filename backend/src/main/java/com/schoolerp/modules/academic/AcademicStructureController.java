package com.schoolerp.modules.academic;

import com.schoolerp.modules.academic.dto.AcademicYearDto;
import com.schoolerp.modules.academic.dto.ClassDto;
import com.schoolerp.modules.academic.dto.CreateClassRequest;
import com.schoolerp.modules.academic.dto.CreateSectionRequest;
import com.schoolerp.modules.academic.dto.SectionDto;
import com.schoolerp.modules.academic.service.AcademicStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/structure")
@RequiredArgsConstructor
public class AcademicStructureController {

    private final AcademicStructureService service;

    @GetMapping("/academic-years")
    public ResponseEntity<List<AcademicYearDto>> getAcademicYears() {
        return ResponseEntity.ok(service.getAcademicYears());
    }

    @GetMapping("/academic-years/current")
    public ResponseEntity<AcademicYearDto> getCurrentYear() {
        return ResponseEntity.ok(service.getCurrentAcademicYear());
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClassDto>> getClasses() {
        return ResponseEntity.ok(service.getClasses());
    }

    @GetMapping("/sections")
    public ResponseEntity<List<SectionDto>> getSections(@RequestParam UUID classId) {
        return ResponseEntity.ok(service.getSectionsByClass(classId));
    }

    @PostMapping("/classes")
    public ResponseEntity<ClassDto> createClass(@RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(service.createClass(request));
    }

    @PutMapping("/classes/{id}")
    public ResponseEntity<ClassDto> updateClass(@PathVariable UUID id, @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(service.updateClass(id, request));
    }

    @DeleteMapping("/classes/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable UUID id) {
        service.deleteClass(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/classes/{classId}/sections")
    public ResponseEntity<SectionDto> createSection(@PathVariable UUID classId, @RequestBody CreateSectionRequest request) {
        return ResponseEntity.ok(service.createSection(classId, request));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<SectionDto> updateSection(@PathVariable UUID id, @RequestBody CreateSectionRequest request) {
        return ResponseEntity.ok(service.updateSection(id, request));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        service.deleteSection(id);
        return ResponseEntity.ok().build();
    }
}
