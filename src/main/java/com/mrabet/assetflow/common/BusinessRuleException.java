package com.mrabet.assetflow.common;

/** Violation d'une règle métier (transition interdite, stock insuffisant, cycle de nomenclature...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
