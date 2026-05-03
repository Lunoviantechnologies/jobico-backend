package com.example.jobico.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CandidateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Surname is required")
    private String surname;

    @NotNull(message = "Date of birth is required")
    private LocalDate dob;

    private String email;

    @NotBlank(message = "Category is required (IT / Non-IT / Blue Collar)")
    private String category;

    @NotBlank(message = "Job role is required")
    private String role;

    private int experience;
    private String workType;
    private String description;   
    private String location;

    @Valid
    private List<EducationDTO> educationList;

    @Valid
    private List<SkillDTO> skills;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public List<EducationDTO> getEducationList() { return educationList; }
    public void setEducationList(List<EducationDTO> educationList) { this.educationList = educationList; }
    public List<SkillDTO> getSkills() { return skills; }
    public void setSkills(List<SkillDTO> skills) { this.skills = skills; }
	public String getDescription() {return description;}
	public void setDescription(String description) {this.description = description;}
	public String getLocation() {return location;}
	public void setLocation(String location) {this.location = location;}
    
}