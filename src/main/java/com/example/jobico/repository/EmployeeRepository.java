package com.example.jobico.repository;

import com.example.jobico.entity.Employee;
import com.example.jobico.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UPDATED EmployeeRepository — replace the existing one in your project.
 * Added: list all, search by name, filter by dept/status.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCandidateId(Long candidateId);

    boolean existsByCandidateId(Long candidateId);

    // ── NEW: Employees page ───────────────────────────────────────

    Page<Employee> findAll(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.candidate.firstName, ' ', e.candidate.surname)) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Employee> searchByName(@Param("name") String name, Pageable pageable);

    Page<Employee> findByDepartment(String department, Pageable pageable);

    Page<Employee> findByEmployeeStatus(EmployeeStatus status, Pageable pageable);

    Page<Employee> findByDepartmentAndEmployeeStatus(String department, EmployeeStatus status, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.candidate.firstName, ' ', e.candidate.surname)) LIKE LOWER(CONCAT('%', :name, '%')) AND e.department = :dept")
    Page<Employee> searchByNameAndDepartment(@Param("name") String name, @Param("dept") String dept, Pageable pageable);

    // Dashboard: count active employees
    long countByEmployeeStatus(EmployeeStatus status);
}
