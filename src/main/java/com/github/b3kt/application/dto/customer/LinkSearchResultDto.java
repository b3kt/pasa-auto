package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

/**
 * A candidate pelanggan record for the customer to confirm before linking. Deliberately partial/
 * masked - the customer hasn't proven ownership yet, just matched a search.
 */
@Getter
@Setter
public class LinkSearchResultDto {
    private Long pelangganId;
    private String maskedName;
    private String maskedPhone;
    private String maskedEmail;
    private String nopol;
}
