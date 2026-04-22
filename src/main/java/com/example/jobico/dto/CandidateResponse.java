package com.example.jobico.dto;

import com.example.jobico.entity.CandidateStatus;
import java.time.LocalDate;
import java.util.List;

public class CandidateResponse {

    private Long id;
    private String firstName;
    private String surname;
    private LocalDate dob;
    private int age;
    private String category;
    private String role;
    private int experience;
    private String workType;
    private String resumePath;
    private String mobile;
    private String email;
    private CandidateStatus status;
    private List<EducationDTO> educationList;
    private List<SkillDTO> skills;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public CandidateStatus getStatus() { return status; }
    public void setStatus(CandidateStatus status) { this.status = status; }
    public List<EducationDTO> getEducationList() { return educationList; }
    public void setEducationList(List<EducationDTO> educationList) { this.educationList = educationList; }
    public List<SkillDTO> getSkills() { return skills; }
    public void setSkills(List<SkillDTO> skills) { this.skills = skills; }
}