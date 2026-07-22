package com.hrms.payroll.model;

public class Department {
    private int departmentId;
    private String departmentName;
    private String departmentHead;

    public Department() {}

    public Department(int departmentId, String departmentName, String departmentHead) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentHead = departmentHead;
    }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentHead() { return departmentHead; }
    public void setDepartmentHead(String departmentHead) { this.departmentHead = departmentHead; }

    @Override
    public String toString() { return departmentName; }
}
