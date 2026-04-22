package com.example.jobico.dto;

import jakarta.validation.constraints.NotBlank;

public class SkillDTO {

    private Long id;

    @NotBlank(message = "Skill name is required")
    private String skillName;

    private int experience;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
}