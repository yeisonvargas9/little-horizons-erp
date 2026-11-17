package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.SchoolSettings;

import java.nio.file.Path;

/** Turns an invoice into a file. The controller depends on this interface, not on a specific PDF library. */
public interface InvoiceRenderer {

    /** Creates the invoice file and returns its location. */
    Path render(Invoice invoice, SchoolSettings school, Guardian customer);
}
