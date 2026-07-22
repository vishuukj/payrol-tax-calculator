package com.hrms.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Audit trail entry recorded whenever an employee's compensation changes. */
public class SalaryHistory {
    private int historyId;
    private int employeeId;
    private BigDecimal oldBasicSalary;
    private BigDecimal newBasicSalary;
    private BigDecimal oldBonus;
    private BigDecimal newBonus;
    private Integer changedBy;
    private String changeReason;
    private LocalDateTime changedAt;

    public int getHistoryId() { return historyId; }
    public void setHistoryId(int historyId) { this.historyId = historyId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public BigDecimal getOldBasicSalary() { return oldBasicSalary; }
    public void setOldBasicSalary(BigDecimal oldBasicSalary) { this.oldBasicSalary = oldBasicSalary; }

    public BigDecimal getNewBasicSalary() { return newBasicSalary; }
    public void setNewBasicSalary(BigDecimal newBasicSalary) { this.newBasicSalary = newBasicSalary; }

    public BigDecimal getOldBonus() { return oldBonus; }
    public void setOldBonus(BigDecimal oldBonus) { this.oldBonus = oldBonus; }

    public BigDecimal getNewBonus() { return newBonus; }
    public void setNewBonus(BigDecimal newBonus) { this.newBonus = newBonus; }

    public Integer getChangedBy() { return changedBy; }
    public void setChangedBy(Integer changedBy) { this.changedBy = changedBy; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
