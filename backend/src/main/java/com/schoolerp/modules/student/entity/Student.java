package com.schoolerp.modules.student.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "admission_number", nullable = false, length = 20)
    private String admissionNumber;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "primary_contact_phone", nullable = false, length = 15)
    private String primaryContactPhone;

    @Column(name = "alternate_phone", length = 15)
    private String alternatePhone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_staff_child", nullable = false)
    private boolean staffChild;

    @Builder.Default // field defaults are ignored by @Builder otherwise (BUG-17)
    @Column(name = "concession_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal concessionAmount = BigDecimal.ZERO;

    @Column(name = "concession_note", columnDefinition = "TEXT")
    private String concessionNote;

    @Builder.Default // field defaults are ignored by @Builder otherwise (BUG-17)
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
