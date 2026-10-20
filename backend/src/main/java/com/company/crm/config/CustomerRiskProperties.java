package com.company.crm.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.company.crm.exception.CustomerRiskException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static com.company.crm.exception.CustomerRiskException.Code.NOT_READY;

@Data
@Component
@ConfigurationProperties(prefix = "crm.ai.risk.rules")
public class CustomerRiskProperties {
    private String version = "V2.0-1";
    private String aMediumDays = "14";
    private String aHighDays = "30";
    private String otherMediumDays = "30";
    private String otherHighDays = "60";
    private String neverMediumDays = "7";
    private String neverHighDays = "14";
    private String maxCandidates = "5000";
    public record RuleSnapshot(String version, int aMediumDays, int aHighDays,
            int otherMediumDays, int otherHighDays, int neverMediumDays, int neverHighDays, int maxCandidates) {}
    /** Validate at the V2 boundary; invalid V2 rules must not abort the V1 application. */
    public RuleSnapshot snapshot() {
        try { return validatedSnapshot(); }
        catch (IllegalArgumentException e) { throw new CustomerRiskException(NOT_READY); }
    }
    private RuleSnapshot validatedSnapshot() {
        int aMediumDays = Integer.parseInt(this.aMediumDays);
        int aHighDays = Integer.parseInt(this.aHighDays);
        int otherMediumDays = Integer.parseInt(this.otherMediumDays);
        int otherHighDays = Integer.parseInt(this.otherHighDays);
        int neverMediumDays = Integer.parseInt(this.neverMediumDays);
        int neverHighDays = Integer.parseInt(this.neverHighDays);
        int maxCandidates = Integer.parseInt(this.maxCandidates);
        if (version == null || version.isBlank() || !valid(aMediumDays,aHighDays)
                || !valid(otherMediumDays,otherHighDays) || !valid(neverMediumDays,neverHighDays)
                || maxCandidates < 1 || maxCandidates > 100000) throw new CustomerRiskException(NOT_READY);
        String canonical = "duration24h-v1;sort=severity,grade,excess-desc,id-asc;states=normal,observing,data_review|" + version + "|" + aMediumDays + "|" + aHighDays + "|"
                + otherMediumDays + "|" + otherHighDays + "|" + neverMediumDays + "|" + neverHighDays;
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
            return new RuleSnapshot(version + ":" + digest, aMediumDays,aHighDays,otherMediumDays,otherHighDays,neverMediumDays,neverHighDays,maxCandidates);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private boolean valid(int medium, int high) { return medium > 0 && high > medium && high <= 36500; }
}
