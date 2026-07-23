package com.schoolerp.modules.academic;

import com.schoolerp.modules.academic.dto.AcademicYearDto;
import com.schoolerp.modules.academic.dto.ClassDto;
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
}
