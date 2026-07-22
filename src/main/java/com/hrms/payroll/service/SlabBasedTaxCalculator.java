package com.hrms.payroll.service;

import com.hrms.payroll.dao.TaxSlabDAO;
import com.hrms.payroll.model.TaxSlab;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implements progressive ("slab-based") income tax the way most real payroll
 * systems compute it: each portion of income is taxed at the rate for the
 * bracket it falls into, not the marginal bracket's rate applied to the whole
 * amount. Slabs are loaded from the database so HR can edit them at runtime.
 */
public class SlabBasedTaxCalculator implements TaxCalculationStrategy {

    private final TaxSlabDAO taxSlabDAO;

    public SlabBasedTaxCalculator(TaxSlabDAO taxSlabDAO) {
        this.taxSlabDAO = taxSlabDAO;
    }

    @Override
    public BigDecimal calculateTax(BigDecimal taxableAmount) {
        List<TaxSlab> slabs = taxSlabDAO.findAllActiveOrdered();
        if (slabs.isEmpty()) {
            throw new ValidationException("No active tax slabs configured. Please set up tax slabs first.");
        }

        BigDecimal totalTax = BigDecimal.ZERO;

        // For each slab, tax only the portion of taxableAmount that falls within [min, max].
        for (TaxSlab slab : slabs) {
            BigDecimal slabFloor = slab.getMinAmount();
            BigDecimal slabCeiling = slab.getMaxAmount(); // null = unbounded ("and above")

            if (taxableAmount.compareTo(slabFloor) < 0) {
                continue; // income doesn't reach this bracket at all
            }

            // The upper edge of income that falls inside this bracket.
            BigDecimal upperEdge = (slabCeiling == null) ? taxableAmount : taxableAmount.min(slabCeiling);

            BigDecimal amountInThisSlab = upperEdge.subtract(slabFloor);
            if (amountInThisSlab.compareTo(BigDecimal.ZERO) < 0) {
                amountInThisSlab = BigDecimal.ZERO;
            }

            BigDecimal taxForSlab = amountInThisSlab
                    .multiply(slab.getTaxPercent())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            totalTax = totalTax.add(taxForSlab);
        }

        return totalTax.setScale(2, RoundingMode.HALF_UP);
    }
}
