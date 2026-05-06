package com.example.jobico.repository;

import com.example.jobico.entity.Employee;
import com.example.jobico.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCandidateId(Long candidateId);

    boolean existsByCandidateId(Long candidateId);

    // NEW: Employees page

    Page<Employee> findAll(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.candidate.firstName, ' ', e.candidate.surname)) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Employee> searchByName(@Param("name") String name, Pageable pageable);

    Page<Employee> findByDepartment(String department, Pageable pageable);

    Page<Employee> findByEmployeeStatus(EmployeeStatus status, Pageable pageable);

    Page<Employee> findByDepartmentAndEmployeeStatus(String department, EmployeeStatus status, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.candidate.firstName, ' ', e.candidate.surname)) LIKE LOWER(CONCAT('%', :name, '%')) AND e.department = :dept")
    Page<Employee> searchByNameAndDepartment(@Param("name") String name, @Param("dept") String dept, Pageable pageable);
    long countByEmployeeStatus(EmployeeStatus status);
    @Query("SELECT e FROM Employee e WHERE e.candidate.user.mobile = :mobile")
    Optional<Employee> findByUserMobile(@Param("mobile") String mobile);
    @Query("""
    	    SELECT e FROM Employee e
    	    WHERE (:search IS NULL OR
    	           LOWER(e.candidate.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
    	           LOWER(e.candidate.surname) LIKE LOWER(CONCAT('%', :search, '%')))
    	    AND (:department IS NULL OR e.department = :department)
    	    AND e.employeeStatus IN :statuses
    	""")
    	Page<Employee> findExitedEmployees(
    	        @Param("search") String search,
    	        @Param("department") String department,
    	        @Param("statuses") List<EmployeeStatus> statuses,
    	        Pageable pageable
    	);
    @Query("""
            SELECT e FROM Employee e
            WHERE e.employeeStatus IN :statuses
            AND (:search IS NULL OR
                 LOWER(e.candidate.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(e.candidate.surname)   LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:department IS NULL OR e.department = :department)
            AND e.id NOT IN (
                SELECT el.employee.id FROM ExperienceLetter el
            )
            """)
    Page<Employee> findExitedEmployeesWithoutExperienceLetter(
            @Param("search") String search,
            @Param("department") String department,
            @Param("statuses") List<EmployeeStatus> statuses,
            Pageable pageable
    );
    Optional<Employee> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
    @EntityGraph(attributePaths = {"candidate"})
    List<Employee> findByEmployeeIdIn(List<String> employeeIds);
    
    
}
