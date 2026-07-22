package com.hrms.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain model representing an Employee record.
 * Demonstrates encapsulation: all fields are private with controlled access via getters/setters.
 */
public class Employee {

    public enum Gender { Male, Female, Other }

    private int employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String email;
    private String phone;
    private int departmentId;
    private String departmentName; // populated via JOIN for display purposes
    private String designation;
    private LocalDate joiningDate;
    private BigDecimal basicSalary;
    private BigDecimal bonus;
    private BigDecimal allowances;
    private BigDecimal pfPercent;
    private BigDecimal otherDeductions;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Employee() {
    }

    public Employee(String employeeCode, String firstName, String lastName, Gender gender,
                     String email, String phone, int departmentId, String designation,
                     LocalDate joiningDate, BigDecimal basicSalary, BigDecimal bonus,
                     BigDecimal allowances, BigDecimal pfPercent, BigDecimal otherDeductions) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.departmentId = departmentId;
        this.designation = designation;
        this.joiningDate = joiningDate;
        this.basicSalary = basicSalary;
        this.bonus = bonus;
        this.allowances = allowances;
        this.pfPercent = pfPercent;
        this.otherDeductions = otherDeductions;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ----- Getters & Setters -----
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }

    public BigDecimal getPfPercent() { return pfPercent; }
    public void setPfPercent(BigDecimal pfPercent) { this.pfPercent = pfPercent; }

    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { this.otherDeductions = otherDeductions; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return employeeCode + " - " + getFullName();
    }
}
