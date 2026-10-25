package com.company.crm.exception;

public class RiskAdviceUnavailableException extends RuntimeException {
    public enum Code { RISK_ADVICE_UNAVAILABLE, RISK_ADVICE_INPUT_LIMIT, RISK_ADVICE_BUSY, RISK_ADVICE_TIMEOUT, RISK_ADVICE_NOT_READY }
    private final Code code;
    public RiskAdviceUnavailableException(Code code) { super(code.name()); this.code=code; }
    public Code getCode() { return code; }
}
