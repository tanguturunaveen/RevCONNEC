package org.revature.revconnect.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequest {

    @Size(max = 500, message = "Share comment must not exceed 500 characters")
    private String comment;
}
