package com.hrms.payroll.model;

import java.math.BigDecimal;

/**
 * Represents one bracket of a progressive/slab-based income tax structure.
 * maxAmount == null means "and above" (no upper bound).
 */
public class TaxSlab {
    private int slabId;
    private String slabName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount; // nullable
    private BigDecimal taxPercent;
    private boolean active = true;

    public TaxSlab() {}

    public TaxSlab(String slabName, BigDecimal minAmount, BigDecimal maxAmount, BigDecimal taxPercent) {
        this.slabName = slabName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.taxPercent = taxPercent;
    }

    public boolean contains(BigDecimal amount) {
        boolean aboveMin = amount.compareTo(minAmount) >= 0;
        boolean belowMax = (maxAmount == null) || amount.compareTo(maxAmount) <= 0;
        return aboveMin && belowMax;
    }

    public int getSlabId() { return slabId; }
    public void setSlabId(int slabId) { this.slabId = slabId; }

    public String getSlabName() { return slabName; }
    public void setSlabName(String slabName) { this.slabName = slabName; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        String upper = (maxAmount == null) ? "and above" : "- " + maxAmount;
        return slabName + " (" + minAmount + " " + upper + ") : " + taxPercent + "%";
    }
}
