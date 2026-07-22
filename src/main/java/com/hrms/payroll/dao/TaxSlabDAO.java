package com.hrms.payroll.dao;

import com.hrms.payroll.model.TaxSlab;

import java.util.List;

public interface TaxSlabDAO extends CrudDAO<TaxSlab, Integer> {
    List<TaxSlab> findAllActiveOrdered();

    /**
     * Atomically replaces the entire tax-slab table with the given list.
     * Used by the Tax Calculator settings screen so re-saving edited slabs
     * never collides with the unique (min_amount, max_amount) constraint
     * left behind by the previous version of the table.
     */
    void replaceAll(List<TaxSlab> slabs);
}
