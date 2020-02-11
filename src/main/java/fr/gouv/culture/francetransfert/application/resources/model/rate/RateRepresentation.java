package fr.gouv.culture.francetransfert.application.resources.model.rate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RateRepresentation {
    @NotBlank
    private String mailAdress;

    @Min(0)
    @Max(3)
    private int satisfaction;

    private String message;
}
