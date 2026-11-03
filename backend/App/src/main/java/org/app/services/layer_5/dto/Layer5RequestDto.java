package org.app.services.layer_5.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Layer5RequestDto {
    private Boolean confirmGeneration;
    private Boolean approveFinalText;
    private String previewId;
    @NotBlank @Size(max = 150)
    private String fullName;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{5,20}")
    private String idCard;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Pattern(regexp = "[+0-9() -]{7,25}")
    private String phone;
    @NotBlank @Size(max = 200)
    private String providerCommercialName;
    @Pattern(regexp = "(?:[0-9]{13})?")
    private String providerRuc = "";
    @Size(max = 300)
    private String providerAddress = "";

    public String getContactInfo() {
        return email + " / " + phone;
    }
}
