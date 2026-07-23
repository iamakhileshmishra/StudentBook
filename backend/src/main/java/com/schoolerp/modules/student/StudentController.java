package com.schoolerp.modules.student;

import com.schoolerp.modules.student.dto.*;
import com.schoolerp.modules.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<StudentPageResponse> searchStudents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(studentService.searchStudents(q, classId, page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDetailDto> getStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.getStudent(id));
    }

    @PostMapping
    public ResponseEntity<StudentDetailDto> createStudent(
            @Valid @RequestBody StudentCreateRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDetailDto> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }
}
